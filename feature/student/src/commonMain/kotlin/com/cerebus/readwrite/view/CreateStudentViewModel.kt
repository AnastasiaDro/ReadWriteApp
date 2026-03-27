package com.cerebus.readwrite.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.utils.CustomResult
import com.cerebus.readwrite.navigation.CreateStudentNavigationState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateStudentUiState(
    val name: String = "",
    val avatarUri: String? = null,
    val isPhotoSourceDialogVisible: Boolean = false,
    val pendingPickerRequest: StudentPickerRequest? = null,
)

enum class StudentPickerRequest {
    GALLERY,
    CAMERA,
}

sealed interface CreateStudentAction {
    data object OnAvatarClick : CreateStudentAction
    data object OnAddPhotoClick : CreateStudentAction
    data object OnDismissPhotoSourceDialog : CreateStudentAction
    data object OnPickFromGalleryClick : CreateStudentAction
    data object OnTakePhotoClick : CreateStudentAction
    data class OnPhotoPicked(val uri: String) : CreateStudentAction
    data object OnPickerRequestConsumed : CreateStudentAction
    data class OnNameChanged(val value: String) : CreateStudentAction
    data object OnNextClick : CreateStudentAction
}

sealed interface CreateStudentEffect {
    data object NavigateToDeckList : CreateStudentEffect
    data object ShowCreateStudentFailed : CreateStudentEffect
}

class CreateStudentViewModel(
    private val starterStudentService: StarterStudentService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateStudentUiState())
    val uiState: StateFlow<CreateStudentUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<CreateStudentEffect>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<CreateStudentEffect> = _effects.asSharedFlow()

    fun onAction(action: CreateStudentAction) {
        when (action) {
            CreateStudentAction.OnAvatarClick -> onPhotoPickRequested()
            CreateStudentAction.OnAddPhotoClick -> onPhotoPickRequested()
            CreateStudentAction.OnDismissPhotoSourceDialog -> {
                _uiState.update { it.copy(isPhotoSourceDialogVisible = false) }
            }
            CreateStudentAction.OnPickFromGalleryClick -> {
                _uiState.update {
                    it.copy(
                        isPhotoSourceDialogVisible = false,
                        pendingPickerRequest = StudentPickerRequest.GALLERY,
                    )
                }
            }
            CreateStudentAction.OnTakePhotoClick -> {
                _uiState.update {
                    it.copy(
                        isPhotoSourceDialogVisible = false,
                        pendingPickerRequest = StudentPickerRequest.CAMERA,
                    )
                }
            }
            is CreateStudentAction.OnPhotoPicked -> {
                _uiState.update { it.copy(avatarUri = action.uri) }
            }
            CreateStudentAction.OnPickerRequestConsumed -> {
                _uiState.update { it.copy(pendingPickerRequest = null) }
            }
            is CreateStudentAction.OnNameChanged -> {
                _uiState.update { it.copy(name = action.value) }
            }

            CreateStudentAction.OnNextClick -> {
                createStudentAndContinue()
            }
        }
    }

    private fun onPhotoPickRequested() {
        _uiState.update { it.copy(isPhotoSourceDialogVisible = true) }
    }

    private fun createStudentAndContinue() {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            when (
                val result = starterStudentService.createStudentWithStarterDeck(
                    name = name,
                    avatarUri = _uiState.value.avatarUri,
                )
            ) {
                is CustomResult.Success -> {
                    CreateStudentNavigationState.setPendingCreatedStudentId(result.data)
                    _effects.emit(CreateStudentEffect.NavigateToDeckList)
                }
                is CustomResult.Failure -> {
                    _effects.emit(CreateStudentEffect.ShowCreateStudentFailed)
                }
            }
        }
    }
}
