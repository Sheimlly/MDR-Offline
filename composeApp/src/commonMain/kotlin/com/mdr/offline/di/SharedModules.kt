package com.mdr.offline.di

import com.mdr.offline.chapters.di.chaptersModule
import com.mdr.offline.mangas.di.mangasModule
import com.mdr.offline.user.di.userModule
import com.mdr.offline.ui.navigation.di.navigationModule

val sharedKoinModules = listOf(
    mangasModule,
    chaptersModule,
    userModule,
    navigationModule,
    networkModule,
    downloadManagerModule,
)