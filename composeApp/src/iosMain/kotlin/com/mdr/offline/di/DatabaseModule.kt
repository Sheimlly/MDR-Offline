package com.mdr.offline.di

import app.cash.sqldelight.db.SqlDriver
import com.mdr.offline.db.DatabaseDriverFactory
import com.mdr.offline.db.DownloadedChapter
import com.mdr.offline.db.DownloadedManga
import com.mdr.offline.db.FetchedManga
import com.mdr.offline.db.MDROfflineDatabase
import com.mdr.offline.db.adapters.stringListAdapter
import org.koin.dsl.module

val databaseModule = module {
    single<SqlDriver> { DatabaseDriverFactory().createDriver() }

    single<MDROfflineDatabase> {
        MDROfflineDatabase(
            driver = get(),
            DownloadedChapter.Adapter(
                imagesPathAdapter = stringListAdapter
            ),
            DownloadedManga.Adapter(
                genresAdapter = stringListAdapter
            ),
            FetchedManga.Adapter(
                genresAdapter = stringListAdapter
            )
        )
    }
}