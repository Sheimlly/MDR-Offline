package com.mdr.offline.ui.screens.manga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.mdr.offline.ui.navigation.manga.MangaPanelComponent
import com.mdr.offline.ui.screens.manga.chapter.ChapterPagesScreen

@Composable
fun MangaPanelUI(mangaPanelComponent: MangaPanelComponent) {
    val childStack by mangaPanelComponent.childStack.subscribeAsState()

    Children(stack = childStack) {
        createChild ->
        when(val instance = createChild.instance) {
            is MangaPanelComponent.Child.MangaList -> MangaListScreen(component = instance.component)
            is MangaPanelComponent.Child.MangaDetails -> MangaScreen(component = instance.component)
            is MangaPanelComponent.Child.ChapterPages -> ChapterPagesScreen(component = instance.component)
        }
    }
}