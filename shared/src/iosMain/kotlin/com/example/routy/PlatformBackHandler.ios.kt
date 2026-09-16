@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.example.routy

import androidx.compose.runtime.*
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.Foundation.NSSelectorFromString
import platform.UIKit.*
import platform.darwin.NSObject

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    val controller = LocalUIViewController.current
    val latestBack by rememberUpdatedState(onBack)
    DisposableEffect(controller, enabled) {
        val target = BackGestureTarget { latestBack() }
        val gesture = UIScreenEdgePanGestureRecognizer(target, NSSelectorFromString("handlePan:"))
        gesture.edges = UIRectEdgeLeft
        gesture.enabled = enabled
        controller.view.addGestureRecognizer(gesture)
        onDispose { controller.view.removeGestureRecognizer(gesture) }
    }
}

private class BackGestureTarget(
    val onBack: () -> Unit,
) : NSObject() {
    @ObjCAction
    fun handlePan(gesture: UIScreenEdgePanGestureRecognizer) {
        if (gesture.state == UIGestureRecognizerStateEnded && gesture.translationInView(gesture.view).useContents { x } > 60.0) onBack()
    }
}
