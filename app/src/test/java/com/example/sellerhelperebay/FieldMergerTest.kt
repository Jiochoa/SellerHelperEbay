package com.example.sellerhelperebay

import com.example.sellerhelperebay.data.ai.AiField
import com.example.sellerhelperebay.data.db.FieldValueEntity
import com.example.sellerhelperebay.domain.FieldMerger
import com.example.sellerhelperebay.domain.model.Provenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldMergerTest {

    private val entryId = 1L
    private val now = 1000L

    private fun existing(
        key: String,
        value: String?,
        confidence: Int?,
        provenance: Provenance,
        evidence: String? = null
    ) = FieldValueEntity(entryId, key, value, confidence, provenance.name, evidence, 0L)

    @Test
    fun `rule 1 - manual fields are never overwritten`() {
        val result = FieldMerger.merge(
            entryId,
            listOf(existing("BRAND", "Adidas", null, Provenance.MANUAL)),
            listOf(AiField("BRAND", "Nike", 99, "swoosh visible")),
            now
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `rule 1 - web fields are never overwritten`() {
        val result = FieldMerger.merge(
            entryId,
            listOf(existing("MATERIAL", "Cotton", 50, Provenance.WEB)),
            listOf(AiField("MATERIAL", "Polyester", 99, "tag visible")),
            now
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `rule 2 - blank or absent field takes the incoming value`() {
        val result = FieldMerger.merge(
            entryId,
            emptyList(),
            listOf(AiField("BRAND", "Nike", 80, "swoosh on chest")),
            now
        )
        assertEquals(1, result.size)
        with(result.single()) {
            assertEquals("BRAND", fieldKey)
            assertEquals("Nike", value)
            assertEquals(80, confidence)
            assertEquals(Provenance.AI.name, provenance)
            assertEquals("swoosh on chest", evidence)
        }
    }

    @Test
    fun `rule 3 - same AI value keeps max confidence`() {
        val result = FieldMerger.merge(
            entryId,
            listOf(existing("BRAND", "Nike", 85, Provenance.AI)),
            listOf(AiField("BRAND", "nike", 60, "logo, blurry")),
            now
        )
        assertEquals(1, result.size)
        assertEquals("Nike", result.single().value)
        assertEquals(85, result.single().confidence)
    }

    @Test
    fun `rule 4 - different AI value with higher confidence wins`() {
        val result = FieldMerger.merge(
            entryId,
            listOf(existing("COLOR", "Navy", 55, Provenance.AI, "dim photo")),
            listOf(AiField("COLOR", "Black", 90, "well-lit close-up")),
            now
        )
        assertEquals(1, result.size)
        with(result.single()) {
            assertEquals("Black", value)
            assertEquals(90, confidence)
            assertTrue(evidence!!.contains("previously: \"Navy\""))
        }
    }

    @Test
    fun `rule 4 - different AI value with lower confidence loses`() {
        val result = FieldMerger.merge(
            entryId,
            listOf(existing("COLOR", "Navy", 90, Provenance.AI)),
            listOf(AiField("COLOR", "Black", 40, "dark photo")),
            now
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `unknown field keys and blank values are dropped`() {
        val result = FieldMerger.merge(
            entryId,
            emptyList(),
            listOf(
                AiField("NOT_A_REAL_KEY", "x", 80),
                AiField("BRAND", "   ", 80)
            ),
            now
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `confidence is clamped to 0-100`() {
        val result = FieldMerger.merge(
            entryId,
            emptyList(),
            listOf(AiField("BRAND", "Nike", 250, "x")),
            now
        )
        assertEquals(100, result.single().confidence)
    }
}
