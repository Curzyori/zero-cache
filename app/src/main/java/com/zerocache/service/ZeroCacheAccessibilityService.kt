package com.zerocache.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * No-Root automation. Flow:
 *  1. Caller (ViewModel/ClearEngine) calls openAppInfoAndClearCache(pkg).
 *  2. Service launches ACTION_APPLICATION_DETAILS_SETTINGS for that package.
 *  3. Service watches the AccessibilityEvent stream and, once the Settings page
 *     is on screen, finds the "Clear cache" button by id/text and taps it.
 *  4. Returns success once the click is dispatched.
 *
 * The class also exposes a static [instance] for the engine to talk to without
 * needing a binder round-trip.
 */
class ZeroCacheAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ZCAccessibility"
        @Volatile
        var instance: ZeroCacheAccessibilityService? = null
            private set

        // Android Settings uses these well-known resource names for the buttons.
        // AOSP / Pixel: "com.android.settings:id/button2" is "Clear cache" on App Info → Storage.
        // Samsung OneUI:  "com.android.settings:id/button2" too, but text fallback used just in case.
        private val CLEAR_CACHE_IDS = listOf(
            "com.android.settings:id/button2",
            "com.android.settings:id/clear_cache_button"
        )
        private val CLEAR_CACHE_TEXTS = listOf(
            "clear cache", "hapus cache", "hapus data", "clear data"
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pendingPackage = AtomicReference<String?>(null)
    private val pendingResult = AtomicReference<(Boolean) -> Unit>(null)

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            // Make sure we can retrieve window content
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        Log.d(TAG, "onServiceConnected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        instance = null
    }

    /**
     * Public entry point used by ClearEngine. Suspends (off the main thread) until the
     * service either taps the clear-cache button or gives up after a timeout.
     */
    suspend fun openAppInfoAndClearCache(packageName: String): Boolean {
        val current = pendingPackage.getAndSet(packageName)
        if (current != null) {
            Log.w(TAG, "another package already in flight: $current, aborting")
            return false
        }
        return kotlinx.coroutines.withTimeoutOrNull(15_000L) {
            kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { cont ->
                pendingResult.set { ok -> if (cont.isActive) cont.resumeWith(Result.success(ok)) }
                launchAppInfo(packageName)
            }
        }.also {
            pendingPackage.set(null)
            pendingResult.set(null)
        } ?: run {
            Log.w(TAG, "openAppInfoAndClearCache timed out for $packageName")
            false
        }
    }

    private fun launchAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "launchAppInfo failed", t)
            pendingResult.get()?.invoke(false)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (pendingPackage.get() == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val root = rootInActiveWindow ?: return
        try {
            if (tryFindAndClickClearCache(root)) {
                Log.d(TAG, "Clear cache tapped for ${pendingPackage.get()}")
                pendingResult.get()?.invoke(true)
            }
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "onInterrupt")
    }

    private fun tryFindAndClickClearCache(root: AccessibilityNodeInfo): Boolean {
        // 1. Try by view id (fastest)
        for (id in CLEAR_CACHE_IDS) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            for (n in nodes) {
                if (n.isClickable && n.isEnabled) {
                    // Make sure the visible text matches what we expect
                    val text = (n.text ?: n.contentDescription)?.toString()?.lowercase().orEmpty()
                    val looksRight = CLEAR_CACHE_TEXTS.any { it in text } ||
                        text.contains("cache") || text.contains("data")
                    if (looksRight) {
                        return performClick(n)
                    }
                }
                n.recycle()
            }
        }
        // 2. Try by text (fallback for OEM skins)
        return clickByText(root, CLEAR_CACHE_TEXTS)
    }

    private fun clickByText(root: AccessibilityNodeInfo, needles: List<String>): Boolean {
        val all = ArrayList<AccessibilityNodeInfo>(64)
        collectClickableNodes(root, all)
        for (n in all) {
            val text = (n.text ?: n.contentDescription)?.toString()?.lowercase().orEmpty()
            if (needles.any { it in text }) {
                val ok = performClick(n)
                recycleAll(all)
                return ok
            }
            n.recycle()
        }
        recycleAll(all)
        return false
    }

    private fun collectClickableNodes(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (node.isClickable) out.add(AccessibilityNodeInfo.obtain(node))
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectClickableNodes(child, out)
            child.recycle()
        }
    }

    private fun recycleAll(nodes: List<AccessibilityNodeInfo>) {
        for (n in nodes) {
            try { n.recycle() } catch (_: Throwable) {}
        }
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val args = Bundle()
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK, args)
            } else {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "performClick failed", t)
            false
        }
    }
}
