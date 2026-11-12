package de.aws1313.pomodoro

import android.app.Application
import de.aws1313.pomodoro.mobile.di.mobileModule
import de.aws1313.pomodoro.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PomodoroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PomodoroApp)
            modules(sharedModule, mobileModule)
        }
    }
}
