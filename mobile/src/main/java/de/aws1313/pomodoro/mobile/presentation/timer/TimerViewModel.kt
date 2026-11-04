package de.aws1313.pomodoro.mobile.presentation.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.aws1313.pomodoro.shared.data.timer.DefaultTimerRepository
import de.aws1313.pomodoro.shared.domain.timer.model.TimerPhase
import de.aws1313.pomodoro.shared.domain.timer.model.TimerState
import de.aws1313.pomodoro.shared.domain.timer.repository.TimerRepository
import de.aws1313.pomodoro.shared.domain.timer.usecase.ObserveCompletedSessionsUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.ObserveTimerStateUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.PauseTimerUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.ResetTimerUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.ResumeTimerUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.SkipPhaseUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.StartTimerUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

class TimerViewModel(
    private val startTimerUseCase: StartTimerUseCase,
    private val pauseTimerUseCase: PauseTimerUseCase,
    private val resumeTimerUseCase: ResumeTimerUseCase,
    private val resetTimerUseCase: ResetTimerUseCase,
    private val skipPhaseUseCase: SkipPhaseUseCase,
    observeTimerStateUseCase: ObserveTimerStateUseCase,
    observeCompletedSessionsUseCase: ObserveCompletedSessionsUseCase
) : ViewModel() {

    private val _effectChannel = Channel<TimerEffect>(Channel.BUFFERED)
    val effect: Flow<TimerEffect> = _effectChannel.receiveAsFlow()

    val uiState: StateFlow<TimerUiState> = combine(
        observeTimerStateUseCase(),
        observeCompletedSessionsUseCase()
    ) { timerState, completedSessions ->
        mapToUiState(timerState, completedSessions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TimerUiState()
    )

    fun onIntent(intent: TimerIntent) {
        when (intent) {
            is TimerIntent.Start -> startTimerUseCase(intent.phase)
            TimerIntent.Pause -> pauseTimerUseCase()
            TimerIntent.Resume -> resumeTimerUseCase()
            TimerIntent.Reset -> resetTimerUseCase()
            TimerIntent.Skip -> skipPhaseUseCase()
        }
    }

    private fun mapToUiState(timerState: TimerState, completedSessions: Int): TimerUiState {
        return when (timerState) {
            TimerState.Idle -> TimerUiState(
                formattedTime = "25:00",
                phase = TimerPhase.FOCUS,
                phaseTitle = "Focus Session",
                progress = 1.0f,
                isRunning = false,
                isPaused = false,
                isIdle = true,
                isFinished = false,
                completedSessions = completedSessions
            )
            is TimerState.Running -> {
                val progress = if (timerState.totalMs > 0) {
                    timerState.remainingMs.toFloat() / timerState.totalMs.toFloat()
                } else {
                    1.0f
                }
                TimerUiState(
                    formattedTime = formatMilliseconds(timerState.remainingMs),
                    phase = timerState.phase,
                    phaseTitle = phaseTitle(timerState.phase),
                    progress = progress.coerceIn(0f, 1f),
                    isRunning = true,
                    isPaused = false,
                    isIdle = false,
                    isFinished = false,
                    completedSessions = completedSessions
                )
            }
            is TimerState.Paused -> {
                val progress = if (timerState.totalMs > 0) {
                    timerState.remainingMs.toFloat() / timerState.totalMs.toFloat()
                } else {
                    1.0f
                }
                TimerUiState(
                    formattedTime = formatMilliseconds(timerState.remainingMs),
                    phase = timerState.phase,
                    phaseTitle = "${phaseTitle(timerState.phase)} (Paused)",
                    progress = progress.coerceIn(0f, 1f),
                    isRunning = false,
                    isPaused = true,
                    isIdle = false,
                    isFinished = false,
                    completedSessions = completedSessions
                )
            }
            is TimerState.Finished -> {
                TimerUiState(
                    formattedTime = "00:00",
                    phase = timerState.phase,
                    phaseTitle = "${phaseTitle(timerState.phase)} Completed!",
                    progress = 0.0f,
                    isRunning = false,
                    isPaused = false,
                    isIdle = false,
                    isFinished = true,
                    completedSessions = timerState.completedSessionCount
                )
            }
        }
    }

    private fun phaseTitle(phase: TimerPhase): String {
        return when (phase) {
            TimerPhase.FOCUS -> "Focus"
            TimerPhase.SHORT_BREAK -> "Short Break"
            TimerPhase.LONG_BREAK -> "Long Break"
            TimerPhase.BREAK -> "Break"
            TimerPhase.OVERTIME -> "Overtime"
        }
    }

    private fun formatMilliseconds(ms: Long): String {
        val totalSeconds = (ms + 999) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val minStr = if (minutes < 10) "0$minutes" else minutes.toString()
        val secStr = if (seconds < 10) "0$seconds" else seconds.toString()
        return "$minStr:$secStr"
    }

    companion object {
        fun provideFactory(
            repository: TimerRepository = DefaultTimerRepository()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TimerViewModel(
                    startTimerUseCase = StartTimerUseCase(repository),
                    pauseTimerUseCase = PauseTimerUseCase(repository),
                    resumeTimerUseCase = ResumeTimerUseCase(repository),
                    resetTimerUseCase = ResetTimerUseCase(repository),
                    skipPhaseUseCase = SkipPhaseUseCase(repository),
                    observeTimerStateUseCase = ObserveTimerStateUseCase(repository),
                    observeCompletedSessionsUseCase = ObserveCompletedSessionsUseCase(repository)
                ) as T
            }
        }
    }
}
