package com.cerebus.readwrite.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.deck_package.domain.service.DeckImportMode
import com.cerebus.core.deck_package.domain.service.DeckPackageImportPreview
import com.cerebus.core.deck_package.domain.service.DeckPackageService
import com.cerebus.core.game_engine.domain.logic.SrsAvailability
import com.cerebus.core.game_engine.domain.logic.SrsSessionCandidate
import com.cerebus.core.game_engine.domain.logic.planSrsSession
import com.cerebus.core.game_engine.domain.logic.toAvailability
import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.SrsConfig
import com.cerebus.core.game_engine.domain.repository.CardProgressRepository
import com.cerebus.core.game_engine.domain.repository.ReviewLogRepository
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository
import com.cerebus.core.utils.CustomResult
import com.cerebus.core.utils.localStartOfDayMillis
import com.cerebus.core.utils.nowMillis
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.flashcards.domain.models.Flashcard
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.student.domain.repositories.StudentRepository
import com.cerebus.data.studentdeck.domain.models.StudentWithDecks
import com.cerebus.data.studentdeck.domain.repositories.StudentDeckRepository
import com.cerebus.core.utils.GameLaunchMode
import com.cerebus.readwrite.navigation.CreateStudentNavigationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

private val learnedLevelThreshold = SrsConfig().learnedLevelThreshold
private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

data class PendingStudentDeckImportConfirmation(
    val archiveUri: String,
    val importedDeckName: String,
    val existingDeckName: String,
    val matchingCardsCount: Int,
    val newCardsCount: Int,
    val staleCardsCount: Int,
)

data class ActiveStudentUiState(
    val isLoading: Boolean = true,
    val studentId: String? = null,
    val studentName: String = "",
    val studentAvatarUri: String? = null,
    val activeLetters: String = "",
    val activeDecks: List<DeckProgressItem> = emptyList(),
    val studiedDecks: List<DeckProgressItem> = emptyList(),
    val otherDecks: List<DeckProgressItem> = emptyList(),
    val isTrainingModeDialogVisible: Boolean = false,
    val isGalleryDeckDialogVisible: Boolean = false,
    val isEditStudentDialogVisible: Boolean = false,
    val isEditStudentPhotoSourceDialogVisible: Boolean = false,
    val isDeleteStudentDialogVisible: Boolean = false,
    val pendingDeckImportConfirmation: PendingStudentDeckImportConfirmation? = null,
    val editStudentName: String = "",
    val editStudentAvatarUri: String? = null,
    val pendingPickerRequest: StudentPickerRequest? = null,
    val srsAvailability: SrsAvailability? = null,
)

sealed interface ActiveStudentAction {
    data object OnStartClick : ActiveStudentAction
    data object OnDismissTrainingModeDialog : ActiveStudentAction
    data class OnTrainingModeSelected(val mode: GameLaunchMode) : ActiveStudentAction
    data object OnGalleryClick : ActiveStudentAction
    data object OnDismissGalleryDeckDialog : ActiveStudentAction
    data class OnGalleryDeckSelected(val deckId: String) : ActiveStudentAction
    data object OnChangeStudentClick : ActiveStudentAction
    data object OnMoreDecksClick : ActiveStudentAction
    data object OnCreateDeckClick : ActiveStudentAction
    data object OnImportDeckClick : ActiveStudentAction
    data class OnImportDeckFilePicked(val uri: String) : ActiveStudentAction
    data object OnConfirmImportDeckReplacement : ActiveStudentAction
    data object OnConfirmImportDeckAddMissingCards : ActiveStudentAction
    data object OnDismissImportDeckReplacement : ActiveStudentAction
    data class OnDeckClick(val deckId: String) : ActiveStudentAction
    data object OnEditStudentClick : ActiveStudentAction
    data object OnDismissEditStudentDialog : ActiveStudentAction
    data class OnEditStudentNameChanged(val value: String) : ActiveStudentAction
    data object OnEditStudentAvatarClick : ActiveStudentAction
    data object OnDismissEditStudentPhotoSourceDialog : ActiveStudentAction
    data object OnEditStudentPickFromGalleryClick : ActiveStudentAction
    data object OnEditStudentTakePhotoClick : ActiveStudentAction
    data class OnEditStudentPhotoPicked(val uri: String) : ActiveStudentAction
    data object OnEditStudentPickerRequestConsumed : ActiveStudentAction
    data object OnSaveStudentChanges : ActiveStudentAction
    data object OnDeleteStudentClick : ActiveStudentAction
    data object OnDismissDeleteStudentDialog : ActiveStudentAction
    data object OnConfirmDeleteStudent : ActiveStudentAction
}

sealed interface ActiveStudentEffect {
    data class OpenDeck(val deckId: String) : ActiveStudentEffect
    data class OpenGame(
        val deckIds: List<String>,
        val mode: GameLaunchMode,
    ) : ActiveStudentEffect
    data class OpenDeckList(val openCreateDialog: Boolean) : ActiveStudentEffect
    data class OpenDeckGallery(val deckId: String) : ActiveStudentEffect
    data object OpenImportDeckPicker : ActiveStudentEffect
    data object ShowImportDeckFailed : ActiveStudentEffect
    data object ShowStudentUpdateFailed : ActiveStudentEffect
    data object ShowDeleteStudentFailed : ActiveStudentEffect
    data object OpenChangeStudent : ActiveStudentEffect
}

class ActiveStudentViewModel(
    private val studentDeckRepository: StudentDeckRepository,
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val cardProgressRepository: CardProgressRepository,
    private val studentPrefsRepository: StudentPrefsRepository,
    private val reviewLogRepository: ReviewLogRepository,
    private val preferencesRepository: PreferencesRepository,
    private val deckPackageService: DeckPackageService,
    private val studentRepository: StudentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveStudentUiState())
    val uiState: StateFlow<ActiveStudentUiState> = _uiState.asStateFlow()

    private val _effects = MutableStateFlow<ActiveStudentEffect?>(null)
    val effects: StateFlow<ActiveStudentEffect?> = _effects.asStateFlow()

    private val preferredStudentId = MutableStateFlow<String?>(preferencesRepository.getLastActiveStudentId())

    init {
        observeActiveStudent()
    }

    fun onAction(action: ActiveStudentAction) {
        when (action) {
            ActiveStudentAction.OnStartClick -> {
                if (_uiState.value.activeDecks.isEmpty()) return
                _uiState.update { it.copy(isTrainingModeDialogVisible = true) }
            }

            ActiveStudentAction.OnDismissTrainingModeDialog -> {
                _uiState.update { it.copy(isTrainingModeDialogVisible = false) }
            }

            is ActiveStudentAction.OnTrainingModeSelected -> {
                val deckIds = _uiState.value.activeDecks.map { it.deck.id }
                if (deckIds.isEmpty()) return
                _uiState.update { it.copy(isTrainingModeDialogVisible = false) }
                _effects.value = ActiveStudentEffect.OpenGame(
                    deckIds = deckIds,
                    mode = action.mode,
                )
            }

            ActiveStudentAction.OnGalleryClick -> {
                _uiState.update {
                    it.copy(
                        isTrainingModeDialogVisible = false,
                        isGalleryDeckDialogVisible = true,
                    )
                }
            }

            ActiveStudentAction.OnDismissGalleryDeckDialog -> {
                _uiState.update { it.copy(isGalleryDeckDialogVisible = false) }
            }

            is ActiveStudentAction.OnGalleryDeckSelected -> {
                _uiState.update { it.copy(isGalleryDeckDialogVisible = false) }
                _effects.value = ActiveStudentEffect.OpenDeckGallery(action.deckId)
            }

            ActiveStudentAction.OnChangeStudentClick -> {
                _effects.value = ActiveStudentEffect.OpenChangeStudent
            }

            ActiveStudentAction.OnEditStudentClick -> {
                val current = _uiState.value
                _uiState.update {
                    it.copy(
                        isEditStudentDialogVisible = true,
                        editStudentName = current.studentName,
                        editStudentAvatarUri = current.studentAvatarUri,
                    )
                }
            }

            ActiveStudentAction.OnDismissEditStudentDialog -> {
                _uiState.update {
                    it.copy(
                        isEditStudentDialogVisible = false,
                        isEditStudentPhotoSourceDialogVisible = false,
                        pendingPickerRequest = null,
                    )
                }
            }

            is ActiveStudentAction.OnEditStudentNameChanged -> {
                _uiState.update { it.copy(editStudentName = action.value) }
            }

            ActiveStudentAction.OnEditStudentAvatarClick -> {
                _uiState.update { it.copy(isEditStudentPhotoSourceDialogVisible = true) }
            }

            ActiveStudentAction.OnDismissEditStudentPhotoSourceDialog -> {
                _uiState.update { it.copy(isEditStudentPhotoSourceDialogVisible = false) }
            }

            ActiveStudentAction.OnEditStudentPickFromGalleryClick -> {
                _uiState.update {
                    it.copy(
                        isEditStudentPhotoSourceDialogVisible = false,
                        pendingPickerRequest = StudentPickerRequest.GALLERY,
                    )
                }
            }

            ActiveStudentAction.OnEditStudentTakePhotoClick -> {
                _uiState.update {
                    it.copy(
                        isEditStudentPhotoSourceDialogVisible = false,
                        pendingPickerRequest = StudentPickerRequest.CAMERA,
                    )
                }
            }

            is ActiveStudentAction.OnEditStudentPhotoPicked -> {
                _uiState.update { it.copy(editStudentAvatarUri = action.uri) }
            }

            ActiveStudentAction.OnEditStudentPickerRequestConsumed -> {
                _uiState.update { it.copy(pendingPickerRequest = null) }
            }

            ActiveStudentAction.OnSaveStudentChanges -> saveStudentChanges()

            ActiveStudentAction.OnDeleteStudentClick -> {
                _uiState.update { it.copy(isDeleteStudentDialogVisible = true) }
            }

            ActiveStudentAction.OnDismissDeleteStudentDialog -> {
                _uiState.update { it.copy(isDeleteStudentDialogVisible = false) }
            }

            ActiveStudentAction.OnConfirmDeleteStudent -> deleteCurrentStudent()

            ActiveStudentAction.OnMoreDecksClick -> {
                _effects.value = ActiveStudentEffect.OpenDeckList(openCreateDialog = false)
            }

            ActiveStudentAction.OnCreateDeckClick -> {
                _effects.value = ActiveStudentEffect.OpenDeckList(openCreateDialog = true)
            }
            ActiveStudentAction.OnImportDeckClick -> {
                _effects.value = ActiveStudentEffect.OpenImportDeckPicker
            }
            is ActiveStudentAction.OnImportDeckFilePicked -> importDeckArchive(action.uri)
            ActiveStudentAction.OnConfirmImportDeckReplacement -> confirmImportDeckReplacement()
            ActiveStudentAction.OnConfirmImportDeckAddMissingCards -> confirmImportDeckAddMissingCards()
            ActiveStudentAction.OnDismissImportDeckReplacement -> {
                _uiState.update { it.copy(pendingDeckImportConfirmation = null) }
            }

            is ActiveStudentAction.OnDeckClick -> {
                openDeck(action.deckId)
            }
        }
    }

    private fun openDeck(deckId: String) {
        val state = _uiState.value
        val studentId = state.studentId
        if (studentId.isNullOrBlank()) {
            _effects.value = ActiveStudentEffect.OpenDeck(deckId)
            return
        }

        val clickedOtherDeck = state.otherDecks.any { it.deck.id == deckId }
        if (!clickedOtherDeck) {
            _effects.value = ActiveStudentEffect.OpenDeck(deckId)
            return
        }

        viewModelScope.launch {
            runCatching {
                studentDeckRepository.assignDeckToStudent(
                    studentId = studentId,
                    deckId = deckId,
                )
            }
            _effects.value = ActiveStudentEffect.OpenDeck(deckId)
        }
    }

    private fun importDeckArchive(uri: String) {
        if (uri.isBlank()) return
        viewModelScope.launch {
            when (val preview = deckPackageService.inspectDeckImport(uri)) {
                is CustomResult.Success -> handleDeckImportPreview(
                    archiveUri = uri,
                    preview = preview.data,
                )
                is CustomResult.Failure -> {
                    _effects.value = ActiveStudentEffect.ShowImportDeckFailed
                }
            }
        }
    }

    private fun confirmImportDeckReplacement() {
        val archiveUri = _uiState.value.pendingDeckImportConfirmation?.archiveUri ?: return
        _uiState.update { it.copy(pendingDeckImportConfirmation = null) }
        viewModelScope.launch {
            executeDeckImport(
                uri = archiveUri,
                mode = DeckImportMode.REPLACE_EXISTING,
            )
        }
    }

    private fun confirmImportDeckAddMissingCards() {
        val archiveUri = _uiState.value.pendingDeckImportConfirmation?.archiveUri ?: return
        _uiState.update { it.copy(pendingDeckImportConfirmation = null) }
        viewModelScope.launch {
            executeDeckImport(
                uri = archiveUri,
                mode = DeckImportMode.ADD_MISSING_CARDS,
            )
        }
    }

    private suspend fun handleDeckImportPreview(
        archiveUri: String,
        preview: DeckPackageImportPreview,
    ) {
        val existingDeckName = preview.existingDeckName
        if (!existingDeckName.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    pendingDeckImportConfirmation = PendingStudentDeckImportConfirmation(
                        archiveUri = archiveUri,
                        importedDeckName = preview.deckName,
                        existingDeckName = existingDeckName,
                        matchingCardsCount = preview.matchingCardsCount,
                        newCardsCount = preview.newCardsCount,
                        staleCardsCount = preview.staleCardsCount,
                    )
                )
            }
            return
        }

        executeDeckImport(archiveUri)
    }

    private suspend fun executeDeckImport(
        uri: String,
        mode: DeckImportMode = DeckImportMode.REPLACE_EXISTING,
    ) {
        val studentId = _uiState.value.studentId
        when (
            val result = deckPackageService.importDeck(
                archiveUri = uri,
                assignToStudentId = studentId,
                mode = mode,
            )
        ) {
                is CustomResult.Success -> {
                    // Active decks list updates automatically from repository observers.
                }
                is CustomResult.Failure -> {
                    _effects.value = ActiveStudentEffect.ShowImportDeckFailed
                }
        }
    }

    private fun saveStudentChanges() {
        val current = _uiState.value
        val studentId = current.studentId ?: return
        val updatedName = current.editStudentName.trim()
        if (updatedName.isBlank()) {
            _effects.value = ActiveStudentEffect.ShowStudentUpdateFailed
            return
        }
        val updatedAvatarUri = current.editStudentAvatarUri?.trim()?.takeIf { it.isNotBlank() }

        viewModelScope.launch {
            val nameChanged = updatedName != current.studentName
            val avatarChanged = updatedAvatarUri != current.studentAvatarUri
            if (!nameChanged && !avatarChanged) {
                _uiState.update {
                    it.copy(
                        isEditStudentDialogVisible = false,
                        isEditStudentPhotoSourceDialogVisible = false,
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
                        isEditStudentDialogVisible = false,
                        isEditStudentPhotoSourceDialogVisible = false,
                        pendingPickerRequest = null,
                    )
                }
            } else {
                _effects.value = ActiveStudentEffect.ShowStudentUpdateFailed
            }
        }
    }

    private fun deleteCurrentStudent() {
        val studentId = _uiState.value.studentId ?: return
        viewModelScope.launch {
            val deleted = studentRepository.deleteStudent(studentId)
            if (!deleted) {
                _effects.value = ActiveStudentEffect.ShowDeleteStudentFailed
                return@launch
            }

            val nextStudentId = studentRepository.getFirstStudentId()
            _uiState.update {
                it.copy(
                    isDeleteStudentDialogVisible = false,
                    isEditStudentDialogVisible = false,
                    isEditStudentPhotoSourceDialogVisible = false,
                    pendingPickerRequest = null,
                )
            }

            if (nextStudentId.isNullOrBlank()) {
                preferredStudentId.value = null
                preferencesRepository.clearLastActiveStudentId()
                _effects.value = ActiveStudentEffect.OpenChangeStudent
            } else {
                preferredStudentId.value = nextStudentId
                preferencesRepository.setLastActiveStudentId(nextStudentId)
            }
        }
    }

    fun consumeEffect() {
        _effects.value = null
    }

    fun onScreenShown() {
        val pendingCreatedStudentId = CreateStudentNavigationState.consumePendingCreatedStudentId()
        if (!pendingCreatedStudentId.isNullOrBlank()) {
            if (preferredStudentId.value != pendingCreatedStudentId) {
                preferredStudentId.value = pendingCreatedStudentId
            }
            if (preferencesRepository.getLastActiveStudentId() != pendingCreatedStudentId) {
                preferencesRepository.setLastActiveStudentId(pendingCreatedStudentId)
            }
            return
        }

        val storedId = preferencesRepository.getLastActiveStudentId()
        if (preferredStudentId.value != storedId) {
            preferredStudentId.value = storedId
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeActiveStudent() {
        viewModelScope.launch {
            combine(
                studentDeckRepository.observeStudentsWithDecksOrderedByCreation(),
                preferredStudentId,
                deckRepository.observeAllDecks(),
            ) { students, preferredId, allDecks ->
                ActiveStudentSnapshot(
                    preferredId = preferredId,
                    activeStudent = resolveActiveStudent(students, preferredId),
                    allDecks = allDecks,
                )
            }.flatMapLatest { snapshot ->
                val relation = snapshot.activeStudent ?: return@flatMapLatest flowOf(
                    ActiveStudentUiState(
                        isLoading = false,
                        studentId = null,
                        studentName = "",
                        activeLetters = "",
                        activeDecks = emptyList(),
                        studiedDecks = emptyList(),
                        otherDecks = emptyList(),
                        srsAvailability = null,
                    )
                )

                val assignedDeckIds = relation.decks.map { it.id }.toSet()
                combine(
                    observeDeckCards(snapshot.allDecks.map { it.id }),
                    cardProgressRepository.observeProgress(relation.student.id),
                ) { cardsByDeck, progressList ->
                    val buckets = classifyDecks(
                        allDecks = snapshot.allDecks,
                        assignedDeckIds = assignedDeckIds,
                        cardsByDeck = cardsByDeck,
                        progressByCardId = progressList.associateBy { progress -> progress.cardId },
                    )
                    val progressByCardId = progressList.associateBy { progress -> progress.cardId }
                    val srsAvailability = buildSrsAvailability(
                        studentId = relation.student.id,
                        activeDeckIds = buckets.activeDecks.map { deck -> deck.deck.id },
                        cardsByDeck = cardsByDeck,
                        progressByCardId = progressByCardId,
                    )
                    ActiveStudentUiState(
                        isLoading = false,
                        studentId = relation.student.id,
                        studentName = relation.student.name,
                        studentAvatarUri = relation.student.avatarUri,
                        activeLetters = relation.student.activeLetters,
                        activeDecks = buckets.activeDecks,
                        studiedDecks = buckets.studiedDecks,
                        otherDecks = buckets.otherDecks,
                        isTrainingModeDialogVisible = _uiState.value.isTrainingModeDialogVisible,
                        isGalleryDeckDialogVisible = _uiState.value.isGalleryDeckDialogVisible,
                        isEditStudentDialogVisible = _uiState.value.isEditStudentDialogVisible,
                        isEditStudentPhotoSourceDialogVisible = _uiState.value.isEditStudentPhotoSourceDialogVisible,
                        isDeleteStudentDialogVisible = _uiState.value.isDeleteStudentDialogVisible,
                        editStudentName = _uiState.value.editStudentName,
                        editStudentAvatarUri = _uiState.value.editStudentAvatarUri,
                        pendingPickerRequest = _uiState.value.pendingPickerRequest,
                        srsAvailability = srsAvailability,
                    )
                }
            }.collect { state ->
                val studentId = state.studentId
                if (studentId.isNullOrBlank()) {
                    if (!preferencesRepository.getLastActiveStudentId().isNullOrBlank()) {
                        preferencesRepository.clearLastActiveStudentId()
                    }
                    if (!preferredStudentId.value.isNullOrBlank()) {
                        preferredStudentId.value = null
                    }
                    _uiState.value = state
                    return@collect
                }

                if (preferredStudentId.value != studentId) {
                    preferredStudentId.value = studentId
                }
                if (preferencesRepository.getLastActiveStudentId() != studentId) {
                    preferencesRepository.setLastActiveStudentId(studentId)
                }
                _uiState.value = state
            }
        }
    }

    private fun observeDeckCards(deckIds: List<String>): Flow<Map<String, List<Flashcard>>> =
        if (deckIds.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(
                deckIds.map { deckId ->
                    flashcardRepository.observeFlashcardsByDeckId(deckId)
                        .map { cards -> deckId to cards }
                }
            ) { pairs ->
                pairs.associate { pair -> pair.first to pair.second }
            }
        }

    private fun classifyDecks(
        allDecks: List<Deck>,
        assignedDeckIds: Set<String>,
        cardsByDeck: Map<String, List<Flashcard>>,
        progressByCardId: Map<String, CardProgress>,
    ): DeckBuckets {
        val active = mutableListOf<DeckProgressItem>()
        val studied = mutableListOf<DeckProgressItem>()
        val other = mutableListOf<DeckProgressItem>()

        allDecks.forEach { deck ->
            val cards = cardsByDeck[deck.id].orEmpty()
            val learnedCards = cards.count { card ->
                val progress = progressByCardId[card.id] ?: return@count false
                progress.level >= learnedLevelThreshold
            }
            val deckProgress = DeckProgressItem(
                deck = deck,
                learnedCards = learnedCards,
                totalCards = cards.size,
            )
            val isStudied = cards.isNotEmpty() && cards.all { card ->
                val progress = progressByCardId[card.id] ?: return@all false
                progress.level >= learnedLevelThreshold
            }

            when {
                isStudied -> studied += deckProgress
                deck.id in assignedDeckIds -> active += deckProgress
                else -> other += deckProgress
            }
        }

        return DeckBuckets(
            activeDecks = active,
            studiedDecks = studied,
            otherDecks = other,
        )
    }

    private fun resolveActiveStudent(
        students: List<StudentWithDecks>,
        preferredId: String?,
    ): StudentWithDecks? {
        if (students.isEmpty()) return null
        if (!preferredId.isNullOrBlank()) {
            students.firstOrNull { it.student.id == preferredId }?.let { return it }
        }
        return students.first()
    }

    private suspend fun buildSrsAvailability(
        studentId: String,
        activeDeckIds: List<String>,
        cardsByDeck: Map<String, List<Flashcard>>,
        progressByCardId: Map<String, CardProgress>,
    ): SrsAvailability? {
        if (studentId.isBlank() || activeDeckIds.isEmpty()) return null

        val prefs = runCatching { studentPrefsRepository.getPrefs(studentId) }.getOrNull() ?: return null
        val normalizedReviewLimit = prefs.reviewsPerSession.coerceAtLeast(0)
        val normalizedNewSessionLimit = prefs.newCardsPerSession.coerceAtLeast(0)
        val normalizedNewDailyLimit = prefs.maxNewCardsPerDay.coerceAtLeast(0)
        val guidedHintThreshold = prefs.guidedHintSuccessThreshold
            .coerceIn(0, 5)
        val dayStartMillis = localStartOfDayMillis()
        val introducedTodayCardIds = reviewLogRepository.getCardIdsFirstReviewedSince(
            studentId = studentId,
            sinceEpochMillis = dayStartMillis,
        )
        val activeDeckCardIds = activeDeckIds
            .flatMapTo(mutableSetOf()) { deckId ->
                cardsByDeck[deckId].orEmpty().map { card -> card.id }
            }
        val remainingDailyNewSlots = (normalizedNewDailyLimit - introducedTodayCardIds.count { it in activeDeckCardIds })
            .coerceAtLeast(0)

        val candidates = activeDeckIds.flatMap { deckId ->
            cardsByDeck[deckId].orEmpty().map { card ->
                val progress = progressByCardId[card.id]
                SrsSessionCandidate(
                    item = card.id,
                    deckId = deckId,
                    cardId = card.id,
                    progressLevel = progress?.level,
                    dueAtEpochMillis = progress?.dueAtEpochMillis,
                    showHintInitially = shouldShowHintInitially(
                        progress = progress,
                        guidedHintThreshold = guidedHintThreshold,
                    ),
                )
            }
        }
        val planningResult = planSrsSession(
            candidates = candidates,
            introducedTodayCardIds = introducedTodayCardIds,
            reviewLimit = normalizedReviewLimit,
            newSessionLimit = normalizedNewSessionLimit,
            remainingDailyNewLimit = remainingDailyNewSlots,
            nowEpochMillis = nowMillis(),
            dayEndEpochMillis = dayStartMillis + MILLIS_PER_DAY,
            learnedLevelThreshold = learnedLevelThreshold,
        )
        return planningResult.toAvailability()
    }
}

private fun shouldShowHintInitially(
    progress: CardProgress?,
    guidedHintThreshold: Int,
): Boolean {
    if (guidedHintThreshold <= 0) return false
    if (progress == null) return true
    return progress.copySuccessStreak < guidedHintThreshold
}

private data class ActiveStudentSnapshot(
    val preferredId: String?,
    val activeStudent: StudentWithDecks?,
    val allDecks: List<Deck>,
)

data class DeckProgressItem(
    val deck: Deck,
    val learnedCards: Int,
    val totalCards: Int,
)

private data class DeckBuckets(
    val activeDecks: List<DeckProgressItem>,
    val studiedDecks: List<DeckProgressItem>,
    val otherDecks: List<DeckProgressItem>,
)
