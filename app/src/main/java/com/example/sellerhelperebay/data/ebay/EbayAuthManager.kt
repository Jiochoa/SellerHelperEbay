package com.example.sellerhelperebay.data.ebay

import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class EbayAuthManager(
    private val authApi: EbayAuthApi,
    private val tokenStore: EbayTokenStore
) {
    private var pendingState: String? = null

    /** The scope string requested by the most recent buildAuthorizeUrl call. */
    private var pendingScope: String = EbayConfig.scopes(includeDraft = true)

    private val basicAuth: String
        get() = "Basic " + Base64.encodeToString(
            "${EbayConfig.clientId}:${EbayConfig.clientSecret}".toByteArray(),
            Base64.NO_WRAP
        )

    /** Whether the limited-release sell.item.draft scope is requested at connect time. */
    fun requestDraftScopeFlow(): Flow<Boolean> = tokenStore.requestDraftScopeFlow()

    suspend fun setRequestDraftScope(enabled: Boolean) =
        tokenStore.setRequestDraftScope(enabled)

    suspend fun buildAuthorizeUrl(): String {
        val state = UUID.randomUUID().toString()
        pendingState = state
        pendingScope = EbayConfig.scopes(includeDraft = tokenStore.getRequestDraftScope())
        return Uri.parse("${EbayConfig.authBaseUrl}/oauth2/authorize").buildUpon()
            .appendQueryParameter("client_id", EbayConfig.clientId)
            .appendQueryParameter("redirect_uri", EbayConfig.ruName)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", pendingScope)
            .appendQueryParameter("state", state)
            .build()
            .toString()
    }

    /**
     * Inspects a URL the auth WebView is navigating to. Returns the authorization code
     * if this is the consent redirect (with a valid state), null otherwise.
     */
    fun extractAuthCode(url: String): String? {
        val uri = Uri.parse(url)
        val code = runCatching { uri.getQueryParameter("code") }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return null
        val state = runCatching { uri.getQueryParameter("state") }.getOrNull()
        // Reject only on an explicit state mismatch (CSRF guard). eBay may omit state on
        // the redirect to a custom accepted URL, so a missing state is tolerated.
        if (state != null && state != pendingState) return null
        return code
    }

    /** The OAuth `error` code if this URL is an error redirect, else null. */
    fun extractAuthError(url: String): String? =
        runCatching { Uri.parse(url).getQueryParameter("error") }.getOrNull()

    suspend fun exchangeCode(code: String): Boolean {
        val response = authApi.exchangeAuthorizationCode(
            basicAuth = basicAuth,
            code = code,
            redirectUri = EbayConfig.ruName
        )
        val body = response.body()
        if (!response.isSuccessful || body == null) return false
        tokenStore.save(body, grantedScope = pendingScope)
        pendingState = null
        return true
    }

    /**
     * Returns a currently-valid user access token, refreshing if needed.
     * Null means the user has to reconnect their eBay account.
     */
    suspend fun getValidAccessToken(): String? {
        val tokens = tokenStore.get() ?: return null
        val now = System.currentTimeMillis()
        if (now < tokens.accessTokenExpiresAt - EXPIRY_SKEW_MS) return tokens.accessToken

        val refreshToken = tokens.refreshToken ?: return null
        if (tokens.refreshTokenExpiresAt in 1 until now) return null

        // Refresh with the same scopes that were granted, so we never request more than
        // eBay authorized (which would fail).
        val scope = tokens.grantedScope ?: EbayConfig.BASE_SCOPE
        val response = try {
            authApi.refreshAccessToken(
                basicAuth = basicAuth,
                refreshToken = refreshToken,
                scope = scope
            )
        } catch (e: Exception) {
            return null
        }
        val body = response.body()
        if (!response.isSuccessful || body == null) return null
        tokenStore.save(body, grantedScope = scope)
        return body.accessToken
    }

    fun isConnectedFlow() = tokenStore.isConnectedFlow()

    suspend fun disconnect() = tokenStore.clear()

    companion object {
        private const val EXPIRY_SKEW_MS = 5 * 60 * 1000L
    }
}
