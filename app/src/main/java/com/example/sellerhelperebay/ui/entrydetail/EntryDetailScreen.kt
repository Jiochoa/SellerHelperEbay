package com.example.sellerhelperebay.ui.entrydetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.sellerhelperebay.data.db.PhotoEntity
import com.example.sellerhelperebay.domain.model.FieldKey
import com.example.sellerhelperebay.domain.model.PhotoMatchStatus
import com.example.sellerhelperebay.domain.model.Provenance
import com.example.sellerhelperebay.domain.model.confidenceLevel
import com.example.sellerhelperebay.ui.components.ConfidenceDot

private val dropdownOptions: Map<FieldKey, List<String>> = mapOf(
    FieldKey.CONDITION to listOf(
        "New with tags", "New without tags", "Used - Excellent", "Used - Good", "Used - Fair"
    ),
    FieldKey.DEPARTMENT to listOf("Men", "Women", "Unisex Adult", "Boys", "Girls"),
    FieldKey.SIZE_TYPE to listOf("Regular", "Petite", "Plus", "Tall", "Big & Tall", "Maternity")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    entryId: Long,
    onBack: () -> Unit,
    onReviewPhotos: () -> Unit,
    onPushToEbay: () -> Unit,
    viewModel: EntryDetailViewModel = viewModel(factory = EntryDetailViewModel.factory(entryId))
) {
    val entry by viewModel.entry.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(userMessage) {
        userMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeUserMessage()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> viewModel.addPhotos(uris) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> viewModel.onCameraResult(success) }

    var photoPendingDelete by remember { mutableStateOf<PhotoEntity?>(null) }
    var evidenceField by remember { mutableStateOf<FieldKey?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry?.entry?.displayTitle ?: "Item") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { innerPadding ->
        val details = entry
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val pendingCount = details?.photos
                ?.count { it.matchStatus == PhotoMatchStatus.MISMATCH_PENDING.name } ?: 0
            if (pendingCount > 0) {
                Card(
                    onClick = onReviewPhotos,
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "$pendingCount photo${if (pendingCount == 1) "" else "s"} may show a different item — tap to review",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Text("Photos", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(details?.photos.orEmpty(), key = { it.id }) { photo ->
                    PhotoThumb(
                        photo = photo,
                        fileProvider = { viewModel.photoFile(it) },
                        onLongClick = { photoPendingDelete = photo }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) { Text("Add from gallery") }
                OutlinedButton(onClick = {
                    cameraLauncher.launch(viewModel.newCameraUri())
                }) { Text("Take photo") }
            }

            Button(
                onClick = { viewModel.analyze() },
                enabled = !details?.photos.isNullOrEmpty() &&
                    analysisState != AnalysisUiState.Running,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (analysisState == AnalysisUiState.Running) {
                    CircularProgressIndicator(
                        modifier = Modifier.requiredSize(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text("  Analyzing…")
                } else {
                    Text("Analyze photos")
                }
            }

            (analysisState as? AnalysisUiState.Error)?.let { error ->
                Text(
                    error.message,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            }

            Text("Details", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)

            FieldKey.entries.forEach { key ->
                val field = details?.fields?.firstOrNull { it.fieldKey == key.name }
                FieldRow(
                    key = key,
                    dbValue = field?.value.orEmpty(),
                    provenance = field?.provenance?.let { p ->
                        Provenance.entries.firstOrNull { it.name == p }
                    },
                    confidence = field?.confidence,
                    onCommit = { viewModel.setField(key, it) },
                    onDotClick = { evidenceField = key }
                )
            }

            val hasAnyField = details?.fields?.any { !it.value.isNullOrBlank() } == true
            Button(
                onClick = onPushToEbay,
                enabled = hasAnyField && pendingCount == 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Send to eBay as a draft") }
        }
    }

    photoPendingDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoPendingDelete = null },
            title = { Text("Remove photo?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removePhoto(photo.id)
                    photoPendingDelete = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { photoPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    evidenceField?.let { key ->
        val field = entry?.fields?.firstOrNull { it.fieldKey == key.name }
        AlertDialog(
            onDismissRequest = { evidenceField = null },
            title = { Text(key.displayName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        field == null || field.value.isNullOrBlank() ->
                            Text("Not filled in yet. The analysis fills this only when the photos show evidence for it.")
                        field.provenance == Provenance.MANUAL.name ->
                            Text("Entered by you.")
                        field.provenance == Provenance.WEB.name ->
                            Text("Filled from a web search of similar items, not from your photos.")
                        else -> {
                            Text("AI confidence: ${field.confidence ?: 0}%")
                            Text(field.evidence ?: "No evidence details recorded.")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { evidenceField = null }) { Text("Close") }
            }
        )
    }

    (analysisState as? AnalysisUiState.Done)?.let { done ->
        if (done.blankFields.isEmpty()) {
            androidx.compose.runtime.LaunchedEffect(done) {
                viewModel.dismissAnalysisState()
            }
        } else {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAnalysisState() },
                title = { Text("Some details are still missing") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Couldn't find: " +
                                done.blankFields.joinToString(", ") { it.displayName }
                        )
                        Text("You can fill them in yourself, or add more photos (like the tag or label) and analyze again.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissAnalysisState() }) {
                        Text("Fill manually")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { viewModel.dismissAnalysisState() }) {
                            Text("Add more photos")
                        }
                        TextButton(onClick = {}, enabled = false) {
                            Text("Web search (soon)")
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoThumb(
    photo: PhotoEntity,
    fileProvider: (String) -> java.io.File,
    onLongClick: () -> Unit
) {
    val flagged = photo.matchStatus == PhotoMatchStatus.MISMATCH_PENDING.name
    AsyncImage(
        model = fileProvider(photo.relativePath),
        contentDescription = "Item photo",
        modifier = Modifier
            .requiredSize(120.dp)
            .then(
                if (flagged) Modifier.border(
                    BorderStroke(
                        3.dp,
                        androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                ) else Modifier
            )
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldRow(
    key: FieldKey,
    dbValue: String,
    provenance: Provenance?,
    confidence: Int?,
    onCommit: (String) -> Unit,
    onDotClick: () -> Unit
) {
    val options = dropdownOptions[key]
    var text by remember(dbValue) { mutableStateOf(dbValue) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (options != null) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(key.displayName) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("(none)") },
                        onClick = {
                            text = ""
                            onCommit("")
                            expanded = false
                        }
                    )
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                text = option
                                onCommit(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(key.displayName) },
                singleLine = key != FieldKey.DESCRIPTION,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { state ->
                        if (!state.isFocused && text != dbValue) onCommit(text)
                    }
            )
        }
        ConfidenceDot(
            level = confidenceLevel(provenance, confidence),
            modifier = Modifier.clickable(onClick = onDotClick)
        )
    }
}
