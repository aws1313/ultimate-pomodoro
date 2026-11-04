package de.aws1313.pomodoro.shared.domain.timer.usecase

import de.aws1313.pomodoro.shared.domain.timer.repository.TimerRepository

class SkipPhaseUseCase(
    private val timerRepository: TimerRepository
) {
    operator fun invoke() {
        timerRepository.skip()
    }
}
