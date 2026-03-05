package com.cerebus.readwrite.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.CardState
import com.cerebus.core.game_engine.domain.model.Grade
import com.cerebus.core.game_engine.domain.repository.CardProgressRepository
import com.cerebus.core.game_engine.domain.repository.ReviewLogRepository
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.flashcards.domain.models.Flashcard
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.studentdeck.domain.models.StudentWithDecks
import com.cerebus.data.studentdeck.domain.repositories.StudentDeckRepository
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

private const val MIN_MASTERED_INTERVAL_DAYS = 3.0
private const val MIN_SUCCESS_STREAK = 3

data class ActiveStudentUiState(
    val isLoading: Boolean = true,
    val studentId: String? = null,
    val studentName: String = "",
    val activeDecks: List<Deck> = emptyList(),
    val studiedDecks: List<Deck> = emptyList(),
    val otherDecks: List<Deck> = emptyList(),
)

sealed interface ActiveStudentAction {
    data object OnStartClick : ActiveStudentAction
    data object OnChangeStudentClick : ActiveStudentAction
    data object OnMoreDecksClick : ActiveStudentAction
    data object OnCreateDeckClick : ActiveStudentAction
    data class OnDeckClick(val deckId: String) : ActiveStudentAction
}

sealed interface ActiveStudentEffect {
    data class OpenDeck(val deckId: String) : ActiveStudentEffect
    data class OpenGame(val deckIds: List<String>) : ActiveStudentEffect
    data class OpenDeckList(val openCreateDialog: Boolean) : ActiveStudentEffect
    data object OpenChangeStudent : ActiveStudentEffect
}

class ActiveStudentViewModel(
    private val studentDeckRepository: StudentDeckRepository,
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val cardProgressRepository: CardProgressRepository,
    private val reviewLogRepository: ReviewLogRepository,
    private val preferencesRepository: PreferencesRepository,
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
                val deckIds = _uiState.value.activeDecks.map { it.id }
                if (deckIds.isEmpty()) return
                _effects.value = ActiveStudentEffect.OpenGame(deckIds)
            }

            ActiveStudentAction.OnChangeStudentClick -> {
                _effects.value = ActiveStudentEffect.OpenChangeStudent
            }

            ActiveStudentAction.OnMoreDecksClick -> {
                _effects.value = ActiveStudentEffect.OpenDeckList(openCreateDialog = false)
            }

            ActiveStudentAction.OnCreateDeckClick -> {
                _effects.value = ActiveStudentEffect.OpenDeckList(openCreateDialog = true)
            }

            is ActiveStudentAction.OnDeckClick -> {
                _effects.value = ActiveStudentEffect.OpenDeck(action.deckId)
            }
        }
    }

    fun consumeEffect() {
        _effects.value = null
    }

    fun onScreenShown() {
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
                        activeDecks = emptyList(),
                        studiedDecks = emptyList(),
                        otherDecks = emptyList(),
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
                        successStreakByCardId = reviewLogRepository.getRecentGradesByCards(
                            studentId = relation.student.id,
                            cardIds = cardsByDeck.values.flatten().map { card -> card.id }.distinct(),
                            limitPerCard = MIN_SUCCESS_STREAK,
                        ).mapValues { (_, grades) ->
                            calculateSuccessStreak(grades)
                        },
                    )
                    ActiveStudentUiState(
                        isLoading = false,
                        studentId = relation.student.id,
                        studentName = relation.student.name,
                        activeDecks = buckets.activeDecks,
                        studiedDecks = buckets.studiedDecks,
                        otherDecks = buckets.otherDecks,
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
        successStreakByCardId: Map<String, Int>,
    ): DeckBuckets {
        val active = mutableListOf<Deck>()
        val studied = mutableListOf<Deck>()
        val other = mutableListOf<Deck>()

        allDecks.forEach { deck ->
            val cards = cardsByDeck[deck.id].orEmpty()
            val isStudied = cards.isNotEmpty() && cards.all { card ->
                val progress = progressByCardId[card.id] ?: return@all false
                progress.state == CardState.REVIEW &&
                    progress.intervalDays >= MIN_MASTERED_INTERVAL_DAYS &&
                    (successStreakByCardId[card.id] ?: 0) >= MIN_SUCCESS_STREAK
            }

            when {
                isStudied -> studied += deck
                deck.id in assignedDeckIds -> active += deck
                else -> other += deck
            }
        }

        return DeckBuckets(
            activeDecks = active,
            studiedDecks = studied,
            otherDecks = other,
        )
    }

    private fun calculateSuccessStreak(grades: List<Grade>): Int {
        var streak = 0
        for (grade in grades) {
            if (grade == Grade.AGAIN) break
            streak++
        }
        return streak
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
}

private data class ActiveStudentSnapshot(
    val preferredId: String?,
    val activeStudent: StudentWithDecks?,
    val allDecks: List<Deck>,
)

private data class DeckBuckets(
    val activeDecks: List<Deck>,
    val studiedDecks: List<Deck>,
    val otherDecks: List<Deck>,
)
