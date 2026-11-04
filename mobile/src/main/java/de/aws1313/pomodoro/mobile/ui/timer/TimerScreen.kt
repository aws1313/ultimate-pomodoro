package de.aws1313.pomodoro.mobile.ui.timer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.aws1313.pomodoro.mobile.presentation.timer.TimerIntent
import de.aws1313.pomodoro.mobile.presentation.timer.TimerUiState
import de.aws1313.pomodoro.mobile.presentation.timer.TimerViewModel
import de.aws1313.pomodoro.mobile.service.TimerForegroundService
import de.aws1313.pomodoro.mobile.ui.theme.PomodoroTheme
import de.aws1313.pomodoro.shared.domain.timer.model.TimerPhase
import org.koin.androidx.compose.koinViewModel

@Composable
fun TimerRoute(
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Request notification permission on Android 13+ (API 33+) for Live Updates
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Handled by user */ }

        LaunchedEffect(Unit) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Automatically synchronize the Foreground Service with the active timer state
    LaunchedEffect(uiState.isRunning) {
        if (uiState.isRunning) {
            TimerForegroundService.startService(context)
        }
    }

    TimerScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    uiState: TimerUiState,
    onIntent: (TimerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ultimate Pomodoro",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Phase indicator badge
            PhaseBadge(phase = uiState.phase, phaseTitle = uiState.phaseTitle)

            // Circular countdown display
            CircularTimer(
                progress = uiState.progress,
                formattedTime = uiState.formattedTime,
                phase = uiState.phase,
                completedSessions = uiState.completedSessions
            )

            // Controls
            TimerControls(
                uiState = uiState,
                onIntent = onIntent
            )
        }
    }
}

@Composable
private fun PhaseBadge(
    phase: TimerPhase,
    phaseTitle: String,
    modifier: Modifier = Modifier
) {
    val badgeColor by animateColorAsState(
        targetValue = when (phase) {
            TimerPhase.FOCUS -> MaterialTheme.colorScheme.primaryContainer
            TimerPhase.SHORT_BREAK -> MaterialTheme.colorScheme.secondaryContainer
            TimerPhase.LONG_BREAK -> MaterialTheme.colorScheme.tertiaryContainer
            TimerPhase.BREAK -> MaterialTheme.colorScheme.secondaryContainer
            TimerPhase.OVERTIME -> MaterialTheme.colorScheme.errorContainer
        },
        label = "badgeColor"
    )

    val contentColor = when (phase) {
        TimerPhase.FOCUS -> MaterialTheme.colorScheme.onPrimaryContainer
        TimerPhase.SHORT_BREAK -> MaterialTheme.colorScheme.onSecondaryContainer
        TimerPhase.LONG_BREAK -> MaterialTheme.colorScheme.onTertiaryContainer
        TimerPhase.BREAK -> MaterialTheme.colorScheme.onSecondaryContainer
        TimerPhase.OVERTIME -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = badgeColor
    ) {
        Text(
            text = phaseTitle,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CircularTimer(
    progress: Float,
    formattedTime: String,
    phase: TimerPhase,
    completedSessions: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "timerProgress"
    )

    val progressColor = when (phase) {
        TimerPhase.FOCUS -> MaterialTheme.colorScheme.primary
        TimerPhase.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
        TimerPhase.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
        TimerPhase.BREAK -> MaterialTheme.colorScheme.secondary
        TimerPhase.OVERTIME -> MaterialTheme.colorScheme.error
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(280.dp)
    ) {
        // Background track
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 12.dp,
            strokeCap = StrokeCap.Round
        )

        // Active countdown indicator
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = progressColor,
            strokeWidth = 12.dp,
            strokeCap = StrokeCap.Round
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Completed: $completedSessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimerControls(
    uiState: TimerUiState,
    onIntent: (TimerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                uiState.isIdle -> {
                    Button(
                        onClick = { onIntent(TimerIntent.Start()) },
                        modifier = Modifier
                            .height(56.dp)
                            .width(180.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = "Start Focus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                uiState.isRunning -> {
                    Button(
                        onClick = { onIntent(TimerIntent.Pause) },
                        modifier = Modifier
                            .height(56.dp)
                            .width(140.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(text = "Pause", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = { onIntent(TimerIntent.Skip) },
                        modifier = Modifier.height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(text = "Skip")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = { onIntent(TimerIntent.Reset) },
                        modifier = Modifier.height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(text = "Reset")
                    }
                }
                uiState.isPaused -> {
                    Button(
                        onClick = { onIntent(TimerIntent.Resume) },
                        modifier = Modifier
                            .height(56.dp)
                            .width(140.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(text = "Resume", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = { onIntent(TimerIntent.Skip) },
                        modifier = Modifier.height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(text = "Skip")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = { onIntent(TimerIntent.Reset) },
                        modifier = Modifier.height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(text = "Reset")
                    }
                }
                uiState.isFinished -> {
                    Button(
                        onClick = { onIntent(TimerIntent.Skip) },
                        modifier = Modifier
                            .height(56.dp)
                            .width(180.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = "Next Phase",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = { onIntent(TimerIntent.Reset) },
                        modifier = Modifier.height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(text = "Reset")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenIdlePreview() {
    PomodoroTheme {
        TimerScreen(
            uiState = TimerUiState(
                formattedTime = "25:00",
                phase = TimerPhase.FOCUS,
                phaseTitle = "Focus Session",
                progress = 1f,
                isIdle = true
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenRunningPreview() {
    PomodoroTheme {
        TimerScreen(
            uiState = TimerUiState(
                formattedTime = "18:42",
                phase = TimerPhase.FOCUS,
                phaseTitle = "Focus Session",
                progress = 0.74f,
                isRunning = true,
                isIdle = false,
                completedSessions = 2
            ),
            onIntent = {}
        )
    }
}
