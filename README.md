# Mneme

A cards-first flashcard and lesson app for Android. Decks are containers; the studyable unit is a **slice** (a saved query over your cards). User owns the files (local or Google Drive), AI agents author content offline by writing to the documented file format.

See `docs/CHARTER.md` and `docs/DECK_FORMAT.md` for the locked v0.2 spec.

**Tracking:** Jira project [ANKI](https://higumasoft.atlassian.net/jira/software/projects/ANKI), Confluence space [ANKI](https://higumasoft.atlassian.net/wiki/spaces/ANKI/overview).

See [`../../../OneDrive/Документы/Claude/Projects/Anki card/CHARTER.md`](../../../OneDrive/Документы/Claude/Projects/Anki%20card/CHARTER.md) for the project charter and [`DECK_FORMAT.md`](./docs/DECK_FORMAT.md) for the file format spec (the public AI-agent API).

## Status

Pre-alpha. Scaffolding only. See `Jira project <KEY>` for the live backlog (TBD).

## Stack

- Kotlin 2.0 + Jetpack Compose
- Gradle Kotlin DSL + version catalog
- Hilt (DI), Room (local DB), Coroutines + Flow, kotlinx.serialization
- Google Sign-In + Google Drive REST v3 (`drive.file` scope)
- min SDK 26 (Android 8), target SDK 35

## Project layout

```
app/src/main/kotlin/com/higumasoft/mneme/
├── MnemeApp.kt        ← Application entry, Hilt
├── MainActivity.kt         ← Single-Activity host for Compose nav
├── ui/                     ← Compose screens & theme
│   ├── theme/
│   ├── decks/
│   └── study/
├── domain/model/           ← Pure Kotlin domain types (Deck, Card)
├── sr/                     ← SM-2 spaced repetition engine (pure, tested)
├── format/                 ← Deck file format parsers (JSON, CSV)
├── data/
│   ├── local/              ← Room DB (cache)
│   ├── drive/              ← Google Drive sync adapter
│   └── repo/               ← Repository layer that combines local + Drive
└── di/                     ← Hilt modules
```

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:test           # JVM unit tests (SR engine, format parsers)
./gradlew :app:connectedCheck # Instrumented tests (needs emulator/device)
./gradlew lint
```

The Gradle wrapper isn't checked in yet — generate it once with a local Gradle install:

```bash
gradle wrapper --gradle-version 8.10 --distribution-type bin
```

## Status by milestone

| Milestone | Status | Notes |
|---|---|---|
| M0 Charter + format | ✅ Done | See `docs/` and the OneDrive `Anki card/` folder. |
| M1 Repo + CI + scaffold | 🟡 In progress | This commit. CI pending GitHub remote. |
| M2 Local SR works | ⏳ | SM-2 engine + tests landed; UI to follow. |
| M3 Drive sync | ⏳ | Interfaces stubbed. |
| M4 Bulk import | ⏳ | |
| M5 Lessons v1 | ⏳ | |
| M6 MCP server | ⏳ | Will live in `mcp-server/` subdir or sibling repo. |
| M7–M9 Beta + launch | ⏳ | |

## License

TBD. Suggest Apache-2.0 to encourage AI-agent ecosystem.
