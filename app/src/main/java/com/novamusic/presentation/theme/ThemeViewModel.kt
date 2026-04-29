package com.novamusic.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 0 = Follow System
 * 1 = Light
 * 2 = Dark
 * 3 = Pure Black (AMOLED)
 */
enum class ThemeMode(val value: Int) {
    SYSTEM(0), LIGHT(1), DARK(2), PURE_BLACK(3);

    companion object {
        fun fromValue(v: Int) = entries.find { it.value == v } ?: SYSTEM
    }
}

data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val currentAccentColor: Long = 0xFF6750A4, // Fallback purple
    val isDark: Boolean = false
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _themeSettings = MutableStateFlow(ThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getThemeMode().collect { mode ->
                _themeSettings.update { it.copy(themeMode = ThemeMode.fromValue(mode)) }
            }
        }
        viewModelScope.launch {
            settingsRepository.getDynamicColorEnabled().collect { enabled ->
                _themeSettings.update { it.copy(dynamicColorEnabled = enabled) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode.value)
            _themeSettings.update { it.copy(themeMode = mode) }
        }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColorEnabled(enabled)
            _themeSettings.update { it.copy(dynamicColorEnabled = enabled) }
        }
    }

    fun setAccentColor(color: Long) {
        _themeSettings.update { it.copy(currentAccentColor = color) }
    }
}
