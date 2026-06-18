package com.example.sellerhelperebay

import com.example.sellerhelperebay.data.ai.AnalysisResult
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisDtosTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun `parses a full well-formed response`() {
        val raw = """
            {
              "primaryItemSummary": "Blue Nike t-shirt",
              "images": [
                {"index": 0, "samePrimaryItem": true, "reason": "front of shirt"},
                {"index": 1, "samePrimaryItem": false, "reason": "shows a shoe"}
              ],
              "fields": [
                {"key": "BRAND", "value": "Nike", "confidence": 88, "evidence": "swoosh, image 0"}
              ]
            }
        """.trimIndent()

        val result = json.decodeFromString<AnalysisResult>(raw)
        assertEquals("Blue Nike t-shirt", result.primaryItemSummary)
        assertEquals(2, result.images.size)
        assertTrue(result.images[0].samePrimaryItem)
        assertFalse(result.images[1].samePrimaryItem)
        assertEquals("BRAND", result.fields.single().key)
        assertEquals(88, result.fields.single().confidence)
    }

    @Test
    fun `tolerates unknown keys and missing optional fields`() {
        val raw = """
            {
              "primaryItemSummary": "Shirt",
              "someNewField": 42,
              "images": [{"index": 0, "samePrimaryItem": true}],
              "fields": [{"key": "WAIST_SIZE", "value": "32", "confidence": 70}]
            }
        """.trimIndent()

        // Unknown field keys parse fine here; FieldMerger drops them later.
        val result = json.decodeFromString<AnalysisResult>(raw)
        assertEquals("", result.images.single().reason)
        assertEquals("WAIST_SIZE", result.fields.single().key)
        assertEquals("", result.fields.single().evidence)
    }

    @Test
    fun `tolerates empty response object`() {
        val result = json.decodeFromString<AnalysisResult>("{}")
        assertTrue(result.images.isEmpty())
        assertTrue(result.fields.isEmpty())
    }
}
