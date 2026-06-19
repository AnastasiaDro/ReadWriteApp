package com.cerebus.create_screen.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cerebus.core.game_engine.domain.logic.SrsAvailability
import com.cerebus.core.ui.components.AppAnimatedDialog
import com.cerebus.core.ui.components.AppEntityEditorDialog
import com.cerebus.core.ui.components.AppEntityEditorMode
import com.cerebus.core.ui.components.AppConfirmationDialog
import com.cerebus.core.utils.nowMillis
import com.cerebus.data.flashcards.domain.models.Flashcard
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val MAX_DECK_NAME_LENGTH = 40

//TODO подумать и вынести в отдельный модуль
@Composable
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
fun DeckScreen(
    state: DeckUiState,
    strings: DeckScreenStrings,
    validationErrorText: String?,
    onAction: (DeckScreenAction) -> Unit,
    onBackClick: () -> Unit,
) {
    var isTrainingModesHelpVisible by remember { mutableStateOf(false) }
    val trainingModesHelpSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val widthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val heightDp = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val shortestSideDp = minOf(widthDp, heightDp)
    val isTablet = shortestSideDp >= 600.dp
    val isSelectionMode = state.isCardSelectionMode
    val columns = if (isTablet) 4 else 3
    val gridRows = ((state.flashcards.size + columns - 1) / columns).coerceAtLeast(1)
    val gridSpacing = 16.dp
    val gridHorizontalPadding = 16.dp + 16.dp + 18.dp + 18.dp
    val gridAvailableWidth = (widthDp - gridHorizontalPadding - (gridSpacing * (columns - 1))).coerceAtLeast(0.dp)
    val gridItemWidth = gridAvailableWidth / columns
    val gridItemHeight = gridItemWidth + 44.dp
    val gridBottomPadding = if (isSelectionMode) 120.dp else 24.dp
    val gridHeight = (gridItemHeight * gridRows) + (gridSpacing * (gridRows - 1)) + gridBottomPadding
    val addCardCoverSize = if (isTablet) 88.dp else 180.dp
    val addCardDialogMinHeight = if (isTablet) 300.dp else 430.dp
    val isCardValidationError = state.validationError == DeckValidationError.EMPTY_CARD_NAME ||
        state.validationError == DeckValidationError.ADD_CARD_FAILED ||
        state.validationError == DeckValidationError.UPDATE_CARD_FAILED
    val cardValidationText = if (isCardValidationError) validationErrorText else null
    val deleteValidationText =
        if (state.validationError == DeckValidationError.DELETE_CARDS_FAILED) validationErrorText else null
    var editDeckNameFieldValue by remember(state.isEditNameDialogVisible) {
        mutableStateOf(
            TextFieldValue(
                text = state.editingName,
                selection = TextRange(state.editingName.length),
            )
        )
    }

    LaunchedEffect(state.editingName, state.isEditNameDialogVisible) {
        if (state.editingName != editDeckNameFieldValue.text) {
            val selectionIndex = editDeckNameFieldValue.selection.end.coerceIn(0, state.editingName.length)
            editDeckNameFieldValue = TextFieldValue(
                text = state.editingName,
                selection = TextRange(selectionIndex),
            )
        }
    }

    LaunchedEffect(isTrainingModesHelpVisible) {
        if (isTrainingModesHelpVisible) {
            trainingModesHelpSheetState.show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 8.dp),
                title = {
                    Text(
                        text = if (isSelectionMode) {
                            "${state.selectedCardIds.size} ${strings.selectedCount}"
                        } else {
                            state.deckName.ifBlank { strings.unnamedDeck }
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = {
                    TextButton(
                        onClick = {
                            if (isSelectionMode) {
                                onAction(DeckScreenAction.OnClearCardSelection)
                            } else {
                                onBackClick()
                            }
                        },
                    ) {
                        Text(strings.back)
                    }
                },
                actions = {
                    if (!isSelectionMode) {
                        TextButton(
                            onClick = { onAction(DeckScreenAction.OnExportDeckClick) },
                            enabled = !state.isExporting && !state.isLoading,
                        ) {
                            Text(strings.exportDeck)
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (isSelectionMode) {
                SelectionBottomBar(
                    selectedCount = state.selectedCardIds.size,
                    selectedText = strings.selectedCount,
                    deleteText = strings.delete,
                    onDeleteClick = { onAction(DeckScreenAction.OnDeleteSelectedCardsClick) },
                )
            }
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DeckOverviewCard(
                deckName = state.deckName,
                coverUri = state.coverUri,
                cardsCount = state.flashcards.size,
                isCompact = !isTablet,
                strings = strings,
                onEditNameClick = { onAction(DeckScreenAction.OnEditNameClick) },
                onEditCoverClick = { onAction(DeckScreenAction.OnEditCoverClick) },
            )

            SectionCard(
                title = strings.trainingModesTitle,
                titleTrailing = if (isTablet) {
                    { TrainingModesHelpButton(onClick = { isTrainingModesHelpVisible = true }) }
                } else {
                    null
                },
                headerAction = if (!isTablet) {
                    { TrainingModesHelpButton(onClick = { isTrainingModesHelpVisible = true }) }
                } else {
                    null
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DeckTrainingModesSection(
                        isCompact = !isTablet,
                        strings = strings,
                        onStartPlanClick = { onAction(DeckScreenAction.OnStartTrainingClick) },
                        onStartRandomLearnedClick = { onAction(DeckScreenAction.OnStartRandomLearnedClick) },
                        onStartRandomAllClick = { onAction(DeckScreenAction.OnStartRandomAllClick) },
                        onOpenGalleryClick = { onAction(DeckScreenAction.OnOpenGalleryClick) },
                    )

                    state.srsAvailability?.let { availability ->
                        DeckSrsStatusSummary(
                            availability = availability,
                            strings = strings,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            SectionCard(
                title = strings.cards,
                headerAction = {
                    if (!isSelectionMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onAction(DeckScreenAction.OnEnterCardSelectionMode) }) {
                                Text(strings.select)
                            }
                            OutlinedButton(onClick = { onAction(DeckScreenAction.OnAddCardClick) }) {
                                Text(strings.addCard)
                            }
                        }
                    }
                },
            ) {
                if (deleteValidationText != null) {
                    Text(
                        text = deleteValidationText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                ReorderableFlashcardGrid(
                    cards = state.flashcards,
                    columns = columns,
                    gridHeight = gridHeight,
                    gridBottomPadding = gridBottomPadding,
                    isSelectionMode = isSelectionMode,
                    selectedCardIds = state.selectedCardIds,
                    onCardClick = { onAction(DeckScreenAction.OnCardClick(it)) },
                    onEditClick = { onAction(DeckScreenAction.OnOpenCardEditor(it)) },
                    onCardsReordered = { onAction(DeckScreenAction.OnCardsReordered(it)) },
                )
            }
        }
    }

    AppAnimatedDialog(visible = state.isEditNameDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(DeckScreenAction.OnDismissEditNameDialog) },
            title = { Text(strings.editName) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    OutlinedTextField(
                        value = editDeckNameFieldValue,
                        onValueChange = { updated ->
                            editDeckNameFieldValue = updated
                            if (updated.text != state.editingName) {
                                onAction(DeckScreenAction.OnNameChanged(updated.text))
                            }
                        },
                        label = { Text(strings.deckNameLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    )
                    Text(
                        text = "${state.editingName.length}/$MAX_DECK_NAME_LENGTH",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                    if (state.validationError == DeckValidationError.EMPTY_DECK_NAME ||
                        state.validationError == DeckValidationError.UPDATE_NAME_FAILED
                    ) {
                        Text(
                            text = validationErrorText.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onAction(DeckScreenAction.OnSaveNameClick) }) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(DeckScreenAction.OnDismissEditNameDialog) }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    if (state.isEditCoverSourceDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(DeckScreenAction.OnDismissEditCoverSourceDialog) },
            title = { Text(strings.editCover) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onAction(
                                DeckScreenAction.OnPickImageFromGallery(DeckPickerTarget.DECK_COVER)
                            )
                        }
                    ) {
                        Text(strings.chooseFromGallery)
                    }
                    TextButton(
                        onClick = {
                            onAction(
                                DeckScreenAction.OnTakeImagePhoto(DeckPickerTarget.DECK_COVER)
                            )
                        }
                    ) {
                        Text(strings.takePhoto)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { onAction(DeckScreenAction.OnDismissEditCoverSourceDialog) }) {
                    Text(strings.close)
                }
            },
        )
    }

    AppEntityEditorDialog(
        visible = state.isAddCardDialogVisible,
        mode = if (state.cardEditorMode == CardEditorMode.CREATE) {
            AppEntityEditorMode.CREATE
        } else {
            AppEntityEditorMode.EDIT
        },
        createTitle = strings.addCardTitle,
        editTitle = strings.editCardTitle,
        createConfirmText = strings.create,
        editConfirmText = strings.save,
        value = state.cardName,
        onValueChange = { onAction(DeckScreenAction.OnCardNameChanged(it)) },
        fieldLabel = strings.cardNameLabel,
        coverButtonText = if (state.cardImageUrl.isNullOrBlank()) strings.addCover else strings.editCover,
        onCoverButtonClick = { onAction(DeckScreenAction.OnCardCoverButtonClick) },
        onConfirm = { onAction(DeckScreenAction.OnConfirmAddCard) },
        onDismiss = { onAction(DeckScreenAction.OnDismissAddCardDialog) },
        cancelText = strings.cancel,
        maxLength = MAX_DECK_NAME_LENGTH,
        minDialogHeight = addCardDialogMinHeight,
        validationErrorText = cardValidationText,
        isSaving = state.isCardSaving,
        coverContent = {
            DeckCover(
                coverUri = state.cardImageUrl,
                noCoverText = strings.noCover,
                modifier = Modifier
                    .size(addCardCoverSize)
                    .aspectRatio(1f),
            )
        },
    )

    if (state.isCardCoverSourceDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(DeckScreenAction.OnDismissCardCoverSourceDialog) },
            title = { Text(strings.chooseSource) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onAction(
                                DeckScreenAction.OnPickImageFromGallery(DeckPickerTarget.CARD_IMAGE)
                            )
                        }
                    ) {
                        Text(strings.chooseFromGallery)
                    }
                    TextButton(
                        onClick = {
                            onAction(
                                DeckScreenAction.OnTakeImagePhoto(DeckPickerTarget.CARD_IMAGE)
                            )
                        }
                    ) {
                        Text(strings.takePhoto)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { onAction(DeckScreenAction.OnDismissCardCoverSourceDialog) }) {
                    Text(strings.close)
                }
            },
        )
    }

    AppConfirmationDialog(
        visible = state.isDeleteSelectedDialogVisible,
        title = strings.confirmDeleteCardsTitle,
        message = strings.confirmDeleteCardsMessage,
        confirmText = strings.delete,
        dismissText = strings.cancel,
        confirmEnabled = !state.isDeletingSelectedCards,
        onConfirm = { onAction(DeckScreenAction.OnConfirmDeleteSelectedCards) },
        onDismiss = { onAction(DeckScreenAction.OnDismissDeleteSelectedCardsDialog) },
    )

    if (isTrainingModesHelpVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    trainingModesHelpSheetState.hide()
                    delay(100)
                    isTrainingModesHelpVisible = false
                }
            },
            sheetState = trainingModesHelpSheetState,
            sheetMaxWidth = Dp.Unspecified,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isTablet) {
                            Modifier.wrapContentWidth(Alignment.CenterHorizontally)
                        } else {
                            Modifier
                        }
                    )
                    .widthIn(max = if (isTablet) 560.dp else Dp.Infinity)
                    .padding(horizontal = 20.dp)
                    .padding(top = if (isTablet) 18.dp else 4.dp, bottom = 4.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 14.dp),
                ) {
                    Text(
                        text = strings.trainingModesTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = strings.trainingModesHelpSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TrainingModeHelpItem(
                        title = strings.trainingPlanTitle,
                        description = strings.trainingPlanHint,
                        onClick = {
                            scope.launch {
                                trainingModesHelpSheetState.hide()
                                delay(100)
                                isTrainingModesHelpVisible = false
                                onAction(DeckScreenAction.OnStartTrainingClick)
                            }
                        },
                    )
                    TrainingModeHelpItem(
                        title = strings.galleryTitle,
                        description = strings.galleryHint,
                        onClick = {
                            scope.launch {
                                trainingModesHelpSheetState.hide()
                                delay(100)
                                isTrainingModesHelpVisible = false
                                onAction(DeckScreenAction.OnOpenGalleryClick)
                            }
                        },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                trainingModesHelpSheetState.hide()
                                delay(100)
                                isTrainingModesHelpVisible = false
                            }
                        },
                    ) {
                        Text(strings.trainingModesHelpAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckSrsStatusSummary(
    availability: SrsAvailability,
    strings: DeckScreenStrings,
    modifier: Modifier = Modifier,
) {
    val currentTimeMillis by rememberSrsNowMillis()
    val summary = buildDeckSrsStatusSummary(
        availability = availability,
        strings = strings,
        currentTimeMillis = currentTimeMillis,
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            summary.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun buildDeckSrsStatusSummary(
    availability: SrsAvailability,
    strings: DeckScreenStrings,
    currentTimeMillis: Long,
): SrsStatusSummary {
    val cardCountLabel = formatSrsCountLabel(
        count = availability.availableNow,
        one = strings.srsStatusCardOne,
        few = strings.srsStatusCardFew,
        many = strings.srsStatusCardMany,
    )
    val laterTodayLabel = formatSrsCountLabel(
        count = availability.laterTodayCount,
        one = strings.srsStatusCardOne,
        few = strings.srsStatusCardFew,
        many = strings.srsStatusCardMany,
    )
    val remainingNewLabel = formatSrsCountLabel(
        count = availability.remainingNewToday,
        one = strings.srsStatusCardOne,
        few = strings.srsStatusCardFew,
        many = strings.srsStatusCardMany,
    )

    val title = when {
        availability.availableNow > 0 -> formatTemplate(strings.srsStatusAvailableNow, cardCountLabel)
        availability.laterTodayCount > 0 -> strings.srsStatusNoReviewsNow
        else -> strings.srsStatusAllDoneToday
    }

    val nextDueAtEpochMillis = availability.nextDueAtEpochMillis
    val subtitle = when {
        availability.availableNow == 0 &&
            availability.laterTodayCount > 0 &&
            nextDueAtEpochMillis != null -> formatTemplate(
            strings.srsStatusNextDue,
            laterTodayLabel,
            formatSrsDelayLabel(
                targetEpochMillis = nextDueAtEpochMillis,
                currentTimeMillis = currentTimeMillis,
                minutesShort = strings.srsTimeMinutesShort,
                hoursShort = strings.srsTimeHoursShort,
            ),
        )

        availability.laterTodayCount > 0 -> formatTemplate(strings.srsStatusLaterToday, laterTodayLabel)
        availability.remainingNewToday == 0 -> strings.srsStatusNoNewToday
        availability.availableNow > 0 -> formatTemplate(strings.srsStatusRemainingNew, remainingNewLabel)
        else -> null
    }

    return SrsStatusSummary(
        title = title,
        subtitle = subtitle,
    )
}

@Composable
private fun DeckTrainingModesSection(
    isCompact: Boolean,
    strings: DeckScreenStrings,
    onStartPlanClick: () -> Unit,
    onStartRandomLearnedClick: () -> Unit,
    onStartRandomAllClick: () -> Unit,
    onOpenGalleryClick: () -> Unit,
) {
    val modes = listOf(
        DeckTrainingModeUi(
            title = strings.trainingPlanTitle,
            hint = strings.trainingPlanHint,
            isPrimary = true,
            onClick = onStartPlanClick,
        ),
        DeckTrainingModeUi(
            title = strings.galleryTitle,
            hint = strings.galleryHint,
            onClick = onOpenGalleryClick,
        ),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val modeRows = if (isCompact) modes.chunked(2) else listOf(modes)
        modeRows.forEach { rowModes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowModes.forEach { mode ->
                    DeckTrainingModeTile(
                        title = mode.title,
                        hint = if (isCompact) null else mode.hint,
                        isCompact = isCompact,
                        isPrimary = mode.isPrimary,
                        onClick = mode.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowModes.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class SrsStatusSummary(
    val title: String,
    val subtitle: String? = null,
)

private fun formatSrsCountLabel(
    count: Int,
    one: String,
    few: String,
    many: String,
): String {
    return "$count ${selectPluralForm(count, one, few, many)}"
}

private fun selectPluralForm(
    count: Int,
    one: String,
    few: String,
    many: String,
): String {
    val normalized = count % 100
    if (normalized in 11..14) return many
    return when (count % 10) {
        1 -> one
        2, 3, 4 -> few
        else -> many
    }
}

private fun formatSrsDelayLabel(
    targetEpochMillis: Long,
    currentTimeMillis: Long,
    minutesShort: String,
    hoursShort: String,
): String {
    val deltaMinutes = ((targetEpochMillis - currentTimeMillis).coerceAtLeast(0L) + 59_999L) / 60_000L
    if (deltaMinutes < 60L) {
        return "${deltaMinutes.coerceAtLeast(1L)} $minutesShort"
    }

    val hours = deltaMinutes / 60L
    val minutes = deltaMinutes % 60L
    return if (minutes == 0L) {
        "$hours $hoursShort"
    } else {
        "$hours $hoursShort $minutes $minutesShort"
    }
}

@Composable
private fun rememberSrsNowMillis() = produceState(initialValue = nowMillis()) {
    while (true) {
        delay(60_000L)
        value = nowMillis()
    }
}

private fun formatTemplate(
    template: String,
    vararg args: String,
): String {
    return args.foldIndexed(template) { index, acc, value ->
        acc.replace("%${index + 1}\$s", value)
    }
}

@Composable
private fun DeckTrainingModeTile(
    title: String,
    hint: String?,
    isCompact: Boolean,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (hint != null) 4.dp else 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = if (hint != null) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = if (hint != null) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPrimary) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (isPrimary) {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier
                .then(
                    if (isCompact) {
                        Modifier.requiredHeight(68.dp)
                    } else {
                        Modifier.heightIn(min = 124.dp)
                    }
                ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            content()
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .then(
                    if (isCompact) {
                        Modifier.requiredHeight(68.dp)
                    } else {
                        Modifier.heightIn(min = 124.dp)
                    }
                ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
        ) {
            content()
        }
    }
}

private data class DeckTrainingModeUi(
    val title: String,
    val hint: String,
    val isPrimary: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun DeckOverviewCard(
    deckName: String,
    coverUri: String?,
    cardsCount: Int,
    isCompact: Boolean,
    strings: DeckScreenStrings,
    onEditNameClick: () -> Unit,
    onEditCoverClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (isCompact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = deckName.ifBlank { strings.unnamedDeck },
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = strings.cardsCount.replace("%1\$d", cardsCount.toString()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            } else {
                Text(
                    text = deckName.ifBlank { strings.unnamedDeck },
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = strings.cardsCount.replace("%1\$d", cardsCount.toString()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (isCompact) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    DeckCover(
                        coverUri = coverUri,
                        noCoverText = strings.noCover,
                        modifier = Modifier.size(112.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onEditCoverClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(strings.editCover, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onEditNameClick,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(strings.editName, maxLines = 1)
                        }
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DeckCover(
                        coverUri = coverUri,
                        noCoverText = strings.noCover,
                        modifier = Modifier.size(112.dp),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = onEditCoverClick) {
                            Text(strings.editCover)
                        }
                        OutlinedButton(onClick = onEditNameClick) {
                            Text(strings.editName)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    title: String,
    titleTrailing: (@Composable () -> Unit)? = null,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        titleTrailing?.invoke()
                    }
                }
                headerAction?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun TrainingModesHelpButton(
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Text(
            text = "?",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TrainingModeHelpItem(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun ReorderableFlashcardGrid(
    cards: List<Flashcard>,
    columns: Int,
    gridHeight: Dp,
    gridBottomPadding: Dp,
    isSelectionMode: Boolean,
    selectedCardIds: Set<String>,
    onCardClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onCardsReordered: (List<String>) -> Unit,
) {
    val gridState = rememberLazyGridState()
    var previewOrderIds by remember { mutableStateOf<List<String>?>(null) }
    var draggedCardId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hasDragged by remember { mutableStateOf(false) }
    val displayedCards = remember(cards, previewOrderIds) {
        previewOrderIds?.let(cards::sortedBySavedOrder) ?: cards
    }
    LaunchedEffect(cards, previewOrderIds, draggedCardId) {
        val currentPreviewOrderIds = previewOrderIds ?: return@LaunchedEffect
        if (draggedCardId == null && cards.map(Flashcard::id) == currentPreviewOrderIds) {
            previewOrderIds = null
        }
    }
    val latestDisplayedCards by rememberUpdatedState(displayedCards)
    val latestCards by rememberUpdatedState(cards)
    val latestPreviewOrderIds by rememberUpdatedState(previewOrderIds)

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = gridBottomPadding),
    ) {
        items(displayedCards, key = { it.id }) { card ->
            val isDragged = draggedCardId == card.id
            FlashcardGridItem(
                name = card.name,
                imageUrl = card.imageUrl,
                isSelectionMode = isSelectionMode,
                isSelected = card.id in selectedCardIds,
                onClick = { onCardClick(card.id) },
                onEditClick = { onEditClick(card.id) },
                modifier = Modifier
                    .then(if (isDragged) Modifier else Modifier.animateItem())
                    .zIndex(if (isDragged) 1f else 0f)
                    .offset {
                        val itemOffset = if (isDragged) dragOffset else Offset.Zero
                        IntOffset(
                            x = itemOffset.x.roundToInt(),
                            y = itemOffset.y.roundToInt(),
                        )
                    }
                    .graphicsLayer {
                        if (isDragged) {
                            scaleX = 1.03f
                            scaleY = 1.03f
                            shadowElevation = 10.dp.toPx()
                        }
                    }
                    .let { baseModifier ->
                        if (isSelectionMode) {
                            baseModifier
                        } else {
                            baseModifier.pointerInput(card.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedCardId = card.id
                                        dragOffset = Offset.Zero
                                        hasDragged = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val currentDraggedCardId = draggedCardId ?: return@detectDragGesturesAfterLongPress
                                        val draggedItem = gridState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.key == currentDraggedCardId }
                                            ?: return@detectDragGesturesAfterLongPress
                                        val updatedDragOffset = dragOffset + dragAmount
                                        dragOffset = updatedDragOffset
                                        if (!hasDragged && dragAmount != Offset.Zero) {
                                            hasDragged = true
                                        }
                                        val draggedCenter = draggedItem.draggedCenter(updatedDragOffset)
                                        val targetItem = findDragTargetItem(
                                            draggedCardId = currentDraggedCardId,
                                            draggedCenter = draggedCenter,
                                            visibleItems = gridState.layoutInfo.visibleItemsInfo,
                                        ) ?: return@detectDragGesturesAfterLongPress
                                        val currentCards = latestDisplayedCards
                                        val fromIndex = currentCards.indexOfFirst { it.id == currentDraggedCardId }
                                        val toIndex = targetItem.index.coerceIn(0, currentCards.lastIndex)
                                        if (fromIndex == -1 || fromIndex == toIndex) {
                                            return@detectDragGesturesAfterLongPress
                                        }
                                        previewOrderIds = currentCards
                                            .moveCardBetweenIndices(fromIndex, toIndex)
                                            .map(Flashcard::id)
                                        dragOffset = updatedDragOffset + Offset(
                                            x = draggedItem.offset.x - targetItem.offset.x.toFloat(),
                                            y = draggedItem.offset.y - targetItem.offset.y.toFloat(),
                                        )
                                    },
                                    onDragEnd = {
                                        val reorderedIds = latestPreviewOrderIds ?: latestCards.map(Flashcard::id)
                                        val originalIds = latestCards.map(Flashcard::id)
                                        val moved = hasDragged && reorderedIds != originalIds
                                        draggedCardId = null
                                        dragOffset = Offset.Zero
                                        hasDragged = false
                                        if (moved) {
                                            onCardsReordered(reorderedIds)
                                        }
                                    },
                                    onDragCancel = {
                                        draggedCardId = null
                                        dragOffset = Offset.Zero
                                        previewOrderIds = null
                                        hasDragged = false
                                    },
                                )
                            }
                        }
                    },
            )
        }
    }
}

@Composable
private fun FlashcardGridItem(
    name: String,
    imageUrl: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            FlashcardPreview(
                imageUrl = imageUrl,
                fallbackText = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            if (isSelectionMode) {
                SelectionIndicator(
                    isSelected = isSelected,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                )
            } else {
                CardEditButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp),
                )
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun List<Flashcard>.moveCardBetweenIndices(
    fromIndex: Int,
    toIndex: Int,
): List<Flashcard> {
    if (fromIndex == toIndex) return this
    if (fromIndex !in indices || toIndex !in indices) return this

    val mutableCards = toMutableList()
    val movedCard = mutableCards.removeAt(fromIndex)
    mutableCards.add(toIndex, movedCard)
    return mutableCards
}

private fun findDragTargetItem(
    draggedCardId: String,
    draggedCenter: Offset,
    visibleItems: List<LazyGridItemInfo>,
): LazyGridItemInfo? {
    val sortedItems = visibleItems.sortedBy { it.index }
    val directTarget = sortedItems.firstOrNull { item ->
        item.key != draggedCardId && item.containsPoint(draggedCenter)
    }
    if (directTarget != null) return directTarget

    val nonDraggedItems = sortedItems.filter { it.key != draggedCardId }
    val firstItem = nonDraggedItems.firstOrNull() ?: return null
    val lastItem = nonDraggedItems.lastOrNull() ?: return null

    return when {
        draggedCenter.shouldMoveBefore(firstItem) -> firstItem
        draggedCenter.shouldMoveAfter(lastItem) -> lastItem
        else -> null
    }
}

private fun LazyGridItemInfo.draggedCenter(offset: Offset): Offset {
    return Offset(
        x = offset.x + this.offset.x + (size.width / 2f),
        y = offset.y + this.offset.y + (size.height / 2f),
    )
}

private fun LazyGridItemInfo.containsPoint(point: Offset): Boolean {
    val left = offset.x.toFloat()
    val right = left + size.width
    val top = offset.y.toFloat()
    val bottom = top + size.height
    return point.x in left..right && point.y in top..bottom
}

private fun Offset.shouldMoveBefore(item: LazyGridItemInfo): Boolean {
    if (y < item.offset.y) return true
    val top = item.offset.y.toFloat()
    val bottom = top + item.size.height
    return y in top..bottom && x < item.offset.x + (item.size.width / 2f)
}

private fun Offset.shouldMoveAfter(item: LazyGridItemInfo): Boolean {
    val top = item.offset.y.toFloat()
    val bottom = top + item.size.height
    if (y > bottom) return true
    return y in top..bottom && x > item.offset.x + (item.size.width / 2f)
}

@Composable
private fun CardEditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun FlashcardPreview(
    imageUrl: String,
    fallbackText: String,
    modifier: Modifier = Modifier,
) {
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) { ImageLoader.Builder(platformContext).build() }
    var imageLoadFailed by remember(imageUrl) { mutableStateOf(false) }
    val normalizedUrl = imageUrl.trim()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        if (normalizedUrl.isBlank() || imageLoadFailed) {
            CardTextFallback(
                text = fallbackText,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = normalizedUrl,
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { imageLoadFailed = false },
                onError = { imageLoadFailed = true },
            )
        }
    }
}

@Composable
private fun CardTextFallback(
    text: String,
    modifier: Modifier = Modifier,
) {
    val normalizedText = text.trim().ifBlank { "?" }
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val isSmallPreview = minOf(maxWidth, maxHeight) < 110.dp
        val textStyle = when {
            normalizedText.length <= 2 -> if (isSmallPreview) {
                MaterialTheme.typography.displaySmall
            } else {
                MaterialTheme.typography.displayLarge
            }
            normalizedText.length <= 4 -> if (isSmallPreview) {
                MaterialTheme.typography.headlineLarge
            } else {
                MaterialTheme.typography.displayMedium
            }
            normalizedText.length <= 8 -> if (isSmallPreview) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.displaySmall
            }
            normalizedText.length <= 12 -> if (isSmallPreview) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.headlineLarge
            }
            normalizedText.length <= 20 -> if (isSmallPreview) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.headlineMedium
            }
            normalizedText.length <= 32 -> if (isSmallPreview) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.titleLarge
            }
            else -> if (isSmallPreview) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.titleMedium
            }
        }

        Text(
            text = normalizedText,
            style = textStyle,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    selectedText: String,
    deleteText: String,
    onDeleteClick: () -> Unit,
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$selectedCount $selectedText",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onDeleteClick) {
                Text(deleteText)
            }
        }
    }
}

@Composable
private fun SelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary),
            )
        }
    }
}

@Composable
private fun DeckCover(
    coverUri: String?,
    noCoverText: String,
    modifier: Modifier = Modifier,
) {
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) { ImageLoader.Builder(platformContext).build() }
    var imageLoadFailed by remember(coverUri) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        if (coverUri.isNullOrBlank() || imageLoadFailed) {
            Text(noCoverText)
        } else {
            Text(noCoverText)
            AsyncImage(
                model = coverUri,
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { imageLoadFailed = false },
                onError = { imageLoadFailed = true },
            )
        }
    }
}
