package com.cerebus.readwrite.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import com.cerebus.core.ui.components.AppEntityEditorDialog
import com.cerebus.core.ui.components.AppEntityEditorMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cerebus.create_screen.navigation.DeckNavigationState
import com.cerebus.readwrite.media.rememberCoverImagePicker
import com.cerebus.readwrite.media.rememberPlatformMessenger
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import readwriteapp.feature.student.generated.resources.Res
import readwriteapp.feature.student.generated.resources.active_student_no_decks
import readwriteapp.feature.student.generated.resources.active_student_delete
import readwriteapp.feature.student.generated.resources.active_student_delete_student_message
import readwriteapp.feature.student.generated.resources.active_student_delete_student_secondary_message
import readwriteapp.feature.student.generated.resources.active_student_delete_student_title
import readwriteapp.feature.student.generated.resources.active_student_edit
import readwriteapp.feature.student.generated.resources.active_student_edit_student_title
import readwriteapp.feature.student.generated.resources.active_student_error_delete_student_failed
import readwriteapp.feature.student.generated.resources.active_student_error_open_photo_picker_failed
import readwriteapp.feature.student.generated.resources.active_student_error_update_student_failed
import readwriteapp.feature.student.generated.resources.add_photo
import readwriteapp.feature.student.generated.resources.cancel
import readwriteapp.feature.student.generated.resources.choose_from_gallery
import readwriteapp.feature.student.generated.resources.choose_source
import readwriteapp.feature.student.generated.resources.change_student_back
import readwriteapp.feature.student.generated.resources.change_student_create
import readwriteapp.feature.student.generated.resources.change_student_last_deck
import readwriteapp.feature.student.generated.resources.change_student_title
import readwriteapp.feature.student.generated.resources.close
import readwriteapp.feature.student.generated.resources.create_student_name_subtitle
import readwriteapp.feature.student.generated.resources.save
import readwriteapp.feature.student.generated.resources.take_photo

@Composable
fun ChangeStudentRoute(
    onBackClick: () -> Unit,
    onOpenDeck: (String) -> Unit,
    onOpenCreateStudent: () -> Unit,
) {
    val viewModel = koinViewModel<ChangeStudentViewModel>()
    val state by viewModel.uiState.collectAsState()
    val effect by viewModel.effects.collectAsState()
    val messenger = rememberPlatformMessenger()
    val photoPickerErrorText = stringResource(Res.string.active_student_error_open_photo_picker_failed)
    val updateStudentErrorText = stringResource(Res.string.active_student_error_update_student_failed)
    val deleteStudentErrorText = stringResource(Res.string.active_student_error_delete_student_failed)
    val photoPicker = rememberCoverImagePicker(
        onImagePicked = { uri ->
            viewModel.onAction(ChangeStudentAction.OnPhotoPicked(uri))
        },
        onError = {
            messenger.showMessage(photoPickerErrorText)
        },
    )

    LaunchedEffect(
        state.pendingPickerRequest,
        state.isPhotoSourceDialogVisible,
    ) {
        if (state.isPhotoSourceDialogVisible) return@LaunchedEffect
        when (state.pendingPickerRequest) {
            StudentPickerRequest.GALLERY -> {
                photoPicker.openGallery()
                viewModel.onAction(ChangeStudentAction.OnPickerRequestConsumed)
            }
            StudentPickerRequest.CAMERA -> {
                photoPicker.openCamera()
                viewModel.onAction(ChangeStudentAction.OnPickerRequestConsumed)
            }
            null -> Unit
        }
    }

    LaunchedEffect(effect) {
        when (val current = effect) {
            ChangeStudentEffect.NavigateBack -> {
                onBackClick()
                viewModel.consumeEffect()
            }

            is ChangeStudentEffect.OpenDeck -> {
                DeckNavigationState.selectDeck(deckId = current.deckId)
                onOpenDeck(current.deckId)
                viewModel.consumeEffect()
            }

            ChangeStudentEffect.OpenCreateStudent -> {
                onOpenCreateStudent()
                viewModel.consumeEffect()
            }

            ChangeStudentEffect.ShowStudentUpdateFailed -> {
                messenger.showMessage(updateStudentErrorText)
                viewModel.consumeEffect()
            }

            ChangeStudentEffect.ShowDeleteStudentFailed -> {
                messenger.showMessage(deleteStudentErrorText)
                viewModel.consumeEffect()
            }

            null -> Unit
        }
    }

    ChangeStudentScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
    )
}

@Composable
private fun ChangeStudentScreen(
    state: ChangeStudentUiState,
    onAction: (ChangeStudentAction) -> Unit,
    onBackClick: () -> Unit,
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val windowHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
    val isLandscape = windowWidthDp > windowHeightDp
    val isTablet = minOf(windowWidthDp, windowHeightDp) >= 600.dp
    val listHorizontalPadding = when {
        isLandscape && isTablet -> 60.dp
        isLandscape -> 32.dp
        else -> 0.dp
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = 10.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(onClick = onBackClick) {
            Text(text = stringResource(Res.string.change_student_back))
        }
        Text(
            text = stringResource(Res.string.change_student_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = listHorizontalPadding),
        ) {
            items(state.students) { student ->
                StudentRow(
                    item = student,
                    isActive = student.studentId == state.activeStudentId,
                    isMenuExpanded = state.expandedMenuStudentId == student.studentId,
                    onStudentClick = { onAction(ChangeStudentAction.OnStudentClick(student.studentId)) },
                    onDeckClick = { deckId ->
                        onAction(ChangeStudentAction.OnDeckClick(student.studentId, deckId))
                    },
                    onMoreActionsClick = {
                        onAction(ChangeStudentAction.OnStudentMenuClick(student.studentId))
                    },
                    onDismissMenu = {
                        onAction(ChangeStudentAction.OnDismissStudentMenu)
                    },
                    onEditClick = {
                        onAction(ChangeStudentAction.OnEditStudentClick(student.studentId))
                    },
                    onDeleteClick = {
                        onAction(ChangeStudentAction.OnDeleteStudentClick(student.studentId))
                    },
                )
            }
        }

        Button(
            onClick = { onAction(ChangeStudentAction.OnCreateStudentClick) },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Text(text = stringResource(Res.string.change_student_create))
        }
    }

    val deletingStudent = state.students.firstOrNull { it.studentId == state.deletingStudentId }

    AppEntityEditorDialog(
        visible = state.isEditStudentDialogVisible,
        mode = AppEntityEditorMode.EDIT,
        createTitle = stringResource(Res.string.active_student_edit_student_title),
        editTitle = stringResource(Res.string.active_student_edit_student_title),
        createConfirmText = stringResource(Res.string.save),
        editConfirmText = stringResource(Res.string.save),
        value = state.editStudentName,
        onValueChange = { onAction(ChangeStudentAction.OnEditStudentNameChanged(it)) },
        fieldLabel = stringResource(Res.string.create_student_name_subtitle),
        coverButtonText = stringResource(Res.string.add_photo),
        onCoverButtonClick = { onAction(ChangeStudentAction.OnEditStudentAvatarClick) },
        onConfirm = { onAction(ChangeStudentAction.OnSaveStudentChanges) },
        onDismiss = { onAction(ChangeStudentAction.OnDismissEditStudentDialog) },
        cancelText = stringResource(Res.string.cancel),
        maxLength = 40,
        minDialogHeight = 220.dp,
        validationErrorText = null,
        isSaving = false,
        coverContent = {
            StudentAvatar(
                avatarUri = state.editStudentAvatarUri,
                size = 104.dp,
            )
        },
    )

    if (state.isPhotoSourceDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(ChangeStudentAction.OnDismissPhotoSourceDialog) },
            title = { Text(stringResource(Res.string.choose_source)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onAction(ChangeStudentAction.OnPickFromGalleryClick) }) {
                        Text(stringResource(Res.string.choose_from_gallery))
                    }
                    TextButton(onClick = { onAction(ChangeStudentAction.OnTakePhotoClick) }) {
                        Text(stringResource(Res.string.take_photo))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { onAction(ChangeStudentAction.OnDismissPhotoSourceDialog) }) {
                    Text(stringResource(Res.string.close))
                }
            },
        )
    }

    if (state.isDeleteStudentDialogVisible && deletingStudent != null) {
        AlertDialog(
            onDismissRequest = { onAction(ChangeStudentAction.OnDismissDeleteStudentDialog) },
            title = { Text(stringResource(Res.string.active_student_delete_student_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StudentAvatar(
                        avatarUri = deletingStudent.avatarUri,
                        size = 88.dp,
                    )
                    Text(
                        text = deletingStudent.studentName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(
                            Res.string.active_student_delete_student_message,
                            deletingStudent.studentName,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.active_student_delete_student_secondary_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onAction(ChangeStudentAction.OnConfirmDeleteStudent) },
                ) {
                    Text(stringResource(Res.string.active_student_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ChangeStudentAction.OnDismissDeleteStudentDialog) }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun StudentRow(
    item: ChangeStudentListItem,
    isActive: Boolean,
    isMenuExpanded: Boolean,
    onStudentClick: () -> Unit,
    onDeckClick: (String) -> Unit,
    onMoreActionsClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val rowBackground = if (isActive) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onStudentClick)
            .background(rowBackground)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StudentAvatar(
            avatarUri = item.avatarUri,
            size = 72.dp,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = item.studentName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.change_student_last_deck),
                style = MaterialTheme.typography.bodyMedium,
            )

            val deck = item.lastLessonDeck
            if (deck == null) {
                Text(
                    text = stringResource(Res.string.active_student_no_decks),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                DeckInlineItem(
                    deck = deck,
                    onClick = { onDeckClick(deck.id) },
                )
            }
        }

        Box {
            IconButton(onClick = onMoreActionsClick) {
                Text(
                    text = "⋯",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = onDismissMenu,
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.active_student_edit)) },
                    onClick = onEditClick,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.active_student_delete)) },
                    onClick = onDeleteClick,
                )
            }
        }
    }
}
