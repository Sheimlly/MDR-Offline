package com.mdr.offline.ui.navigation.manga.chapter

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.mdr.offline.chapters.application.Chapter
import com.mdr.offline.data.ContentType
import kotlinx.serialization.Serializable

interface ChapterPagesComponent {
    val pages: Value<ChildPages<*, ChapterComponent>>

    val format: String

    val currentChapterComponent: MutableState<ChapterComponent?>
    fun setCurrentChapterComponent(component: ChapterComponent)

    fun onSelectPage(index: Int)

    val showTopBar: MutableState<Boolean>
    fun onShowTopBarChange()

    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            allChapters: List<Chapter>,
            format: String,
            contentType: ContentType,
            initialChapter: Chapter,
            isUserLoggedIn: MutableValue<Boolean>,
            updateChapter: (Chapter, Boolean) -> Unit?,
            onFinished: () -> Unit,
        ): ChapterPagesComponent
    }
}

class DefaultChapterPagesComponent(
    private val componentContext: ComponentContext,
    private val initialChapter: Chapter,
    private val allChapters: List<Chapter>,
    override val format: String,
    private val contentType: ContentType,
    private val isUserLoggedIn: MutableValue<Boolean>,
    private val updateChapter: (Chapter, Boolean) -> Unit?,
    private val onFinished: () -> Unit,
    private val chapterComponentFactory: ChapterComponent.Factory,
) : ChapterPagesComponent, ComponentContext by componentContext {

    private val navigation = PagesNavigation<Config>()

    override val pages: Value<ChildPages<*, ChapterComponent>> =
        childPages(
            source = navigation,
            serializer = Config.serializer(),
            initialPages = {
                Pages(
                    items = allChapters.map { chapter -> Config(chapter.id) },
                    selectedIndex = allChapters.indexOf(initialChapter)
                )
            },
            key = initialChapter.id,
            handleBackButton = true,
            childFactory = { config, childContext ->
                chapterComponentFactory(
                    componentContext = childContext,
                    chapter = allChapters.firstOrNull { it.id == config.data } ?: initialChapter,
                    format = format,
                    contentType = contentType,
                    isUserLoggedIn = isUserLoggedIn,
                    updateChapter = updateChapter,
                    onBack = { onFinished() },
                )
            }
        )

    override fun onSelectPage(index: Int) {
        if (index in allChapters.indices) {
            navigation.select(index)
        }
    }


    private val _currentChapterComponent = mutableStateOf<ChapterComponent?>(null)
    override val currentChapterComponent: MutableState<ChapterComponent?> get() = _currentChapterComponent

    override fun setCurrentChapterComponent(component: ChapterComponent) {
        _currentChapterComponent.value = component
        if(contentType is ContentType.Downloaded) updateChapter(component.model.value.chapter, true)
    }

    private val _showTopBar = mutableStateOf<Boolean>(true)
    override val showTopBar: MutableState<Boolean> get() = _showTopBar

    override fun onShowTopBarChange() {
        _showTopBar.value = !_showTopBar.value
    }


    @Serializable
    private data class Config(val data: String)

    class Factory(
        private val chapterComponentFactory: ChapterComponent.Factory,
    ) : ChapterPagesComponent.Factory {
        override fun invoke(
            componentContext: ComponentContext,
            allChapters: List<Chapter>,
            format: String,
            contentType: ContentType,
            initialChapter: Chapter,
            isUserLoggedIn: MutableValue<Boolean>,
            updateChapter: (Chapter, Boolean) -> Unit?,
            onFinished: () -> Unit,
        ): ChapterPagesComponent {
            return DefaultChapterPagesComponent(
                componentContext = componentContext,
                allChapters = allChapters,
                format = format,
                contentType = contentType,
                initialChapter = initialChapter,
                updateChapter = updateChapter,
                isUserLoggedIn = isUserLoggedIn,
                onFinished = onFinished,
                chapterComponentFactory = chapterComponentFactory,
            )
        }
    }
}