package de.aws1313.pomodoro.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import de.aws1313.pomodoro.MainActivity
import de.aws1313.pomodoro.R
import de.aws1313.pomodoro.shared.domain.timer.model.TimerPhase
import de.aws1313.pomodoro.shared.domain.timer.model.TimerState
import de.aws1313.pomodoro.shared.domain.timer.repository.TimerRepository
import de.aws1313.pomodoro.shared.domain.timer.usecase.PauseTimerUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.ResetTimerUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.ResumeTimerUseCase
import de.aws1313.pomodoro.shared.domain.timer.usecase.SkipPhaseUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TimerForegroundService : Service(), KoinComponent {

    private val timerRepository: TimerRepository by inject()
    private val pauseTimerUseCase: PauseTimerUseCase by inject()
    private val resumeTimerUseCase: ResumeTimerUseCase by inject()
    private val resetTimerUseCase: ResetTimerUseCase by inject()
    private val skipPhaseUseCase: SkipPhaseUseCase by inject()

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var notificationManager: NotificationManager

    private var isForegroundActive = false
    private var lastNotifiedSecond: Long = -1L
    private var lastNotifiedStateClass: Class<*>? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        observeTimerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> pauseTimerUseCase()
            ACTION_RESUME -> resumeTimerUseCase()
            ACTION_RESET -> {
                resetTimerUseCase()
                stopForegroundAndSelf()
            }
            ACTION_SKIP -> skipPhaseUseCase()
            ACTION_STOP -> stopForegroundAndSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeTimerState() {
        timerRepository.timerState
            .onEach { state ->
                when (state) {
                    is TimerState.Running -> {
                        val currentSecond = state.remainingMs / 1000
                        val stateChanged = lastNotifiedStateClass != state::class.java
                        if (stateChanged || currentSecond != lastNotifiedSecond) {
                            lastNotifiedSecond = currentSecond
                            lastNotifiedStateClass = state::class.java
                            updateNotification(state)
                        }
                    }
                    is TimerState.Paused -> {
                        val currentSecond = state.remainingMs / 1000
                        val stateChanged = lastNotifiedStateClass != state::class.java
                        if (stateChanged || currentSecond != lastNotifiedSecond) {
                            lastNotifiedSecond = currentSecond
                            lastNotifiedStateClass = state::class.java
                            updateNotification(state)
                        }
                    }
                    is TimerState.Finished -> {
                        lastNotifiedSecond = -1L
                        lastNotifiedStateClass = state::class.java
                        updateNotification(state)
                    }
                    TimerState.Idle -> {
                        lastNotifiedSecond = -1L
                        lastNotifiedStateClass = null
                        if (isForegroundActive) {
                            stopForegroundAndSelf()
                        }
                    }
                }
            }
            .launchIn(serviceScope)
    }

    private fun updateNotification(state: TimerState) {
        val notification = buildNotification(state)
        if (!isForegroundActive) {
            isForegroundActive = true
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(state: TimerState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Live Update / Promoted Ongoing request (for Android 16+ status bar chip)
        val promotedExtras = Bundle().apply {
            putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
        }
        builder.addExtras(promotedExtras)

        try {
            val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
            method.invoke(builder, true)
        } catch (_: Throwable) {
            // Handled by bundle extra
        }

        when (state) {
            is TimerState.Running -> {
                val phaseName = phaseName(state.phase)
                val targetWhen = System.currentTimeMillis() + state.remainingMs
                val maxProgress = 1000
                val elapsedMs = (state.totalMs - state.remainingMs).coerceAtLeast(0L)
                val currentProgress = if (state.totalMs > 0) {
                    ((elapsedMs * maxProgress) / state.totalMs).toInt().coerceIn(0, maxProgress)
                } else {
                    0
                }
                val percent = if (state.totalMs > 0) {
                    ((elapsedMs * 100) / state.totalMs).toInt().coerceIn(0, 100)
                } else {
                    0
                }

                builder.setContentTitle("$phaseName in progress")
                    .setContentText("$percent% completed • ${formatMs(state.remainingMs)} remaining")
                    .setProgress(maxProgress, currentProgress, false)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setWhen(targetWhen)
                    .setShowWhen(true)
                    .addAction(
                        android.R.drawable.ic_media_pause,
                        "Pause",
                        createServiceActionPendingIntent(ACTION_PAUSE, 1)
                    )
                    .addAction(
                        android.R.drawable.ic_media_next,
                        "Skip",
                        createServiceActionPendingIntent(ACTION_SKIP, 2)
                    )
            }
            is TimerState.Paused -> {
                val phaseName = phaseName(state.phase)
                val maxProgress = 1000
                val elapsedMs = (state.totalMs - state.remainingMs).coerceAtLeast(0L)
                val currentProgress = if (state.totalMs > 0) {
                    ((elapsedMs * maxProgress) / state.totalMs).toInt().coerceIn(0, maxProgress)
                } else {
                    0
                }
                val percent = if (state.totalMs > 0) {
                    ((elapsedMs * 100) / state.totalMs).toInt().coerceIn(0, 100)
                } else {
                    0
                }

                builder.setContentTitle("$phaseName (Paused)")
                    .setContentText("Paused at $percent% • ${formatMs(state.remainingMs)} remaining")
                    .setProgress(maxProgress, currentProgress, false)
                    .setUsesChronometer(false)
                    .setShowWhen(false)
                    .addAction(
                        android.R.drawable.ic_media_play,
                        "Resume",
                        createServiceActionPendingIntent(ACTION_RESUME, 3)
                    )
                    .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Reset",
                        createServiceActionPendingIntent(ACTION_RESET, 4)
                    )
            }
            is TimerState.Finished -> {
                val phaseName = phaseName(state.phase)
                builder.setContentTitle("$phaseName Completed!")
                    .setContentText("Session finished. Tap to continue.")
                    .setProgress(1000, 1000, false)
                    .setUsesChronometer(false)
                    .setShowWhen(false)
                    .addAction(
                        android.R.drawable.ic_media_next,
                        "Next Phase",
                        createServiceActionPendingIntent(ACTION_SKIP, 5)
                    )
                    .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Reset",
                        createServiceActionPendingIntent(ACTION_RESET, 6)
                    )
            }
            TimerState.Idle -> {
                builder.setContentTitle("Pomodoro Timer")
                    .setContentText("Ready to start")
                    .setProgress(0, 0, false)
            }
        }

        return builder.build()
    }

    private fun createServiceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TimerForegroundService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Active Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows active countdown and live progress in the status bar"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun stopForegroundAndSelf() {
        isForegroundActive = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun phaseName(phase: TimerPhase): String {
        return when (phase) {
            TimerPhase.FOCUS -> "Focus"
            TimerPhase.SHORT_BREAK -> "Short Break"
            TimerPhase.LONG_BREAK -> "Long Break"
            TimerPhase.BREAK -> "Break"
            TimerPhase.OVERTIME -> "Overtime"
        }
    }

    private fun formatMs(ms: Long): String {
        val totalSec = (ms + 999) / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }

    companion object {
        const val CHANNEL_ID = "timer_live_update_channel"
        const val NOTIFICATION_ID = 1001

        const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"

        const val ACTION_PAUSE = "de.aws1313.pomodoro.action.PAUSE"
        const val ACTION_RESUME = "de.aws1313.pomodoro.action.RESUME"
        const val ACTION_RESET = "de.aws1313.pomodoro.action.RESET"
        const val ACTION_SKIP = "de.aws1313.pomodoro.action.SKIP"
        const val ACTION_STOP = "de.aws1313.pomodoro.action.STOP"

        fun startService(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
