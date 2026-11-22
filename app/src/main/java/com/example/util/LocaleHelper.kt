package com.example.util

import android.content.Context
import android.content.res.Configuration
import com.example.data.AppLanguage
import java.util.Locale

object LocaleHelper {
    fun localizedContext(base: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return base
        val locale = Locale.forLanguageTag(language.code)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
