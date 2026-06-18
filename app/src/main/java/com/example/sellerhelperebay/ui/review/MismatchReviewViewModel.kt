package com.example.sellerhelperebay.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.sellerhelperebay.data.db.PhotoEntity
import com.example.sellerhelperebay.data.images.ImageStore
import com.example.sellerhelperebay.data.repo.ItemRepository
import com.example.sellerhelperebay.domain.model.PhotoMatchStatus
import com.example.sellerhelperebay.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MismatchReviewViewModel(
    private val repository: ItemRepository,
    private val imageStore: ImageStore,
    val entryId: Long
) : ViewModel() {

    val pendingPhotos: StateFlow<List<PhotoEntity>> = repository.observeEntry(entryId)
        .map { details ->
            details?.photos
                ?.filter { it.matchStatus == PhotoMatchStatus.MISMATCH_PENDING.name }
                .orEmpty()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch { repository.removePhoto(photoId) }
    }

    fun markSameItem(photoId: Long) {
        viewModelScope.launch {
            repository.resolveMismatch(photoId, PhotoMatchStatus.USER_CONFIRMED_SAME)
        }
    }

    fun markPartOfLot(photoId: Long) {
        viewModelScope.launch {
            repository.resolveMismatch(photoId, PhotoMatchStatus.USER_MARKED_LOT)
        }
    }

    fun photoFile(relativePath: String): File = imageStore.fileFor(relativePath)

    companion object {
        fun factory(entryId: Long) = viewModelFactory {
            initializer {
                val container = appContainer()
                MismatchReviewViewModel(container.itemRepository, container.imageStore, entryId)
            }
        }
    }
}
