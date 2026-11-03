# Product Requirements — Ultimate Pomodoro Timer

> This file describes **what the product must do** — features, domain logic, and behaviour
> specifications. It is the single source of truth for all product decisions.
>
> AI agents: read this alongside `.ai/INSTRUCTIONS.md` before writing any code.
> Requirements here can be discussed and refined with the project owner; they are not
> immutable law, but changes must be explicitly confirmed before implementation.

---

## 1. Project Overview

**Name:** UltimatePomodoroTimer  
**Goal:** A cross-platform Pomodoro and Flowmodoro timer app with real-time, local P2P
synchronization between devices on the same network or within Bluetooth range.  
**Primary languages:** Kotlin (logic), English (all repository text)

---

## 2. Target Platforms & Priorities

| Priority | Platform                        | UI Framework                | Module        |
|----------|---------------------------------|-----------------------------|---------------|
| 1        | Android (phone & tablet)        | Compose Multiplatform       | `:mobile`     |
| 2        | Desktop (macOS, Windows, Linux) | Compose Multiplatform       | `:desktop`    |
| 3        | Wear OS                         | Compose for Wear OS         | `:wear`       |
| 4        | iOS (phone & tablet)            | Compose Multiplatform       | `:iosApp`     |
| 5        | watchOS                         | SwiftUI + KMP Core Bridge   | `:watchosApp` |

**watchOS constraint:** Compose Multiplatform does not support watchOS. The KMP `commonMain`
logic is exported as a Kotlin/Native XCFramework and consumed from SwiftUI via Swift interop.
The watchOS UI must be kept minimal: timer display, start, pause, and skip only.

---

## 3. Module Structure

```
ultimate-pomodoro/
├── shared/           # KMP library — all business logic, domain, sync protocol
│   └── src/
│       ├── commonMain/   # Platform-agnostic code (timer engine, domain, sync interfaces)
│       ├── androidMain/  # Android-specific implementations (NSD, BluetoothSocket)
│       ├── iosMain/      # iOS-specific implementations (Bonjour, CoreBluetooth)
│       ├── desktopMain/  # JVM desktop implementations (JmDNS, Java Bluetooth)
│       └── watchosMain/  # Data classes and timer logic only — no UI
├── mobile/           # Android app module (depends on :shared)
├── wear/             # Wear OS app module (depends on :shared)
├── desktop/          # Compose Desktop module (to be created)
├── iosApp/           # iOS Xcode project (to be created)
└── watchosApp/       # watchOS Xcode project (to be created)
```

---

## 4. Timer Domain

### 4.1 Timer Modes

The app supports three timer modes:

```kotlin
enum class TimerMode {
    /**
     * Classic Pomodoro: fixed focus intervals followed by short breaks,
     * with a long break after every N focus sessions.
     * The user cannot extend a focus session once the timer ends.
     */
    POMODORO,

    /**
     * Flowmodoro: the user works for as long as they want (no fixed duration).
     * When the focus session ends, the break duration is calculated as:
     *   breakDuration = focusDuration / flowmodoroBreakFactor
     * The break timer counts down. No long-break concept.
     */
    FLOWMODORO,

    /**
     * Hybrid (Pomodoro → Flowmodoro):
     * Starts as a Pomodoro session with a fixed focus countdown.
     * When the focus timer finishes, the user is offered a choice:
     *   • Take a break (normal Pomodoro short/long break), OR
     *   • Keep working — the session seamlessly transitions to Flowmodoro mode,
     *     tracking additional focus time beyond the original Pomodoro duration.
     * When the user eventually stops, the break duration is calculated on the
     * *total* focus time (original Pomodoro duration + overtime) using the
     * Flowmodoro break factor.
     * The Pomodoro session counter still increments normally.
     */
    HYBRID
}
```

### 4.2 Timer Phases

```kotlin
enum class TimerPhase {
    FOCUS,            // Active work — all modes
    SHORT_BREAK,      // Pomodoro & Hybrid: short break between sessions
    LONG_BREAK,       // Pomodoro & Hybrid: long break after N focus sessions
    BREAK,            // Flowmodoro: dynamically computed break
    OVERTIME          // Hybrid only: user chose to keep working past the Pomodoro timer
}
```

### 4.3 Timer State Model

```kotlin
sealed interface TimerState {
    /** No session is active. App is at rest. */
    data object Idle : TimerState

    /** Timer is actively counting. */
    data class Running(
        val phase: TimerPhase,
        val remainingMs: Long,       // countdown remaining; -1 if phase has no fixed end (OVERTIME)
        val totalMs: Long,           // total duration of this phase; -1 for OVERTIME
        val elapsedOvertimeMs: Long  // only meaningful in OVERTIME phase, else 0
    ) : TimerState

    /** Timer is paused mid-phase. */
    data class Paused(
        val phase: TimerPhase,
        val remainingMs: Long,
        val totalMs: Long,
        val elapsedOvertimeMs: Long
    ) : TimerState

    /** A phase has just completed; waiting for user action or auto-start. */
    data class Finished(
        val phase: TimerPhase,
        val completedSessionCount: Int  // total focus sessions completed in this run
    ) : TimerState
}
```

### 4.4 Session Settings

```kotlin
data class TimerSettings(
    // --- Pomodoro & Hybrid ---
    val focusDurationMin: Int = 25,
    val shortBreakDurationMin: Int = 5,
    val longBreakDurationMin: Int = 15,
    val pomodorosUntilLongBreak: Int = 4,

    // --- Flowmodoro & Hybrid ---
    /** Break duration = focus duration / this factor. Default: 5 (20 % of focus time). */
    val flowmodoroBreakFactor: Float = 5f,

    // --- Automation ---
    val autoStartBreak: Boolean = false,
    val autoStartFocus: Boolean = false,

    // --- Mode ---
    val timerMode: TimerMode = TimerMode.POMODORO
)
```

### 4.5 Hybrid Mode — Detailed Flow

```
[FOCUS countdown running]
        │
        ▼ timer reaches 0
[Finished(FOCUS)] ──► notification / prompt shown to user
        │
        ├─► User picks "Take a break"
        │       └─► normal Pomodoro break logic (SHORT_BREAK or LONG_BREAK)
        │               │
        │               ▼ break ends
        │       [automatically returns to FOCUS with Pomodoro countdown]
        │       (session counter incremented, long-break threshold checked)
        │
        └─► User picks "Keep working"
                └─► phase transitions to OVERTIME
                        • remainingMs = -1 (no fixed end)
                        • elapsedOvertimeMs counts up
                        │
                        ▼ user manually stops / taps "Done"
                [break duration = (focusDurationMin*60*1000 + elapsedOvertimeMs) / breakFactor]
                        └─► BREAK phase (dynamic, Flowmodoro-style)
                                │
                                ▼ break ends
                        [automatically returns to FOCUS with Pomodoro countdown]
                        (session counter incremented, long-break threshold checked)
```

**Key rule:** In `HYBRID` mode, every new focus session always starts as a fresh Pomodoro
countdown, regardless of whether the previous session was a normal Pomodoro or an Overtime.
The "Pomodoro or keep working" choice is offered fresh at the end of every focus countdown.

---

## 5. Synchronization Protocol

### 5.1 Principle

- One device acts as **Host**: opens a Ktor WebSocket server on a random free port.
- Other devices are **Clients**: discover the host via mDNS and connect.
- The host pushes a `TimerStateSync` packet to all connected clients on every state change.
- Clients may send `ControlMessage` packets to the host (start, pause, skip, etc.).
- **The host is always the single source of truth.**
- On connection loss, the client falls back to: last known state + local clock extrapolation.
- When reconnecting, the host immediately pushes the current state to the rejoining client.

### 5.2 mDNS Service Name

```
_ultimatepomodoro._tcp.local.
```

### 5.3 Message Format (kotlinx.serialization / JSON)

```kotlin
@Serializable
sealed interface SyncMessage

@Serializable
data class TimerStateSync(
    val state: TimerState,
    val serverTimeMs: Long,       // host wall-clock ms at time of send — used for drift correction
    val sessionId: String         // UUID of the current timer run; resets on Idle → Running
) : SyncMessage

@Serializable
data class ControlMessage(
    val intent: TimerIntent,
    val senderDeviceId: String    // stable UUID per device, persisted locally
) : SyncMessage
```

### 5.4 Platform-Specific Discovery

| Platform      | Technology                              | `actual` class        |
|---------------|-----------------------------------------|-----------------------|
| Android       | `NsdManager`                            | `AndroidNsdDiscovery` |
| iOS / macOS   | `NSNetServiceBrowser` (Bonjour)         | `AppleNsdDiscovery`   |
| Desktop (JVM) | JmDNS library                           | `JvmJmDnsDiscovery`   |
| watchOS       | No own discovery — relays via iOS app   | —                     |

### 5.5 Known Hard Problems

1. **iOS background execution:** Apple does not allow persistent background network connections.
   On backgrounding, persist timer state locally; re-establish WebSocket on foreground return.
   Use the iOS `BackgroundTasks` framework.
2. **watchOS independence:** Newer Apple Watch models can operate without an iPhone, but the
   primary sync path is through the iPhone companion app. No discovery logic in watchOS.
3. **Desktop firewall:** On Windows and Linux, firewalls may block mDNS and WebSocket ports.
   Always provide a manual IP-entry fallback in the UI.
4. **Battery:** The Ktor WebSocket server must idle when no client is connected.
   Heartbeat interval: 30 seconds.
5. **Clock drift:** Clients must compute `drift = localTime - serverTimeMs` from each received
   `TimerStateSync` and apply it to displayed remaining time.

---

## 6. Non-Functional Requirements

| Requirement       | Target                                                      |
|-------------------|-------------------------------------------------------------|
| Offline operation | App must work fully without any sync partner connected      |
| Accuracy          | Timer drift vs. wall clock must be < 500 ms over 60 minutes |
| Sync latency      | State update must arrive on all clients within 1 second     |
| Cold start time   | < 2 seconds on a mid-range Android device                   |
| Persistence       | Active session must survive app process death and restore   |

---

## 7. Out of Scope (for now)

- Cloud sync / server-based sync (all sync is local P2P only)
- User accounts or login
- Sharing statistics to social media
- Calendar integration
- Push notifications from a cloud backend
