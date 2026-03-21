package com.cerebus.fairy_tales.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

@Composable
actual fun rememberPlatformMessenger(): PlatformMessenger =
    remember {
        IosPlatformMessenger()
    }

private class IosPlatformMessenger : PlatformMessenger {
    override fun showMessage(message: String) {
        val top = topViewController() ?: return
        val host = when (top) {
            is UIAlertController -> top.presentingViewController ?: top
            else -> top
        }
        val alert = UIAlertController.alertControllerWithTitle(
            title = null,
            message = message,
            preferredStyle = UIAlertControllerStyleAlert,
        )
        alert.addAction(
            UIAlertAction.actionWithTitle(
                title = "OK",
                style = UIAlertActionStyleDefault,
                handler = null,
            )
        )
        host.presentViewController(
            viewControllerToPresent = alert,
            animated = true,
            completion = null,
        )
    }
}

private fun topViewController(): UIViewController? {
    val application = UIApplication.sharedApplication
    val keyWindow = application.keyWindow
        ?: (
            application.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
        )
        ?: return null
    var top = keyWindow.rootViewController ?: return null
    while (true) {
        val presented = top.presentedViewController ?: break
        top = presented
    }
    return top
}
