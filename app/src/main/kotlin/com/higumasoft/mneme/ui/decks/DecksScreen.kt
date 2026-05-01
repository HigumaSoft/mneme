package com.higumasoft.mneme.ui.decks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.higumasoft.mneme.R
import com.higumasoft.mneme.domain.model.Deck

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(
    onStudy: (String) -> Unit,
    decks: List<Deck> = sampleDecks
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.decks_title)) }) }
    ) { padding ->
        if (decks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.decks_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(decks, key = { it.id }) { deck ->
                    DeckRow(deck = deck, onClick = { onStudy(deck.id) })
                }
            }
        }
    }
}

@Composable
private fun DeckRow(deck: Deck, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(deck.name, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("${deck.cardCount} cards · ${deck.dueToday} due", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}

// Stand-in until Drive sync + Repository are wired up.
private val sampleDecks = listOf(
    Deck(id = "kanji-n5", name = "Kanji N5", cardCount = 80, dueToday = 12),
    Deck(id = "spanish-verbs", name = "Spanish verbs", cardCount = 120, dueToday = 0)
)
