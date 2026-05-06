package com.mdr.offline.ui.navigation.manga

import androidx.compose.runtime.State
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.mdr.offline.chapters.application.Chapter
import com.mdr.offline.data.ContentType
import com.mdr.offline.mangas.application.Manga
import com.mdr.offline.ui.navigation.manga.chapter.ChapterPagesComponent
import com.mdr.offline.user.application.MDList
import com.mdr.offline.user.application.Tokens
import kotlinx.serialization.Serializable

interface MangaPanelComponent {
    val childStack: Value<ChildStack<*, Child>>

    sealed interface Child {
        data class MangaList(val component: MangaListComponent) : Child
        data class MangaDetails(val component: MangaComponent) : Child
        data class ChapterPages(val component: ChapterPagesComponent) : Child
    }

    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            isUserLoggedIn: MutableValue<Boolean>,
            logOut: () -> Unit,
            rootNavigation: (String) -> Unit,
            token: State<Tokens>,
            mdList: MDList?
        ): MangaPanelComponent
    }
}

class DefaultMangaPanelComponent(
    componentContext: ComponentContext,
    private val mangaListComponentFactory: MangaListComponent.Factory,
    private val mangaComponentFactory: MangaComponent.Factory,
    private val chapterPagesComponentFactory: ChapterPagesComponent.Factory,
    private val isUserLoggedIn: MutableValue<Boolean>,
    private val logOut: () -> Unit,
    private val rootNavigation: (String) -> Unit,
    private val token: State<Tokens>,
    private val mdList: MDList?
): MangaPanelComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    override val childStack: Value<ChildStack<*, MangaPanelComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.MangaList(null),
            handleBackButton = true,
            childFactory = ::createChild
        )

    @OptIn(DelicateDecomposeApi::class)
    private fun createChild(
        config: Config,
        context: ComponentContext
    ): MangaPanelComponent.Child {
        return when (config) {
            is Config.MangaList -> MangaPanelComponent.Child.MangaList(
                mangaListComponentFactory(
                    componentContext = context,
                    onMangaSelected = { manga, type -> navigation.push(Config.MangaDetails(manga, type)) },
                    isUserLoggedIn = isUserLoggedIn,
                    logOut = { logOut() },
                    rootNavigation = rootNavigation,
                    mdList = mdList
                )
            )
            is Config.MangaDetails -> MangaPanelComponent.Child.MangaDetails(
                mangaComponentFactory(
                    componentContext = context,
                    manga = config.manga,
                    contentType = config.contentType,
                    isUserLoggedIn = isUserLoggedIn,
                    onChapterSelected = { initialChapter, allChapters, format, type, updateChapter ->
                        navigation.push(
                            Config.ChapterPages(initialChapter, allChapters, format, type, updateChapter)
                        )
                    },
                    token = token,
                    onBack = { navigation.pop() }
                )
            )
            is Config.ChapterPages -> MangaPanelComponent.Child.ChapterPages(
                chapterPagesComponentFactory(
                    componentContext = context,
                    initialChapter = config.initialChapter,
                    allChapters = config.allChapters,
                    format = config.format,
                    contentType = config.contentType,
                    isUserLoggedIn = isUserLoggedIn,
                    updateChapter = config.updateChapter,
                    onFinished = { navigation.pop() }
                )
            )
        }
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data class MangaList(val mdList: MDList?) : Config

        @Serializable
        data class MangaDetails(val manga: Manga, val contentType: ContentType) : Config

        @Serializable
        data class ChapterPages(
            val initialChapter: Chapter,
            val allChapters: List<Chapter>,
            val format: String,
            val contentType: ContentType,
            val updateChapter: (Chapter, Boolean) -> Unit?
        ) : Config

    }

    class Factory(
        private val mangaListComponentFactory: MangaListComponent.Factory,
        private val mangaComponentFactory: MangaComponent.Factory,
        private val chapterPagesComponentFactory: ChapterPagesComponent.Factory,
    ) : MangaPanelComponent.Factory {
        override fun invoke(
            componentContext: ComponentContext,
            isUserLoggedIn: MutableValue<Boolean>,
            logOut: () -> Unit,
            rootNavigation: (String) -> Unit,
            token: State<Tokens>,
            mdList: MDList?
        ): MangaPanelComponent = DefaultMangaPanelComponent(
            componentContext = componentContext,
            mangaListComponentFactory = mangaListComponentFactory,
            mangaComponentFactory = mangaComponentFactory,
            chapterPagesComponentFactory = chapterPagesComponentFactory,
            isUserLoggedIn = isUserLoggedIn,
            logOut = logOut,
            rootNavigation = rootNavigation,
            token = token,
            mdList = mdList
        )
    }
}