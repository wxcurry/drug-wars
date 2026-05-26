package com.neoncartel.drugwars

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neoncartel.drugwars.app.GameViewModel
import com.neoncartel.drugwars.audio.GameAudio
import com.neoncartel.drugwars.ui.DrugWarsApp
import com.neoncartel.drugwars.ui.theme.DrugWarsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrugWarsTheme {
                val context = LocalContext.current
                val app = context.applicationContext as DrugWarsApplication
                val audio = remember { GameAudio(context) }
                DisposableEffect(Unit) {
                    onDispose { audio.release() }
                }
                val viewModel: GameViewModel = viewModel(
                    factory = GameViewModel.Factory(
                        engine = app.container.engine,
                        repository = app.container.saveRepository,
                    ),
                )
                DrugWarsApp(viewModel = viewModel, audio = audio)
            }
        }
    }
}
