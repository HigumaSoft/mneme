# Mneme — Project Charter

**Status:** Draft v0.2
**Date:** 2026-04-30 *(updated post-discovery)*
**Owner:** Dmitry
**Studio:** Higumasoft
**Repository:** [github.com/HigumaSoft/mneme](https://github.com/HigumaSoft/mneme)
**Tracking:** Jira `ANKI` + Confluence `ANKI` (higumasoft.atlassian.net)

---

## 1. Vision

A flashcard and lesson app for Android, **built first for the author's own daily study**, where the unit of practice is a *slice* of cards — not a giant deck. Cards live in the user's own files (Google Drive or local), tagged and categorised so any combination ("Italian restaurant vocab + N5 grammar I've seen this month") becomes a one-tap study session. AI agents can generate decks and lessons in a documented file format and the user imports them; no third-party service touches the user's data.

> **One-line pitch:** Anki's persistence, slicing that actually works, AI-friendly file format, user owns the files.

## 2. Problem

I've been studying Italian, Japanese, and others using AI directly — and hit two walls:

1. **AI alone is expensive and forgetful.** Asking Claude for "Italian verb forms" daily burns tokens and produces inconsistent, undeduplicated output. I want the cards persisted once, then practiced.
2. **Anki has the cards but the wrong UX.** Tags exist but the experience punishes anything except mechanical end-to-end deck grinding. I want to study "all my restaurant-related Italian, including the phrases that came up in last week's lesson" — that's a query, not a deck.

Existing players (Anki, Quizlet, Memrise, Memora) either lock content into their cloud, hide tags as a power-user feature, or aren't designed to consume agent-authored content. There's room for a small, opinionated, user-owns-everything app aimed at slicing.

## 3. Target users (in priority order)

1. **Me.** Self-sustainability is the v1.0 goal — if I don't use it daily, nothing else matters.
2. **Self-directed learners** who already use AI for study and want a cheap, persistent home for the output.
3. **Tutors / teachers** who want to hand a learner a folder of decks and lessons.
4. **AI agent ecosystem** (Claude / ChatGPT / Cursor users) — eventually. Format is designed to invite this.

## 4. Scope

### In scope (v1.0)

- Android app, Kotlin + Jetpack Compose, min SDK 26.
- **Cards-first** model. Decks are containers; the studyable unit is a *slice* (saved query).
- **Browse screen** — universal card list with multi-axis filter (deck × category × tags × source × due-state × free-text). Multi-select bulk actions.
- **Saved slices** — user composes filters, saves them, taps once to study.
- **SM-2 spaced repetition** — Anki-compatible scheduling, applied per-card regardless of which slice surfaces them.
- **Manual card creation** — fast in-app editor.
- **Lessons as card factories** (Option D, see §6) — guided curriculum that extracts cards into a target deck.
- **Bulk import** — paste JSON / CSV into a sheet, file picker, share-intent ("Open with Mneme" from any app), Drive folder watch.
- **File format v0.2** as the public AI-agent contract — cards, lessons, slices, manifests.
- **Storage:** local-first by default; Google Drive sync optional but supported.
- Privacy policy, Data Safety form, internal → closed beta → public on Play.

### Out of scope (v1.0)

- iOS / web. Architecture leaves room for KMP later.
- Native Anki `.apkg` import. *(v1.1 — non-blocking.)*
- Real-time multi-device collaboration. (Drive = eventual consistency.)
- A central server, user accounts, shared cloud catalogue.
- An MCP server with Drive access. **(Dropped from v1.0; revisit if external users show up.)**
- Audio recording, speech recognition. *(v1.1+; format already accommodates audio media.)*
- Marketplace / monetisation.

### Non-goals

- We will **never** require a Mneme account.
- We will **never** store deck content on a server we control.
- We will **not** lock decks to a proprietary binary format.

## 5. Architecture overview

```
┌────────────────────────────────────────────────┐
│  Mneme app (Kotlin + Compose)                  │
│  ┌─────────┐  ┌────────┐  ┌────────┐  ┌─────┐  │
│  │ Browse  │  │ Slices │  │ Lesson │  │ SR  │  │
│  │ + filter│  │ runner │  │ player │  │ SM-2│  │
│  └────┬────┘  └────┬───┘  └────┬───┘  └──┬──┘  │
│       └────── Repository (Room cache) ────┘    │
│                       │                        │
│         ┌─────────────┴─────────────┐          │
│         │ Storage adapter           │          │
│         │  ├─ local files (default) │          │
│         │  └─ Google Drive (opt-in) │          │
│         └─────────────┬─────────────┘          │
└───────────────────────┬────────────────────────┘
                        ▼
         User's chosen folder (Drive or device)
              decks/  lessons/  slices/

         AI agents author files offline:
         ─► user pastes / shares-into / drops file ─► same folder
```

**Key invariants:**

- The folder is the source of truth.
- Local-only mode is fully supported. Drive is opt-in.
- No third-party service touches user content. Ever.

## 6. Lessons direction — locked: Option D (card-factory)

Decision made and locked. A lesson is a curriculum that produces cards into a target deck as the user works through it. The lesson file is read-only content; cards live in regular decks and are practiced via slices. Spec: `DECK_FORMAT.md` §7.

| Option (history) | Why not | |
|---|---|---|
| A. Ordered curriculum (decks-in-order) | Doesn't address the slicing problem. | rejected |
| B. Rich self-contained lesson (Duolingo-flavoured) | Too big a v1.0 build, content authoring UX is hard. | deferred to v1.1+ |
| C. Hybrid A/B | Bigger scope, no payoff. | rejected |
| **D. Card-factory** | Matches the actual workflow: lessons author cards, slices study them. | **locked** |

Module kinds for v1.0: `vocabulary`, `dialogue`. Forward-compatible additions later: `reading`, `listening`, `quiz`, `audio_drill`, `prompt`.

## 7. File format & storage

Defined fully in [`DECK_FORMAT.md`](./DECK_FORMAT.md). v0.2 highlights:

- One folder per deck under `<root>/decks/<deck-id>/`.
- Cards carry `category` (enum-ish: vocabulary / grammar / kanji / phrase / listening / reading) and namespaced `tags` (`topic:restaurant`, `level:a1`, `book:genki1`).
- Card `source` records provenance (manual / lesson / agent / import).
- Deterministic card ids — re-imports of the same content dedupe naturally.
- Lessons (`lesson.json`) are card factories with vocabulary + dialogue modules.
- Slices (`slices/<id>.json`) are saved queries. Sync between devices like decks do.
- Manifest is optional fast-index, app rebuilds from folder scan if missing.

This format is the only AI-agent contract we ship in v1.0. Agents emit valid JSON or CSV; users import in two taps.

## 8. AI-agent surface (single layer)

**File-format-as-API.** Agent generates a JSON or CSV file matching the spec. User saves it to their Mneme folder, or pastes into the app's import sheet, or shares-into the app from any other app. Done.

That's the whole integration. No OAuth, no service, no SDK, no rate limit, nothing for an agent author to learn beyond the schema. The format already supports media (images, audio) — agents can encode small assets inline as base64 in v1.1.

If external usage justifies it later, an MCP wrapper can be added without changing the format. Format is the durable contract.

## 9. Tech stack

| Concern | Choice | Why |
|---|---|---|
| Language | Kotlin | Modern, official, your prior exposure. |
| UI | Jetpack Compose | Declarative, current standard. |
| DI | Hilt | Standard for Compose apps. |
| Local DB (cache) | Room | First-party, plays with Coroutines/Flow. |
| Async | Kotlin Coroutines + Flow | Standard. |
| Drive (optional) | Drive REST v3 + Sign-In, `drive.file` scope only | Narrow surface, faster verification. |
| Build | Gradle Kotlin DSL + version catalog | Modern. |
| CI | GitHub Actions | Free, integrates with Play via Gradle Play Publisher. |
| Tracking | Jira (Scrum) + Confluence | higumasoft.atlassian.net, project key `ANKI`. |

## 10. Milestones

Personal-first ordering — the "I can use it daily" milestone (M3) precedes the "anyone else can use it" milestones (M5+).

| # | Milestone | Exit criteria |
|---|---|---|
| M0 | Charter + format spec | This doc + `DECK_FORMAT.md` v0.2 reviewed and locked. ✅ |
| M1 | Repo + CI + scaffold | Compose app builds, runs an empty Browse screen on emulator, CI green. *(In progress.)* |
| M2 | Local SR + manual cards | Hand-create a deck, hand-create cards, study with SM-2. No filtering yet. |
| M3 | **Browse + slicing — daily-driver milestone** | Filter by deck / category / tags / due-state / text. Save and replay slices. I use it for Italian + Japanese for one week without falling back to Anki. |
| M4 | Bulk import polish | Paste JSON, file pick, share-intent. Import 200-card Italian set in under 30 seconds. |
| M5 | Lessons (card-factory) | Run an Italian "eating out" lesson end-to-end. Cards extract correctly with category + tags + source. |
| M6 | Drive sync (optional) | Sign in with `drive.file`, pick folder, two devices stay in sync. |
| M7 | Internal Play track | Signed AAB on Play Console internal track. 5 testers. Privacy policy live. |
| M8 | Closed beta | 20+ external testers, crash-free ≥ 99% for one week. |
| M9 | Public launch | Listed on Play. Listing copy, screenshots, Data Safety form, OAuth verification done if M6 shipped. |

No hard dates while it's a personal project. We re-introduce dates if we hit M3 and decide to push for an external launch.

## 11. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Solo developer bandwidth | H | H | M0–M3 ruthlessly small. Daily-driver milestone before anything external. |
| Browse-screen scope creep | M | M | Lock filter dimensions in M3 spec; resist adding views. |
| Drive OAuth verification (only if M6 ships) | M | M | `drive.file` scope is non-sensitive. Verification ~3–6 weeks; start during M6. |
| Play Store policy on AI-generated content | L | M | Disclose in listing; cards are user-imported, not AI-generated server-side. |
| Format v0.2 needs breaking changes after v1.0 launch | M | M | Versioning policy in spec; v0.x reserves the right; bump to 1.0 only when stable. |

## 12. Success metrics

**Personal (the only one that matters for v1.0):**

- I open Mneme to study at least 5 days/week for 4 consecutive weeks.
- ≥ 80% of new study sessions go through a saved slice (not a raw deck list).
- I import at least 3 AI-generated decks in the first 4 weeks.

**External (only if/when launched, M9+):**

- 100 production installs in first month.
- ≥ 4.0 star rating.
- Crash-free users ≥ 99%.

## 13. Open questions

- ~~App name~~ — locked: **Mneme** by Higumasoft.
- ~~Lesson direction~~ — locked: Option D.
- ~~MCP server~~ — dropped from v1.0.
- ~~GitHub remote URL~~ — locked: [github.com/HigumaSoft/mneme](https://github.com/HigumaSoft/mneme).
- Drive sync: ship in v1.0 or defer to a 1.x point release? *(Recommend: defer until daily-driver milestone passes.)*
- App icon + branding — when we hit M7, not before.

## 14. Approvals

| Role | Name | Date | Sign-off |
|---|---|---|---|
| Product owner | Dmitry | | ☐ |
| Tech lead | Dmitry | | ☐ |
