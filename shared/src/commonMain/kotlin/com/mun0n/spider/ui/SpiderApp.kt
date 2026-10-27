package com.mun0n.spider.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mun0n.spider.data.GameRepository
import com.mun0n.spider.data.SaveFiles

@Composable
fun SpiderApp(files: SaveFiles) {
    SpiderTheme {
        TableScreen(viewModel { GameViewModel(GameRepository(files)) })
    }
}
