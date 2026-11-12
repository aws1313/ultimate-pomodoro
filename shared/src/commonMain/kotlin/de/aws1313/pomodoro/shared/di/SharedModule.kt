package de.aws1313.pomodoro.shared.di

import de.aws1313.pomodoro.shared.data.timer.DefaultTimerRepository
import de.aws1313.pomodoro.shared.domain.timer.repository.TimerRepository
import de.aws1313.pomodoro.shared.domain.timer.usecase.ObserveCompletedSessionsUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.ObserveTimerStateUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.PauseTimerUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.ResetTimerUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.ResumeTimerUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.SkipPhaseUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.StartTimerUseCase
import org.koin.dsl.module

val sharedModule = module {
    single<TimerRepository> { DefaultTimerRepository() }

    factory { StartTimerUseCase(get()) }
    factory { PauseTimerUseCase(get()) }
    factory { ResumeTimerUseCase(get()) }
    factory { ResetTimerUseCase(get()) }
    factory { SkipPhaseUseCase(get()) }
    factory { ObserveTimerStateUseCase(get()) }
    factory { ObserveCompletedSessionsUseCase(get()) }
}
