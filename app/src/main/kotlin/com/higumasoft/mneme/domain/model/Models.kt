package com.higumasoft.mneme.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain-level model. Decoupled from file format and Room entities.
 * Mirrors DECK_FORMAT.md v0.2.
 */

@Serializable
data class Deck(
    val id: String,
    val name: String,
    val description: String? = null,
    val language: String? = null,
    val tags: List<String> = emptyList(),
    val cardCount: Int = 0,
    val dueToday: Int = 0
)

@Serializable
data class Card(
    val id: String,
    val deckId: String,
    val fields: Map<String, String>,
    /** Recommended values: vocabulary, grammar, kanji, phrase, listening, reading. Custom strings allowed. */
    val category: String? = null,
    /** Free-form, namespaced convention encouraged: "topic:restaurant", "level:a1". */
    val tags: List<String> = emptyList(),
    val source: CardSource? = null,
    val media: List<String> = emptyList()
)

@Serializable
data class CardSource(
    /** "manual" | "lesson" | "agent" | "import" */
    val kind: String,
    val id: String? = null,
    val name: String? = null,
    val filename: String? = null,
    @Suppress("PropertyName") val prompt_id: String? = null
) {
    companion object {
        val Manual = CardSource(kind = "manual")
        fun lesson(id: String) = CardSource(kind = "lesson", id = id)
        fun agent(name: String, promptId: String? = null) =
            CardSource(kind = "agent", name = name, prompt_id = promptId)
        fun import(filename: String) = CardSource(kind = "import", filename = filename)
    }
}

/** Per-card scheduling state. Local-only in v1.0. */
data class CardSchedule(
    val cardId: String,
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueEpochDay: Long = 0
)

/** SM-2 user-facing rating. Mapped to a quality score by the SR engine. */
enum class Rating(val quality: Int) {
    Again(0), Hard(3), Good(4), Easy(5)
}

/**
 * Saved query — the studyable unit per charter §6/§7. See DECK_FORMAT.md §8.
 */
@Serializable
data class Slice(
    val id: String,
    val name: String,
    val filter: SliceFilter = SliceFilter(),
    val order: SliceOrder = SliceOrder.DueFirst,
    val createdAt: String? = null
)

@Serializable
data class SliceFilter(
    val decks: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val tagsAll: List<String> = emptyList(),
    val tagsAny: List<String> = emptyList(),
    val tagsNone: List<String> = emptyList(),
    val sourceKinds: List<String> = emptyList(),
    val dueState: DueState = DueState.Any,
    val text: String? = null,
    val maxCards: Int? = null
)

enum class DueState { Due, New, Learned, Any }
enum class SliceOrder { DueFirst, Random, OldestFirst, NewestFirst }
