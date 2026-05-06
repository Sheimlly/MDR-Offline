package com.mdr.offline.di

import com.mdr.offline.DownloadManager
import org.koin.dsl.module

val downloadManagerModule = module {
    single<DownloadManager> { DownloadManager(get()) }
}