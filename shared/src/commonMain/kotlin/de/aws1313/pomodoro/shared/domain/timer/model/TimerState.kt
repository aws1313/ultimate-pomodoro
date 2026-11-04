package de.aws1313.pomodoro.shared.domain.timer.model

sealed interface TimerState {
    /** No session is active. App is at rest. */
    data object Idle : TimerState

    /** Timer is actively counting. */
    data class Running(
        val phase: TimerPhase,
        val remainingMs: Long,
        val totalMs: Long,
        val elapsedOvertimeMs: Long = 0L
    ) : TimerState

    /** Timer is paused mid-phase. */
    data class Paused(
        val phase: TimerPhase,
        val remainingMs: Long,
        val totalMs: Long,
        val elapsedOvertimeMs: Long = 0L
    ) : TimerState

    /** A phase has just completed; waiting for user action or auto-start. */
    data class Finished(
        val phase: TimerPhase,
        val completedSessionCount: Int
    ) : TimerState
}
