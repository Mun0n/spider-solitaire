package com.mun0n.spider.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

// iOS no tiene botón atrás.
@Composable
internal actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

@Composable
internal actual fun rememberRejectFeedback(): () -> Unit {
    val generator = remember { UINotificationFeedbackGenerator() }
    return remember(generator) {
        { generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError) }
    }
}

// En iPhone, además del notch, deja libre la barra de inicio.
@Composable
internal actual fun tableInsets(): WindowInsets = WindowInsets.safeDrawing
