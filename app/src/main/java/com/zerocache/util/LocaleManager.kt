package com.zerocache.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

/**
 * In-app language switcher. Stores selection in SharedPreferences and applies it
 * to the activity context via Configuration. This is independent of system locale.
 */
object LocaleManager {

    private const val PREFS = "zero_cache_locale"
    private const val KEY = "language"
    const val LANG_INDONESIAN = "id"
    const val LANG_ENGLISH = "en"

    fun availableLanguages(): List<String> = listOf(LANG_INDONESIAN, LANG_ENGLISH)

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY, LANG_INDONESIAN) ?: LANG_INDONESIAN
    }

    fun setLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY, language) }
    }

    fun toggle(context: Context): String {
        val current = getLanguage(context)
        val next = if (current == LANG_INDONESIAN) LANG_ENGLISH else LANG_INDONESIAN
        setLanguage(context, next)
        return next
    }

    fun wrap(context: Context): Context {
        val lang = getLanguage(context)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
