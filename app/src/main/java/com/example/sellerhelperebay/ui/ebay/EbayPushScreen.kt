package com.example.sellerhelperebay.ui.ebay

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sellerhelperebay.data.ebay.CategoryMap
import com.example.sellerhelperebay.data.ebay.CategoryOption
import com.example.sellerhelperebay.data.ebay.ConditionMap
import com.example.sellerhelperebay.domain.model.FieldKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EbayPushScreen(
    entryId: Long,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    viewModel: EbayPushViewModel = viewModel(factory = EbayPushViewModel.factory(entryId))
) {
    val context = LocalContext.current
    val entry by viewModel.entry.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val pushState by viewModel.pushState.collectAsState()

    val fields = entry?.fields?.filter { !it.value.isNullOrBlank() }.orEmpty()
    val blankKeys = FieldKey.entries.filter { key -> fields.none { it.fieldKey == key.name } }

    var price by remember(entry?.entry?.priceValue) {
        mutableStateOf(entry?.entry?.priceValue.orEmpty())
    }
    val suggestedCondition = fields.firstOrNull { it.fieldKey == FieldKey.CONDITION.name }?.value
    var conditionLabel by remember(suggestedCondition) {
        mutableStateOf(suggestedCondition ?: "")
    }
    val suggested = remember(fields) {
        CategoryMap.suggest(
            department = fields.firstOrNull { it.fieldKey == FieldKey.DEPARTMENT.name }?.value,
            type = fields.firstOrNull { it.fieldKey == FieldKey.TYPE.name }?.value
        )
    }
    var category by remember(suggested) { mutableStateOf<CategoryOption>(suggested) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send to eBay") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val state = pushState) {
                is PushUiState.Success -> {
                    Text("Draft created on eBay!", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Open it on eBay to add photos, review everything, and post it " +
                            "when you're ready. This app never posts for you."
                    )
                    state.sellFlowUrl?.let { url ->
                        Button(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Open in eBay") }
                    }
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Done")
                    }
                }

                else -> {
                    if (!isConnected) {
                        Text("You need to connect your eBay account before pushing a draft.")
                        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                            Text("Connect eBay account")
                        }
                    }

                    Text("What will be sent", style = MaterialTheme.typography.titleMedium)
                    fields.forEach { field ->
                        val key = FieldKey.fromNameOrNull(field.fieldKey)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "${key?.displayName ?: field.fieldKey}:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(field.value.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (blankKeys.isNotEmpty()) {
                        Text(
                            "Left blank (fill on eBay): " +
                                blankKeys.joinToString(", ") { it.displayName },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        "Photos aren't sent with the draft — add them in the eBay app " +
                            "when you finish the listing.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text("Confirm before sending", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (USD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownField(
                        label = "Condition",
                        value = conditionLabel,
                        options = ConditionMap.options.map { it.label },
                        onSelected = { conditionLabel = it }
                    )

                    DropdownField(
                        label = "eBay category",
                        value = category.label,
                        options = CategoryMap.options.map { it.label },
                        onSelected = { label ->
                            category = CategoryMap.options.first { it.label == label }
                        }
                    )

                    (pushState as? PushUiState.Error)?.let { error ->
                        Text(error.message, color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = { viewModel.push(category.id, price, conditionLabel) },
                        enabled = isConnected && pushState != PushUiState.Pushing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (pushState == PushUiState.Pushing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                            Text("Creating draft…")
                        } else {
                            Text("Create draft on eBay")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
