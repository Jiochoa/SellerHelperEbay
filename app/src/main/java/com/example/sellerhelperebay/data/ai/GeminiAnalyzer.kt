package com.example.sellerhelperebay.data.ai

import android.graphics.Bitmap
import com.example.sellerhelperebay.domain.model.FieldKey
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.json.Json

sealed class AnalysisException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class Network(cause: Throwable) :
        AnalysisException("Could not reach the analysis service. Check your connection.", cause)

    class Parse(cause: Throwable) :
        AnalysisException("The analysis returned an unexpected result. Try again.", cause)

    class Empty : AnalysisException("The analysis returned no result. Try again.")
}

class GeminiAnalyzer {

    private val model = Firebase.ai.generativeModel(
        modelName = "gemini-flash-latest",
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = AnalysisSchema.schema
        }
    )

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /**
     * Analyzes the photos in one call: verifies all images show the same primary item
     * and extracts form-field values with per-field confidence.
     *
     * @param lockedKeys fields the model must not emit (manual/web values the user owns)
     * @param hints user verdicts from mismatch review, phrased as prompt lines
     */
    suspend fun analyze(
        bitmaps: List<Bitmap>,
        lockedKeys: Set<FieldKey>,
        hints: List<String>
    ): AnalysisResult {
        val response = try {
            model.generateContent(
                content {
                    bitmaps.forEach { image(it) }
                    text(buildPrompt(bitmaps.size, lockedKeys, hints))
                }
            )
        } catch (e: Exception) {
            throw AnalysisException.Network(e)
        }

        val raw = response.text ?: throw AnalysisException.Empty()
        return try {
            json.decodeFromString<AnalysisResult>(raw)
        } catch (e: Exception) {
            throw AnalysisException.Parse(e)
        }
    }

    private fun buildPrompt(
        imageCount: Int,
        lockedKeys: Set<FieldKey>,
        hints: List<String>
    ): String = buildString {
        appendLine(
            "You are helping someone list a secondhand clothing item for sale on eBay. " +
                "You are given $imageCount photos (image 0 through image ${imageCount - 1}, " +
                "in the order provided)."
        )
        appendLine()
        appendLine("Step 1 - verify: decide which images show the same primary item. ")
        appendLine(
            "Report every image in the `images` array with samePrimaryItem true or false " +
                "and a short reason."
        )
        appendLine()
        appendLine(
            "Step 2 - extract: using ONLY the images that show the primary item " +
                "(or that the user confirmed below), fill the `fields` array."
        )
        appendLine(
            "Only emit a field when you can see direct visual evidence for it in the photos. " +
                "If you cannot verify a field from the images, omit it entirely - do not guess. " +
                "For example, do not emit SIZE unless a tag or label showing the size is visible."
        )
        appendLine(
            "Confidence is 0-100: how certain you are given the image quality and clarity " +
                "of the evidence. Poor lighting or blur should lower confidence."
        )
        appendLine(
            "TITLE should be a concise eBay-style listing title built only from details " +
                "you verified. DESCRIPTION should be a short honest paragraph describing what " +
                "is visible, including any flaws."
        )
        if (lockedKeys.isNotEmpty()) {
            appendLine()
            appendLine(
                "Do NOT emit these fields, the user has already set them: " +
                    lockedKeys.joinToString(", ") { it.name }
            )
        }
        if (hints.isNotEmpty()) {
            appendLine()
            appendLine("The user has clarified the following about specific images:")
            hints.forEach { appendLine("- $it") }
        }
    }
}
