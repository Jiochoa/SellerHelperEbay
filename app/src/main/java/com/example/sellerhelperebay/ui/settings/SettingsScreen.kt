package com.example.sellerhelperebay.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.sellerhelperebay.data.ebay.EbayAuthManager
import com.example.sellerhelperebay.data.ebay.EbayConfig
import com.example.sellerhelperebay.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val authManager: EbayAuthManager) : ViewModel() {

    val isConnected: StateFlow<Boolean> = authManager.isConnectedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val requestDraftScope: StateFlow<Boolean> = authManager.requestDraftScopeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setRequestDraftScope(enabled: Boolean) {
        viewModelScope.launch { authManager.setRequestDraftScope(enabled) }
    }

    fun disconnect() {
        viewModelScope.launch { authManager.disconnect() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(appContainer().ebayAuthManager) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onConnect: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val requestDraftScope by viewModel.requestDraftScope.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("eBay account", style = MaterialTheme.typography.titleMedium)
            Text(
                if (EbayConfig.isSandbox) "Environment: Sandbox (test)" else "Environment: Production",
                style = MaterialTheme.typography.bodySmall
            )
            if (!EbayConfig.isConfigured) {
                Text(
                    "eBay API keys are missing. Add ebay.sandbox.clientId, " +
                        "ebay.sandbox.clientSecret and ebay.sandbox.ruName to " +
                        "local.properties, then rebuild.",
                    color = MaterialTheme.colorScheme.error
                )
            } else if (isConnected) {
                Text("Connected to eBay.")
                OutlinedButton(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Disconnect") }
            } else {
                Text("Not connected. Connect your eBay account to push drafts.")
                Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                    Text("Connect eBay account")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Request draft permission")
                    Text(
                        "Includes the sell.item.draft scope. Turn OFF to connect with " +
                            "basic access only — useful to test sign-in before eBay grants " +
                            "Listing API access. Disconnect and reconnect to apply a change.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = requestDraftScope,
                    onCheckedChange = { viewModel.setRequestDraftScope(it) }
                )
            }
        }
    }
}
