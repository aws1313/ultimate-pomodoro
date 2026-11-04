package de.aws1313.pomodoro.shared.domain.timer.model

data class TimerSettings(
    val focusDurationMin: Int = 25,
    val shortBreakDurationMin: Int = 5,
    val longBreakDurationMin: Int = 15,
    val pomodorosUntilLongBreak: Int = 4,
    val flowmodoroBreakFactor: Float = 5f,
    val autoStartBreak: Boolean = false,
    val autoStartFocus: Boolean = false,
    val timerMode: TimerMode = TimerMode.POMODORO
)
