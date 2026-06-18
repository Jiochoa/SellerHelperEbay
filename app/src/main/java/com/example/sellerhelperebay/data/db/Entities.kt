package com.example.sellerhelperebay.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "item_entries")
data class ItemEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayTitle: String,
    val status: String,
    val categoryId: String? = null,
    val priceValue: String? = null,
    val conditionEnum: String? = null,
    val ebayItemDraftId: String? = null,
    val ebaySellFlowUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(
        entity = ItemEntryEntity::class,
        parentColumns = ["id"],
        childColumns = ["entryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("entryId")]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val relativePath: String,
    val position: Int,
    val matchStatus: String,
    val mismatchReason: String? = null
)

@Entity(
    tableName = "field_values",
    primaryKeys = ["entryId", "fieldKey"],
    foreignKeys = [ForeignKey(
        entity = ItemEntryEntity::class,
        parentColumns = ["id"],
        childColumns = ["entryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("entryId")]
)
data class FieldValueEntity(
    val entryId: Long,
    val fieldKey: String,
    val value: String?,
    val confidence: Int?,
    val provenance: String,
    val evidence: String?,
    val updatedAt: Long
)

data class ItemEntryWithDetails(
    @Embedded val entry: ItemEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val photos: List<PhotoEntity>,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val fields: List<FieldValueEntity>
)
