package com.cerebus.readwrite.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.deck_package.domain.service.StudentPackageImportPreview
import com.cerebus.core.deck_package.domain.service.StudentPackageService
import com.cerebus.core.utils.CustomResult
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.student.domain.repositories.StudentRepository
import com.cerebus.data.studentdeck.domain.models.StudentWithDecks
import com.cerebus.data.studentdeck.domain.repositories.StudentDeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangeStudentListItem(
    val studentId: String,
    val studentName: String,
    val avatarUri: String?,
    val lastLessonDeck: Deck?,
)

data class ChangeStudentUiState(
    val isLoading: Boolean = true,
    val students: List<ChangeStudentListItem> = emptyList(),
    val activeStudentId: String? = null,
    val expandedMenuStudentId: String? = null,
    val editingStudentId: String? = null,
    val isEditStudentDialogVisible: Boolean = false,
    val editStudentName: String = "",
    val editStudentAvatarUri: String? = null,
    val isPhotoSourceDialogVisible: Boolean = false,
    val pendingPickerRequest: StudentPickerRequest? = null,
    val deletingStudentId: String? = null,
    val isDeleteStudentDialogVisible: Boolean = false,
    val pendingStudentImportConfirmation: ChangeStudentPendingStudentImportConfirmation? = null,
)

data class ChangeStudentPendingStudentImportConfirmation(
    val archiveUri: String,
    val importedStudentName: String,
    val existingStudentName: String,
    val matchedDecksCount: Int,
    val missingDecksCount: Int,
    val matchedCardsCount: Int,
    val missingCardsCount: Int,
)

sealed interface ChangeStudentAction {
    data class OnStudentClick(val studentId: String) : ChangeStudentAction
    data class OnDeckClick(val studentId: String, val deckId: String) : ChangeStudentAction
    data class OnStudentMenuClick(val studentId: String) : ChangeStudentAction
    data object OnDismissStudentMenu : ChangeStudentAction
    data class OnEditStudentClick(val studentId: String) : ChangeStudentAction
    data object OnDismissEditStudentDialog : ChangeStudentAction
    data class OnEditStudentNameChanged(val value: String) : ChangeStudentAction
    data object OnEditStudentAvatarClick : ChangeStudentAction
    data object OnDismissPhotoSourceDialog : ChangeStudentAction
    data object OnPickFromGalleryClick : ChangeStudentAction
    data object OnTakePhotoClick : ChangeStudentAction
    data class OnPhotoPicked(val uri: String) : ChangeStudentAction
    data object OnPickerRequestConsumed : ChangeStudentAction
    data object OnSaveStudentChanges : ChangeStudentAction
    data class OnDeleteStudentClick(val studentId: String) : ChangeStudentAction
    data object OnDismissDeleteStudentDialog : ChangeStudentAction
    data object OnConfirmDeleteStudent : ChangeStudentAction
    data object OnCreateStudentClick : ChangeStudentAction
    data object OnImportStudentClick : ChangeStudentAction
    data class OnImportStudentFilePicked(val uri: String) : ChangeStudentAction
    data object OnConfirmImportStudentUpdate : ChangeStudentAction
    data object OnDismissImportStudentUpdate : ChangeStudentAction
}

sealed interface ChangeStudentEffect {
    data object NavigateBack : ChangeStudentEffect
    data class OpenDeck(val deckId: String) : ChangeStudentEffect
    data object OpenCreateStudent : ChangeStudentEffect
    data object ShowStudentUpdateFailed : ChangeStudentEffect
    data object ShowDeleteStudentFailed : ChangeStudentEffect
    data object OpenImportStudentPicker : ChangeStudentEffect
    data object ShowImportStudentFailed : ChangeStudentEffect
}

class ChangeStudentViewModel(
    private val studentDeckRepository: StudentDeckRepository,
    private val preferencesRepository: PreferencesRepository,
    private val studentRepository: StudentRepository,
    private val studentPackageService: StudentPackageService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangeStudentUiState())
    val uiState: StateFlow<ChangeStudentUiState> = _uiState.asStateFlow()

    private val _effects = MutableStateFlow<ChangeStudentEffect?>(null)
    val effects: StateFlow<ChangeStudentEffect?> = _effects.asStateFlow()
    private val activeStudentId = MutableStateFlow<String?>(preferencesRepository.getLastActiveStudentId())

    init {
        observeStudents()
    }

    fun onAction(action: ChangeStudentAction) {
        when (action) {
            is ChangeStudentAction.OnStudentClick -> {
                selectStudent(studentId = action.studentId, deckIdToOpen = null)
            }

            is ChangeStudentAction.OnDeckClick -> {
                selectStudent(studentId = action.studentId, deckIdToOpen = action.deckId)
            }

            is ChangeStudentAction.OnStudentMenuClick -> {
                _uiState.update { it.copy(expandedMenuStudentId = action.studentId) }
            }

            ChangeStudentAction.OnDismissStudentMenu -> {
                _uiState.update { it.copy(expandedMenuStudentId = null) }
            }

            is ChangeStudentAction.OnEditStudentClick -> {
                val student = _uiState.value.students.firstOrNull { it.studentId == action.studentId } ?: return
                _uiState.update {
                    it.copy(
                        expandedMenuStudentId = null,
                        editingStudentId = student.studentId,
                        isEditStudentDialogVisible = true,
                        editStudentName = student.studentName,
                        editStudentAvatarUri = student.avatarUri,
                    )
                }
            }

            ChangeStudentAction.OnDismissEditStudentDialog -> {
                _uiState.update {
                    it.copy(
                        expandedMenuStudentId = null,
                        editingStudentId = null,
                        isEditStudentDialogVisible = false,
                        isPhotoSourceDialogVisible = false,
                        pendingPickerRequest = null,
                    )
                }
            }

            is ChangeStudentAction.OnEditStudentNameChanged -> {
                _uiState.update { it.copy(editStudentName = action.value) }
            }

            ChangeStudentAction.OnEditStudentAvatarClick -> {
                _uiState.update { it.copy(isPhotoSourceDialogVisible = true) }
            }

            ChangeStudentAction.OnDismissPhotoSourceDialog -> {
                _uiState.update { it.copy(isPhotoSourceDialogVisible = false) }
            }

            ChangeStudentAction.OnPickFromGalleryClick -> {
                _uiState.update {
                    it.copy(
                        isPhotoSourceDialogVisible = false,
                        pendingPickerRequest = StudentPickerRequest.GALLERY,
                    )
                }
            }

            ChangeStudentAction.OnTakePhotoClick -> {
                _uiState.update {
                    it.copy(
                        isPhotoSourceDialogVisible = false,
                        pendingPickerRequest = StudentPickerRequest.CAMERA,
                    )
                }
            }

            is ChangeStudentAction.OnPhotoPicked -> {
                _uiState.update { it.copy(editStudentAvatarUri = action.uri) }
            }

            ChangeStudentAction.OnPickerRequestConsumed -> {
                _uiState.update { it.copy(pendingPickerRequest = null) }
            }

            ChangeStudentAction.OnSaveStudentChanges -> saveStudentChanges()

            is ChangeStudentAction.OnDeleteStudentClick -> {
                _uiState.update {
                    it.copy(
                        expandedMenuStudentId = null,
                        deletingStudentId = action.studentId,
                        isDeleteStudentDialogVisible = true,
                    )
                }
            }

            ChangeStudentAction.OnDismissDeleteStudentDialog -> {
                _uiState.update {
                    it.copy(
                        deletingStudentId = null,
                        isDeleteStudentDialogVisible = false,
                    )
                }
            }

            ChangeStudentAction.OnConfirmDeleteStudent -> deleteStudent()

            ChangeStudentAction.OnCreateStudentClick -> {
                _effects.value = ChangeStudentEffect.OpenCreateStudent
            }
            ChangeStudentAction.OnImportStudentClick -> {
                _effects.value = ChangeStudentEffect.OpenImportStudentPicker
            }
            is ChangeStudentAction.OnImportStudentFilePicked -> importStudentArchive(action.uri)
            ChangeStudentAction.OnConfirmImportStudentUpdate -> confirmImportStudentUpdate()
            ChangeStudentAction.OnDismissImportStudentUpdate -> {
                _uiState.update { it.copy(pendingStudentImportConfirmation = null) }
            }
        }
    }

    fun consumeEffect() {
        _effects.value = null
    }

    private fun observeStudents() {
        viewModelScope.launch {
            combine(
                studentDeckRepository.observeStudentsWithDecksOrderedByCreation(),
                activeStudentId,
            ) { studentsWithDecks, preferredId ->
                val resolvedActiveId = resolveActiveStudentId(
                    students = studentsWithDecks,
                    preferredId = preferredId,
                )
                val orderedStudents = if (resolvedActiveId.isNullOrBlank()) {
                    studentsWithDecks
                } else {
                    studentsWithDecks.sortedByDescending { it.student.id == resolvedActiveId }
                }
                ChangeStudentSnapshot(
                    activeStudentId = resolvedActiveId,
                    students = orderedStudents.map { relation ->
                        ChangeStudentListItem(
                            studentId = relation.student.id,
                            studentName = relation.student.name,
                            avatarUri = relation.student.avatarUri,
                            lastLessonDeck = relation.decks.firstOrNull(),
                        )
                    },
                )
            }.collect { snapshot ->
                if (snapshot.activeStudentId != activeStudentId.value) {
                    activeStudentId.value = snapshot.activeStudentId
                }

                when {
                    snapshot.activeStudentId.isNullOrBlank() -> {
                        if (!preferencesRepository.getLastActiveStudentId().isNullOrBlank()) {
                            preferencesRepository.clearLastActiveStudentId()
                        }
                    }

                    preferencesRepository.getLastActiveStudentId() != snapshot.activeStudentId -> {
                        preferencesRepository.setLastActiveStudentId(snapshot.activeStudentId)
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        students = snapshot.students,
                        activeStudentId = snapshot.activeStudentId,
                        expandedMenuStudentId = it.expandedMenuStudentId?.takeIf { id ->
                            snapshot.students.any { student -> student.studentId == id }
                        },
                        editingStudentId = it.editingStudentId?.takeIf { id ->
                            snapshot.students.any { student -> student.studentId == id }
                        },
                        deletingStudentId = it.deletingStudentId?.takeIf { id ->
                            snapshot.students.any { student -> student.studentId == id }
                        },
                        pendingStudentImportConfirmation = it.pendingStudentImportConfirmation,
                    )
                }
            }
        }
    }

    private fun importStudentArchive(uri: String) {
        if (uri.isBlank()) return
        viewModelScope.launch {
            when (val preview = studentPackageService.inspectStudentImport(uri)) {
                is CustomResult.Success -> handleStudentImportPreview(uri, preview.data)
                is CustomResult.Failure -> {
                    _effects.value = ChangeStudentEffect.ShowImportStudentFailed
                }
            }
        }
    }

    private suspend fun handleStudentImportPreview(
        archiveUri: String,
        preview: StudentPackageImportPreview,
    ) {
        val existingStudentName = preview.existingStudentName
        if (!existingStudentName.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    pendingStudentImportConfirmation = ChangeStudentPendingStudentImportConfirmation(
                        archiveUri = archiveUri,
                        importedStudentName = preview.studentName,
                        existingStudentName = existingStudentName,
                        matchedDecksCount = preview.matchedDecksCount,
                        missingDecksCount = preview.missingDecksCount,
                        matchedCardsCount = preview.matchedCardsCount,
                        missingCardsCount = preview.missingCardsCount,
                    )
                )
            }
            return
        }

        executeStudentImport(archiveUri)
    }

    private fun confirmImportStudentUpdate() {
        val archiveUri = _uiState.value.pendingStudentImportConfirmation?.archiveUri ?: return
        _uiState.update { it.copy(pendingStudentImportConfirmation = null) }
        viewModelScope.launch {
            executeStudentImport(archiveUri)
        }
    }

    private suspend fun executeStudentImport(uri: String) {
        when (val result = studentPackageService.importStudent(uri)) {
            is CustomResult.Success -> {
                val importedStudentId = result.data.studentId
                preferencesRepository.setLastActiveStudentId(importedStudentId)
                activeStudentId.value = importedStudentId
                _effects.value = ChangeStudentEffect.NavigateBack
            }
            is CustomResult.Failure -> {
                _effects.value = ChangeStudentEffect.ShowImportStudentFailed
            }
        }
    }

    private fun selectStudent(studentId: String, deckIdToOpen: String?) {
        viewModelScope.launch {
            preferencesRepository.setLastActiveStudentId(studentId)
            activeStudentId.value = studentId
            _effects.value = if (deckIdToOpen == null) {
                ChangeStudentEffect.NavigateBack
            } else {
                ChangeStudentEffect.OpenDeck(deckIdToOpen)
            }
        }
    }

    private fun resolveActiveStudentId(
        students: List<StudentWithDecks>,
        preferredId: String?,
    ): String? {
        if (students.isEmpty()) return null
        if (!preferredId.isNullOrBlank()) {
            if (students.any { it.student.id == preferredId }) {
                return preferredId
            }
            return students.first().student.id
        }
        return null
    }

    private fun saveStudentChanges() {
        val current = _uiState.value
        val studentId = current.editingStudentId ?: return
        val updatedName = current.editStudentName.trim()
        if (updatedName.isBlank()) {
            _effects.value = ChangeStudentEffect.ShowStudentUpdateFailed
            return
        }
        val existingStudent = current.students.firstOrNull { it.studentId == studentId } ?: return
        val updatedAvatarUri = current.editStudentAvatarUri?.trim()?.takeIf { it.isNotBlank() }

        viewModelScope.launch {
            val nameChanged = updatedName != existingStudent.studentName
            val avatarChanged = updatedAvatarUri != existingStudent.avatarUri
            if (!nameChanged && !avatarChanged) {
                _uiState.update {
                    it.copy(
                        editingStudentId = null,
                        isEditStudentDialogVisible = false,
                        isPhotoSourceDialogVisible = false,
                        pendingPickerRequest = null,
                    )
                }
                return@launch
            }

            val updatedNameSuccessfully = !nameChanged || studentRepository.updateName(studentId, updatedName)
            val updatedAvatarSuccessfully = !avatarChanged || studentRepository.updateAvatarUri(studentId, updatedAvatarUri)

            if (updatedNameSuccessfully && updatedAvatarSuccessfully) {
                _uiState.update {
                    it.copy(
                        editingStudentId = null,
                        isEditStudentDialogVisible = false,
                        isPhotoSourceDialogVisible = false,
                        pendingPickerRequest = null,
                    )
                }
            } else {
                _effects.value = ChangeStudentEffect.ShowStudentUpdateFailed
            }
        }
    }

    private fun deleteStudent() {
        val studentId = _uiState.value.deletingStudentId ?: return
        viewModelScope.launch {
            val deleted = studentRepository.deleteStudent(studentId)
            if (!deleted) {
                _effects.value = ChangeStudentEffect.ShowDeleteStudentFailed
                return@launch
            }

            val currentActiveStudentId = _uiState.value.activeStudentId
            if (currentActiveStudentId == studentId) {
                val nextStudentId = studentRepository.getFirstStudentId()
                if (nextStudentId.isNullOrBlank()) {
                    preferencesRepository.clearLastActiveStudentId()
                    activeStudentId.value = null
                } else {
                    preferencesRepository.setLastActiveStudentId(nextStudentId)
                    activeStudentId.value = nextStudentId
                }
            }

            _uiState.update {
                it.copy(
                    deletingStudentId = null,
                    isDeleteStudentDialogVisible = false,
                    editingStudentId = null,
                    isEditStudentDialogVisible = false,
                    isPhotoSourceDialogVisible = false,
                    pendingPickerRequest = null,
                    expandedMenuStudentId = null,
                )
            }
        }
    }
}

private data class ChangeStudentSnapshot(
    val activeStudentId: String?,
    val students: List<ChangeStudentListItem>,
)
