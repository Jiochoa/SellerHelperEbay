package com.example.sellerhelperebay.data.ebay

import com.example.sellerhelperebay.data.db.FieldValueEntity
import com.example.sellerhelperebay.domain.model.FieldKey

/**
 * Builds the createItemDraft request from the entry's field values. Blank fields are
 * omitted entirely — eBay treats every ItemDraft field as optional, and the user
 * completes the draft in the eBay app.
 */
object DraftMapper {

    fun toItemDraftRequest(
        fields: List<FieldValueEntity>,
        categoryId: String?,
        priceValue: String?,
        conditionEnum: String?
    ): ItemDraftRequest {
        val byKey = fields
            .filter { !it.value.isNullOrBlank() }
            .associate { it.fieldKey to it.value!!.trim() }

        val aspects = FieldKey.entries
            .filter { it.ebayAspectName != null && it != FieldKey.BRAND }
            .mapNotNull { key ->
                byKey[key.name]?.let { DraftAspect(key.ebayAspectName!!, listOf(it)) }
            }

        val product = DraftProduct(
            title = byKey[FieldKey.TITLE.name],
            description = byKey[FieldKey.DESCRIPTION.name],
            brand = byKey[FieldKey.BRAND.name],
            aspects = aspects.ifEmpty { null }
        )

        return ItemDraftRequest(
            categoryId = categoryId?.ifBlank { null },
            condition = conditionEnum?.ifBlank { null },
            format = "FIXED_PRICE",
            pricingSummary = priceValue
                ?.ifBlank { null }
                ?.let { PricingSummary(Amount(value = it)) },
            product = product
        )
    }
}
