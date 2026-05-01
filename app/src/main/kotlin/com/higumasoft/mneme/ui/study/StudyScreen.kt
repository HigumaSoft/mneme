package com.higumasoft.mneme.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.higumasoft.mneme.R

/**
 * Skeleton study screen — wired to a real ViewModel + SR engine in M2.
 * For now it just shows the deckId and lets the user "rate" a card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    deckId: String,
    onDone: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study: $deckId") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Text("←") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("(card front placeholder)")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {}) { Text(stringResource(R.string.study_again)) }
                Button(onClick = {}) { Text(stringResource(R.string.study_hard)) }
                Button(onClick = {}) { Text(stringResource(R.string.study_good)) }
                Button(onClick = {}) { Text(stringResource(R.string.study_easy)) }
            }
        }
    }
}
