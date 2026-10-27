package com.mun0n.spider.ui

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.mun0n.spider.data.DirSaveFiles
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    SpiderApp(remember { DirSaveFiles() })
}
