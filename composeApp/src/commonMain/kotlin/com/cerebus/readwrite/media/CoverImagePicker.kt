package com.cerebus.readwrite.media

import androidx.compose.runtime.Composable

interface CoverImagePicker {
    fun openGallery()
    fun openCamera()
}

@Composable
expect fun rememberCoverImagePicker(
    onImagePicked: (String) -> Unit,
    onError: (String) -> Unit,
): CoverImagePicker
