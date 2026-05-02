package com.example.tossday.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.tossday.ui.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

enum class NoteBackground { NONE, LINES, GRID }
enum class ChipsLayout { LINEAR, WRAP }

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

    private val _pinnedDate = MutableStateFlow(loadPinnedDate())
    val pinnedDate: StateFlow<LocalDate?> = _pinnedDate.asStateFlow()

    private val _noteBackground = MutableStateFlow(
        NoteBackground.entries.getOrElse(prefs.getInt(KEY_NOTE_BG, 0)) { NoteBackground.NONE }
    )
    val noteBackground: StateFlow<NoteBackground> = _noteBackground.asStateFlow()

    // Дефолт LINEAR — щоб поведінка для старих користувачів не змінилась.
    private val _chipsLayout = MutableStateFlow(
        ChipsLayout.entries.getOrElse(prefs.getInt(KEY_CHIPS_LAYOUT, 0)) { ChipsLayout.LINEAR }
    )
    val chipsLayout: StateFlow<ChipsLayout> = _chipsLayout.asStateFlow()

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _isHapticEnabled.value = enabled
    }

    fun setAppTheme(theme: AppTheme) {
        prefs.edit().putInt(KEY_THEME, theme.ordinal).apply()
        _appTheme.value = theme
    }

    fun setPinnedDate(date: LocalDate?) {
        if (date == null) {
            prefs.edit().remove(KEY_PINNED_DATE).apply()
        } else {
            prefs.edit().putLong(KEY_PINNED_DATE, date.toEpochDay()).apply()
        }
        _pinnedDate.value = date
    }

    fun setNoteBackground(bg: NoteBackground) {
        prefs.edit().putInt(KEY_NOTE_BG, bg.ordinal).apply()
        _noteBackground.value = bg
    }

    fun setChipsLayout(layout: ChipsLayout) {
        prefs.edit().putInt(KEY_CHIPS_LAYOUT, layout.ordinal).apply()
        _chipsLayout.value = layout
    }

    private fun loadPinnedDate(): LocalDate? {
        if (!prefs.contains(KEY_PINNED_DATE)) return null
        val epochDay = prefs.getLong(KEY_PINNED_DATE, Long.MIN_VALUE)
        return if (epochDay == Long.MIN_VALUE) null else LocalDate.ofEpochDay(epochDay)
    }

    companion object {
        private const val KEY_HAPTIC = "key_haptic"
        private const val KEY_THEME  = "key_theme"
        private const val KEY_PINNED_DATE = "key_pinned_date"
        private const val KEY_NOTE_BG = "key_note_bg"
        private const val KEY_CHIPS_LAYOUT = "key_chips_layout"
    }
}
