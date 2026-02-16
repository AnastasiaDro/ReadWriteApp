package com.cerebus.create_screen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.create_screen.navigation.DeckNavigationState
import com.cerebus.decks.domain.repositories.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeckScreenViewModel(
    private val deckRepository: DeckRepository,
) : ViewModel() {

    private companion object {
        const val MAX_DECK_NAME_LENGTH = 40
    }

    private val _uiState = MutableStateFlow(DeckUiState())
    val uiState: StateFlow<DeckUiState> = _uiState.asStateFlow()

    fun onAction(action: DeckScreenAction) {
        when (action) {
            is DeckScreenAction.Initialize -> loadDeck(action.deckId)
            DeckScreenAction.OnEditNameClick -> {
                _uiState.update {
                    it.copy(
                        isEditNameDialogVisible = true,
                        editingName = it.deckName,
                        validationError = null,
                    )
                }
            }

            DeckScreenAction.OnDismissEditNameDialog -> {
                _uiState.update {
                    it.copy(
                        isEditNameDialogVisible = false,
                        editingName = it.deckName,
                        validationError = null,
                    )
                }
            }

            is DeckScreenAction.OnNameChanged -> {
                _uiState.update {
                    it.copy(
                        editingName = action.value.take(MAX_DECK_NAME_LENGTH),
                        validationError = null,
                    )
                }
            }

            DeckScreenAction.OnSaveNameClick -> updateDeckName()
            DeckScreenAction.OnEditCoverClick -> {
                _uiState.update { it.copy(isEditCoverSourceDialogVisible = true) }
            }

            DeckScreenAction.OnDismissEditCoverSourceDialog -> {
                _uiState.update { it.copy(isEditCoverSourceDialogVisible = false) }
            }

            DeckScreenAction.OnPickCoverFromGalleryClick -> {
                _uiState.update {
                    it.copy(
                        isEditCoverSourceDialogVisible = false,
                        pendingPickerRequest = DeckPickerRequest.GALLERY,
                    )
                }
            }

            DeckScreenAction.OnTakeCoverPhotoClick -> {
                _uiState.update {
                    it.copy(
                        isEditCoverSourceDialogVisible = false,
                        pendingPickerRequest = DeckPickerRequest.CAMERA,
                    )
                }
            }

            DeckScreenAction.OnPickerRequestConsumed -> {
                _uiState.update { it.copy(pendingPickerRequest = null) }
            }

            is DeckScreenAction.OnCoverUriSelected -> updateDeckCover(action.uri)
            DeckScreenAction.OnAddCardClick -> Unit
        }
    }

    private fun loadDeck(deckId: String) {
        if (_uiState.value.deckId == deckId && !_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, deckId = deckId) }
            val deck = deckRepository.getDeckById(deckId)
            if (deck != null) {
                _uiState.update {
                    it.copy(
                        deckName = deck.name,
                        coverUri = deck.coverUri,
                        editingName = deck.name,
                        isLoading = false,
                        validationError = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        validationError = DeckValidationError.DECK_NOT_FOUND,
                    )
                }
            }
        }
    }

    private fun updateDeckName() {
        val state = _uiState.value
        val normalizedName = state.editingName.trim()
        if (normalizedName.isBlank()) {
            _uiState.update { it.copy(validationError = DeckValidationError.EMPTY_DECK_NAME) }
            return
        }

        viewModelScope.launch {
            val updated = deckRepository.updateDeckName(state.deckId, normalizedName)
            if (updated) {
                DeckNavigationState.notifyDeckChanged()
                _uiState.update {
                    it.copy(
                        deckName = normalizedName,
                        editingName = normalizedName,
                        isEditNameDialogVisible = false,
                        validationError = null,
                    )
                }
            } else {
                _uiState.update { it.copy(validationError = DeckValidationError.UPDATE_NAME_FAILED) }
            }
        }
    }

    private fun updateDeckCover(uri: String) {
        val state = _uiState.value
        viewModelScope.launch {
            val updated = deckRepository.updateDeckCoverUri(state.deckId, uri)
            if (updated) {
                DeckNavigationState.notifyDeckChanged()
                _uiState.update {
                    it.copy(
                        coverUri = uri,
                        validationError = null,
                    )
                }
            } else {
                _uiState.update { it.copy(validationError = DeckValidationError.UPDATE_COVER_FAILED) }
            }
        }
    }
}
