package de.aws1313.pomodoro.mobile.presentation.timer

import de.aws1313.pomodoro.shared.domain.timer.model.TimerPhase

sealed interface TimerIntent {
    data class Start(val phase: TimerPhase = TimerPhase.FOCUS) : TimerIntent
    data object Pause : TimerIntent
    data object Resume : TimerIntent
    data object Reset : TimerIntent
    data object Skip : TimerIntent
}
