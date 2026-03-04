package com.cerebus.game_screen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val FEEDBACK_DURATION_MS = 1200L

class GameScreenViewModel(
    private val deckId: String,
    private val flashcardRepository: FlashcardRepository,
    private val deckRepository: DeckRepository,
) : ViewModel() {

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
        }
    }

    fun consumeEffect() {
        _effects.value = null
    }

    private fun loadDeckCards() {
        viewModelScope.launch {
            _uiState.value = GameUiState.Loading

            if (deckId.isBlank()) {
                _uiState.value = GameUiState.Finished(
                    deckTitle = "Колода",
                    totalCards = 0,
                    correctAnswers = 0,
                )
                return@launch
            }

            val deck = deckRepository.getDeckById(deckId)
            val deckData = GameDeckData(
                id = deckId,
                title = deck?.name?.ifBlank { "Колода" } ?: "Колода",
            )

            val cards = flashcardRepository.getFlashcardsByDeckId(deckId)
                .map { card ->
                    GameCardData(
                        id = card.id,
                        answer = card.name,
                        imagePath = card.imageUrl.ifBlank { null },
                    )
                }

            val initialSession = GameSessionData(
                deck = deckData,
                cards = cards,
            )
            session = initialSession

            _uiState.value = if (cards.isEmpty()) {
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
        val isCorrect = current.answerInput.trim().equals(card.answer.trim(), ignoreCase = true)

        if (isCorrect) {
            handleCorrectAnswer(current)
        } else {
            handleWrongAnswer(current)
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
    val id: String,
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
    val currentIndex: Int = 0,
    val correctAnswers: Int = 0,
    val answerInput: String = "",
    val isHintVisible: Boolean = false,
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
