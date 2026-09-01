package io.github.meko123456.targmani.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.targmani.data.TranslationRecord
import io.github.meko123456.targmani.data.directionOrNull
import io.github.meko123456.targmani.targmaniApp

/** Recent translations: star to keep, tap to reuse, swipe-free delete, clear-all with a confirm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, onReuse: (TranslationRecord) -> Unit) {
    val context = LocalContext.current
    val vm: HistoryViewModel = viewModel { HistoryViewModel(context.targmaniApp.history) }
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.entries.isNotEmpty()) {
                        IconButton(
                            onClick = { confirmClear = true },
                            modifier = Modifier.semantics { contentDescription = "Clear history" },
                        ) { Icon(Icons.Default.Delete, contentDescription = null) }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryFilter.entries.forEach { option ->
                    FilterChip(
                        selected = state.filter == option,
                        onClick = { vm.setFilter(option) },
                        label = { Text(if (option == HistoryFilter.ALL) "All" else "Starred") },
                    )
                }
            }

            if (state.entries.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (state.filter == HistoryFilter.FAVOURITES) "Nothing starred yet" else "No translations yet",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Translations you make are saved here automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.entries, key = { it.id }) { record ->
                        HistoryRow(
                            record = record,
                            onReuse = { onReuse(record) },
                            onToggleFavourite = { vm.toggleFavourite(record) },
                            onDelete = { vm.delete(record) },
                        )
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This removes your recent translations. Starred ones are kept.") },
            confirmButton = { TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun HistoryRow(
    record: TranslationRecord,
    onReuse: () -> Unit,
    onToggleFavourite: () -> Unit,
    onDelete: () -> Unit,
) {
    val direction = record.directionOrNull()
    val pair = direction?.let { "${it.from.endonym} → ${it.to.endonym}" } ?: "${record.fromCode} → ${record.toCode}"
    Card(
        Modifier.fillMaxWidth()
            .clickable(onClickLabel = "Reuse this translation", onClick = onReuse)
            .semantics { contentDescription = "$pair. ${record.sourceText}. ${record.translatedText}" },
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pair, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(record.sourceText, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    record.translatedText,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier.semantics {
                    contentDescription = if (record.favourite) "Remove from starred" else "Add to starred"
                },
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = if (record.favourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.semantics { contentDescription = "Delete this entry" }) {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        }
    }
}
