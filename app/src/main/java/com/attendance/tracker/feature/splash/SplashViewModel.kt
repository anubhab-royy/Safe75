package com.attendance.tracker.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.domain.repository.SemesterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Destinations resolved during startup checks on the splash screen.
 */
sealed interface SplashDestination {
    data object Loading : SplashDestination
    data object MainGraph : SplashDestination
    data object Welcome : SplashDestination
}

/**
 * ViewModel for [SplashScreen] that evaluates active semester status to determine startup navigation.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val semesterRepository: SemesterRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        checkStartupDestination()
    }

    fun checkStartupDestination() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val activeSemester = runCatching {
                semesterRepository.observeActiveVersion().firstOrNull()
            }.getOrNull()

            val elapsedTime = System.currentTimeMillis() - startTime
            val minSplashMs = 1000L
            if (elapsedTime < minSplashMs) {
                delay(minSplashMs - elapsedTime)
            }

            if (activeSemester != null) {
                _destination.value = SplashDestination.MainGraph
            } else {
                _destination.value = SplashDestination.Welcome
            }
        }
    }
}
