package com.example.sellerhelperebay.data.ebay

import com.example.sellerhelperebay.BuildConfig

/**
 * Sandbox/production switch. Everything reads from here; flipping `ebay.env=PROD`
 * in local.properties is the entire switch.
 */
object EbayConfig {
    val isSandbox: Boolean = BuildConfig.EBAY_ENV != "PROD"

    val authBaseUrl: String =
        if (isSandbox) "https://auth.sandbox.ebay.com" else "https://auth.ebay.com"

    val apiBaseUrl: String =
        if (isSandbox) "https://api.sandbox.ebay.com" else "https://api.ebay.com"

    val clientId: String =
        if (isSandbox) BuildConfig.EBAY_SANDBOX_CLIENT_ID else BuildConfig.EBAY_PROD_CLIENT_ID

    val clientSecret: String =
        if (isSandbox) BuildConfig.EBAY_SANDBOX_CLIENT_SECRET else BuildConfig.EBAY_PROD_CLIENT_SECRET

    /** eBay's RuName; used as the redirect_uri value in OAuth calls. */
    val ruName: String =
        if (isSandbox) BuildConfig.EBAY_SANDBOX_RUNAME else BuildConfig.EBAY_PROD_RUNAME

    // Scope strings always use the production host, even against sandbox.
    // BASE_SCOPE is always granted; DRAFT_SCOPE (sell.item.draft) is limited-release and
    // must be approved by eBay — requesting it before approval fails with invalid_scope.
    const val BASE_SCOPE = "https://api.ebay.com/oauth/api_scope"
    const val DRAFT_SCOPE = "https://api.ebay.com/oauth/api_scope/sell.item.draft"

    fun scopes(includeDraft: Boolean): String =
        if (includeDraft) "$BASE_SCOPE $DRAFT_SCOPE" else BASE_SCOPE

    const val MARKETPLACE_ID = "EBAY_US"

    val isConfigured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank() && ruName.isNotBlank()
}
