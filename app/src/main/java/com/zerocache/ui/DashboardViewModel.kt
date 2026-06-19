package com.zerocache.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerocache.data.AppCacheInfo
import com.zerocache.data.AppCacheScanner
import com.zerocache.data.ClearEngine
import com.zerocache.data.ClearResult
import com.zerocache.data.ClearStrategy
import com.zerocache.util.RootChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
    val isLoading: Boolean = false,
    val apps: List<AppCacheInfo> = emptyList(),
    val totalCacheBytes: Long = 0L,
    val clearedCount: Int = 0,
    val freedBytes: Long = 0L,
    val isClearing: Boolean = false,
    val isRooted: Boolean = false,
    val hasUsageStats: Boolean = false,
    val strategy: ClearStrategy = ClearStrategy.DirectApi,
    val message: String? = null,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val showConfirm: Boolean = false
) {
    val visibleApps: List<AppCacheInfo>
        get() = apps
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = AppCacheScanner(application)
    private val engine = ClearEngine(application, scanner)

    private val _state = MutableStateFlow(
        DashboardUiState(
            isRooted = RootChecker.isRooted(),
            hasUsageStats = scanner.hasUsageStatsPermission()
        )
    )
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        val isRooted = RootChecker.isRooted()
        val hasUsage = scanner.hasUsageStatsPermission()
        _state.update {
            it.copy(
                isRooted = isRooted,
                hasUsageStats = hasUsage,
                strategy = if (isRooted) ClearStrategy.Root else ClearStrategy.DirectApi
            )
        }
        if (hasUsage) {
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val hasUsageStats = scanner.hasUsageStatsPermission()
            _state.update { it.copy(isLoading = true, message = null, hasUsageStats = hasUsageStats) }
            val apps = if (hasUsageStats) {
                withContext(Dispatchers.IO) { scanner.scan() }
            } else {
                emptyList()
            }
            val total = apps.sumOf { it.cacheSizeBytes }
            _state.update {
                it.copy(
                    isLoading = false,
                    apps = apps,
                    totalCacheBytes = total
                )
            }
        }
    }

    fun checkPermissions() {
        val hasUsage = scanner.hasUsageStatsPermission()
        val current = _state.value

        if (hasUsage != current.hasUsageStats) {
            _state.update {
                it.copy(
                    hasUsageStats = hasUsage
                )
            }
            if (hasUsage && (!current.hasUsageStats || current.apps.isEmpty())) {
                refresh()
            } else if (!hasUsage && current.apps.isNotEmpty()) {
                _state.update {
                    it.copy(
                        apps = emptyList(),
                        totalCacheBytes = 0L
                    )
                }
            }
        }
    }

    fun toggleStrategy() {
        _state.update {
            it.copy(
                strategy = when (it.strategy) {
                    ClearStrategy.DirectApi -> if (it.isRooted) ClearStrategy.Root else ClearStrategy.DirectApi
                    ClearStrategy.Root -> ClearStrategy.DirectApi
                }
            )
        }
    }

    fun requestClearAll() {
        val current = _state.value
        if (current.apps.isEmpty()) return
        _state.update { it.copy(showConfirm = true) }
    }

    fun dismissConfirm() {
        _state.update { it.copy(showConfirm = false) }
    }

    fun confirmClearAll() {
        val current = _state.value
        _state.update { it.copy(showConfirm = false, isClearing = true, clearedCount = 0, freedBytes = 0L) }
        viewModelScope.launch {
            val items = current.apps.filter { it.cacheSizeBytes > 0 || current.strategy == ClearStrategy.Root }
            if (items.isEmpty()) {
                _state.update { it.copy(isClearing = false, message = "nothing_to_clear") }
                return@launch
            }
            engine.clearAll(items, current.strategy) { current_, total, _, result ->
                _state.update {
                    when (result) {
                        is ClearResult.Success -> it.copy(
                            clearedCount = it.clearedCount + 1,
                            freedBytes = it.freedBytes + result.freedBytes,
                            progressCurrent = current_,
                            progressTotal = total
                        )
                        is ClearResult.Failure -> it.copy(
                            progressCurrent = current_,
                            progressTotal = total
                        )
                        ClearResult.Skipped -> it.copy(
                            progressCurrent = current_,
                            progressTotal = total
                        )
                    }
                }
            }
            // Re-scan to get fresh sizes after clearing
            val newApps = withContext(Dispatchers.IO) { scanner.scan() }
            val newTotal = newApps.sumOf { it.cacheSizeBytes }
            _state.update {
                it.copy(
                    isClearing = false,
                    apps = newApps,
                    totalCacheBytes = newTotal,
                    message = "done"
                )
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun clearOne(item: AppCacheInfo) {
        viewModelScope.launch {
            _state.update { it.copy(isClearing = true) }
            val result = when (_state.value.strategy) {
                ClearStrategy.Root -> engine.clearCacheRoot(item)
                ClearStrategy.DirectApi -> engine.clearCacheDirect(item)
            }
            val newApps = withContext(Dispatchers.IO) { scanner.scan() }
            val newTotal = newApps.sumOf { it.cacheSizeBytes }
            val freedExtra = if (result is ClearResult.Success) result.freedBytes else 0L
            val clearedExtra = if (result is ClearResult.Success) 1 else 0
            _state.update {
                it.copy(
                    isClearing = false,
                    apps = newApps,
                    totalCacheBytes = newTotal,
                    clearedCount = it.clearedCount + clearedExtra,
                    freedBytes = it.freedBytes + freedExtra,
                    message = if (result is ClearResult.Failure) "clear_failed" else null
                )
            }
        }
    }
}
