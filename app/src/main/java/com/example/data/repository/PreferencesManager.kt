package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import com.example.i18n.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "diyar_prefs")

class PreferencesManager(private val context: Context) {
    private val langKey = stringPreferencesKey("app_language")
    private val themeKey = stringPreferencesKey("app_theme_mode")

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        when (prefs[langKey]) {
            AppLanguage.EN.code -> AppLanguage.EN
            AppLanguage.AR.code -> AppLanguage.AR
            else -> {
                if (Locale.getDefault().language.equals("en", ignoreCase = true)) {
                    AppLanguage.EN
                } else {
                    AppLanguage.AR
                }
            }
        }
    }

    // Default to Dark Mode per modern eye-comfort design
    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        val saved = prefs[themeKey]
        AppThemeMode.entries.firstOrNull { it.key == saved } ?: AppThemeMode.DARK
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[langKey] = language.code }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.key }
    }
}
