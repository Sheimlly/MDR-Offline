package com.mdr.offline


import com.mdr.offline.db.MDROfflineDatabase
import com.mdr.offline.mangas.application.Manga
import com.mdr.offline.chapters.application.Chapter

actual class DownloadManager actual constructor(
    private val database: MDROfflineDatabase,
) {
    actual fun downloadAllChapters(manga: Manga) {
        downloadServiceProvider.downloadMangaAndChapters(db = database, manga = manga, chapter = null)
    }

    actual fun downloadChapter(manga: Manga, chapter: Chapter) {
        downloadServiceProvider.downloadMangaAndChapters(db = database, manga = manga, chapter = chapter)
    }
}

interface DownloadServiceProvider {
    fun downloadMangaAndChapters(db: MDROfflineDatabase, manga: Manga, chapter: Chapter?)
}

lateinit var downloadServiceProvider: DownloadServiceProvider