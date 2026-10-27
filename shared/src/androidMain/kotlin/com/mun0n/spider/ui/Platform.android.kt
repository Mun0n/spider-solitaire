package com.mun0n.spider.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
internal actual fun rememberRejectFeedback(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            val feedback = if (Build.VERSION.SDK_INT >= 30) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(feedback)
        }
    }
}

@Composable
internal actual fun tableInsets(): WindowInsets = WindowInsets.displayCutout
