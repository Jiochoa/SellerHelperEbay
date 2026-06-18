package com.example.sellerhelperebay.ui.entrylist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sellerhelperebay.R
import com.example.sellerhelperebay.data.db.ItemEntryEntity
import com.example.sellerhelperebay.domain.model.EntryStatus
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    onEntryClick: (Long) -> Unit,
    onSettings: () -> Unit,
    viewModel: EntryListViewModel = viewModel(factory = EntryListViewModel.Factory)
) {
    val entries by viewModel.entries.collectAsState()
    var entryPendingDelete by remember { mutableStateOf<ItemEntryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.entry_list_title)) },
                actions = {
                    TextButton(onClick = onSettings) { Text("Settings") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.createEntry(onEntryClick) }) {
                Text(text = "+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.entry_list_empty),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        onClick = { onEntryClick(entry.id) },
                        onLongClick = { entryPendingDelete = entry }
                    )
                }
            }
        }
    }

    entryPendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            title = { Text("Delete item?") },
            text = { Text("\"${entry.displayTitle}\" and its photos will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEntry(entry.id)
                    entryPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    entry: ItemEntryEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.displayTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    DateFormat.getDateInstance().format(Date(entry.updatedAt)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            AssistChip(onClick = {}, label = { Text(statusLabel(entry.status)) })
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    EntryStatus.NEW.name -> "New"
    EntryStatus.NEEDS_REVIEW.name -> "Review"
    EntryStatus.ANALYZED.name -> "Analyzed"
    EntryStatus.PUSHED.name -> "On eBay"
    else -> status
}
