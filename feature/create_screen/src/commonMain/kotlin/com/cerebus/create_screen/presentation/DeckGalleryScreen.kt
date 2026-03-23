package com.cerebus.create_screen.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cerebus.create_screen.presentation.view.DeckGalleryScreen
import com.cerebus.customkeyboard.TrainingKeyboardFeedbackType
import com.cerebus.data.flashcards.domain.models.Flashcard
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

data class DeckGalleryStrings(
    val previous: String,
    val next: String,
    val empty: String,
    val practiceModeTitle: String,
    val practiceModeDescription: String,
    val practiceModeUnderstood: String,
    val correctFeedback: String,
    val wrongFeedback: String,
)

data class DeckGalleryUiState(
    val isLoading: Boolean = true,
    val deckId: String = "",
    val studentId: String = "",
    val cards: List<Flashcard> = emptyList(),
    val currentIndex: Int = 0,
    val activeSymbols: Set<String> = emptySet(),
    val isShiftEnabled: Boolean = false,
    val keyboardFeedbackKey: String? = null,
    val keyboardFeedbackType: TrainingKeyboardFeedbackType? = null,
    val inputFeedbackType: TrainingKeyboardFeedbackType? = null,
    val answerInput: String = "",
    val isHintVisible: Boolean = true,
    val isInputHintEnabled: Boolean = false,
    val isSimplifiedKeyboardEnabled: Boolean = false,
    val hideDigitsOnTightScreen: Boolean = true,
    val usedHint: Boolean = false,
    val usedShowWord: Boolean = false,
    val usedSimplifiedKeyboard: Boolean = false,
    val feedback: DeckGalleryFeedbackUi? = null,
) {
    val currentCard: Flashcard?
        get() = cards.getOrNull(currentIndex)
}

data class DeckGalleryFeedbackUi(
    val message: String,
    val emoji: String,
)

sealed interface DeckGalleryEffect {
    data class AnimateToCard(
        val index: Int,
    ) : DeckGalleryEffect
}

@Composable
fun DeckGalleryRoute(
    deckId: String,
    initialCardId: String?,
    strings: DeckGalleryStrings,
    onBackClick: () -> Unit,
    onOpenKeyboardSettings: (String) -> Unit,
) {
    val viewModel = koinViewModel<DeckGalleryViewModel>(
        parameters = { parametersOf(deckId, initialCardId) },
    )
    val state by viewModel.uiState.collectAsState()
    var animatedScrollTargetIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(deckId, initialCardId) {
        viewModel.load()
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DeckGalleryEffect.AnimateToCard -> {
                    animatedScrollTargetIndex = effect.index
                }
            }
        }
    }

    DeckGalleryScreen(
        state = state,
        strings = strings,
        animatedScrollTargetIndex = animatedScrollTargetIndex,
        onAnimatedScrollTargetConsumed = {
            animatedScrollTargetIndex = null
        },
        onBackClick = onBackClick,
        onOpenKeyboardSettings = {
            val studentId = state.studentId
            if (studentId.isNotBlank()) {
                onOpenKeyboardSettings(studentId)
            }
        },
        onPreviousClick = viewModel::showPrevious,
        onNextClick = viewModel::showNext,
        onShiftChanged = viewModel::onShiftChanged,
        onSymbolPressed = viewModel::onSymbolPressed,
        onBackspacePressed = viewModel::onBackspacePressed,
        onSubmitPressed = { viewModel.onSubmitPressed(strings) },
        onHintToggle = viewModel::onHintToggle,
        onShowWordToggle = viewModel::onShowWordToggle,
        onSimplifyKeyboardToggle = viewModel::onSimplifyKeyboardToggle,
    )
}
