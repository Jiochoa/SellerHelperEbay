package com.example.sellerhelperebay.domain.model

/**
 * Color buckets for the per-field accuracy indicator. Blue means the value did not
 * come from image analysis (manual entry or web search); the others reflect the
 * AI's stated confidence.
 */
enum class ConfidenceLevel { GREEN, YELLOW, RED, BLUE, EMPTY }

fun confidenceLevel(provenance: Provenance?, confidence: Int?): ConfidenceLevel = when {
    provenance == null -> ConfidenceLevel.EMPTY
    provenance == Provenance.MANUAL || provenance == Provenance.WEB -> ConfidenceLevel.BLUE
    confidence == null -> ConfidenceLevel.EMPTY
    confidence > 75 -> ConfidenceLevel.GREEN
    confidence >= 60 -> ConfidenceLevel.YELLOW
    else -> ConfidenceLevel.RED
}
