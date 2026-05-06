package com.mdr.offline

import com.mdr.offline.chapters.application.Chapter
import com.mdr.offline.db.MDROfflineDatabase
import com.mdr.offline.mangas.application.Manga

expect class DownloadManager(
    database: MDROfflineDatabase
) {
    fun downloadChapter(manga: Manga, chapter: Chapter)

    fun downloadAllChapters(manga: Manga)
}