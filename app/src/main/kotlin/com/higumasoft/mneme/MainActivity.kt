package com.higumasoft.mneme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.higumasoft.mneme.ui.decks.DecksScreen
import com.higumasoft.mneme.ui.study.StudyScreen
import com.higumasoft.mneme.ui.theme.MnemeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MnemeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = Routes.Decks) {
                        composable(Routes.Decks) {
                            DecksScreen(
                                onStudy = { deckId -> nav.navigate("${Routes.Study}/$deckId") }
                            )
                        }
                        composable("${Routes.Study}/{deckId}") { entry ->
                            val deckId = entry.arguments?.getString("deckId") ?: return@composable
                            StudyScreen(deckId = deckId, onDone = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

object Routes {
    const val Decks = "decks"
    const val Study = "study"
}
