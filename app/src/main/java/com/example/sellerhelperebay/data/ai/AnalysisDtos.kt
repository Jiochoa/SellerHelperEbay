package com.example.sellerhelperebay.data.ai

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisResult(
    val primaryItemSummary: String = "",
    val images: List<ImageAssessment> = emptyList(),
    val fields: List<AiField> = emptyList()
)

@Serializable
data class ImageAssessment(
    val index: Int,
    val samePrimaryItem: Boolean,
    val reason: String = ""
)

@Serializable
data class AiField(
    val key: String,
    val value: String,
    val confidence: Int,
    val evidence: String = ""
)
