package com.example.sellerhelperebay.data.ebay

import com.example.sellerhelperebay.data.db.AppDatabase
import com.example.sellerhelperebay.domain.model.EntryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed class EbayException(message: String) : Exception(message) {
    class NotConnected : EbayException("Connect your eBay account first.")
    class NotConfigured : EbayException(
        "eBay API keys are missing. Add them to local.properties and rebuild."
    )
    class Api(message: String) : EbayException(message)
    class Network : EbayException("Could not reach eBay. Check your connection.")
}

@Serializable
private data class EbayErrorBody(val errors: List<EbayError> = emptyList())

@Serializable
private data class EbayError(val message: String? = null, val longMessage: String? = null)

class EbayRepository(
    private val db: AppDatabase,
    private val listingApi: EbayListingApi,
    private val authManager: EbayAuthManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Creates the eBay draft from the entry's current data and records the result.
     * Returns the sell-flow URL the user can open to finish the listing.
     */
    suspend fun pushDraft(
        entryId: Long,
        categoryId: String,
        priceValue: String?,
        conditionLabel: String?
    ): ItemDraftResponse = withContext(Dispatchers.IO) {
        if (!EbayConfig.isConfigured) throw EbayException.NotConfigured()
        val token = authManager.getValidAccessToken() ?: throw EbayException.NotConnected()

        val entryDao = db.itemEntryDao()
        val fields = db.fieldValueDao().getForEntry(entryId)
        val conditionEnum = ConditionMap.enumForLabel(conditionLabel)
        val request = DraftMapper.toItemDraftRequest(
            fields = fields,
            categoryId = categoryId,
            priceValue = priceValue,
            conditionEnum = conditionEnum
        )

        val response = try {
            listingApi.createItemDraft(
                bearerAuth = "Bearer $token",
                marketplaceId = EbayConfig.MARKETPLACE_ID,
                draft = request
            )
        } catch (e: Exception) {
            throw EbayException.Network()
        }

        if (!response.isSuccessful) {
            throw EbayException.Api(parseErrorMessage(response.errorBody()?.string(), response.code()))
        }
        val body = response.body() ?: throw EbayException.Api("eBay returned an empty response.")

        entryDao.getById(entryId)?.let { entry ->
            entryDao.update(
                entry.copy(
                    status = EntryStatus.PUSHED.name,
                    categoryId = categoryId,
                    priceValue = priceValue,
                    conditionEnum = conditionEnum,
                    ebayItemDraftId = body.itemDraftId,
                    ebaySellFlowUrl = body.sellFlowUrl,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        body
    }

    private fun parseErrorMessage(rawBody: String?, httpCode: Int): String {
        // The Listing API (createItemDraft) isn't served in Sandbox and, in Production,
        // requires eBay to grant the app Listing API access — both surface as a 404.
        if (httpCode == 404) {
            return if (EbayConfig.isSandbox) {
                "Creating drafts isn't available in eBay's sandbox. Switch to production " +
                    "(ebay.env=PROD) once your app has Sell Listing API access."
            } else {
                "eBay couldn't find the draft service (404). Your app likely needs Sell " +
                    "Listing API access granted by eBay before createItemDraft will work."
            }
        }
        if (rawBody.isNullOrBlank()) return "eBay rejected the draft (HTTP $httpCode)."
        val parsed = runCatching { json.decodeFromString<EbayErrorBody>(rawBody) }.getOrNull()
        val message = parsed?.errors?.firstNotNullOfOrNull { it.longMessage ?: it.message }
        return message ?: "eBay rejected the draft (HTTP $httpCode)."
    }
}
