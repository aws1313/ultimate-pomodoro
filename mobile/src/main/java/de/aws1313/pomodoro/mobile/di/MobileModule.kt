package de.aws1313.pomodoro.mobile.di

import de.aws1313.pomodoro.mobile.presentation.timer.TimerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mobileModule = module {
    viewModel {
        TimerViewModel(
            startTimerUseCase = get(),
            pauseTimerUseCase = get(),
            resumeTimerUseCase = get(),
            resetTimerUseCase = get(),
            skipPhaseUseCase = get(),
            observeTimerStateUseCase = get(),
            observeCompletedSessionsUseCase = get()
        )
    }
}
