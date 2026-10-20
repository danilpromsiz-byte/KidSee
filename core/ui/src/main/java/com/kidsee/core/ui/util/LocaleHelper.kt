package com.kidsee.core.ui.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "kidsee_locale_prefs"
    private const val KEY_LANG = "app_language"

    fun getPersistedLanguage(context: Context, defaultLang: String = "ru"): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, null) ?: defaultLang
    }

    fun persistLanguage(context: Context, langCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, langCode).apply()
    }

    fun applyLanguage(context: Context, langCode: String, recreateActivity: Boolean = true): Context {
        persistLanguage(context, langCode)

        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val appLocale = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        val localizedContext = context.createConfigurationContext(config)

        if (recreateActivity && context is Activity) {
            context.recreate()
        }

        return localizedContext
    }

    fun wrapContext(context: Context): Context {
        val lang = getPersistedLanguage(context)
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }
}
