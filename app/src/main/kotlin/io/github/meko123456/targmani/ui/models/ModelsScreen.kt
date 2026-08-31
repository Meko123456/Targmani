package io.github.meko123456.targmani.ui.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.targmani.domain.Language
import io.github.meko123456.targmani.domain.ModelPlanner
import io.github.meko123456.targmani.domain.ModelState
import io.github.meko123456.targmani.domain.ModelStatus
import io.github.meko123456.targmani.targmaniApp

/** Per-language model management: what's available offline, and download/delete for each. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: ModelsViewModel = viewModel { ModelsViewModel(context.targmaniApp.modelStore, context.targmaniApp.settings) }
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmDelete: Language? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline languages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Downloaded languages translate with no internet. English is always needed — " +
                    "every translation routes through it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.rows.forEach { row ->
                ModelRow(
                    status = row,
                    onDownload = { vm.download(row.language) },
                    onDelete = {
                        if (ModelPlanner.deletingBreaksAllPairs(row.language)) confirmDelete = row.language
                        else vm.delete(row.language)
                    },
                )
            }

            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Download over Wi-Fi only", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Language models are tens of megabytes each.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.wifiOnly,
                        onCheckedChange = vm::setWifiOnly,
                        modifier = Modifier.semantics { contentDescription = "Download language models over Wi-Fi only" },
                    )
                }
            }

            state.error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = vm::dismissError) { Text("Dismiss") }
            }
        }
    }

    confirmDelete?.let { language ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${language.englishName}?") },
            text = {
                Text(
                    "Every translation routes through English, so deleting it turns off offline " +
                        "translation for all languages until you download it again.",
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.delete(language); confirmDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ModelRow(status: ModelStatus, onDownload: () -> Unit, onDelete: () -> Unit) {
    val stateLabel = when (status.state) {
        ModelState.DOWNLOADED -> "Available offline"
        ModelState.NOT_DOWNLOADED -> "Not downloaded"
        ModelState.DOWNLOADING -> "Downloading…"
        ModelState.DELETING -> "Deleting…"
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp)
                .semantics { contentDescription = "${status.language.englishName}: $stateLabel" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("${status.language.endonym} · ${status.language.englishName}", style = MaterialTheme.typography.titleMedium)
                Text(stateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                status.isBusy -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                status.isReady -> TextButton(onClick = onDelete) { Text("Delete") }
                else -> TextButton(onClick = onDownload) { Text("Download") }
            }
        }
    }
}
