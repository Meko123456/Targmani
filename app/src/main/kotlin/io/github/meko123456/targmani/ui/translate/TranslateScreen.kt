package io.github.meko123456.targmani.ui.translate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.targmani.domain.Language
import io.github.meko123456.targmani.domain.LanguageCatalog
import io.github.meko123456.targmani.domain.SpeechLocale
import io.github.meko123456.targmani.speech.Speaker
import io.github.meko123456.targmani.targmaniApp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(onOpenModels: () -> Unit = {}, onOpenHistory: () -> Unit = {}, vm: TranslateViewModel? = null) {
    val context = LocalContext.current
    val viewModel: TranslateViewModel = vm ?: viewModel {
        TranslateViewModel(
            context.targmaniApp.translator,
            context.targmaniApp.settings,
            context.targmaniApp.detector,
            context.targmaniApp.history,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // One engine per screen; released when the screen leaves composition.
    val speaker = remember {
        Speaker(context) { language ->
            scope.launch { snackbar.showSnackbar(SpeechLocale.missingVoiceMessage(language)) }
        }
    }
    DisposableEffect(Unit) { onDispose { speaker.close() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Targmani", style = MaterialTheme.typography.titleLarge)
                        Text("თარგმანი", style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.semantics { contentDescription = "History" },
                    ) { Icon(Icons.Default.List, contentDescription = null) }
                    IconButton(
                        onClick = onOpenModels,
                        modifier = Modifier.semantics { contentDescription = "Offline languages" },
                    ) { Icon(Icons.Default.Settings, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LanguageBar(
                source = state.direction.from,
                target = state.direction.to,
                onSource = viewModel::onSourceLanguage,
                onTarget = viewModel::onTargetLanguage,
                onSwap = viewModel::swap,
            )
            if (state.input.isNotBlank()) {
                TextButton(
                    onClick = viewModel::detectSourceLanguage,
                    modifier = Modifier.semantics { contentDescription = "Detect the language of the entered text" },
                ) { Text("Detect language") }
            }
            // Source card
            TranslateCard(
                language = state.direction.from,
                text = state.input,
                rtl = state.direction.sourceRtl,
                editable = true,
                onTextChange = viewModel::onInputChange,
                placeholder = "Enter text",
            )
            StatusLine(state.status)
            // Target card
            TranslateCard(
                language = state.direction.to,
                text = state.output,
                rtl = state.direction.targetRtl,
                editable = false,
                onTextChange = {},
                placeholder = "Translation",
            )
            if (state.output.isNotBlank()) {
                ResultActions(
                    onCopy = {
                        clipboard.setText(AnnotatedString(state.output))
                        scope.launch { snackbar.showSnackbar("Copied") }
                    },
                    onShare = { shareText(context, state.output) },
                    onSpeak = { speaker.speak(state.output, state.direction.to) },
                )
            }
        }
    }
}

@Composable
private fun LanguageBar(
    source: Language,
    target: Language,
    onSource: (Language) -> Unit,
    onTarget: (Language) -> Unit,
    onSwap: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LanguagePicker(source, Modifier.weight(1f), onSource)
        IconButton(onClick = onSwap, modifier = Modifier.semantics { contentDescription = "Swap languages" }) {
            Icon(Icons.Default.Refresh, contentDescription = null)
        }
        LanguagePicker(target, Modifier.weight(1f), onTarget)
    }
}

@Composable
private fun LanguagePicker(selected: Language, modifier: Modifier = Modifier, onSelect: (Language) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected.endonym)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LanguageCatalog.languages.forEach { lang ->
                DropdownMenuItem(
                    text = { Text("${lang.endonym} · ${lang.englishName}") },
                    onClick = { open = false; onSelect(lang) },
                )
            }
        }
    }
}

@Composable
private fun TranslateCard(
    language: Language,
    text: String,
    rtl: Boolean,
    editable: Boolean,
    onTextChange: (String) -> Unit,
    placeholder: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(language.endonym, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            val style = MaterialTheme.typography.bodyLarge.merge(
                TextStyle(textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr),
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                readOnly = !editable,
                textStyle = style,
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)
                    .semantics { contentDescription = "${language.englishName} ${if (editable) "input" else "translation"}" },
            )
        }
    }
}

@Composable
private fun StatusLine(status: TranslateStatus) {
    when (status) {
        TranslateStatus.Idle -> Unit
        TranslateStatus.Downloading -> InlineProgress("Downloading language model…")
        TranslateStatus.Translating -> InlineProgress("Translating…")
        is TranslateStatus.Error -> Text(
            status.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun InlineProgress(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Copy, share and speak the translated text. */
@Composable
private fun ResultActions(onCopy: () -> Unit, onShare: () -> Unit, onSpeak: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = onCopy, modifier = Modifier.semantics { contentDescription = "Copy translation" }) {
            Text("Copy")
        }
        TextButton(onClick = onShare, modifier = Modifier.semantics { contentDescription = "Share translation" }) {
            Icon(Icons.Default.Share, contentDescription = null)
            Text(" Share")
        }
        TextButton(onClick = onSpeak, modifier = Modifier.semantics { contentDescription = "Speak translation aloud" }) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Text(" Speak")
        }
    }
}

/** Hands the translation to the system share sheet. */
private fun shareText(context: android.content.Context, text: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(android.content.Intent.createChooser(intent, null)) }
}
