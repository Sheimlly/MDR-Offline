package com.mdr.offline

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.mdr.offline.ui.App
import com.mdr.offline.ui.navigation.RootComponent
import org.koin.compose.getKoin

fun MainViewController() = ComposeUIViewController {
    val rootComponentFactory: RootComponent.Factory = getKoin().get()

    val rootComponent = remember {
        rootComponentFactory(DefaultComponentContext(LifecycleRegistry()))
    }

    App(rootComponent)
}
