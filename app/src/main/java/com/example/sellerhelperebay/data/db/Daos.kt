package com.example.sellerhelperebay.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemEntryDao {
    @Query("SELECT * FROM item_entries ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ItemEntryEntity>>

    @Transaction
    @Query("SELECT * FROM item_entries WHERE id = :id")
    fun observeWithDetails(id: Long): Flow<ItemEntryWithDetails?>

    @Transaction
    @Query("SELECT * FROM item_entries WHERE id = :id")
    suspend fun getWithDetails(id: Long): ItemEntryWithDetails?

    @Query("SELECT * FROM item_entries WHERE id = :id")
    suspend fun getById(id: Long): ItemEntryEntity?

    @Insert
    suspend fun insert(entry: ItemEntryEntity): Long

    @Update
    suspend fun update(entry: ItemEntryEntity)

    @Query("DELETE FROM item_entries WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE entryId = :entryId ORDER BY position")
    suspend fun getForEntry(entryId: Long): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getById(id: Long): PhotoEntity?

    @Insert
    suspend fun insert(photo: PhotoEntity): Long

    @Update
    suspend fun update(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM photos WHERE entryId = :entryId")
    suspend fun nextPosition(entryId: Long): Int
}

@Dao
interface FieldValueDao {
    @Query("SELECT * FROM field_values WHERE entryId = :entryId")
    suspend fun getForEntry(entryId: Long): List<FieldValueEntity>

    @Upsert
    suspend fun upsert(field: FieldValueEntity)

    @Upsert
    suspend fun upsertAll(fields: List<FieldValueEntity>)

    @Query("DELETE FROM field_values WHERE entryId = :entryId AND fieldKey = :fieldKey")
    suspend fun delete(entryId: Long, fieldKey: String)
}
