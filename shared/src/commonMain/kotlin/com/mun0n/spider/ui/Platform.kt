package com.mun0n.spider.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

@Composable
internal expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)

@Composable
internal expect fun rememberRejectFeedback(): () -> Unit

@Composable
internal expect fun tableInsets(): WindowInsets
