package com.example.sellerhelperebay.domain

import com.example.sellerhelperebay.data.ai.AiField
import com.example.sellerhelperebay.data.db.FieldValueEntity
import com.example.sellerhelperebay.domain.model.FieldKey
import com.example.sellerhelperebay.domain.model.Provenance

/**
 * Merges a fresh analysis into the existing field values.
 *
 * Rules:
 * 1. MANUAL/WEB fields are never overwritten.
 * 2. A blank or absent field takes the incoming value.
 * 3. Both AI with the same value (case-insensitive): keep it, confidence = max(old, new).
 * 4. Both AI with different values: higher confidence wins; the losing value is
 *    preserved in the evidence string ("previously: ...").
 * 5. (Handled upstream: clearing a field deletes its row; typing sets MANUAL.)
 *
 * Returns only the rows that need upserting.
 */
object FieldMerger {

    fun merge(
        entryId: Long,
        existing: List<FieldValueEntity>,
        incoming: List<AiField>,
        now: Long = System.currentTimeMillis()
    ): List<FieldValueEntity> {
        val byKey = existing.associateBy { it.fieldKey }
        val upserts = mutableListOf<FieldValueEntity>()

        for (field in incoming) {
            if (FieldKey.fromNameOrNull(field.key) == null) continue
            val value = field.value.trim()
            if (value.isEmpty()) continue
            val confidence = field.confidence.coerceIn(0, 100)

            val old = byKey[field.key]
            when {
                old == null || old.value.isNullOrBlank() -> upserts.add(
                    FieldValueEntity(
                        entryId = entryId,
                        fieldKey = field.key,
                        value = value,
                        confidence = confidence,
                        provenance = Provenance.AI.name,
                        evidence = field.evidence.ifBlank { null },
                        updatedAt = now
                    )
                )

                old.provenance != Provenance.AI.name -> Unit // rule 1: user-owned

                old.value.trim().equals(value, ignoreCase = true) -> upserts.add(
                    old.copy(
                        confidence = maxOf(old.confidence ?: 0, confidence),
                        evidence = field.evidence.ifBlank { old.evidence },
                        updatedAt = now
                    )
                )

                confidence > (old.confidence ?: 0) -> upserts.add(
                    old.copy(
                        value = value,
                        confidence = confidence,
                        evidence = buildString {
                            append(field.evidence.ifBlank { "(no evidence given)" })
                            append(" | previously: \"${old.value}\"")
                            old.evidence?.let { append(" ($it)") }
                        },
                        updatedAt = now
                    )
                )

                else -> Unit // keep the existing higher-confidence value
            }
        }
        return upserts
    }
}
