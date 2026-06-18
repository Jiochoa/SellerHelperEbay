package com.example.sellerhelperebay.data.repo

import android.net.Uri
import androidx.room.withTransaction
import com.example.sellerhelperebay.data.ai.AnalysisException
import com.example.sellerhelperebay.data.ai.GeminiAnalyzer
import com.example.sellerhelperebay.data.db.AppDatabase
import com.example.sellerhelperebay.data.db.FieldValueEntity
import com.example.sellerhelperebay.data.db.ItemEntryEntity
import com.example.sellerhelperebay.data.db.ItemEntryWithDetails
import com.example.sellerhelperebay.data.db.PhotoEntity
import com.example.sellerhelperebay.data.images.ImageStore
import com.example.sellerhelperebay.domain.FieldMerger
import com.example.sellerhelperebay.domain.model.EntryStatus
import com.example.sellerhelperebay.domain.model.FieldKey
import com.example.sellerhelperebay.domain.model.PhotoMatchStatus
import com.example.sellerhelperebay.domain.model.Provenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class ItemRepository(
    private val db: AppDatabase,
    private val imageStore: ImageStore,
    private val analyzer: GeminiAnalyzer
) {
    private val entryDao = db.itemEntryDao()
    private val photoDao = db.photoDao()
    private val fieldDao = db.fieldValueDao()

    fun observeEntries(): Flow<List<ItemEntryEntity>> = entryDao.observeAll()

    fun observeEntry(id: Long): Flow<ItemEntryWithDetails?> = entryDao.observeWithDetails(id)

    suspend fun createEntry(): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        entryDao.insert(
            ItemEntryEntity(
                displayTitle = "New item",
                status = EntryStatus.NEW.name,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun deleteEntry(id: Long) = withContext(Dispatchers.IO) {
        entryDao.delete(id) // photos + fields cascade
        imageStore.deleteEntryDir(id)
    }

    /** Imports the given images; returns the number that failed (skipped, not fatal). */
    suspend fun addPhotosFromUris(entryId: Long, uris: List<Uri>): Int =
        withContext(Dispatchers.IO) {
            var failures = 0
            uris.forEach { uri ->
                val relativePath = try {
                    imageStore.importUri(entryId, uri)
                } catch (e: Exception) {
                    failures++
                    return@forEach
                }
                photoDao.insert(
                    PhotoEntity(
                        entryId = entryId,
                        relativePath = relativePath,
                        position = photoDao.nextPosition(entryId),
                        matchStatus = PhotoMatchStatus.UNANALYZED.name
                    )
                )
            }
            touch(entryId)
            failures
        }

    /** Imports a camera capture; returns true on success, false if it couldn't be read. */
    suspend fun addPhotoFromCamera(entryId: Long, tempFile: File): Boolean =
        withContext(Dispatchers.IO) {
            val relativePath = try {
                imageStore.importCameraCapture(entryId, tempFile)
            } catch (e: Exception) {
                return@withContext false
            }
            photoDao.insert(
                PhotoEntity(
                    entryId = entryId,
                    relativePath = relativePath,
                    position = photoDao.nextPosition(entryId),
                    matchStatus = PhotoMatchStatus.UNANALYZED.name
                )
            )
            touch(entryId)
            true
        }

    suspend fun removePhoto(photoId: Long) = withContext(Dispatchers.IO) {
        val photo = photoDao.getById(photoId) ?: return@withContext
        photoDao.delete(photoId)
        imageStore.delete(photo.relativePath)
        recomputeReviewStatus(photo.entryId)
        touch(photo.entryId)
    }

    /** Records the user's verdict on a photo that was flagged as a mismatch. */
    suspend fun resolveMismatch(photoId: Long, verdict: PhotoMatchStatus) =
        withContext(Dispatchers.IO) {
            require(
                verdict == PhotoMatchStatus.USER_CONFIRMED_SAME ||
                    verdict == PhotoMatchStatus.USER_MARKED_LOT
            ) { "verdict must be a user resolution" }
            val photo = photoDao.getById(photoId) ?: return@withContext
            photoDao.update(photo.copy(matchStatus = verdict.name, mismatchReason = null))
            recomputeReviewStatus(photo.entryId)
            touch(photo.entryId)
        }

    /** Once no photos are pending review, the entry leaves NEEDS_REVIEW. */
    private suspend fun recomputeReviewStatus(entryId: Long) {
        val entry = entryDao.getById(entryId) ?: return
        if (entry.status != EntryStatus.NEEDS_REVIEW.name) return
        val anyPending = photoDao.getForEntry(entryId)
            .any { it.matchStatus == PhotoMatchStatus.MISMATCH_PENDING.name }
        if (!anyPending) entryDao.update(entry.copy(status = EntryStatus.ANALYZED.name))
    }

    suspend fun setFieldManually(entryId: Long, key: FieldKey, value: String) =
        withContext(Dispatchers.IO) {
            fieldDao.upsert(
                FieldValueEntity(
                    entryId = entryId,
                    fieldKey = key.name,
                    value = value,
                    confidence = null,
                    provenance = Provenance.MANUAL.name,
                    evidence = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            if (key == FieldKey.TITLE) updateDisplayTitle(entryId, value)
            touch(entryId)
        }

    suspend fun clearField(entryId: Long, key: FieldKey) = withContext(Dispatchers.IO) {
        fieldDao.delete(entryId, key.name)
        touch(entryId)
    }

    /**
     * Runs the Gemini analysis over the entry's photos and merges the results.
     * Photos already flagged MISMATCH_PENDING are excluded until the user resolves them.
     * Throws [AnalysisException] with a user-readable message on failure.
     */
    suspend fun analyzeEntry(entryId: Long) = withContext(Dispatchers.IO) {
        val photos = photoDao.getForEntry(entryId)
            .filter { it.matchStatus != PhotoMatchStatus.MISMATCH_PENDING.name }
            .take(MAX_PHOTOS_PER_ANALYSIS)
        if (photos.isEmpty()) throw AnalysisException.Empty()

        val bitmaps = photos.mapNotNull { imageStore.loadBitmap(it.relativePath) }
        if (bitmaps.size != photos.size) throw AnalysisException.Empty()

        val existingFields = fieldDao.getForEntry(entryId)
        val lockedKeys = existingFields
            .filter { it.provenance != Provenance.AI.name }
            .mapNotNull { FieldKey.fromNameOrNull(it.fieldKey) }
            .toSet()

        val hints = photos.mapIndexedNotNull { index, photo ->
            when (photo.matchStatus) {
                PhotoMatchStatus.USER_CONFIRMED_SAME.name ->
                    "Image $index IS the same primary item (confirmed by the user), " +
                        "even if it looks different."
                PhotoMatchStatus.USER_MARKED_LOT.name ->
                    "Image $index is a DIFFERENT item that is sold together with the primary " +
                        "item as a package (e.g. an accessory or spare part). Mention it in the " +
                        "DESCRIPTION but do not use it for any other field."
                else -> null
            }
        }

        val result = analyzer.analyze(bitmaps, lockedKeys, hints)

        db.withTransaction {
            val assessmentsByIndex = result.images.associateBy { it.index }
            photos.forEachIndexed { index, photo ->
                // User verdicts are final; only machine-judged photos change status.
                if (photo.matchStatus == PhotoMatchStatus.USER_CONFIRMED_SAME.name ||
                    photo.matchStatus == PhotoMatchStatus.USER_MARKED_LOT.name
                ) return@forEachIndexed

                val assessment = assessmentsByIndex[index]
                val matches = assessment?.samePrimaryItem ?: true
                photoDao.update(
                    photo.copy(
                        matchStatus = if (matches) PhotoMatchStatus.MATCHED.name
                        else PhotoMatchStatus.MISMATCH_PENDING.name,
                        mismatchReason = if (matches) null
                        else assessment?.reason?.ifBlank { null }
                    )
                )
            }

            val upserts = FieldMerger.merge(entryId, existingFields, result.fields)
            if (upserts.isNotEmpty()) fieldDao.upsertAll(upserts)

            val anyMismatch = photoDao.getForEntry(entryId)
                .any { it.matchStatus == PhotoMatchStatus.MISMATCH_PENDING.name }
            val entry = entryDao.getById(entryId) ?: return@withTransaction
            val mergedTitle = fieldDao.getForEntry(entryId)
                .firstOrNull { it.fieldKey == FieldKey.TITLE.name }?.value
            entryDao.update(
                entry.copy(
                    status = if (anyMismatch) EntryStatus.NEEDS_REVIEW.name
                    else EntryStatus.ANALYZED.name,
                    displayTitle = mergedTitle?.ifBlank { null } ?: entry.displayTitle,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun getFilledFieldKeys(entryId: Long): Set<FieldKey> = withContext(Dispatchers.IO) {
        fieldDao.getForEntry(entryId)
            .filter { !it.value.isNullOrBlank() }
            .mapNotNull { FieldKey.fromNameOrNull(it.fieldKey) }
            .toSet()
    }

    private suspend fun updateDisplayTitle(entryId: Long, title: String) {
        val entry = entryDao.getById(entryId) ?: return
        entryDao.update(entry.copy(displayTitle = title.ifBlank { "New item" }))
    }

    private suspend fun touch(entryId: Long) {
        val entry = entryDao.getById(entryId) ?: return
        entryDao.update(entry.copy(updatedAt = System.currentTimeMillis()))
    }

    companion object {
        const val MAX_PHOTOS_PER_ANALYSIS = 8
    }
}
