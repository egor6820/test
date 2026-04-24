package com.example.tossday.ui.settings

import androidx.lifecycle.ViewModel
import com.example.tossday.data.repository.SettingsRepository
import com.example.tossday.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsHolderViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val appTheme: StateFlow<AppTheme> = settingsRepository.appTheme

    fun setTheme(theme: AppTheme) {
        settingsRepository.setAppTheme(theme)
    }
}
