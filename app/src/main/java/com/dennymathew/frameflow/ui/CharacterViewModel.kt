package com.dennymathew.frameflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dennymathew.frameflow.data.local.CharacterEntity
import com.dennymathew.frameflow.data.repository.CharacterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterViewModel @Inject constructor(
    private val repository: CharacterRepository
) : ViewModel() {

    val charactersFlow: Flow<PagingData<CharacterEntity>> = repository.getCharacters()
        .cachedIn(viewModelScope)

    private val _searchQuery = MutableStateFlow("")
    private val _isSearchSyncing = MutableStateFlow(false)
    val isSearchSyncing: Flow<Boolean> = _isSearchSyncing

    private val _searchFlow = _searchQuery
        .debounce(350)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            val query = q.trim()
            if (query.isBlank()) flowOf(PagingData.empty())
            else repository.searchCharacters(query)
        }
    val searchFlow = _searchFlow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { q ->
                    val query = q.trim()
                    if (query.isBlank()) {
                        _isSearchSyncing.value = false
                        return@collectLatest
                    }
                    _isSearchSyncing.value = true
                    try {
                        repository.syncSearchCharacters(query)
                    } finally {
                        _isSearchSyncing.value = false
                    }
                }
        }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun refreshSearch() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearchSyncing.value = true
            try {
                repository.syncSearchCharacters(query)
            } finally {
                _isSearchSyncing.value = false
            }
        }
    }
}
