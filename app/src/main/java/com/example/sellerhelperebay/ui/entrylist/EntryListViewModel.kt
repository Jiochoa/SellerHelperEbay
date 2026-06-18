package com.example.sellerhelperebay.ui.entrylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.sellerhelperebay.data.db.ItemEntryEntity
import com.example.sellerhelperebay.data.repo.ItemRepository
import com.example.sellerhelperebay.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EntryListViewModel(private val repository: ItemRepository) : ViewModel() {

    val entries: StateFlow<List<ItemEntryEntity>> = repository.observeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createEntry(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.createEntry())
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { repository.deleteEntry(id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { EntryListViewModel(appContainer().itemRepository) }
        }
    }
}
