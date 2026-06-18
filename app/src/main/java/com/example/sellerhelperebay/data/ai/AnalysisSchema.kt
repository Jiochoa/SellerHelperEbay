package com.example.sellerhelperebay.data.ai

import com.example.sellerhelperebay.domain.model.FieldKey
import com.google.firebase.ai.type.Schema

object AnalysisSchema {
    val schema: Schema = Schema.obj(
        mapOf(
            "primaryItemSummary" to Schema.string(
                description = "One-line description of the main item shown in the photos"
            ),
            "images" to Schema.array(
                items = Schema.obj(
                    mapOf(
                        "index" to Schema.integer(
                            description = "0-based index of the image, in the order they were provided"
                        ),
                        "samePrimaryItem" to Schema.boolean(
                            description = "Whether this image shows the same primary item"
                        ),
                        "reason" to Schema.string(
                            description = "Short reason why this image does or does not match"
                        )
                    )
                )
            ),
            "fields" to Schema.array(
                items = Schema.obj(
                    mapOf(
                        "key" to Schema.enumeration(
                            FieldKey.entries.map { it.name },
                            description = "The form field this value belongs to"
                        ),
                        "value" to Schema.string(),
                        "confidence" to Schema.integer(
                            description = "How confident you are in this value, 0-100"
                        ),
                        "evidence" to Schema.string(
                            description = "What visual evidence supports this value, citing image numbers"
                        )
                    )
                )
            )
        )
    )
}
