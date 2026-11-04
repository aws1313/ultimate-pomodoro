package de.aws1313.pomodoro.shared.domain.timer.model

enum class TimerMode {
    /**
     * Classic Pomodoro: fixed focus intervals followed by short breaks,
     * with a long break after every N focus sessions.
     */
    POMODORO,

    /**
     * Flowmodoro: the user works for as long as they want (no fixed duration).
     * Break duration = focus duration / flowmodoroBreakFactor.
     */
    FLOWMODORO,

    /**
     * Hybrid (Pomodoro -> Flowmodoro): Starts as Pomodoro; upon finish user
     * can take a break or transition into overtime (Flowmodoro style).
     */
    HYBRID
}
