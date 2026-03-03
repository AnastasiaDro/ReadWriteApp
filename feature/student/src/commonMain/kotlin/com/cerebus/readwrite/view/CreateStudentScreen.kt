package com.cerebus.readwrite.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.cerebus.core.ui.components.AppAnimatedDialog
import com.cerebus.readwrite.media.rememberCoverImagePicker
import com.cerebus.readwrite.navigation.CreateStudentNavigationState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import readwriteapp.feature.student.generated.resources.Res
import readwriteapp.feature.student.generated.resources.add_photo
import readwriteapp.feature.student.generated.resources.add_student_next
import readwriteapp.feature.student.generated.resources.choose_from_gallery
import readwriteapp.feature.student.generated.resources.choose_source
import readwriteapp.feature.student.generated.resources.close
import readwriteapp.feature.student.generated.resources.create_student_avatar_placeholder
import readwriteapp.feature.student.generated.resources.create_student_name_hint
import readwriteapp.feature.student.generated.resources.create_student_name_subtitle
import readwriteapp.feature.student.generated.resources.create_student_title
import readwriteapp.feature.student.generated.resources.take_photo

@Composable
fun CreateStudentRoute(
    onNavigateToDeckList: () -> Unit,
    onNavigateToActiveStudent: () -> Unit,
) {
    val viewModel = koinViewModel<CreateStudentViewModel>()
    val state by viewModel.uiState.collectAsState()
    val picker = rememberCoverImagePicker(
        onImagePicked = { uri ->
            viewModel.onAction(CreateStudentAction.OnPhotoPicked(uri))
        },
        onError = {
            // TODO: show photo picker errors when screen has snackbar/notification host.
        },
    )

    LaunchedEffect(state.pendingPickerRequest) {
        when (state.pendingPickerRequest) {
            StudentPickerRequest.GALLERY -> {
                picker.openGallery()
                viewModel.onAction(CreateStudentAction.OnPickerRequestConsumed)
            }
            StudentPickerRequest.CAMERA -> {
                picker.openCamera()
                viewModel.onAction(CreateStudentAction.OnPickerRequestConsumed)
            }
            null -> Unit
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CreateStudentEffect.NavigateToDeckList -> {
                    if (CreateStudentNavigationState.consumeReturnToActiveStudent()) {
                        onNavigateToActiveStudent()
                    } else {
                        onNavigateToDeckList()
                    }
                }
            }
        }
    }

    CreateStudentScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun CreateStudentScreen(
    state: CreateStudentUiState,
    onAction: (CreateStudentAction) -> Unit,
) {
    val density = LocalDensity.current
    val widthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val isTablet = widthDp >= 840.dp
    val buttonWidthFraction = if (isTablet) 0.62f else 0.86f
    val buttonMinHeight = if (isTablet) (widthDp * 0.08f) else 54.dp

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.create_student_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        AvatarPlaceholder(
            avatarUri = state.avatarUri,
            onClick = { onAction(CreateStudentAction.OnAvatarClick) },
        )

        Text(
            text = stringResource(Res.string.add_photo),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 12.dp, bottom = 24.dp)
                .clickable { onAction(CreateStudentAction.OnAddPhotoClick) },
        )

        Text(
            text = stringResource(Res.string.create_student_name_subtitle),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth(buttonWidthFraction)
                .padding(bottom = 8.dp),
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = { onAction(CreateStudentAction.OnNameChanged(it)) },
            modifier = Modifier
                .fillMaxWidth(buttonWidthFraction),
            placeholder = {
                Text(text = stringResource(Res.string.create_student_name_hint))
            },
            singleLine = true,
        )

        Button(
            onClick = { onAction(CreateStudentAction.OnNextClick) },
            enabled = state.name.trim().isNotEmpty(),
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(buttonWidthFraction)
                .heightIn(min = buttonMinHeight),
        ) {
            Text(text = stringResource(Res.string.add_student_next))
        }
    }

    AppAnimatedDialog(visible = state.isPhotoSourceDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(CreateStudentAction.OnDismissPhotoSourceDialog) },
            title = { Text(stringResource(Res.string.choose_source)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onAction(CreateStudentAction.OnPickFromGalleryClick) }) {
                        Text(stringResource(Res.string.choose_from_gallery))
                    }
                    TextButton(onClick = { onAction(CreateStudentAction.OnTakePhotoClick) }) {
                        Text(stringResource(Res.string.take_photo))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { onAction(CreateStudentAction.OnDismissPhotoSourceDialog) }) {
                    Text(stringResource(Res.string.close))
                }
            },
        )
    }
}

@Composable
private fun AvatarPlaceholder(
    avatarUri: String?,
    onClick: () -> Unit,
) {
    val imageLoader = ImageLoader.Builder(LocalPlatformContext.current).build()

    Box(
        modifier = Modifier
            .size(164.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUri.isNullOrBlank()) {
            Text(
                text = stringResource(Res.string.create_student_avatar_placeholder),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AsyncImage(
                model = avatarUri,
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
