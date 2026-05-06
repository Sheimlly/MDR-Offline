package com.mdr.offline.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.mdr.offline.ui.navigation.RootComponent
import com.mdr.offline.ui.screens.AboutAppScreen
import com.mdr.offline.ui.screens.manga.MDListsScreen
import com.mdr.offline.ui.screens.manga.MangaPanelUI
import com.mdr.offline.ui.screens.user.LoginScreenContent
import org.koin.compose.koinInject

@Composable
fun App(
    component: RootComponent = koinInject()
) {
    MangaDexTheme{
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MangaDexTheme.color.background
        ) {
            val slot by component.slot.subscribeAsState()

            slot.child?.instance?.let { child ->
                when (child) {
                    is RootComponent.Child.MangaPanel -> {
                        MangaPanelUI(child.component) // Render manga panel
                    }
                    is RootComponent.Child.Authentication -> {
                        LoginScreenContent(child.component) // Render login
                    }
                    is RootComponent.Child.MDLists -> {
                        MDListsScreen(child.component)
                    }
                    is RootComponent.Child.AboutApp -> {
                        AboutAppScreen(child.component)
                    }
                    // Add more child types if needed in the future
                }
            }

        }
    }
}