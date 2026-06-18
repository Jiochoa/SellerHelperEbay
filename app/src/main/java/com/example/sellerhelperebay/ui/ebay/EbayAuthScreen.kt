package com.example.sellerhelperebay.ui.ebay

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.sellerhelperebay.data.ebay.EbayAuthManager
import com.example.sellerhelperebay.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Browsing : AuthUiState
    data object Exchanging : AuthUiState
    data object Success : AuthUiState
    data class Failed(val message: String) : AuthUiState
}

class EbayAuthViewModel(private val authManager: EbayAuthManager) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Browsing)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    // Built asynchronously because it reads the persisted scope preference.
    private val _authorizeUrl = MutableStateFlow<String?>(null)
    val authorizeUrl: StateFlow<String?> = _authorizeUrl.asStateFlow()

    init {
        viewModelScope.launch {
            _authorizeUrl.value = authManager.buildAuthorizeUrl()
        }
    }

    /** True if this URL carries our authorization code (used by shouldInterceptRequest). */
    fun containsAuthCode(url: String): Boolean = authManager.extractAuthCode(url) != null

    private fun messageForAuthError(error: String): String = when (error) {
        "invalid_scope" ->
            "eBay hasn't granted this app the \"sell.item.draft\" scope. You need Sell " +
                "Listing API access approved by eBay for this production app before you " +
                "can connect. (See EBAY_SETUP.md.)"
        "access_denied", "consent_declined" -> "eBay sign-in was cancelled."
        else -> "eBay sign-in failed ($error)."
    }

    /** Returns true when the WebView should stop loading (we took over). */
    fun onNavigation(url: String): Boolean {
        logNavigation(url)
        if (_state.value != AuthUiState.Browsing) return true
        val code = authManager.extractAuthCode(url)
        if (code != null) {
            _state.value = AuthUiState.Exchanging
            viewModelScope.launch {
                val ok = try {
                    authManager.exchangeCode(code)
                } catch (e: Exception) {
                    false
                }
                _state.value = if (ok) AuthUiState.Success
                else AuthUiState.Failed("Could not finish connecting. Try again.")
            }
            return true
        }
        val error = authManager.extractAuthError(url)
        if (error != null) {
            _state.value = AuthUiState.Failed(messageForAuthError(error))
            return true
        }
        return false
    }

    /** Debug-only: logs the full navigated URL so we can trace the OAuth redirect chain. */
    private fun logNavigation(url: String) {
        if (!com.example.sellerhelperebay.BuildConfig.DEBUG) return
        android.util.Log.d("EbayAuth", "nav -> $url")
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { EbayAuthViewModel(appContainer().ebayAuthManager) }
        }
    }
}

/**
 * In-app WebView OAuth. eBay only allows https redirect URIs tied to a RuName, and the
 * official App Links route needs a hosted assetlinks.json; intercepting the consent
 * redirect in a WebView needs no hosting. Upgrade path if this app is ever distributed:
 * Custom Tabs + App Links with assetlinks.json (e.g. on GitHub Pages).
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EbayAuthScreen(
    onDone: () -> Unit,
    viewModel: EbayAuthViewModel = viewModel(factory = EbayAuthViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    val authorizeUrl by viewModel.authorizeUrl.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect eBay") },
                navigationIcon = { TextButton(onClick = onDone) { Text("Cancel") } }
            )
        }
    ) { innerPadding ->
        when (state) {
            AuthUiState.Browsing -> if (authorizeUrl == null) {
                CenteredText(innerPadding, "Loading…")
            } else AndroidView(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            // shouldInterceptRequest fires for every request hop, including
                            // eBay's redirect to the accepted URL *with* the code, before the
                            // accepted-URL's own host can redirect and strip the query. This
                            // is the reliable capture point; the page-level callbacks below
                            // are backups for setups where the accepted URL passes the code
                            // straight through.
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): android.webkit.WebResourceResponse? {
                                val req = request ?: return null
                                val url = req.url?.toString() ?: return null
                                if (req.isForMainFrame && viewModel.containsAuthCode(url)) {
                                    viewModel.onNavigation(url)
                                    // Halt the redirect chain with an empty response; the UI
                                    // has already switched to the "finishing" state.
                                    return android.webkit.WebResourceResponse(
                                        "text/plain", "utf-8",
                                        java.io.ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                                return null
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                return viewModel.onNavigation(url)
                            }

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                if (url != null && viewModel.onNavigation(url)) {
                                    view?.stopLoading()
                                } else {
                                    super.onPageStarted(view, url, favicon)
                                }
                            }

                            override fun doUpdateVisitedHistory(
                                view: WebView?,
                                url: String?,
                                isReload: Boolean
                            ) {
                                if (url != null && viewModel.onNavigation(url)) {
                                    view?.stopLoading()
                                } else {
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                if (com.example.sellerhelperebay.BuildConfig.DEBUG) {
                                    android.util.Log.d(
                                        "EbayAuth",
                                        "error on ${request?.url} : ${error?.errorCode} ${error?.description}"
                                    )
                                }
                                super.onReceivedError(view, request, error)
                            }
                        }
                        loadUrl(authorizeUrl!!)
                    }
                }
            )

            AuthUiState.Exchanging -> CenteredText(innerPadding, "Finishing connection…")

            AuthUiState.Success -> {
                CenteredText(innerPadding, "Connected!")
                androidx.compose.runtime.LaunchedEffect(Unit) { onDone() }
            }

            is AuthUiState.Failed -> CenteredText(
                innerPadding,
                (state as AuthUiState.Failed).message
            )
        }
    }
}

@Composable
private fun CenteredText(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    text: String
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.padding(innerPadding).fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) { Text(text) }
}
