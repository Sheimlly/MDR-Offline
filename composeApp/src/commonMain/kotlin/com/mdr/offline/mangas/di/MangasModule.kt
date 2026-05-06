package com.mdr.offline.mangas.di


import com.mdr.offline.mangas.application.MangasUseCase
import com.mdr.offline.mangas.data.MangaDetailsService
import com.mdr.offline.mangas.data.MangaService
import com.mdr.offline.mangas.data.MangasDataSource
import com.mdr.offline.mangas.data.MangasRepository
import org.koin.dsl.module

val mangasModule = module {
    single<MangaService> { MangaService(get(), get()) }
    single<MangasUseCase> { MangasUseCase(get(), get(), get()) }
    single<MangaDetailsService> { MangaDetailsService(get()) }
    single<MangasRepository> { MangasRepository(get(), get()) }
    single<MangasDataSource> {MangasDataSource(get())}
}