package com.higumasoft.mneme.format

import com.higumasoft.mneme.domain.model.Card
import com.higumasoft.mneme.domain.model.CardSource
import com.higumasoft.mneme.domain.model.Deck
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.text.Normalizer

/**
 * Wire format for deck.json — see docs/DECK_FORMAT.md v0.2.
 *
 * Forward-compatible: unknown fields are ignored. Unknown lesson module
 * kinds are skipped at the lesson layer (not here).
 */
@Serializable
data class DeckFile(
    @SerialName("schema_version") val schemaVersion: String,
    val id: String,
    val name: String,
    val description: String? = null,
    val language: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val author: AuthorBlock? = null,
    @SerialName("card_template") val cardTemplate: CardTemplate,
    val cards: List<CardEntry>
)

@Serializable
data class AuthorBlock(
    val name: String? = null,
    val agent: String? = null
)

@Serializable
data class CardTemplate(
    val fields: List<String>,
    val front: String,
    val back: String
)

@Serializable
data class CardEntry(
    /** Optional in v0.2 — derived deterministically when absent (see CardId). */
    val id: String? = null,
    val fields: Map<String, String>,
    /** v0.2: vocabulary | grammar | kanji | phrase | listening | reading | <custom> */
    val category: String? = null,
    val tags: List<String> = emptyList(),
    /** v0.2: provenance — see DECK_FORMAT.md §4 */
    val source: SourceBlock? = null,
    val media: List<String> = emptyList()
)

@Serializable
data class SourceBlock(
    val kind: String,
    val id: String? = null,
    val name: String? = null,
    val filename: String? = null,
    @SerialName("prompt_id") val promptId: String? = null
) {
    fun toDomain() = CardSource(kind, id, name, filename, promptId)
}

object DeckJson {
    val codec: Json = Json {
        ignoreUnknownKeys = true       // v0.1 readers tolerate v0.2, etc.
        prettyPrint = true
        encodeDefaults = false
    }

    fun parse(text: String): DeckFile = codec.decodeFromString(DeckFile.serializer(), text)
    fun serialize(deck: DeckFile): String = codec.encodeToString(DeckFile.serializer(), deck)

    fun toDomain(file: DeckFile): Pair<Deck, List<Card>> {
        val deck = Deck(
            id = file.id,
            name = file.name,
            description = file.description,
            language = file.language,
            tags = file.tags,
            cardCount = file.cards.size
        )
        val firstField = file.cardTemplate.fields.firstOrNull() ?: "front"
        val secondField = file.cardTemplate.fields.getOrNull(1) ?: "back"
        val cards = file.cards.map { entry ->
            val front = entry.fields["front"] ?: entry.fields[firstField] ?: ""
            val back = entry.fields["back"] ?: entry.fields[secondField] ?: ""
            Card(
                id = entry.id ?: CardId.derive(file.id, front, back),
                deckId = file.id,
                fields = entry.fields,
                category = entry.category,
                tags = entry.tags,
                source = entry.source?.toDomain(),
                media = entry.media
            )
        }
        return deck to cards
    }
}

/**
 * Deterministic card-id derivation per DECK_FORMAT.md §6.
 * SHA-256(deck_id + "\n" + canonical(front) + "\n" + canonical(back)) → first 16 hex chars.
 * canonical() = NFKC-normalise → trim → collapse whitespace → lowercase.
 */
object CardId {
    fun derive(deckId: String, front: String, back: String): String {
        val payload = "$deckId\n${canonical(front)}\n${canonical(back)}"
        val hash = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return hash.take(8).joinToString("") { "%02x".format(it) }
    }

    internal fun canonical(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFKC)
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
}

/**
 * Simple deck — `deck.csv` v0.2.
 * Columns: front, back, category?, tags?, source?
 * Tags semicolon-separated. Source is either a kind ("manual", "import")
 * or "kind:id" short form ("lesson:italian-eating-out").
 */
object DeckCsv {
    fun parse(text: String, deckId: String, deckName: String): Pair<Deck, List<Card>> {
        val rows = SimpleCsv.parse(text)
        require(rows.isNotEmpty()) { "Empty CSV" }
        val header = rows.first().map { it.lowercase().trim() }
        val frontIdx = header.indexOf("front").also { require(it >= 0) { "Missing 'front' column" } }
        val backIdx = header.indexOf("back").also { require(it >= 0) { "Missing 'back' column" } }
        val categoryIdx = header.indexOf("category")
        val tagsIdx = header.indexOf("tags")
        val sourceIdx = header.indexOf("source")

        val cards = rows.drop(1).mapNotNull { row ->
            if (row.size <= maxOf(frontIdx, backIdx)) return@mapNotNull null
            val front = row[frontIdx]
            val back = row[backIdx]
            Card(
                id = CardId.derive(deckId, front, back),
                deckId = deckId,
                fields = mapOf("front" to front, "back" to back),
                category = if (categoryIdx >= 0 && row.size > categoryIdx && row[categoryIdx].isNotBlank()) row[categoryIdx] else null,
                tags = if (tagsIdx >= 0 && row.size > tagsIdx)
                    row[tagsIdx].split(";").map { it.trim() }.filter { it.isNotBlank() }
                else emptyList(),
                source = if (sourceIdx >= 0 && row.size > sourceIdx && row[sourceIdx].isNotBlank())
                    parseSourceShort(row[sourceIdx])
                else null
            )
        }
        val deck = Deck(id = deckId, name = deckName, cardCount = cards.size)
        return deck to cards
    }

    private fun parseSourceShort(text: String): CardSource {
        val t = text.trim()
        val parts = t.split(":", limit = 2)
        return if (parts.size == 2) CardSource(kind = parts[0], id = parts[1])
        else CardSource(kind = t)
    }
}

/** RFC-4180-ish CSV reader. Handles quoted fields, embedded commas/quotes, CRLF. */
internal object SimpleCsv {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val s = text.replace("\r\n", "\n").replace('\r', '\n')
        var i = 0
        while (i < s.length) {
            val (row, next) = parseRow(s, i)
            if (row.isNotEmpty() && !(row.size == 1 && row[0].isEmpty())) rows.add(row)
            i = next
        }
        return rows
    }

    private fun parseRow(s: String, start: Int): Pair<List<String>, Int> {
        val cells = mutableListOf<String>()
        val cell = StringBuilder()
        var i = start
        var quoted = false
        while (i < s.length) {
            val c = s[i]
            when {
                quoted -> when {
                    c == '"' && i + 1 < s.length && s[i + 1] == '"' -> { cell.append('"'); i += 2 }
                    c == '"' -> { quoted = false; i++ }
                    else -> { cell.append(c); i++ }
                }
                c == '"' && cell.isEmpty() -> { quoted = true; i++ }
                c == ',' -> { cells.add(cell.toString()); cell.clear(); i++ }
                c == '\n' -> { cells.add(cell.toString()); return cells to (i + 1) }
                else -> { cell.append(c); i++ }
            }
        }
        cells.add(cell.toString())
        return cells to i
    }
}
