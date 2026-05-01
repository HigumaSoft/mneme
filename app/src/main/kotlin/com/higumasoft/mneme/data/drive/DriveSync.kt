package com.higumasoft.mneme.data.drive

import com.higumasoft.mneme.domain.model.Card
import com.higumasoft.mneme.domain.model.Deck

/**
 * Abstraction over the Google Drive sync layer.
 *
 * Why an interface: keeps the repository unit-testable with a fake, and lets
 * us swap the backing implementation (`drive.file` scope vs. full-Drive scope
 * vs. headless server-to-server) without touching callers.
 *
 * Real implementation in M3.
 */
interface DriveSync {

    /** True iff the user is signed in and has granted the Drive scope. */
    suspend fun isAuthorized(): Boolean

    /** Trigger sign-in flow. Caller is responsible for the activity context. */
    suspend fun signIn()

    /** ID of the chosen root folder ("Mneme" by default). null until picked. */
    suspend fun rootFolderId(): String?

    /** Lists every deck folder under the root and returns shallow metadata. */
    suspend fun listDecks(): List<DriveDeckMeta>

    /** Downloads a deck (json or csv) and parses it into domain types. */
    suspend fun fetchDeck(deckId: String): Pair<Deck, List<Card>>

    /** Writes (creates or overwrites) a deck.json into the deck's folder. */
    suspend fun writeDeck(deck: Deck, cards: List<Card>)
}

data class DriveDeckMeta(
    val id: String,
    val folderId: String,
    val format: Format,
    val updatedAtMillis: Long
) {
    enum class Format { JSON, CSV }
}

/** No-op implementation used until M3 lands the real one. Logs would go here. */
class NoOpDriveSync : DriveSync {
    override suspend fun isAuthorized() = false
    override suspend fun signIn() = Unit
    override suspend fun rootFolderId(): String? = null
    override suspend fun listDecks(): List<DriveDeckMeta> = emptyList()
    override suspend fun fetchDeck(deckId: String) = error("Drive not configured")
    override suspend fun writeDeck(deck: Deck, cards: List<Card>) = Unit
}
