package de.aws1313.pomodoro.shared.domain.timer.repository

import de.aws1313.pomodoro.shared.domain.timer.model.TimerPhase
import de.aws1313.pomodoro.shared.domain.timer.model.TimerSettings
import de.aws1313.pomodoro.shared.domain.timer.model.TimerState
import kotlinx.coroutines.flow.StateFlow

interface TimerRepository {
    val timerState: StateFlow<TimerState>
    val completedSessions: StateFlow<Int>
    val settings: StateFlow<TimerSettings>

    fun start(phase: TimerPhase = TimerPhase.FOCUS)
    fun pause()
    fun resume()
    fun reset()
    fun skip()
    fun updateSettings(newSettings: TimerSettings)
}
