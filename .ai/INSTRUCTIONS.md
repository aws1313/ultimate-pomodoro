# AI Agent Instructions — Ultimate Pomodoro Timer

> This file defines **how AI agents must behave and work** in this project.
> Product requirements, domain logic, and feature specifications live in
> **[`.ai/REQUIREMENTS.md`](.ai/REQUIREMENTS.md)** — read that file too before writing any code.
>
> **Read both files in full before writing any code or making suggestions.**

---

## 0. Communication & Language

- **Code, comments, documentation, commit messages, PR descriptions, and all files in this
  repository must be written in English.**
- The human owner of this project may communicate with AI agents in **German or English**.
  Respond in whichever language the human uses in their message.
- When in doubt, default to English for any artifact that ends up in the repository.

---

## 1. How Agents Find These Files

This file is referenced from multiple entry points so that different AI tools pick it up
automatically:

| Entry point                        | Tool                               |
|------------------------------------|------------------------------------|
| `GEMINI.md` (repo root)            | Gemini CLI / Android Studio Gemini |
| `.github/copilot-instructions.md`  | GitHub Copilot                     |
| `CLAUDE.md` (repo root)            | Claude (Anthropic)                 |
| `.cursor/rules`                    | Cursor IDE                         |
| `.ai/INSTRUCTIONS.md` (this file)  | Canonical source — all agents      |

Each entry-point file contains only a short pointer to this canonical document.

---

## 2. Architecture Rules

### 2.1 Layered Architecture (must be strictly followed)

```
Platform UI  →  ViewModel (shared or per-platform)  →  UseCases  →  Repository  →  DataSource
                                                                           ↕
                                                                     SyncManager
```

- **UI layer** (`*/ui/`): Compose functions only. No business logic, no direct repository calls.
- **ViewModel layer** (`shared/commonMain/viewmodel/`): Emit `StateFlow<UiState>`, receive sealed `Intent` classes.
- **UseCase layer** (`shared/commonMain/domain/usecase/`): One class, one responsibility. No dependency on UI.
- **Repository layer** (`shared/commonMain/data/repository/`): Interface in `commonMain`, platform implementations in `*Main`.
- **DataSource layer**: Local persistence (SQLDelight) and network sync (Ktor WebSocket).

### 2.2 MVI Convention

Every screen has exactly these three types in the same package:

```kotlin
// State — immutable data class
data class TimerUiState(...)

// Intent — sealed interface describing user intentions
sealed interface TimerIntent {
    data object StartTimer : TimerIntent
    data object PauseTimer : TimerIntent
    // ...
}

// Effect — one-shot events (navigation, snackbar)
sealed interface TimerEffect {
    data class ShowError(val message: String) : TimerEffect
}
```

### 2.3 Coroutines & Threading

- **Never** use `GlobalScope`.
- ViewModels use `viewModelScope` (Android) or a Koin-injected `CoroutineScope`.
- All I/O operations run on `Dispatchers.IO`.
- All UI updates run on `Dispatchers.Main`.
- Platform-specific dispatcher implementations via `expect`/`actual`.

---

## 3. Tech Stack (binding)

| Category           | Library / Tool                       | Notes                |
|--------------------|--------------------------------------|----------------------|
| DI                 | Koin Multiplatform                   | `4.x`                |
| Networking         | Ktor (Client + Server)               | `3.x`                |
| Serialization      | kotlinx.serialization                | always latest stable |
| Reactive streams   | kotlinx.coroutines + StateFlow       | always latest stable |
| Local DB           | SQLDelight 2.x                       | `2.x`                |
| Service discovery  | mDNS / NSD (platform-specific)       | —                    |
| Logging            | Kermit (KMP logging)                 | always latest stable |
| Unit testing       | kotlin.test + Turbine (Flow testing) | always latest stable |
| Android UI testing | Compose UI Test                      | always latest stable |
| Static analysis    | Detekt                               | always latest stable |

**Forbidden:** No Rx libraries (RxJava, RxKotlin), no LiveData in new code, no `AsyncTask`.  
Do not introduce dependencies not listed here without explicit discussion with the project owner.

---

## 4. Code Style & Conventions

- Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) strictly.
- No `!!` operator unless null is genuinely impossible (add an explanatory comment in that case).
- Prefer `val` over `var`. Mutable state lives in ViewModels only.
- Use sealed interfaces (not sealed classes) for `Intent` and `Effect` types.
- Use data classes for all state objects.
- Use extension functions for platform helpers — never add them to domain classes.
- All `suspend` functions that may block must have a KDoc comment explaining why.
- Package naming scheme: `de.aws1313.pomodoro.<module>.<layer>.<feature>`

---

## 5. Git Conventions

- **Branch naming:** `feature/<short-description>`, `fix/<short-description>`, `chore/<short-description>`
- **Commit format:** Conventional Commits — `feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`
- **Never commit directly to `main`.** Always use feature branches + PR (even in a solo project).
- Each platform extension gets its own branch.

---

## 6. What AI Agents Must NOT Do

- ❌ Create Java classes (unless a platform API leaves no Kotlin alternative — add a comment).
- ❌ Use `GlobalScope`.
- ❌ Use `LiveData` in new code.
- ❌ Use `!!` without an explanatory comment.
- ❌ Introduce dependencies not listed in Section 3 without discussing with the project owner.
- ❌ Place business logic in the UI layer.
- ❌ Use the Singleton pattern outside of the Koin DI container.
- ❌ Modify the sync protocol specification (`.ai/REQUIREMENTS.md` §5) without asking the project owner first.
- ❌ Add any Compose UI dependency to `watchosMain`.
- ❌ Write repository files, documentation, or commit messages in a language other than English.
- ❌ Treat `REQUIREMENTS.md` as fixed law — requirements can be discussed and changed by the owner.
- ❌ Treat `INSTRUCTIONS.md` rules as suggestions — they are binding process constraints.

---

## 7. Related Documents

| Document                    | Path                        |
|-----------------------------|-----------------------------|
| **Product Requirements**    | `.ai/REQUIREMENTS.md`       |
| Feature Roadmap             | `.ai/ROADMAP.md`            |
| Architecture Diagrams       | `.ai/ARCHITECTURE.md`       |
| Sync Protocol Specification | `.ai/SYNC_PROTOCOL.md`      |
| Decision Log (ADRs)         | `.ai/decisions/`            |
