package com.higumasoft.mneme.format

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeckFormatTest {

    @Test
    fun `parses v0_2 deck json with category, namespaced tags, source`() {
        val json = """
        {
          "schema_version": "0.2",
          "id": "italian",
          "name": "Italian",
          "created_at": "2026-04-30T12:00:00Z",
          "updated_at": "2026-04-30T12:00:00Z",
          "card_template": { "fields": ["front", "back"], "front": "{{front}}", "back": "{{back}}" },
          "cards": [
            {
              "id": "c1",
              "fields": { "front": "ristorante", "back": "restaurant" },
              "category": "vocabulary",
              "tags": ["topic:restaurant", "level:a1"],
              "source": { "kind": "lesson", "id": "italian-eating-out" }
            }
          ]
        }
        """.trimIndent()
        val deck = DeckJson.parse(json)
        val (_, cards) = DeckJson.toDomain(deck)
        val c = cards.single()
        assertThat(c.category).isEqualTo("vocabulary")
        assertThat(c.tags).containsExactly("topic:restaurant", "level:a1").inOrder()
        assertThat(c.source?.kind).isEqualTo("lesson")
        assertThat(c.source?.id).isEqualTo("italian-eating-out")
    }

    @Test
    fun `forward-compatible — unknown fields ignored`() {
        val json = """
        {
          "schema_version": "0.2",
          "id": "t", "name": "T",
          "created_at": "2026-04-30T12:00:00Z",
          "updated_at": "2026-04-30T12:00:00Z",
          "future_field": "ignored",
          "card_template": { "fields": ["a"], "front": "{{a}}", "back": "{{a}}" },
          "cards": [
            { "fields": { "a": "x" }, "future_card_field": 42 }
          ]
        }
        """.trimIndent()
        DeckJson.toDomain(DeckJson.parse(json))  // must not throw
    }

    @Test
    fun `card without explicit id gets a deterministic derived id`() {
        val json = """
        {
          "schema_version": "0.2",
          "id": "italian",
          "name": "Italian",
          "created_at": "2026-04-30T12:00:00Z",
          "updated_at": "2026-04-30T12:00:00Z",
          "card_template": { "fields": ["front", "back"], "front": "{{front}}", "back": "{{back}}" },
          "cards": [
            { "fields": { "front": "ristorante", "back": "restaurant" } }
          ]
        }
        """.trimIndent()
        val (_, a) = DeckJson.toDomain(DeckJson.parse(json))
        assertThat(a.single().id).hasLength(16)
        // Idempotent: parsing again yields the same id.
        val (_, b) = DeckJson.toDomain(DeckJson.parse(json))
        assertThat(b.single().id).isEqualTo(a.single().id)
    }

    @Test
    fun `derived id is stable across whitespace and case differences`() {
        val a = CardId.derive("italian", "Ristorante", "Restaurant")
        val b = CardId.derive("italian", "ristorante  ", "  restaurant")
        val c = CardId.derive("italian", " RISTORANTE ", "RESTAURANT")
        assertThat(a).isEqualTo(b)
        assertThat(b).isEqualTo(c)
    }

    @Test
    fun `csv v0_2 parses category, namespaced tags, source short-form`() {
        val csv = """
            front,back,category,tags,source
            ristorante,restaurant,vocabulary,topic:restaurant;level:a1,lesson:italian-eating-out
            andare,"to go",grammar,verb;level:a1,manual
        """.trimIndent()
        val (_, cards) = DeckCsv.parse(csv, deckId = "italian", deckName = "Italian")
        assertThat(cards).hasSize(2)

        val ristorante = cards[0]
        assertThat(ristorante.category).isEqualTo("vocabulary")
        assertThat(ristorante.tags).containsExactly("topic:restaurant", "level:a1").inOrder()
        assertThat(ristorante.source?.kind).isEqualTo("lesson")
        assertThat(ristorante.source?.id).isEqualTo("italian-eating-out")

        val andare = cards[1]
        assertThat(andare.fields["back"]).isEqualTo("to go")
        assertThat(andare.source?.kind).isEqualTo("manual")
    }

    @Test
    fun `csv works with only required columns (back-compat)`() {
        val csv = """
            front,back
            日,sun
        """.trimIndent()
        val (_, cards) = DeckCsv.parse(csv, "jp", "Japanese")
        assertThat(cards).hasSize(1)
        assertThat(cards[0].category).isNull()
        assertThat(cards[0].source).isNull()
    }
}
