package com.example.sellerhelperebay.ui.entrydetail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.sellerhelperebay.data.ai.AnalysisException
import com.example.sellerhelperebay.data.db.ItemEntryWithDetails
import com.example.sellerhelperebay.data.images.ImageStore
import com.example.sellerhelperebay.data.repo.ItemRepository
import com.example.sellerhelperebay.domain.model.FieldKey
import com.example.sellerhelperebay.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Running : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState

    /** Analysis finished; [blankFields] lists what is still missing. */
    data class Done(val blankFields: List<FieldKey>) : AnalysisUiState
}

class EntryDetailViewModel(
    private val repository: ItemRepository,
    private val imageStore: ImageStore,
    val entryId: Long
) : ViewModel() {

    val entry: StateFlow<ItemEntryWithDetails?> = repository.observeEntry(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private var pendingCameraFile: File? = null

    fun analyze() {
        if (_analysisState.value == AnalysisUiState.Running) return
        _analysisState.value = AnalysisUiState.Running
        viewModelScope.launch {
            try {
                repository.analyzeEntry(entryId)
                val filledKeys = repository.getFilledFieldKeys(entryId)
                _analysisState.value =
                    AnalysisUiState.Done(FieldKey.entries.filter { it !in filledKeys })
            } catch (e: AnalysisException) {
                _analysisState.value = AnalysisUiState.Error(e.message ?: "Analysis failed.")
            } catch (e: Exception) {
                _analysisState.value = AnalysisUiState.Error("Something went wrong. Try again.")
            }
        }
    }

    fun dismissAnalysisState() {
        _analysisState.value = AnalysisUiState.Idle
    }

    fun newCameraUri(): Uri {
        val (uri, file) = imageStore.newCameraTempUri()
        pendingCameraFile = file
        return uri
    }

    fun onCameraResult(success: Boolean) {
        val file = pendingCameraFile ?: return
        pendingCameraFile = null
        if (success) {
            viewModelScope.launch {
                if (!repository.addPhotoFromCamera(entryId, file)) {
                    _userMessage.value = "Couldn't read that photo."
                }
            }
        } else {
            file.delete()
        }
    }

    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val failures = repository.addPhotosFromUris(entryId, uris)
            if (failures > 0) {
                _userMessage.value =
                    "Couldn't add $failures image${if (failures == 1) "" else "s"}."
            }
        }
    }

    fun consumeUserMessage() {
        _userMessage.value = null
    }

    fun removePhoto(photoId: Long) {
        viewModelScope.launch { repository.removePhoto(photoId) }
    }

    fun setField(key: FieldKey, value: String) {
        viewModelScope.launch {
            if (value.isBlank()) repository.clearField(entryId, key)
            else repository.setFieldManually(entryId, key, value)
        }
    }

    fun photoFile(relativePath: String): File = imageStore.fileFor(relativePath)

    companion object {
        fun factory(entryId: Long) = viewModelFactory {
            initializer {
                val container = appContainer()
                EntryDetailViewModel(container.itemRepository, container.imageStore, entryId)
            }
        }
    }
}
