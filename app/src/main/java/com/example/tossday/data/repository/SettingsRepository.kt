package com.example.tossday.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.tossday.ui.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tossday_settings", Context.MODE_PRIVATE)

    private val _isHapticEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC, true))
    val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled.asStateFlow()

    private val _appTheme = MutableStateFlow(
        AppTheme.entries.getOrElse(prefs.getInt(KEY_THEME, 0)) { AppTheme.OCEAN }
    )
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _isHapticEnabled.value = enabled
    }

    fun setAppTheme(theme: AppTheme) {
        prefs.edit().putInt(KEY_THEME, theme.ordinal).apply()
        _appTheme.value = theme
    }

    companion object {
        private const val KEY_HAPTIC = "key_haptic"
        private const val KEY_THEME  = "key_theme"
    }
}
