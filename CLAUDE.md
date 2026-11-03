# Claude Agent Instructions

This project has two canonical AI context files. Read **both** before writing any code or making suggestions:

| File | Purpose |
|------|---------|
| **[`.ai/INSTRUCTIONS.md`](.ai/INSTRUCTIONS.md)** | How to work in this project (architecture rules, tech stack, conventions, what agents must not do) |
| **[`.ai/REQUIREMENTS.md`](.ai/REQUIREMENTS.md)** | What the product must do (features, domain logic, timer modes, sync protocol) |

Key points at a glance:
- Kotlin Multiplatform (KMP) + Compose Multiplatform project
- Platforms: Android, Desktop, Wear OS, iOS, watchOS
- Architecture: MVI, strict layering (UI → ViewModel → UseCase → Repository)
- Timer modes: Pomodoro, Flowmodoro, and Hybrid (Pomodoro that transitions to Flowmodoro)
- All repository files, code, comments, and commits must be in **English**
- The human owner may communicate in German or English — respond in kind
