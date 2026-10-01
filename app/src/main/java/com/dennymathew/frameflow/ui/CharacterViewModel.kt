package com.dennymathew.frameflow.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dennymathew.frameflow.data.local.CharacterEntity
import com.dennymathew.frameflow.data.network.ConnectivityObserver
import com.dennymathew.frameflow.data.repository.CharacterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class CharacterViewModel @Inject constructor(
    private val repository: CharacterRepository,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(viewModelScope, SharingStarted.Eagerly, connectivityObserver.isCurrentlyOnline())

    val charactersFlow: Flow<PagingData<CharacterEntity>> = repository.getCharacters()
        .cachedIn(viewModelScope)

    private val _searchQuery = MutableStateFlow("")
    private val _isSearchSyncing = MutableStateFlow(false)
    val isSearchSyncing: Flow<Boolean> = _isSearchSyncing

    // True when the last network sync for the current query failed; results are local-only.
    private val _searchSyncFailed = MutableStateFlow(false)
    val searchSyncFailed: StateFlow<Boolean> = _searchSyncFailed

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
                        _searchSyncFailed.value = false
                        return@collectLatest
                    }
                    syncSearch(query)
                }
        }
        // Re-run a search sync that failed while offline once the connection is back.
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online && _searchSyncFailed.value) refreshSearch()
            }
        }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun refreshSearch() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return
        viewModelScope.launch { syncSearch(query) }
    }

    private suspend fun syncSearch(query: String) {
        _isSearchSyncing.value = true
        try {
            repository.syncSearchCharacters(query)
            _searchSyncFailed.value = false
        } catch (e: IOException) {
            Log.w(TAG, "Search sync failed for \"$query\"", e)
            _searchSyncFailed.value = true
        } catch (e: HttpException) {
            Log.w(TAG, "Search sync failed for \"$query\"", e)
            _searchSyncFailed.value = true
        } finally {
            _isSearchSyncing.value = false
        }
    }

    private companion object {
        const val TAG = "CharacterViewModel"
    }
}
