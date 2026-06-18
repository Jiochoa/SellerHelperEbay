package com.example.sellerhelperebay

import com.example.sellerhelperebay.data.db.FieldValueEntity
import com.example.sellerhelperebay.data.ebay.DraftMapper
import com.example.sellerhelperebay.domain.model.FieldKey
import com.example.sellerhelperebay.domain.model.Provenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftMapperTest {

    private fun field(key: FieldKey, value: String?) =
        FieldValueEntity(1L, key.name, value, 80, Provenance.AI.name, null, 0L)

    @Test
    fun `maps title description and brand to product top-level fields`() {
        val request = DraftMapper.toItemDraftRequest(
            fields = listOf(
                field(FieldKey.TITLE, "Nike Blue Tee"),
                field(FieldKey.DESCRIPTION, "A blue shirt"),
                field(FieldKey.BRAND, "Nike")
            ),
            categoryId = "15687",
            priceValue = "12.99",
            conditionEnum = "USED_EXCELLENT"
        )

        assertEquals("Nike Blue Tee", request.product?.title)
        assertEquals("A blue shirt", request.product?.description)
        assertEquals("Nike", request.product?.brand)
        assertEquals("15687", request.categoryId)
        assertEquals("USED_EXCELLENT", request.condition)
        assertEquals("FIXED_PRICE", request.format)
        assertEquals("12.99", request.pricingSummary?.price?.value)
        assertEquals("USD", request.pricingSummary?.price?.currency)
    }

    @Test
    fun `brand is not duplicated into aspects`() {
        val request = DraftMapper.toItemDraftRequest(
            fields = listOf(field(FieldKey.BRAND, "Nike")),
            categoryId = "1",
            priceValue = null,
            conditionEnum = null
        )
        assertNull(request.product?.aspects?.firstOrNull { it.name == "Brand" })
    }

    @Test
    fun `aspect fields use exact ebay aspect names`() {
        val request = DraftMapper.toItemDraftRequest(
            fields = listOf(
                field(FieldKey.SIZE, "M"),
                field(FieldKey.COLOR, "Blue"),
                field(FieldKey.COUNTRY_OF_MANUFACTURE, "Vietnam")
            ),
            categoryId = "1",
            priceValue = null,
            conditionEnum = null
        )
        val aspects = request.product?.aspects.orEmpty().associate { it.name to it.values }
        assertEquals(listOf("M"), aspects["Size"])
        assertEquals(listOf("Blue"), aspects["Color"])
        assertEquals(listOf("Vietnam"), aspects["Country/Region of Manufacture"])
    }

    @Test
    fun `blank and missing fields are omitted`() {
        val request = DraftMapper.toItemDraftRequest(
            fields = listOf(
                field(FieldKey.TITLE, "  "),
                field(FieldKey.SIZE, null),
                field(FieldKey.COLOR, "Red")
            ),
            categoryId = null,
            priceValue = "",
            conditionEnum = ""
        )
        assertNull(request.product?.title)
        assertNull(request.categoryId)
        assertNull(request.condition)
        assertNull(request.pricingSummary)
        val aspects = request.product?.aspects.orEmpty()
        assertEquals(1, aspects.size)
        assertEquals("Color", aspects.single().name)
        assertTrue(request.product?.aspects?.none { it.name == "Size" } == true)
    }
}
