# Mneme — Deck, Lesson & Slice File Format

**Spec version:** 0.2 (draft)
**Status:** under review
**Audience:** the Mneme app, AI agents, third-party tools.

This format is the **public API** of Mneme. The app reads it; agents write it; users own it. Anything that can produce a valid file is a first-class author. The unit of organisation is the **card**, not the deck — decks are loose containers; **slices** (saved queries over cards) are how you actually study.

**v0.2 changes from v0.1:** added card-level `category` and `source`, namespaced tag convention, deterministic card-id rule, lesson-as-card-factory model, `slices.json` schema. All additive — v0.1 readers ignore unknown fields and keep working.

---

## 1. Folder layout

A user designates one folder as the "Mneme root" — on Google Drive, on the phone's local storage, or anywhere the user can point the import picker at. Default name: `Mneme`. The app uses Drive's `drive.file` scope, so on Drive it only sees folders it created or that the user explicitly granted.

```
Mneme/
├── manifest.json                  ← optional fast-index
├── decks/
│   ├── italian/
│   │   ├── deck.json              ← rich format (preferred)
│   │   └── media/
│   │       ├── ristorante.png
│   │       └── ristorante.mp3
│   └── spanish-verbs/
│       └── deck.csv               ← simple format
├── lessons/
│   └── italian-eating-out/
│       └── lesson.json
└── slices/
    ├── due-today.json
    └── restaurant-italian.json
```

**Rules:**

- A deck folder MUST contain exactly one of `deck.json` or `deck.csv`.
- `media/` is reserved for binary assets referenced by cards.
- The app ignores any unknown files (forward-compatibility).
- An agent SHOULD update `manifest.json` after writes; the app rebuilds it from a folder scan if missing.

## 2. Rich deck — `deck.json`

```json
{
  "schema_version": "0.2",
  "id": "italian",
  "name": "Italian",
  "description": "Working vocabulary, grammar, and phrases.",
  "language": "it",
  "tags": ["lang:italian"],
  "created_at": "2026-04-30T12:00:00Z",
  "updated_at": "2026-04-30T12:00:00Z",
  "author": { "name": "Dmitry", "agent": "claude-sonnet-4-6" },
  "card_template": {
    "fields": ["front", "back", "example"],
    "front": "{{front}}",
    "back": "**{{back}}**\n\n_{{example}}_"
  },
  "cards": [
    {
      "id": "9f0c1a3e",
      "fields": {
        "front": "ristorante",
        "back": "restaurant",
        "example": "Andiamo al ristorante."
      },
      "category": "vocabulary",
      "tags": ["topic:restaurant", "level:a1"],
      "source": { "kind": "lesson", "id": "italian-eating-out" },
      "media": ["ristorante.mp3"]
    }
  ]
}
```

### Card field reference

| Field | Required | Type | Notes |
|---|---|---|---|
| `id` | yes | string | Stable per card. Auto-derived if omitted (see §6). |
| `fields` | yes | object | Keys MUST match `card_template.fields`. |
| `category` | no | enum-or-string | One of: `vocabulary`, `grammar`, `kanji`, `phrase`, `listening`, `reading`. Custom strings allowed but the Browse UI gives the recommended values primary affordances. |
| `tags` | no | string[] | Free-form. Convention: `namespace:value` (see §3). |
| `source` | no | object | Where the card came from — see §4. |
| `media` | no | string[] | Filenames in the deck's `media/` folder. |

### Deck-level fields

Same as v0.1: `schema_version`, `id`, `name`, `description`, `language`, `tags`, `created_at`, `updated_at`, `author`, `card_template`, `cards`.

## 3. Tag convention

Tags are free-form strings — but agents and users SHOULD use the namespaced form `namespace:value` so the Browse UI can group and filter consistently. Common namespaces:

| Namespace | Examples |
|---|---|
| `topic:` | `topic:restaurant`, `topic:travel`, `topic:business` |
| `level:` | `level:a1`, `level:n5`, `level:beginner` |
| `lang:` | `lang:italian`, `lang:japanese` |
| `book:` | `book:genki1`, `book:nuovo-progetto` |
| `unit:` | `unit:chapter-3` |
| `lesson:` | auto-applied: `lesson:<lesson_id>` |

Bare tags (no namespace) are still legal and treated as topic tags by the search index. Search matches `restaurant` against both `restaurant` and `topic:restaurant`.

## 4. Card source

Every card carries optional provenance so the user can later say "delete everything from this lesson" or "find what came from textbook X". Structured:

```json
{ "kind": "manual" }
{ "kind": "lesson",  "id": "italian-eating-out" }
{ "kind": "agent",   "name": "claude-sonnet-4-6", "prompt_id": "italian-vocab-batch-1" }
{ "kind": "import",  "filename": "anki-export-2024.csv" }
```

`kind` is required when `source` is present. Other fields are advisory.

## 5. Simple deck — `deck.csv`

```csv
front,back,category,tags,source
ristorante,restaurant,vocabulary,topic:restaurant;level:a1,manual
andare,"to go",grammar,verb;level:a1,
```

**Rules:**

- Header required. `front`, `back` mandatory; `category`, `tags`, `source` optional.
- Tags semicolon-separated; namespaced form recommended.
- `source` accepts a kind name (`manual`, `import`) or a `kind:id` short form (`lesson:italian-eating-out`).
- Encoding UTF-8 no BOM. RFC 4180 quoting.
- Deck id/name inferred from folder name. Promote to `deck.json` for media or richer authoring.

## 6. Card-id derivation

If a card omits `id`, the app derives one as the first 16 hex chars of `SHA-256(deck_id + "\n" + canonical(front) + "\n" + canonical(back))` where `canonical(s)` = NFKC-normalise → trim → collapse runs of whitespace to a single space → lowercase. This makes re-imports idempotent: the same deck JSON imported twice produces the same card ids and dedupes naturally.

Agents that want to update an existing card supply the original id explicitly. Manual edits done in-app preserve the original id.

## 7. Lessons — `lesson.json` (card-factory model)

A lesson is a **curriculum** that *produces cards* into a target deck as the user works through it. The lesson file is read-only content; cards live in regular decks and use regular SR.

```json
{
  "schema_version": "0.2",
  "id": "italian-eating-out",
  "name": "Italian — Eating out",
  "description": "Survival vocabulary and dialogues for restaurants.",
  "language": "it",
  "lesson_type": "guided",
  "target_deck": { "id": "italian", "create_if_missing": true },
  "default_card_attrs": {
    "tags": ["topic:restaurant", "level:a1"],
    "source": { "kind": "lesson", "id": "italian-eating-out" }
  },
  "modules": [
    {
      "kind": "vocabulary",
      "title": "Basic words",
      "items": [
        {
          "front": "ristorante",
          "back": "restaurant",
          "category": "vocabulary",
          "intro": "Listen and repeat. *Ristorante* is the everyday word for a sit-down restaurant.",
          "media": ["ristorante.mp3"]
        }
     