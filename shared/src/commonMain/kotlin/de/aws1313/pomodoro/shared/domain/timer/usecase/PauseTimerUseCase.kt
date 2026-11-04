package de.aws1313.pomodoro.shared.domain.timer.usecase

import de.aws1313.pomodoro.shared.domain.timer.repository.TimerRepository

class PauseTimerUseCase(
    private val timerRepository: TimerRepository
) {
    operator fun invoke() {
        timerRepository.pause()
    }
}
