package com.mdr.offline.chapters.data

import com.mdr.offline.chapters.application.Chapter
import com.mdr.offline.db.MDROfflineDatabase

class ChaptersDataSource(
    private val database: MDROfflineDatabase,
) {
    fun getDownloadedChapters(mangaId: String): List<Chapter> =
        database.mDROfflineDatabaseQueries.selectDownloadedChapters(mangaId = mangaId, ::mapToDownloadedChapter).executeAsList()


    fun getDownloadedChapterById(mangaId: String, chapterId: String): Chapter? =
        database.mDROfflineDatabaseQueries.selectDownloadedChapterById("$mangaId$chapterId", ::mapToDownloadedChapter).executeAsOneOrNull()

    fun getLastReadPage(combinedId: String): Int =
        database.mDROfflineDatabaseQueries.selectLastReadPage(combinedId).executeAsOne().toInt()

    fun updateLastReadPage(combinedId: String, lastReadPage: Int) =
        database.mDROfflineDatabaseQueries.updateLastReadPage(lastReadPage.toLong(), combinedId)

    fun getIfChapterRead(combinedId: String): Boolean =
        database.mDROfflineDatabaseQueries.selectIfChapterRead(combinedId).executeAsOne()

    fun updateIfChapterRead(combinedId: String, read: Boolean) =
        database.mDROfflineDatabaseQueries.updateIfChapterRead(read, combinedId)

    fun updateIfChapterFilesDownloaded(combinedId: String, filesDownloaded: Boolean) =
        database.mDROfflineDatabaseQueries.updateIfChapterFilesDownloaded(filesDownloaded, combinedId)

    fun insertDownloadedChapters(chapter: Chapter) {
        database.mDROfflineDatabaseQueries.insertDownloadedChapter(
            chapter.combinedId!!,
            chapter.mangaId!!,
            chapter.id,
            chapter.title,
            chapter.volume,
            chapter.chapter,
            chapter.scanlationGroup,
            chapter.pageNumbers.toLong(),
            chapter.imagesPath ?: emptyList()
        )
    }

    fun deleteDownloadedChapters(combinedId: String) {
        database.mDROfflineDatabaseQueries.deleteDownloadedChapter(combinedId)
    }

    private fun mapToDownloadedChapter(
        combinedId: String,
        mangaId: String,
        id: String,
        title: String?,
        volume: String?,
        chapter: String,
        scanlationGroup: String,
        pageNumbers: Long,
        imagesPath: List<String>,
        lastReadPage: Long?,
        read: Boolean?,
        filesDownloaded: Boolean?
    ): Chapter =
        Chapter(
            combinedId = combinedId,
            mangaId = mangaId,
            id = id,
            title = title,
            volume = volume,
            chapter = chapter,
            scanlationGroup = scanlationGroup,
            pageNumbers = pageNumbers.toInt(),
            imagesPath = imagesPath,
            lastReadPage = lastReadPage!!.toInt(),
            read = read!!,
            filesDownloaded = filesDownloaded!!
        )
}