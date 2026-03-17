package com.cerebus.create_screen.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
    val density = LocalDensity.current
    val widthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val isTablet = widthDp >= 840.dp
    val columns = if (isTablet) 4 else 3
    val addCardCoverSize = if (isTablet) 88.dp else 180.dp
    val addCardDialogMinHeight = if (isTablet) 300.dp else 430.dp
    val isCardValidationError = state.validationError == DeckValidationError.EMPTY_CARD_NAME ||
        state.validationError == DeckValidationError.ADD_CARD_FAILED ||
        state.validationError == DeckValidationError.UPDATE_CARD_FAILED
    val cardValidationText = if (isCardValidationError) validationErrorText else null
    val deleteValidationText =
        if (state.validationError == DeckValidationError.DELETE_CARDS_FAILED) validationErrorText else null
    val isSelectionMode = state.selectedCardIds.isNotEmpty()
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
                title = { Text("") },
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
                    TextButton(
                        onClick = { onAction(DeckScreenAction.OnExportDeckClick) },
                        enabled = !state.isExporting && !state.isLoading,
                    ) {
                        Text(strings.exportDeck)
                    }
                },
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { onAction(DeckScreenAction.OnAddCardClick) }) {
                    Text(
                        text = strings.addCard,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DeckHeader(
                deckName = state.deckName,
                coverUri = state.coverUri,
                isCompact = !isTablet,
                strings = strings,
                onEditNameClick = { onAction(DeckScreenAction.OnEditNameClick) },
                onEditCoverClick = { onAction(DeckScreenAction.OnEditCoverClick) },
            )

            DeckTrainingModesSection(
                strings = strings,
                onStartPlanClick = { onAction(DeckScreenAction.OnStartTrainingClick) },
                onStartRandomLearnedClick = { onAction(DeckScreenAction.OnStartRandomLearnedClick) },
                onStartRandomAllClick = { onAction(DeckScreenAction.OnStartRandomAllClick) },
                onOpenGalleryClick = { onAction(DeckScreenAction.OnOpenGalleryClick) },
            )

            Text(
                text = strings.cards,
                style = MaterialTheme.typography.titleMedium,
            )
            if (deleteValidationText != null) {
                Text(
                    text = deleteValidationText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = if (isSelectionMode) 120.dp else 140.dp),
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
}

@Composable
private fun DeckTrainingModesSection(
    strings: DeckScreenStrings,
    onStartPlanClick: () -> Unit,
    onStartRandomLearnedClick: () -> Unit,
    onStartRandomAllClick: () -> Unit,
    onOpenGalleryClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = strings.trainingModesTitle,
            style = MaterialTheme.typography.titleMedium,
        )

        DeckTrainingModeButton(
            title = strings.trainingPlanTitle,
            hint = strings.trainingPlanHint,
            onClick = onStartPlanClick,
        )
        DeckTrainingModeButton(
            title = strings.randomLearnedTitle,
            hint = strings.randomLearnedHint,
            onClick = onStartRandomLearnedClick,
        )
        DeckTrainingModeButton(
            title = strings.randomAllTitle,
            hint = strings.randomAllHint,
            onClick = onStartRandomAllClick,
        )
        DeckTrainingModeButton(
            title = strings.galleryTitle,
            hint = strings.galleryHint,
            onClick = onOpenGalleryClick,
        )
    }
}

@Composable
private fun DeckTrainingModeButton(
    title: String,
    hint: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeckHeader(
    deckName: String,
    coverUri: String?,
    isCompact: Boolean,
    strings: DeckScreenStrings,
    onEditNameClick: () -> Unit,
    onEditCoverClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = deckName.ifBlank { strings.unnamedDeck },
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

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
