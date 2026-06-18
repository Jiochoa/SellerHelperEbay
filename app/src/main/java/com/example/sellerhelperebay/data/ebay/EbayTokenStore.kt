package com.example.sellerhelperebay.data.ebay

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.ebayDataStore by preferencesDataStore(name = "ebay_tokens")

data class StoredTokens(
    val accessToken: String,
    val accessTokenExpiresAt: Long,
    val refreshToken: String?,
    val refreshTokenExpiresAt: Long,
    /** Scopes that were granted at connect time; reused when refreshing. */
    val grantedScope: String?
)

class EbayTokenStore(private val context: Context) {

    suspend fun save(
        response: TokenResponse,
        grantedScope: String,
        now: Long = System.currentTimeMillis()
    ) {
        context.ebayDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = response.accessToken
            prefs[ACCESS_EXPIRES_AT] = now + response.expiresIn * 1000
            prefs[GRANTED_SCOPE] = grantedScope
            response.refreshToken?.let { prefs[REFRESH_TOKEN] = it }
            response.refreshTokenExpiresIn?.let {
                prefs[REFRESH_EXPIRES_AT] = now + it * 1000
            }
        }
    }

    suspend fun get(): StoredTokens? {
        val prefs = context.ebayDataStore.data.first()
        val access = prefs[ACCESS_TOKEN] ?: return null
        return StoredTokens(
            accessToken = access,
            accessTokenExpiresAt = prefs[ACCESS_EXPIRES_AT] ?: 0L,
            refreshToken = prefs[REFRESH_TOKEN],
            refreshTokenExpiresAt = prefs[REFRESH_EXPIRES_AT] ?: 0L,
            grantedScope = prefs[GRANTED_SCOPE]
        )
    }

    fun isConnectedFlow() = context.ebayDataStore.data.map { it[ACCESS_TOKEN] != null }

    /** Whether to request the limited-release sell.item.draft scope at connect time. */
    fun requestDraftScopeFlow(): Flow<Boolean> =
        context.ebayDataStore.data.map { it[REQUEST_DRAFT_SCOPE] ?: true }

    suspend fun getRequestDraftScope(): Boolean =
        context.ebayDataStore.data.first()[REQUEST_DRAFT_SCOPE] ?: true

    suspend fun setRequestDraftScope(enabled: Boolean) {
        context.ebayDataStore.edit { it[REQUEST_DRAFT_SCOPE] = enabled }
    }

    /** Clears tokens only; preserves the scope preference. */
    suspend fun clear() {
        context.ebayDataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(ACCESS_EXPIRES_AT)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(REFRESH_EXPIRES_AT)
            prefs.remove(GRANTED_SCOPE)
        }
    }

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val ACCESS_EXPIRES_AT = longPreferencesKey("access_expires_at")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val REFRESH_EXPIRES_AT = longPreferencesKey("refresh_expires_at")
        private val GRANTED_SCOPE = stringPreferencesKey("granted_scope")
        private val REQUEST_DRAFT_SCOPE = booleanPreferencesKey("request_draft_scope")
    }
}
