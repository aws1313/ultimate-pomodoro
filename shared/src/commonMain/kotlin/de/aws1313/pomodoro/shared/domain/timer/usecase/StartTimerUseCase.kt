package de.aws1313.pomodoro.shared.domain.timer.usecase

import de.aws1313.pomodoro.shared.domain.timer.model.TimerPhase
import de.aws1313.pomodoro.shared.domain.timer.repository.TimerRepository

class StartTimerUseCase(
    private val timerRepository: TimerRepository
) {
    operator fun invoke(phase: TimerPhase = TimerPhase.FOCUS) {
        timerRepository.start(phase)
    }
}
