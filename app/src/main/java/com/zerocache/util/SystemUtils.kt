package com.zerocache.util

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log

/**
 * Detect root availability by attempting `su -c id`.
 * Returns true if a root shell can be spawned and returns uid=0.
 */
object RootChecker {
    private const val TAG = "RootChecker"

    fun isRooted(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = process.inputStream.bufferedReader().readText().trim()
            val exit = process.waitFor()
            val ok = exit == 0 && output.contains("uid=0")
            if (!ok) Log.d(TAG, "isRooted=false exit=$exit out=$output")
            ok
        } catch (t: Throwable) {
            Log.d(TAG, "isRooted=false ${t.message}")
            false
        }
    }
}

/**
 * Helpers for opening system settings pages the user needs to grant our permissions.
 */
object SettingsOpener {

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.w("SettingsOpener", "openAccessibilitySettings failed", t)
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.w("SettingsOpener", "openUsageAccessSettings failed", t)
        }
    }

    fun openAppInfoSettings(context: Context, packageName: String) {
        try {
            val intent = android.content.Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", packageName, null)
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.w("SettingsOpener", "openAppInfoSettings failed", t)
        }
    }
}
