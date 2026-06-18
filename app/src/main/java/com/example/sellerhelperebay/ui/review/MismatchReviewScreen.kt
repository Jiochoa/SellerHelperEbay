package com.example.sellerhelperebay.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MismatchReviewScreen(
    entryId: Long,
    onBack: () -> Unit,
    viewModel: MismatchReviewViewModel = viewModel(factory = MismatchReviewViewModel.factory(entryId))
) {
    val pending by viewModel.pendingPhotos.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review photos") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        if (pending.isEmpty()) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("All photos reviewed!")
                    Button(onClick = onBack) { Text("Back to item") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pending, key = { it.id }) { photo ->
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = viewModel.photoFile(photo.relativePath),
                                contentDescription = "Photo under review",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth().height(260.dp)
                            )
                            Text(
                                "This photo doesn't seem to match the rest:",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                photo.mismatchReason ?: "It looks like a different item.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.deletePhoto(photo.id) }) {
                                    Text("Delete")
                                }
                                OutlinedButton(onClick = { viewModel.markSameItem(photo.id) }) {
                                    Text("Same item")
                                }
                                OutlinedButton(onClick = { viewModel.markPartOfLot(photo.id) }) {
                                    Text("Part of lot")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
