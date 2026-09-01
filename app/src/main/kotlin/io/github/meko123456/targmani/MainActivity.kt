package io.github.meko123456.targmani

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.targmani.data.directionOrNull
import io.github.meko123456.targmani.ui.history.HistoryScreen
import io.github.meko123456.targmani.ui.models.ModelsScreen
import io.github.meko123456.targmani.ui.theme.TargmaniTheme
import io.github.meko123456.targmani.ui.translate.TranslateScreen
import io.github.meko123456.targmani.ui.translate.TranslateViewModel

private enum class Screen { Translate, Models, History }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TargmaniTheme { TargmaniNav() } }
    }
}

/**
 * Three screens over one shared [TranslateViewModel] — sharing it is what lets a history entry
 * load straight back into the editor when the user taps it.
 */
@Composable
private fun TargmaniNav() {
    val context = LocalContext.current
    val app = context.targmaniApp
    val translateVm: TranslateViewModel = viewModel {
        TranslateViewModel(app.translator, app.settings, app.detector, app.history)
    }
    var screen by rememberSaveable { mutableStateOf(Screen.Translate) }

    when (screen) {
        Screen.Translate -> TranslateScreen(
            onOpenModels = { screen = Screen.Models },
            onOpenHistory = { screen = Screen.History },
            vm = translateVm,
        )
        Screen.Models -> {
            BackHandler { screen = Screen.Translate }
            ModelsScreen(onBack = { screen = Screen.Translate })
        }
        Screen.History -> {
            BackHandler { screen = Screen.Translate }
            HistoryScreen(
                onBack = { screen = Screen.Translate },
                onReuse = { record ->
                    record.directionOrNull()?.let { direction ->
                        translateVm.loadFromHistory(record.sourceText, record.translatedText, direction)
                    }
                    screen = Screen.Translate
                },
            )
        }
    }
}
