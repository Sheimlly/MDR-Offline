package com.mdr.offline.chapters.di

import com.mdr.offline.chapters.application.ChaptersUseCase
import com.mdr.offline.chapters.data.ChapterDetailsService
import com.mdr.offline.chapters.data.ChapterPagesService
import com.mdr.offline.chapters.data.ChapterService
import com.mdr.offline.chapters.data.ChaptersDataSource
import com.mdr.offline.chapters.data.ChaptersRepository
import org.koin.dsl.module

val chaptersModule = module {
    single<ChapterService> { ChapterService(get(), get()) }
    single<ChaptersUseCase> { ChaptersUseCase(get(), get(), get(), get()) }
    single<ChapterPagesService> { ChapterPagesService(get()) }
    single<ChaptersRepository> {ChaptersRepository(get(), get(), get())}
    single<ChaptersDataSource> { ChaptersDataSource(get()) }
    single<ChapterDetailsService> { ChapterDetailsService(get()) }
}