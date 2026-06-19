package com.zerocache.util

import java.util.Locale

/**
 * Convert raw bytes to a compact human-readable size string.
 * e.g. 1024 -> "1.0 KB", 5_242_880 -> "5.0 MB".
 */
object SizeFormatter {

    fun format(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        if (kb < 1.0) return "$bytes B"
        val mb = kb / 1024.0
        if (mb < 1.0) return String.format(Locale.US, "%.1f KB", kb)
        val gb = mb / 1024.0
        if (gb < 1.0) return String.format(Locale.US, "%.1f MB", mb)
        return String.format(Locale.US, "%.2f GB", gb)
    }

    fun formatForLanguage(bytes: Long, locale: Locale = Locale.getDefault()): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        if (kb < 1.0) return String.format(locale, "%d B", bytes)
        val mb = kb / 1024.0
        if (mb < 1.0) return String.format(locale, "%.1f KB", kb)
        val gb = mb / 1024.0
        if (gb < 1.0) return String.format(locale, "%.1f MB", mb)
        return String.format(locale, "%.2f GB", gb)
    }
}
