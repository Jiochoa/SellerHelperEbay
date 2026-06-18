package com.example.sellerhelperebay.ui.ebay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.sellerhelperebay.data.db.ItemEntryWithDetails
import com.example.sellerhelperebay.data.ebay.EbayAuthManager
import com.example.sellerhelperebay.data.ebay.EbayException
import com.example.sellerhelperebay.data.ebay.EbayRepository
import com.example.sellerhelperebay.data.repo.ItemRepository
import com.example.sellerhelperebay.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PushUiState {
    data object Idle : PushUiState
    data object Pushing : PushUiState
    data class Success(val sellFlowUrl: String?) : PushUiState
    data class Error(val message: String) : PushUiState
}

class EbayPushViewModel(
    itemRepository: ItemRepository,
    private val ebayRepository: EbayRepository,
    authManager: EbayAuthManager,
    val entryId: Long
) : ViewModel() {

    val entry: StateFlow<ItemEntryWithDetails?> = itemRepository.observeEntry(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isConnected: StateFlow<Boolean> = authManager.isConnectedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _pushState = MutableStateFlow<PushUiState>(PushUiState.Idle)
    val pushState: StateFlow<PushUiState> = _pushState.asStateFlow()

    fun push(categoryId: String, price: String, conditionLabel: String) {
        if (_pushState.value == PushUiState.Pushing) return
        _pushState.value = PushUiState.Pushing
        viewModelScope.launch {
            try {
                val response = ebayRepository.pushDraft(
                    entryId = entryId,
                    categoryId = categoryId,
                    priceValue = price.trim().ifBlank { null },
                    conditionLabel = conditionLabel.ifBlank { null }
                )
                _pushState.value = PushUiState.Success(response.sellFlowUrl)
            } catch (e: EbayException) {
                _pushState.value = PushUiState.Error(e.message ?: "Push failed.")
            } catch (e: Exception) {
                _pushState.value = PushUiState.Error("Something went wrong. Try again.")
            }
        }
    }

    companion object {
        fun factory(entryId: Long) = viewModelFactory {
            initializer {
                val container = appContainer()
                EbayPushViewModel(
                    itemRepository = container.itemRepository,
                    ebayRepository = container.ebayRepository,
                    authManager = container.ebayAuthManager,
                    entryId = entryId
                )
            }
        }
    }
}
