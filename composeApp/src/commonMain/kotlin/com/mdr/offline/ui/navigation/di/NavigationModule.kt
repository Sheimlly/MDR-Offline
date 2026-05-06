package com.mdr.offline.ui.navigation.di


import com.mdr.offline.ui.navigation.AboutAppComponent
import com.mdr.offline.ui.navigation.DefaultAboutAppComponent
import com.mdr.offline.ui.navigation.RootComponent
import com.mdr.offline.ui.navigation.DefaultRootComponent
import com.mdr.offline.ui.navigation.manga.DefaultMangaComponent
import com.mdr.offline.ui.navigation.manga.DefaultMangaListComponent
import com.mdr.offline.ui.navigation.manga.DefaultMangaPanelComponent
import com.mdr.offline.ui.navigation.manga.MangaComponent
import com.mdr.offline.ui.navigation.manga.MangaListComponent
import com.mdr.offline.ui.navigation.manga.MangaPanelComponent
import com.mdr.offline.ui.navigation.manga.chapter.ChapterComponent
import com.mdr.offline.ui.navigation.manga.chapter.ChapterPagesComponent
import com.mdr.offline.ui.navigation.manga.chapter.DefaultChapterComponent
import com.mdr.offline.ui.navigation.manga.chapter.DefaultChapterPagesComponent
import com.mdr.offline.ui.navigation.user.AuthenticationComponent
import com.mdr.offline.ui.navigation.user.DefaultAuthenticationComponent
import com.mdr.offline.ui.navigation.user.DefaultMDListsComponent
import com.mdr.offline.ui.navigation.user.MDListsComponent
import org.koin.dsl.module

val navigationModule = module {
//    single<DefaultRootComponent> { DefaultRootComponent(get()) }

//    Manga
    single<ChapterComponent.Factory> {
        DefaultChapterComponent.Factory(
            get(),
        )
    }

    single<ChapterPagesComponent.Factory> {
        DefaultChapterPagesComponent.Factory(
            chapterComponentFactory = get(),
        )
    }

    single<MangaComponent.Factory> {
        DefaultMangaComponent.Factory(
            get(),
            get(),
            get()
        )
    }

    single<MangaListComponent.Factory> {
        DefaultMangaListComponent.Factory(
            get(),
            get(),
            get(),
        )
    }

    single<MangaPanelComponent.Factory> {
        DefaultMangaPanelComponent.Factory(
            mangaListComponentFactory = get(),
            mangaComponentFactory = get(),
            chapterPagesComponentFactory = get(),
        )
    }


//    Auth
    single<AuthenticationComponent.Factory> {
        DefaultAuthenticationComponent.Factory(
            get(),
        )
    }

//    MDLists
    single<MDListsComponent.Factory> {
        DefaultMDListsComponent.Factory(
            userUseCase = get(),
        )
    }

    single<AboutAppComponent.Factory> {
        DefaultAboutAppComponent.Factory()
    }

//    Root
    single<RootComponent.Factory> {
        DefaultRootComponent.Factory(
            mangaPanelComponentFactory = get(),
            authenticationComponentFactory = get(),
            mdListsComponentFactory = get(),
            aboutAppComponentFactory = get(),
            authUseCase = get(),
        )
    }
}