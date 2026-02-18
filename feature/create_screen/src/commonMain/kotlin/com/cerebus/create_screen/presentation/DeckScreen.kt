package com.cerebus.create_screen.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cerebus.core.ui.components.AppAnimatedDialog
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext

private const val MAX_DECK_NAME_LENGTH = 40

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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
        state.validationError == DeckValidationError.ADD_CARD_FAILED
    val cardValidationText = if (isCardValidationError) validationErrorText else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(strings.back)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAction(DeckScreenAction.OnAddCardClick) }) {
                Text(
                    text = strings.addCard,
                    modifier = Modifier.padding(8.dp),
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

            Text(
                text = strings.cards,
                style = MaterialTheme.typography.titleMedium,
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                items(state.flashcards, key = { it.id }) { card ->
                    FlashcardGridItem(
                        name = card.name,
                        imageUrl = card.imageUrl,
                        noCoverText = strings.noCover,
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
                        value = state.editingName,
                        onValueChange = { onAction(DeckScreenAction.OnNameChanged(it)) },
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

    AppAnimatedDialog(visible = state.isEditCoverSourceDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(DeckScreenAction.OnDismissEditCoverSourceDialog) },
            title = { Text(strings.editCover) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onAction(DeckScreenAction.OnPickCoverFromGalleryClick) }) {
                        Text(strings.chooseFromGallery)
                    }
                    TextButton(onClick = { onAction(DeckScreenAction.OnTakeCoverPhotoClick) }) {
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

    AppAnimatedDialog(visible = state.isAddCardDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(DeckScreenAction.OnDismissAddCardDialog) },
            title = { Text(strings.addCardTitle) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(min = addCardDialogMinHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DeckCover(
                        coverUri = state.cardImageUrl,
                        noCoverText = strings.noCover,
                        modifier = Modifier
                            .size(addCardCoverSize)
                            .align(Alignment.CenterHorizontally),
                    )

                    TextButton(onClick = { onAction(DeckScreenAction.OnCardCoverButtonClick) }) {
                        Text(if (state.cardImageUrl.isNullOrBlank()) strings.addCover else strings.editCover)
                    }

                    OutlinedTextField(
                        value = state.cardName,
                        onValueChange = { onAction(DeckScreenAction.OnCardNameChanged(it)) },
                        label = { Text(strings.cardNameLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    )
                    Text(
                        text = "${state.cardName.length}/$MAX_DECK_NAME_LENGTH",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                    if (cardValidationText != null) {
                        Text(
                            text = cardValidationText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onAction(DeckScreenAction.OnConfirmAddCard) },
                    enabled = !state.isCardSaving,
                ) {
                    if (state.isCardSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text(strings.create)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(DeckScreenAction.OnDismissAddCardDialog) },
                    enabled = !state.isCardSaving,
                ) {
                    Text(strings.cancel)
                }
            },
        )
    }

    AppAnimatedDialog(visible = state.isCardCoverSourceDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(DeckScreenAction.OnDismissCardCoverSourceDialog) },
            title = { Text(strings.chooseSource) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onAction(DeckScreenAction.OnPickCardCoverFromGalleryClick) }) {
                        Text(strings.chooseFromGallery)
                    }
                    TextButton(onClick = { onAction(DeckScreenAction.OnTakeCardCoverPhotoClick) }) {
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
    noCoverText: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DeckCover(
            coverUri = imageUrl.ifBlank { null },
            noCoverText = noCoverText,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
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
