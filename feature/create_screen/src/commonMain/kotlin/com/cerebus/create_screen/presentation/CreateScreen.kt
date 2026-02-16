package com.cerebus.create_screen.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.cerebus.decks.domain.models.Deck
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
            FloatingActionButton(onClick = { onAction(CreateScreenAction.OnCreateDeckClick) }) {
                Text(
                    text = strings.createDeckTitle,
                    modifier = Modifier.padding(8.dp),
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
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 84.dp),
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
                        )
                    }
                }
            }
        }
    }

    AnimatedAlertDialog(
        visible = state.isCreateDialogVisible,
        onDismissRequest = { onAction(CreateScreenAction.OnDismissCreateDialog) },
    ) {
        AlertDialog(
            onDismissRequest = { onAction(CreateScreenAction.OnDismissCreateDialog) },
            title = { Text(strings.createDeckTitle) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(min = createDialogMinHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CoverPreview(
                        coverUri = state.coverUri,
                        noCoverText = strings.noCover,
                        modifier = Modifier
                            .size(createCoverSize)
                            .aspectRatio(1f)
                            .align(Alignment.CenterHorizontally),
                    )

                    TextButton(
                        onClick = { onAction(CreateScreenAction.OnCoverButtonClick) },
                    ) {
                        Text(if (state.coverUri == null) strings.addCover else strings.editCover)
                    }

                    OutlinedTextField(
                        value = state.deckName,
                        onValueChange = { onAction(CreateScreenAction.OnDeckNameChanged(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        label = { Text(strings.deckNameLabel) },
                        singleLine = true,
                    )
                    Text(
                        text = "${state.deckName.length}/$MAX_DECK_NAME_LENGTH",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )

                    if (validationErrorText != null) {
                        Text(
                            text = validationErrorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onAction(CreateScreenAction.OnConfirmCreateDeck) },
                    enabled = !state.isSaving,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text(strings.create)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(CreateScreenAction.OnDismissCreateDialog) },
                    enabled = !state.isSaving,
                ) {
                    Text(strings.cancel)
                }
            },
        )
    }

    AnimatedAlertDialog(
        visible = state.isCoverSourceDialogVisible,
        onDismissRequest = { onAction(CreateScreenAction.OnDismissCoverSourceDialog) },
    ) {
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

    AnimatedAlertDialog(
        visible = state.isDeleteDialogVisible,
        onDismissRequest = { onAction(CreateScreenAction.OnDismissDeleteDialog) },
    ) {
        AlertDialog(
            onDismissRequest = { onAction(CreateScreenAction.OnDismissDeleteDialog) },
            title = { Text(strings.deleteDeckTitle) },
            text = {
                Text(
                    strings.deleteDeckMessageTemplate.replace(
                        "%s",
                        state.deckPendingDelete?.name.orEmpty(),
                    )
                )
            },
            confirmButton = {
                Button(onClick = { onAction(CreateScreenAction.OnConfirmDeleteDeck) }) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(CreateScreenAction.OnDismissDeleteDialog) }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    AnimatedAlertDialog(
        visible = state.isSuccessDialogVisible,
        onDismissRequest = { onAction(CreateScreenAction.OnCloseSuccessDialog) },
    ) {
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
        CoverPreview(
            coverUri = deck.coverUri,
            noCoverText = noCoverText,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
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
        if (coverUri == null) {
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

@Composable
private fun AnimatedAlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val transitionState = remember { MutableTransitionState(false) }

    LaunchedEffect(visible) {
        transitionState.targetState = visible
    }

    if (transitionState.currentState || transitionState.targetState) {
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(animationSpec = tween(220)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(220),
            ),
            exit = fadeOut(animationSpec = tween(170)) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(170),
            ),
        ) {
            content()
        }
    }
}
