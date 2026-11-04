package de.aws1313.pomodoro.shared.domain.timer.usecase

import de.aws1313.pomodoro.shared.domain.timer.repository.TimerRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveCompletedSessionsUseCase(
    private val timerRepository: TimerRepository
) {
    operator fun invoke(): StateFlow<Int> = timerRepository.completedSessions
}
