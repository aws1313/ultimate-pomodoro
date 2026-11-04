package de.aws1313.pomodoro.mobile.presentation.timer

import de.aws1313.pomodoro.shared.domain.timer.model.TimerPhase

data class TimerUiState(
    val formattedTime: String = "25:00",
    val phase: TimerPhase = TimerPhase.FOCUS,
    val phaseTitle: String = "Focus Session",
    val progress: Float = 1.0f,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isIdle: Boolean = true,
    val isFinished: Boolean = false,
    val completedSessions: Int = 0
)
