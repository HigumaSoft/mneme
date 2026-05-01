package com.higumasoft.mneme.data.repo

import com.higumasoft.mneme.data.drive.DriveSync
import com.higumasoft.mneme.domain.model.Card
import com.higumasoft.mneme.domain.model.Deck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository = single source of truth for the UI. Combines local cache (Room,
 * wired in M2) with the Drive sync layer (wired in M3).
 *
 * v0 implementation is in-memory only so the UI builds and runs.
 */
@Singleton
class DeckRepository @Inject constructor(
    private val driveSync: DriveSync
) {
    private val state = MutableStateFlow<List<Deck>>(emptyList())

    fun observeDecks(): Flow<List<Deck>> = state.asStateFlow()

    suspend fun refresh() {
        if (driveSync.isAuthorized()) {
            // Real path lands in M3. For now just a placeholder.
            // val metas = driveSync.listDecks()
            // ...
        }
    }

    suspend fun cardsFor(deckId: String): List<Card> = emptyList()
}
