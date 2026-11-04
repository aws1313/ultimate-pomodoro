package de.aws1313.pomodoro.shared

import de.aws1313.pomodoro.shared.data.timer.DefaultTimerRepository
import de.aws1313.pomodoro.shared.domain.timer.model.TimerPhase
import de.aws1313.pomodoro.shared.domain.timer.model.TimerSettings
import de.aws1313.pomodoro.shared.domain.timer.model.TimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultTimerRepositoryTest {

    @Test
    fun initialStateIsIdle() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val repo = DefaultTimerRepository(scope = scope)

        assertEquals(TimerState.Idle, repo.timerState.value)
        assertEquals(0, repo.completedSessions.value)
    }

    @Test
    fun startTransitionsToRunning() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val settings = TimerSettings(focusDurationMin = 25)
        val repo = DefaultTimerRepository(scope = scope, initialSettings = settings)

        repo.start(TimerPhase.FOCUS)
        scope.testScheduler.advanceTimeBy(50L)

        val state = repo.timerState.value
        assertIs<TimerState.Running>(state)
        assertEquals(TimerPhase.FOCUS, state.phase)
        assertEquals(25 * 60 * 1000L, state.totalMs)
    }

    @Test
    fun pauseAndResumeWorkAsExpected() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val settings = TimerSettings(focusDurationMin = 10)
        val repo = DefaultTimerRepository(scope = scope, initialSettings = settings)

        repo.start(TimerPhase.FOCUS)
        scope.testScheduler.advanceTimeBy(200L)

        repo.pause()
        val pausedState = repo.timerState.value
        assertIs<TimerState.Paused>(pausedState)
        assertEquals(TimerPhase.FOCUS, pausedState.phase)

        repo.resume()
        scope.testScheduler.advanceTimeBy(50L)
        val resumedState = repo.timerState.value
        assertIs<TimerState.Running>(resumedState)
        assertEquals(TimerPhase.FOCUS, resumedState.phase)
    }

    @Test
    fun resetTransitionsToIdle() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val repo = DefaultTimerRepository(scope = scope)

        repo.start(TimerPhase.FOCUS)
        scope.testScheduler.advanceTimeBy(100L)

        repo.reset()
        assertEquals(TimerState.Idle, repo.timerState.value)
    }

    @Test
    fun skipAdvancesToBreak() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val settings = TimerSettings(pomodorosUntilLongBreak = 4)
        val repo = DefaultTimerRepository(scope = scope, initialSettings = settings)

        repo.start(TimerPhase.FOCUS)
        scope.testScheduler.advanceTimeBy(100L)

        repo.skip()
        scope.testScheduler.advanceTimeBy(50L)

        val state = repo.timerState.value
        assertIs<TimerState.Running>(state)
        assertEquals(TimerPhase.SHORT_BREAK, state.phase)
    }
}
