package com.cerebus.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
fun AppAnimatedDialog(
    visible: Boolean,
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
