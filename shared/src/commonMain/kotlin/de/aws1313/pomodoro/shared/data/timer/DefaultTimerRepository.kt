package de.aws1313.pomodoro.shared.data.timer

import de.aws1313.pomodoro.shared.domain.timer.model.TimerPhase
import de.aws1313.pomodoro.shared.domain.timer.model.TimerSettings
import de.aws1313.pomodoro.shared.domain.timer.model.TimerState
import de.aws1313.pomodoro.shared.domain.timer.repository.TimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class DefaultTimerRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val timeSource: TimeSource = TimeSource.Monotonic,
    initialSettings: TimerSettings = TimerSettings()
) : TimerRepository {

    private val _settings = MutableStateFlow(initialSettings)
    override val settings: StateFlow<TimerSettings> = _settings.asStateFlow()

    private val _completedSessions = MutableStateFlow(0)
    override val completedSessions: StateFlow<Int> = _completedSessions.asStateFlow()

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    override val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var tickerJob: Job? = null

    override fun start(phase: TimerPhase) {
        val durationMs = durationForPhase(phase)
        startCountdown(phase = phase, remainingMs = durationMs, totalMs = durationMs)
    }

    override fun pause() {
        val currentState = _timerState.value
        if (currentState is TimerState.Running) {
            tickerJob?.cancel()
            _timerState.value = TimerState.Paused(
                phase = currentState.phase,
                remainingMs = currentState.remainingMs,
                totalMs = currentState.totalMs,
                elapsedOvertimeMs = currentState.elapsedOvertimeMs
            )
        }
    }

    override fun resume() {
        val currentState = _timerState.value
        if (currentState is TimerState.Paused) {
            startCountdown(
                phase = currentState.phase,
                remainingMs = currentState.remainingMs,
                totalMs = currentState.totalMs,
                elapsedOvertimeMs = currentState.elapsedOvertimeMs
            )
        }
    }

    override fun reset() {
        tickerJob?.cancel()
        _timerState.value = TimerState.Idle
    }

    override fun skip() {
        tickerJob?.cancel()
        val nextPhase = determineNextPhase(_timerState.value)
        start(nextPhase)
    }

    override fun updateSettings(newSettings: TimerSettings) {
        _settings.value = newSettings
    }

    private fun startCountdown(
        phase: TimerPhase,
        remainingMs: Long,
        totalMs: Long,
        elapsedOvertimeMs: Long = 0L
    ) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            val mark: TimeMark = timeSource.markNow()
            val initialRemaining = remainingMs

            while (isActive) {
                val elapsed = mark.elapsedNow().inWholeMilliseconds
                val currentRemaining = maxOf(0L, initialRemaining - elapsed)

                if (currentRemaining <= 0L) {
                    handlePhaseFinished(phase)
                    break
                }

                _timerState.value = TimerState.Running(
                    phase = phase,
                    remainingMs = currentRemaining,
                    totalMs = totalMs,
                    elapsedOvertimeMs = elapsedOvertimeMs
                )
                // Use 100ms tick for responsive UI updates without unnecessary CPU load
                delay(100L)
            }
        }
    }

    private fun handlePhaseFinished(finishedPhase: TimerPhase) {
        if (finishedPhase == TimerPhase.FOCUS) {
            val newCompletedCount = _completedSessions.value + 1
            _completedSessions.value = newCompletedCount
            _timerState.value = TimerState.Finished(
                phase = finishedPhase,
                completedSessionCount = newCompletedCount
            )

            if (_settings.value.autoStartBreak) {
                val nextBreak = if (newCompletedCount % _settings.value.pomodorosUntilLongBreak == 0) {
                    TimerPhase.LONG_BREAK
                } else {
                    TimerPhase.SHORT_BREAK
                }
                start(nextBreak)
            }
        } else {
            _timerState.value = TimerState.Finished(
                phase = finishedPhase,
                completedSessionCount = _completedSessions.value
            )

            if (_settings.value.autoStartFocus) {
                start(TimerPhase.FOCUS)
            }
        }
    }

    private fun determineNextPhase(currentState: TimerState): TimerPhase {
        return when (currentState) {
            is TimerState.Running -> if (currentState.phase == TimerPhase.FOCUS) {
                nextBreakPhase(_completedSessions.value + 1)
            } else {
                TimerPhase.FOCUS
            }
            is TimerState.Paused -> if (currentState.phase == TimerPhase.FOCUS) {
                nextBreakPhase(_completedSessions.value + 1)
            } else {
                TimerPhase.FOCUS
            }
            is TimerState.Finished -> if (currentState.phase == TimerPhase.FOCUS) {
                nextBreakPhase(currentState.completedSessionCount)
            } else {
                TimerPhase.FOCUS
            }
            is TimerState.Idle -> TimerPhase.FOCUS
        }
    }

    private fun nextBreakPhase(sessionCount: Int): TimerPhase {
        return if (sessionCount > 0 && sessionCount % _settings.value.pomodorosUntilLongBreak == 0) {
            TimerPhase.LONG_BREAK
        } else {
            TimerPhase.SHORT_BREAK
        }
    }

    private fun durationForPhase(phase: TimerPhase): Long {
        val currentSettings = _settings.value
        val minutes = when (phase) {
            TimerPhase.FOCUS -> currentSettings.focusDurationMin
            TimerPhase.SHORT_BREAK -> currentSettings.shortBreakDurationMin
            TimerPhase.LONG_BREAK -> currentSettings.longBreakDurationMin
            TimerPhase.BREAK -> (currentSettings.focusDurationMin / currentSettings.flowmodoroBreakFactor).toInt()
            TimerPhase.OVERTIME -> 0
        }
        return minutes * 60 * 1000L
    }
}
