package de.aws1313.pomodoro.mobile.presentation.timer

sealed interface TimerEffect {
    data class ShowSnackbar(val message: String) : TimerEffect
    data class PhaseFinished(val message: String) : TimerEffect
}
