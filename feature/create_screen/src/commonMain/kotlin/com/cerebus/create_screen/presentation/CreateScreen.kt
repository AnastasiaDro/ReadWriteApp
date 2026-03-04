package com.cerebus.create_screen.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.cerebus.core.ui.components.AppAnimatedDialog
import com.cerebus.core.ui.components.AppEntityEditorDialog
import com.cerebus.core.ui.components.AppEntityEditorMode
import com.cerebus.core.ui.components.AppConfirmationDialog
import com.cerebus.data.decks.domain.models.Deck
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext

private const val MAX_DECK_NAME_LENGTH = 40

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    state: CreateUiState,
    strings: CreateScreenStrings,
    validationErrorText: String?,
    onBackClick: () -> Unit,
    onAction: (CreateScreenAction) -> Unit,
) {
    val density = LocalDensity.current
    val windowWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val isTablet = windowWidthDp >= 840.dp

    val createCoverSize = if (isTablet) 80.dp else 220.dp
    val successCoverSize = if (isTablet) 120.dp else 220.dp
    val createDialogMinHeight = if (isTablet) 300.dp else 460.dp
    val successDialogMinHeight = if (isTablet) 220.dp else 340.dp
    val columns = if (isTablet) 4 else 3
    val isSelectionMode = state.selectedDeckIds.isNotEmpty()
    val deleteValidationText = if (state.validationError == CreateValidationError.DELETE_DECKS_FAILED) {
        validationErrorText
    } else {
        null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            if (isSelectionMode) {
                                onAction(CreateScreenAction.OnClearDeckSelection)
                            } else {
                                onBackClick()
                            }
                        },
                    ) {
                        Text(strings.back)
                    }
                },
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { onAction(CreateScreenAction.OnCreateDeckClick) }) {
                    Text(
                        text = strings.createDeckTitle,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                SelectionBottomBar(
                    selectedCount = state.selectedDeckIds.size,
                    selectedText = strings.selectedCount,
                    deleteText = strings.delete,
                    onDeleteClick = { onAction(CreateScreenAction.OnDeleteSelectedDecksClick) },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = strings.myDecks,
                style = MaterialTheme.typography.titleLarge,
            )
            if (deleteValidationText != null) {
                Text(
                    text = deleteValidationText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.decks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(strings.noDecksYet)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(36.dp),
                    contentPadding = PaddingValues(bottom = if (isSelectionMode) 120.dp else 84.dp),
                ) {
                    items(
                        items = state.decks,
                        key = { it.id },
                    ) { deck ->
                        DeckGridItem(
                            deck = deck,
                            noCoverText = strings.noCover,
                            onClick = { onAction(CreateScreenAction.OnDeckClick(deck.id)) },
                            onLongClick = { onAction(CreateScreenAction.OnDeckLongClick(deck.id)) },
                            isSelectionMode = isSelectionMode,
                            isSelected = deck.id in state.selectedDeckIds,
                        )
                    }
                }
            }
        }
    }

    AppEntityEditorDialog(
        visible = state.isCreateDialogVisible,
        mode = AppEntityEditorMode.CREATE,
        createTitle = strings.createDeckTitle,
        editTitle = strings.createDeckTitle,
        createConfirmText = strings.create,
        editConfirmText = strings.create,
        value = state.deckName,
        onValueChange = { onAction(CreateScreenAction.OnDeckNameChanged(it)) },
        fieldLabel = strings.deckNameLabel,
        coverButtonText = if (state.coverUri == null) strings.addCover else strings.editCover,
        onCoverButtonClick = { onAction(CreateScreenAction.OnCoverButtonClick) },
        onConfirm = { onAction(CreateScreenAction.OnConfirmCreateDeck) },
        onDismiss = { onAction(CreateScreenAction.OnDismissCreateDialog) },
        cancelText = strings.cancel,
        maxLength = MAX_DECK_NAME_LENGTH,
        minDialogHeight = createDialogMinHeight,
        validationErrorText = validationErrorText,
        isSaving = state.isSaving,
        coverContent = {
            CoverPreview(
                coverUri = state.coverUri,
                noCoverText = strings.noCover,
                modifier = Modifier
                    .size(createCoverSize)
                    .aspectRatio(1f),
            )
        },
    )

    if (state.isCoverSourceDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(CreateScreenAction.OnDismissCoverSourceDialog) },
            title = { Text(strings.chooseSource) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onAction(CreateScreenAction.OnPickFromGalleryClick) }) {
                        Text(strings.chooseFromGallery)
                    }
                    TextButton(onClick = { onAction(CreateScreenAction.OnTakePhotoClick) }) {
                        Text(strings.takePhoto)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { onAction(CreateScreenAction.OnDismissCoverSourceDialog) }) {
                    Text(strings.close)
                }
            },
        )
    }

    AppConfirmationDialog(
        visible = state.isDeleteSelectedDialogVisible,
        title = strings.confirmDeleteDecksTitle,
        message = strings.confirmDeleteDecksMessage,
        confirmText = strings.delete,
        dismissText = strings.cancel,
        confirmEnabled = !state.isDeletingSelectedDecks,
        onConfirm = { onAction(CreateScreenAction.OnConfirmDeleteSelectedDecks) },
        onDismiss = { onAction(CreateScreenAction.OnDismissDeleteSelectedDialog) },
    )

    AppAnimatedDialog(visible = state.isSuccessDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(CreateScreenAction.OnCloseSuccessDialog) },
            title = {},
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(min = successDialogMinHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CoverPreview(
                        coverUri = state.createdDeckCoverUri,
                        noCoverText = strings.noCover,
                        modifier = Modifier
                            .size(successCoverSize)
                            .aspectRatio(1f)
                            .align(Alignment.CenterHorizontally),
                    )
                    Text(
                        text = strings.deckCreatedTemplate.replace("%s", state.createdDeckName),
                        style = CreateScreenStyles.deckCreatedTitle(windowWidthDp = windowWidthDp),
                    )
                }
            },
            confirmButton = {
                Button(onClick = { onAction(CreateScreenAction.OnAddCardsClick) }) {
                    Text(strings.addCards)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(CreateScreenAction.OnCloseSuccessDialog) }) {
                    Text(strings.close)
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckGridItem(
    deck: Deck,
    noCoverText: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            CoverPreview(
                coverUri = deck.coverUri,
                noCoverText = noCoverText,
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
            text = deck.name,
            style = MaterialTheme.typography.bodyMedium,
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
private fun CoverPreview(
    coverUri: String?,
    noCoverText: String,
    modifier: Modifier = Modifier,
) {
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) {
        ImageLoader.Builder(platformContext).build()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        if (coverUri.isNullOrBlank()) {
            Text(noCoverText)
        } else {
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
