package de.aws1313.pomodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.aws1313.pomodoro.mobile.ui.theme.PomodoroTheme
import de.aws1313.pomodoro.mobile.ui.timer.TimerRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PomodoroTheme {
                TimerRoute()
            }
        }
    }
}