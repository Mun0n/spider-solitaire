package com.mun0n.spider.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FeltScheme = lightColorScheme(
    primary = Color(0xFF0E6A38),
    onPrimary = Color.White,
    secondary = Color(0xFF08381F),
    onSecondary = Color.White,
    background = Color(0xFF0C5A30),
    surface = Color(0xFFF6F0E4),
    onSurface = Color(0xFF1B2418),
)

@Composable
fun SpiderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FeltScheme, content = content)
}
