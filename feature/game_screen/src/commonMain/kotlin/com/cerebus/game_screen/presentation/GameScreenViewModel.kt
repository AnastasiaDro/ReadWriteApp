package com.cerebus.game_screen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.game_engine.domain.model.Grade
import com.cerebus.core.game_engine.domain.usecase.SubmitAnswerAndRescheduleUseCase
import com.cerebus.core.game_engine.domain.usecase.SubmitAnswerCommand
import com.cerebus.core.utils.nowMillis
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val FEEDBACK_DURATION_MS = 1200L

class GameScreenViewModel(
    deckIds: List<String>,
    private val flashcardRepository: FlashcardRepository,
    private val deckRepository: DeckRepository,
    private val preferencesRepository: PreferencesRepository,
    private val submitAnswerAndRescheduleUseCase: SubmitAnswerAndRescheduleUseCase,
) : ViewModel() {
    private val selectedDeckIds = deckIds.filter { it.isNotBlank() }.distinct()

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _effects = MutableStateFlow<GameScreenEffect?>(null)
    val effects: StateFlow<GameScreenEffect?> = _effects.asStateFlow()

    private var session: GameSessionData? = null
    private var feedbackJob: Job? = null

    init {
        loadDeckCards()
    }

    fun onAction(action: GameScreenAction) {
        when (action) {
            is GameScreenAction.OnAnswerChanged -> updateAnswer(action.value)
            GameScreenAction.OnCheckClick -> checkAnswer()
            GameScreenAction.OnRetryClick -> restart()
            GameScreenAction.OnBackToStudentClick -> {
                _effects.value = GameScreenEffect.OpenActiveStudent
            }
            GameScreenAction.OnCloseClick -> {
                _effects.value = GameScreenEffect.CloseGame
            }
        }
    }

    fun consumeEffect() {
        _effects.value = null
    }

    private fun loadDeckCards() {
        viewModelScope.launch {
            _uiState.value = GameUiState.Loading

            if (selectedDeckIds.isEmpty()) {
                _uiState.value = GameUiState.Finished(
                    deckTitle = "Колода",
                    totalCards = 0,
                    correctAnswers = 0,
                )
                return@launch
            }

            val decksById = selectedDeckIds.associateWith { deckId ->
                deckRepository.getDeckById(deckId)
            }
            val deckData = GameDeckData(
                title = buildDeckTitle(selectedDeckIds, decksById),
            )
            val activeStudentId = preferencesRepository.getLastActiveStudentId().orEmpty()

            val mixedCards = buildMixedCards(
                deckIds = selectedDeckIds,
                flashcardRepository = flashcardRepository,
            )

            val initialSession = GameSessionData(
                deck = deckData,
                cards = mixedCards,
                studentId = activeStudentId,
                cardShownAtEpochMillis = nowMillis(),
                attemptIndex = 1,
            )
            session = initialSession

            _uiState.value = if (mixedCards.isEmpty()) {
                initialSession.toFinishedUiState()
            } else {
                initialSession.toActiveUiState()
            }
        }
    }

    private fun updateAnswer(value: String) {
        val current = session ?: return
        if (current.isFinished) return

        val updated = current.copy(
            answerInput = value,
            feedback = null,
        )
        session = updated
        _uiState.value = updated.toActiveUiState()
    }

    private fun checkAnswer() {
        val current = session ?: return
        if (current.isFinished) return

        val card = current.currentCard ?: return
        val studentId = current.studentId

        if (studentId.isBlank()) {
            val isCorrect = current.answerInput.trim().equals(card.answer.trim(), ignoreCase = true)
            if (isCorrect) handleCorrectAnswer(current) else handleWrongAnswer(current)
            return
        }

        viewModelScope.launch {
            val submitResult = submitAnswerAndRescheduleUseCase(
                SubmitAnswerCommand(
                    studentId = studentId,
                    cardId = card.id,
                    expectedAnswers = listOf(card.answer),
                    userInput = current.answerInput,
                    shownAtEpochMillis = current.cardShownAtEpochMillis,
                    submittedAtEpochMillis = nowMillis(),
                    usedHint = current.isHintVisible,
                    attemptIndex = current.attemptIndex,
                )
            )

            val actual = session ?: return@launch
            if (actual.currentCard?.id != card.id) return@launch

            if (submitResult.grade == Grade.AGAIN) {
                handleWrongAnswer(
                    actual.copy(attemptIndex = actual.attemptIndex + 1),
                )
            } else {
                handleCorrectAnswer(actual)
            }
        }
    }

    private fun handleCorrectAnswer(current: GameSessionData) {
        feedbackJob?.cancel()

        val withFeedback = current.copy(
            isHintVisible = false,
            feedback = GameFeedbackData(isCorrect = true),
        )
        session = withFeedback
        _uiState.value = withFeedback.toActiveUiState()

        feedbackJob = viewModelScope.launch {
            delay(FEEDBACK_DURATION_MS)

            val nextIndex = withFeedback.currentIndex + 1
            val progressed = withFeedback.copy(
                currentIndex = nextIndex,
                correctAnswers = withFeedback.correctAnswers + 1,
                answerInput = "",
                isHintVisible = false,
                feedback = null,
                cardShownAtEpochMillis = nowMillis(),
                attemptIndex = 1,
            )
            session = progressed

            _uiState.value = if (progressed.isFinished) {
                progressed.toFinishedUiState()
            } else {
                progressed.toActiveUiState()
            }
        }
    }

    private fun handleWrongAnswer(current: GameSessionData) {
        feedbackJob?.cancel()

        val withFeedback = current.copy(
            isHintVisible = true,
            feedback = GameFeedbackData(isCorrect = false),
        )
        session = withFeedback
        _uiState.value = withFeedback.toActiveUiState()

        feedbackJob = viewModelScope.launch {
            delay(FEEDBACK_DURATION_MS)
            val cleared = withFeedback.copy(feedback = null)
            session = cleared
            _uiState.value = cleared.toActiveUiState()
        }
    }

    private fun restart() {
        feedbackJob?.cancel()

        val current = session ?: return
        val restarted = current.copy(
            currentIndex = 0,
            correctAnswers = 0,
            answerInput = "",
            isHintVisible = false,
            feedback = null,
            cardShownAtEpochMillis = nowMillis(),
            attemptIndex = 1,
        )
        session = restarted

        _uiState.value = if (restarted.cards.isEmpty()) {
            restarted.toFinishedUiState()
        } else {
            restarted.toActiveUiState()
        }
    }
}

private data class GameDeckData(
    val title: String,
)

private data class GameCardData(
    val id: String,
    val answer: String,
    val imagePath: String?,
)

private data class GameSessionData(
    val deck: GameDeckData,
    val cards: List<GameCardData>,
    val studentId: String,
    val currentIndex: Int = 0,
    val correctAnswers: Int = 0,
    val answerInput: String = "",
    val isHintVisible: Boolean = false,
    val cardShownAtEpochMillis: Long,
    val attemptIndex: Int,
    val feedback: GameFeedbackData? = null,
) {
    val currentCard: GameCardData?
        get() = cards.getOrNull(currentIndex)

    val isFinished: Boolean
        get() = currentIndex >= cards.size
}

private data class GameFeedbackData(
    val isCorrect: Boolean,
)

private suspend fun GameScreenViewModel.buildMixedCards(
    deckIds: List<String>,
    flashcardRepository: FlashcardRepository,
): List<GameCardData> {
    val random = Random(nowMillis())
    val perDeckQueues = deckIds.associateWith { deckId ->
        flashcardRepository.getFlashcardsByDeckId(deckId)
            .shuffled(random)
            .map { card ->
                GameCardData(
                    id = card.id,
                    answer = card.name,
                    imagePath = card.imageUrl.ifBlank { null },
                )
            }
            .toMutableList()
    }.toMutableMap()

    val result = mutableListOf<GameCardData>()
    while (perDeckQueues.values.any { it.isNotEmpty() }) {
        deckIds.forEach { deckId ->
            val queue = perDeckQueues[deckId] ?: return@forEach
            if (queue.isNotEmpty()) {
                result += queue.removeFirst()
            }
        }
    }
    return result
}

private fun buildDeckTitle(
    deckIds: List<String>,
    decksById: Map<String, Deck?>,
): String {
    if (deckIds.size == 1) {
        val singleDeck = decksById[deckIds.first()]
        return singleDeck?.name?.ifBlank { "Колода" } ?: "Колода"
    }
    return "Смешанная сессия"
}

private fun GameSessionData.toActiveUiState(): GameUiState.Active {
    val card = requireNotNull(currentCard)
    return GameUiState.Active(
        deckTitle = deck.title,
        currentCard = CardUi(
            id = card.id,
            answer = card.answer,
            imagePath = card.imagePath,
        ),
        cardIndex = currentIndex + 1,
        totalCards = cards.size,
        answerInput = answerInput,
        isHintVisible = isHintVisible,
        feedback = feedback?.toUi(),
    )
}

private fun GameSessionData.toFinishedUiState(): GameUiState.Finished {
    return GameUiState.Finished(
        deckTitle = deck.title,
        totalCards = cards.size,
        correctAnswers = correctAnswers,
    )
}

private fun GameFeedbackData.toUi(): FeedbackUi {
    return if (isCorrect) {
        FeedbackUi(
            message = "Верно!",
            emoji = "🥳",
        )
    } else {
        FeedbackUi(
            message = "Неверно!",
            emoji = "😞",
        )
    }
}
