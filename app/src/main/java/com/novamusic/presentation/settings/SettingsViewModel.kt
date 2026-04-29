package com.novamusic.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novamusic.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val defaultPlayMode: Int = 0,        // 0=Sequential, 1=RepeatAll, 2=Shuffle, 3=RepeatOne
    val pauseOnHeadphoneUnplug: Boolean = true,
    val defaultSleepMinutes: Int = 30,
    val notificationStyle: Int = 0       // 0=Compact, 1=Expanded
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settingsState = MutableStateFlow(SettingsUiState())
    val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    init {
        // Observe default play mode
        viewModelScope.launch {
            settingsRepository.getDefaultPlayMode()
                .collect { mode -> _settingsState.update { state -> state.copy(defaultPlayMode = mode) } }
        }
        // Default sleep timer
        viewModelScope.launch {
            settingsRepository.getDefaultSleepTimerMinutes()
                .collect { mins -> _settingsState.update { state -> state.copy(defaultSleepMinutes = mins) } }
        }
        // Notification style
        viewModelScope.launch {
            settingsRepository.getNotificationStyle()
                .collect { style -> _settingsState.update { state -> state.copy(notificationStyle = style) } }
        }
    }

    fun setDefaultPlayMode(mode: Int) {
        viewModelScope.launch { settingsRepository.setDefaultPlayMode(mode) }
    }

    fun setPauseOnHeadphoneUnplug(enabled: Boolean) {
        _settingsState.update { it.copy(pauseOnHeadphoneUnplug = enabled) }
    }

    fun setDefaultSleepTimer(minutes: Int) {
        viewModelScope.launch { settingsRepository.setDefaultSleepTimerMinutes(minutes) }
    }

    fun setNotificationStyle(style: Int) {
        viewModelScope.launch { settingsRepository.setNotificationStyle(style) }
    }
}
