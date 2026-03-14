package com.cerebus.game_screen.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cerebus.core.game_engine.domain.logic.interleaveReviewAndNewCards
import com.cerebus.core.game_engine.domain.logic.selectBalancedNewCardsByDeck
import com.cerebus.core.game_engine.domain.logic.takeRoundRobinByDeck
import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.CardState
import com.cerebus.core.game_engine.domain.model.Grade
import com.cerebus.core.game_engine.domain.repository.CardProgressRepository
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository
import com.cerebus.core.game_engine.domain.usecase.SubmitAnswerAndRescheduleUseCase
import com.cerebus.core.game_engine.domain.usecase.SubmitAnswerCommand
import com.cerebus.core.utils.localStartOfDayMillis
import com.cerebus.core.utils.nowMillis
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.decks.domain.repositories.DeckRepository
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.student.domain.repositories.StudentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val FEEDBACK_DURATION_MS = 1200L
private const val REVIEW_TO_NEW_RATIO = 3
private const val DEFAULT_LEARN_MORE_STEP = 5
private const val MIN_GUIDED_HINT_THRESHOLD = 0
private const val MAX_GUIDED_HINT_THRESHOLD = 5
private const val RANDOM_REVIEW_LIMIT = 10

class GameScreenViewModel(
    deckIds: List<String>,
    private val flashcardRepository: FlashcardRepository,
    private val deckRepository: DeckRepository,
    private val preferencesRepository: PreferencesRepository,
    private val studentRepository: StudentRepository,
    private val studentPrefsRepository: StudentPrefsRepository,
    private val cardProgressRepository: CardProgressRepository,
    private val submitAnswerAndRescheduleUseCase: SubmitAnswerAndRescheduleUseCase,
) : ViewModel() {
    private val selectedDeckIds = deckIds.filter { it.isNotBlank() }.distinct()

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _effects = MutableStateFlow<GameScreenEffect?>(null)
    val effects: StateFlow<GameScreenEffect?> = _effects.asStateFlow()

    private var session: GameSessionData? = null
    private var feedbackJob: Job? = null
    private var dailyLimitIncrease: Int = 0

    init {
        loadDeckCards()
    }

    fun onAction(action: GameScreenAction) {
        when (action) {
            is GameScreenAction.OnAnswerChanged -> updateAnswer(action.value)
            GameScreenAction.OnCheckClick -> checkAnswer()
            GameScreenAction.OnRetryClick -> repeatLastSession()
            GameScreenAction.OnRandomReviewClick -> repeatRandomStudiedCards()
            GameScreenAction.OnLearnMoreClick -> learnMore()
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
            val activeSymbols = loadActiveSymbols(
                studentId = activeStudentId,
                studentRepository = studentRepository,
            )

            val sessionCards = buildSessionCards(
                deckIds = selectedDeckIds,
                studentId = activeStudentId,
                flashcardRepository = flashcardRepository,
                cardProgressRepository = cardProgressRepository,
                studentPrefsRepository = studentPrefsRepository,
                dailyLimitIncrease = dailyLimitIncrease,
            )
            saveLastSessionCardsSnapshot(
                studentId = activeStudentId,
                deckIds = selectedDeckIds,
                sessionCards = sessionCards,
                preferencesRepository = preferencesRepository,
            )

            val initialSession = GameSessionData(
                deck = deckData,
                cards = sessionCards,
                studentId = activeStudentId,
                activeSymbols = activeSymbols,
                sessionMode = GameSessionMode.Srs,
                isHintVisible = initialHintVisible(sessionCards),
                cardShownAtEpochMillis = nowMillis(),
                attemptIndex = 1,
            )
            session = initialSession

            _uiState.value = if (sessionCards.isEmpty()) {
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

        if (studentId.isBlank() || current.sessionMode == GameSessionMode.RandomReview) {
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
            val nextCard = withFeedback.cards.getOrNull(nextIndex)
            val progressed = withFeedback.copy(
                currentIndex = nextIndex,
                correctAnswers = withFeedback.correctAnswers + 1,
                answerInput = "",
                isHintVisible = nextCard?.showHintInitially == true,
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

    private fun repeatLastSession() {
        feedbackJob?.cancel()

        val current = session ?: return
        viewModelScope.launch {
            val restoredCards = restoreLastSessionCards(
                studentId = current.studentId,
                deckIds = selectedDeckIds,
                flashcardRepository = flashcardRepository,
                cardProgressRepository = cardProgressRepository,
                studentPrefsRepository = studentPrefsRepository,
                preferencesRepository = preferencesRepository,
            ).ifEmpty { current.cards }

            val restarted = restartSessionWithCards(
                current = current,
                cards = restoredCards,
            )
            session = restarted

            _uiState.value = if (restarted.cards.isEmpty()) {
                restarted.toFinishedUiState()
            } else {
                restarted.toActiveUiState()
            }
        }
    }

    private fun repeatRandomStudiedCards() {
        feedbackJob?.cancel()

        val current = session ?: return
        viewModelScope.launch {
            val randomReviewCards = buildRandomReviewCards(
                deckIds = selectedDeckIds,
                studentId = current.studentId,
                flashcardRepository = flashcardRepository,
                cardProgressRepository = cardProgressRepository,
                studentPrefsRepository = studentPrefsRepository,
            ).ifEmpty {
                current.cards.shuffled(Random(nowMillis())).take(RANDOM_REVIEW_LIMIT)
            }

            saveLastSessionCardsSnapshot(
                studentId = current.studentId,
                deckIds = selectedDeckIds,
                sessionCards = randomReviewCards,
                preferencesRepository = preferencesRepository,
            )

            val restarted = restartSessionWithCards(
                current = current,
                cards = randomReviewCards,
                sessionMode = GameSessionMode.RandomReview,
            )
            session = restarted
            _uiState.value = if (restarted.cards.isEmpty()) {
                restarted.toFinishedUiState()
            } else {
                restarted.toActiveUiState()
            }
        }
    }

    private fun learnMore() {
        feedbackJob?.cancel()

        val current = session ?: return
        viewModelScope.launch {
            val learnMoreStep = resolveLearnMoreStep(current.studentId)
            dailyLimitIncrease += learnMoreStep

            val sessionCards = buildSessionCards(
                deckIds = selectedDeckIds,
                studentId = current.studentId,
                flashcardRepository = flashcardRepository,
                cardProgressRepository = cardProgressRepository,
                studentPrefsRepository = studentPrefsRepository,
                dailyLimitIncrease = dailyLimitIncrease,
            )
            saveLastSessionCardsSnapshot(
                studentId = current.studentId,
                deckIds = selectedDeckIds,
                sessionCards = sessionCards,
                preferencesRepository = preferencesRepository,
            )

            val restarted = restartSessionWithCards(
                current = current,
                cards = sessionCards,
                sessionMode = GameSessionMode.Srs,
            )
            session = restarted
            _uiState.value = if (restarted.cards.isEmpty()) {
                restarted.toFinishedUiState()
            } else {
                restarted.toActiveUiState()
            }
        }
    }

    private suspend fun resolveLearnMoreStep(studentId: String): Int {
        if (studentId.isBlank()) return DEFAULT_LEARN_MORE_STEP
        return runCatching { studentPrefsRepository.getPrefs(studentId).learnMoreStep }
            .getOrDefault(DEFAULT_LEARN_MORE_STEP)
            .coerceAtLeast(1)
    }

    private fun restartSessionWithCards(
        current: GameSessionData,
        cards: List<GameCardData>,
        sessionMode: GameSessionMode = current.sessionMode,
    ): GameSessionData {
        return current.copy(
            cards = cards,
            sessionMode = sessionMode,
            currentIndex = 0,
            correctAnswers = 0,
            answerInput = "",
            isHintVisible = initialHintVisible(cards),
            feedback = null,
            cardShownAtEpochMillis = nowMillis(),
            attemptIndex = 1,
        )
    }
}

private data class GameDeckData(
    val title: String,
)

private data class GameCardData(
    val deckId: String,
    val id: String,
    val answer: String,
    val imagePath: String?,
    val showHintInitially: Boolean = false,
)

private enum class GameSessionMode {
    Srs,
    RandomReview,
}

private data class GameSessionData(
    val deck: GameDeckData,
    val cards: List<GameCardData>,
    val studentId: String,
    val activeSymbols: Set<String>,
    val sessionMode: GameSessionMode,
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

private suspend fun buildSessionCards(
    deckIds: List<String>,
    studentId: String,
    flashcardRepository: FlashcardRepository,
    cardProgressRepository: CardProgressRepository,
    studentPrefsRepository: StudentPrefsRepository,
    dailyLimitIncrease: Int = 0,
): List<GameCardData> {
    val random = Random(nowMillis())
    val cardsByDeck = deckIds.associateWith { deckId ->
        flashcardRepository.getFlashcardsByDeckId(deckId)
            .shuffled(random)
            .map { card ->
                GameCardData(
                    deckId = deckId,
                    id = card.id,
                    answer = card.name,
                    imagePath = card.imageUrl.ifBlank { null },
                    showHintInitially = false,
                )
            }
    }

    if (studentId.isBlank()) {
        return takeRoundRobinByDeck(
            cardsByDeck = cardsByDeck,
            limit = cardsByDeck.values.sumOf { it.size },
        )
    }

    val prefs = runCatching { studentPrefsRepository.getPrefs(studentId) }.getOrNull()
        ?: return takeRoundRobinByDeck(
            cardsByDeck = cardsByDeck,
            limit = cardsByDeck.values.sumOf { it.size },
        )
    val normalizedDailyIncrease = dailyLimitIncrease.coerceAtLeast(0)
    val reviewLimit = prefs.reviewsPerSession.coerceAtLeast(0) + normalizedDailyIncrease
    val newLimit = prefs.newCardsPerSession.coerceAtLeast(0) + normalizedDailyIncrease
    val guidedHintThreshold = prefs.guidedHintSuccessThreshold
        .coerceIn(MIN_GUIDED_HINT_THRESHOLD, MAX_GUIDED_HINT_THRESHOLD)
    val progressByCardId = cardProgressRepository.observeProgress(studentId)
        .first()
        .associateBy { it.cardId }
    val now = nowMillis()

    val newByDeck = mutableMapOf<String, MutableList<GameCardData>>()
    val reviewByDeck = mutableMapOf<String, MutableList<GameCardData>>()
    cardsByDeck.forEach { (deckId, cards) ->
        cards.forEach { card ->
            val progress = progressByCardId[card.id]
            val cardWithHintMode = card.copy(
                showHintInitially = shouldShowHintInitially(
                    progress = progress,
                    guidedHintThreshold = guidedHintThreshold,
                ),
            )
            if (progress == null || progress.state == CardState.NEW) {
                newByDeck.getOrPut(deckId) { mutableListOf() }.add(cardWithHintMode)
            } else if (progress.dueAtEpochMillis <= now) {
                reviewByDeck.getOrPut(deckId) { mutableListOf() }.add(cardWithHintMode)
            }
        }
    }

    val selectedReview = takeRoundRobinByDeck(
        cardsByDeck = reviewByDeck,
        limit = reviewLimit,
    )
    val selectedNew = selectBalancedNewCardsByDeck(
        newCardsByDeck = newByDeck,
        newLimit = newLimit,
    )

    return interleaveReviewAndNewCards(
        reviewCards = selectedReview,
        newCards = selectedNew,
        reviewToNewRatio = REVIEW_TO_NEW_RATIO,
    )
}

private fun saveLastSessionCardsSnapshot(
    studentId: String,
    deckIds: List<String>,
    sessionCards: List<GameCardData>,
    preferencesRepository: PreferencesRepository,
) {
    val cardIds = sessionCards.map { it.id }
    if (cardIds.isEmpty()) return
    preferencesRepository.setLastSessionCardIds(
        studentId = studentId,
        deckIds = deckIds,
        cardIds = cardIds,
    )
}

private suspend fun restoreLastSessionCards(
    studentId: String,
    deckIds: List<String>,
    flashcardRepository: FlashcardRepository,
    cardProgressRepository: CardProgressRepository,
    studentPrefsRepository: StudentPrefsRepository,
    preferencesRepository: PreferencesRepository,
): List<GameCardData> {
    val savedCardIds = preferencesRepository.getLastSessionCardIds(
        studentId = studentId,
        deckIds = deckIds,
    ).orEmpty()
    if (savedCardIds.isEmpty()) return emptyList()

    val progressByCardId: Map<String, CardProgress> = if (studentId.isBlank()) {
        emptyMap()
    } else {
        cardProgressRepository.observeProgress(studentId)
            .first()
            .associateBy { it.cardId }
    }
    val guidedHintThreshold = if (studentId.isBlank()) {
        MIN_GUIDED_HINT_THRESHOLD
    } else {
        runCatching { studentPrefsRepository.getPrefs(studentId).guidedHintSuccessThreshold }
            .getOrDefault(2)
            .coerceIn(MIN_GUIDED_HINT_THRESHOLD, MAX_GUIDED_HINT_THRESHOLD)
    }

    val cardsById = buildMap {
        deckIds.forEach { deckId ->
            flashcardRepository.getFlashcardsByDeckId(deckId).forEach { card ->
                val progress = progressByCardId[card.id]
                put(
                    card.id,
                    GameCardData(
                        deckId = deckId,
                        id = card.id,
                        answer = card.name,
                        imagePath = card.imageUrl.ifBlank { null },
                        showHintInitially = shouldShowHintInitially(
                            progress = progress,
                            guidedHintThreshold = guidedHintThreshold,
                        ),
                    )
                )
            }
        }
    }
    return savedCardIds.mapNotNull { cardId -> cardsById[cardId] }
}

private suspend fun buildRandomReviewCards(
    deckIds: List<String>,
    studentId: String,
    flashcardRepository: FlashcardRepository,
    cardProgressRepository: CardProgressRepository,
    studentPrefsRepository: StudentPrefsRepository,
): List<GameCardData> {
    if (studentId.isBlank()) return emptyList()

    val progressByCardId = cardProgressRepository.observeProgress(studentId)
        .first()
        .associateBy { it.cardId }
    val guidedHintThreshold = runCatching { studentPrefsRepository.getPrefs(studentId).guidedHintSuccessThreshold }
        .getOrDefault(2)
        .coerceIn(MIN_GUIDED_HINT_THRESHOLD, MAX_GUIDED_HINT_THRESHOLD)
    val todayStartMillis = localStartOfDayMillis()
    val random = Random(nowMillis())

    val eligibleCards = buildList {
        deckIds.forEach { deckId ->
            flashcardRepository.getFlashcardsByDeckId(deckId).forEach { flashcard ->
                val progress = progressByCardId[flashcard.id] ?: return@forEach
                if (progress.state == CardState.NEW) return@forEach

                add(
                    GameCardWithProgress(
                        card = GameCardData(
                            deckId = deckId,
                            id = flashcard.id,
                            answer = flashcard.name,
                            imagePath = flashcard.imageUrl.ifBlank { null },
                            showHintInitially = shouldShowHintInitially(
                                progress = progress,
                                guidedHintThreshold = guidedHintThreshold,
                            ),
                        ),
                        progress = progress,
                    )
                )
            }
        }
    }

    val reviewedToday = eligibleCards
        .filter { candidate ->
            val lastReviewed = candidate.progress.lastReviewedAtEpochMillis
            lastReviewed != null && lastReviewed >= todayStartMillis
        }
        .shuffled(random)
        .take(RANDOM_REVIEW_LIMIT)

    val reviewedTodayIds = reviewedToday.mapTo(mutableSetOf()) { it.card.id }
    val otherStudied = eligibleCards
        .filterNot { it.card.id in reviewedTodayIds }
        .shuffled(random)
        .take((RANDOM_REVIEW_LIMIT - reviewedToday.size).coerceAtLeast(0))

    return (reviewedToday + otherStudied)
        .map { it.card }
        .shuffled(random)
}

private data class GameCardWithProgress(
    val card: GameCardData,
    val progress: CardProgress,
)

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
        studentId = studentId,
        activeSymbols = activeSymbols,
        answerInput = answerInput,
        isHintVisible = isHintVisible,
        feedback = feedback?.toUi(),
    )
}

private suspend fun loadActiveSymbols(
    studentId: String,
    studentRepository: StudentRepository,
): Set<String> {
    if (studentId.isBlank()) return emptySet()
    return studentRepository.getActiveLettersById(studentId)
        .orEmpty()
        .lowercase()
        .filter { it.isLetter() }
        .map { it.toString() }
        .toSet()
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

private fun initialHintVisible(cards: List<GameCardData>): Boolean {
    return cards.firstOrNull()?.showHintInitially == true
}

private fun shouldShowHintInitially(
    progress: CardProgress?,
    guidedHintThreshold: Int,
): Boolean {
    if (guidedHintThreshold <= 0) return false
    if (progress == null) return true
    val isGuidedStage = progress.state == CardState.NEW || progress.state == CardState.LEARNING
    if (!isGuidedStage) return false
    return progress.guidedHintSuccessCount < guidedHintThreshold
}
