# Ultimate Pomodoro Timer

> [!TIP]
> Large parts of the code in this project are AI-generated. See the [AI & Authorship Disclaimer](#ai--authorship-disclaimer) for context on how and why.

---

## What is this?

**Ultimate Pomodoro Timer** is a cross-platform focus timer app built with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) and [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/). It runs natively on Android, iOS, Windows, macOS, Linux, Wear OS, and watchOS — all from a single shared Kotlin codebase.

The app supports three timer modes:

- **Pomodoro** — the classic technique: fixed focus intervals (default 25 min) followed by short breaks, with a longer break after every four sessions.
- **Flowmodoro** — a flexible variant: you work for as long as you want, then earn a break proportional to your focus time (focus time ÷ break factor).
- **Hybrid** — starts as a Pomodoro countdown, but when the timer finishes you can choose to keep working. The extra time is tracked and a Flowmodoro-style break is calculated on the total focus duration. After the break, the next session always restarts as a fresh Pomodoro.

A key feature is **local P2P synchronization**: the timer state is kept in sync across all your devices on the same Wi-Fi network or via Bluetooth — no cloud, no account required. One device acts as the host; others discover it automatically via mDNS and connect over WebSocket.

---

## Target Platforms

| Platform | Status |
|---|---|
| Android (phone & tablet) | 🚧 In development |
| Desktop (macOS, Windows, Linux) | 🗓 Planned |
| Wear OS | 🗓 Planned |
| iOS (phone & tablet) | 🗓 Planned |
| watchOS | 🗓 Planned |

---

## Project Structure

```
ultimate-pomodoro/
├── shared/       # KMP library — all business logic, domain, sync protocol
├── mobile/       # Android app
├── wear/         # Wear OS app
├── desktop/      # Compose Desktop app (planned)
├── iosApp/       # iOS Xcode project (planned)
└── watchosApp/   # watchOS Xcode project (planned)
```

For the full architecture, domain model, and sync protocol specification, see the [`.ai/`](.ai/) directory.

---

## AI & Authorship Disclaimer

A significant portion of the code and documentation in this repository has been written or co-written with the assistance of AI language models (primarily Google Gemini via Android Studio).

**This is intentional, and here is why:**

I am a student who wants to deliberately explore what modern AI-assisted software development looks like in practice — not as a shortcut, but as a subject of study in itself. That means:

- I **define the architecture**, make the technology choices, review every significant decision, and decide what gets accepted into the codebase.
- I use AI as a **pair programmer**, not as a replacement for understanding. I read the code that gets generated, question it, and push back when it is wrong.
- The project is **not "vibe-coded"**: there are explicit architecture rules, a documented domain model, and a defined sync protocol — all of which I designed and maintain. These live in [`.ai/INSTRUCTIONS.md`](.ai/INSTRUCTIONS.md) and [`.ai/REQUIREMENTS.md`](.ai/REQUIREMENTS.md).
- I am building genuine experience with the full workflow: prompting, reviewing AI output critically, iterating, and understanding the tradeoffs — skills I believe are increasingly important for software engineers.

If you are evaluating this project: the AI wrote a lot of the boilerplate. The design, the decisions, and the direction are mine.
