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
import io.github.meko123456.targmani.ui.models.ModelsScreen
import io.github.meko123456.targmani.ui.theme.TargmaniTheme
import io.github.meko123456.targmani.ui.translate.TranslateScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TargmaniTheme { TargmaniNav() } }
    }
}

/** Two screens, one flag: the translator, and the offline-language manager. */
@Composable
private fun TargmaniNav() {
    var showModels by rememberSaveable { mutableStateOf(false) }
    if (showModels) {
        BackHandler { showModels = false }
        ModelsScreen(onBack = { showModels = false })
    } else {
        TranslateScreen(onOpenModels = { showModels = true })
    }
}
