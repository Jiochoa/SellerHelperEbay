package com.example.sellerhelperebay.data.ebay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("refresh_token_expires_in") val refreshTokenExpiresIn: Long? = null,
    @SerialName("token_type") val tokenType: String = ""
)

@Serializable
data class ItemDraftRequest(
    val categoryId: String? = null,
    val condition: String? = null,
    val format: String? = null,
    val pricingSummary: PricingSummary? = null,
    val product: DraftProduct? = null
)

@Serializable
data class PricingSummary(val price: Amount? = null)

@Serializable
data class Amount(val value: String, val currency: String = "USD")

@Serializable
data class DraftProduct(
    val title: String? = null,
    val description: String? = null,
    val brand: String? = null,
    val aspects: List<DraftAspect>? = null,
    val imageUrls: List<String>? = null
)

@Serializable
data class DraftAspect(val name: String, val values: List<String>)

@Serializable
data class ItemDraftResponse(
    val itemDraftId: String? = null,
    val sellFlowUrl: String? = null,
    val sellFlowNativeUri: String? = null
)
