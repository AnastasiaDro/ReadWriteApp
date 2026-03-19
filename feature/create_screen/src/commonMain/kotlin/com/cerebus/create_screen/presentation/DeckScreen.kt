package com.cerebus.create_screen.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cerebus.core.ui.components.AppAnimatedDialog
import com.cerebus.core.ui.components.AppEntityEditorDialog
import com.cerebus.core.ui.components.AppEntityEditorMode
import com.cerebus.core.ui.components.AppConfirmationDialog
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext

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
    val density = LocalDensity.current
    val widthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val heightDp = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val shortestSideDp = minOf(widthDp, heightDp)
    val isTablet = shortestSideDp >= 600.dp
    val isSelectionMode = state.selectedCardIds.isNotEmpty()
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
                DeckTrainingModesSection(
                    isCompact = !isTablet,
                    strings = strings,
                    onStartPlanClick = { onAction(DeckScreenAction.OnStartTrainingClick) },
                    onStartRandomLearnedClick = { onAction(DeckScreenAction.OnStartRandomLearnedClick) },
                    onStartRandomAllClick = { onAction(DeckScreenAction.OnStartRandomAllClick) },
                    onOpenGalleryClick = { onAction(DeckScreenAction.OnOpenGalleryClick) },
                )
            }

            SectionCard(
                title = strings.cards,
                headerAction = {
                    if (!isSelectionMode) {
                        OutlinedButton(onClick = { onAction(DeckScreenAction.OnAddCardClick) }) {
                            Text(strings.addCard)
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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    userScrollEnabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = gridBottomPadding),
                ) {
                    items(state.flashcards, key = { it.id }) { card ->
                        FlashcardGridItem(
                            name = card.name,
                            imageUrl = card.imageUrl,
                            isSelectionMode = isSelectionMode,
                            isSelected = card.id in state.selectedCardIds,
                            onClick = { onAction(DeckScreenAction.OnCardClick(card.id)) },
                            onLongPress = { onAction(DeckScreenAction.OnCardLongPress(card.id)) },
                        )
                    }
                }
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
            onDismissRequest = { isTrainingModesHelpVisible = false },
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
                            isTrainingModesHelpVisible = false
                            onAction(DeckScreenAction.OnStartTrainingClick)
                        },
                    )
                    TrainingModeHelpItem(
                        title = strings.randomLearnedTitle,
                        description = strings.randomLearnedHint,
                        onClick = {
                            isTrainingModesHelpVisible = false
                            onAction(DeckScreenAction.OnStartRandomLearnedClick)
                        },
                    )
                    TrainingModeHelpItem(
                        title = strings.randomAllTitle,
                        description = strings.randomAllHint,
                        onClick = {
                            isTrainingModesHelpVisible = false
                            onAction(DeckScreenAction.OnStartRandomAllClick)
                        },
                    )
                    TrainingModeHelpItem(
                        title = strings.galleryTitle,
                        description = strings.galleryHint,
                        onClick = {
                            isTrainingModesHelpVisible = false
                            onAction(DeckScreenAction.OnOpenGalleryClick)
                        },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = { isTrainingModesHelpVisible = false },
                    ) {
                        Text(strings.trainingModesHelpAction)
                    }
                }
            }
        }
    }
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
            title = strings.randomLearnedTitle,
            hint = strings.randomLearnedHint,
            onClick = onStartRandomLearnedClick,
        ),
        DeckTrainingModeUi(
            title = strings.randomAllTitle,
            hint = strings.randomAllHint,
            onClick = onStartRandomAllClick,
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
private fun FlashcardGridItem(
    name: String,
    imageUrl: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            ),
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
    val textStyle = when {
        normalizedText.length <= 2 -> MaterialTheme.typography.displayLarge
        normalizedText.length <= 6 -> MaterialTheme.typography.displayMedium
        normalizedText.length <= 12 -> MaterialTheme.typography.displaySmall
        else -> MaterialTheme.typography.headlineLarge
    }

    Text(
        text = normalizedText,
        style = textStyle,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.CenterVertically),
    )
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
