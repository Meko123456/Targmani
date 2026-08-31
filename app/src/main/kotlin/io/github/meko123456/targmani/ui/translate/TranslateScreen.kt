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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.targmani.domain.Language
import io.github.meko123456.targmani.domain.LanguageCatalog
import io.github.meko123456.targmani.targmaniApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen() {
    val context = LocalContext.current
    val vm: TranslateViewModel = viewModel { TranslateViewModel(context.targmaniApp.translator) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Targmani", style = MaterialTheme.typography.titleLarge)
                    Text("თარგმანი", style = MaterialTheme.typography.labelMedium)
                }
            })
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LanguageBar(
                source = state.direction.from,
                target = state.direction.to,
                onSource = vm::onSourceLanguage,
                onTarget = vm::onTargetLanguage,
                onSwap = vm::swap,
            )
            // Source card
            TranslateCard(
                language = state.direction.from,
                text = state.input,
                rtl = state.direction.sourceRtl,
                editable = true,
                onTextChange = vm::onInputChange,
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
