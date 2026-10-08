package com.dennymathew.frameflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dennymathew.frameflow.data.local.CharacterEntity
import com.dennymathew.frameflow.data.repository.CharacterRepository
import com.dennymathew.frameflow.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CharacterDetailViewModel @Inject constructor(
    private val repository: CharacterRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {
    private val _character = MutableStateFlow<CharacterEntity?>(null)
    val character: StateFlow<CharacterEntity?> = _character
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val isFavorite: StateFlow<Boolean> = _character
        .flatMapLatest { character ->
            if (character == null) flowOf(false) else favoritesRepository.isFavorite(character.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun load(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val item = repository.getCharacterById(id)
            _character.value = item
            _isLoading.value = false
        }
    }

    fun toggleFavorite() {
        val character = _character.value ?: return
        viewModelScope.launch { favoritesRepository.toggle(character) }
    }
}
