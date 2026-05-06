package com.mdr.offline.chapters.application

import com.mdr.offline.chapters.data.ChapterRaw
import com.mdr.offline.chapters.data.ChapterService
import com.mdr.offline.chapters.data.ChapterPagesService
import com.mdr.offline.chapters.data.ChaptersDataSource
import com.mdr.offline.chapters.data.ChaptersRepository
import com.mdr.offline.data.Order

class ChaptersUseCase(
    private val service: ChapterService,
    private val chapterPagesService: ChapterPagesService,
    private val repo: ChaptersRepository,
    private val dataSource: ChaptersDataSource
) {
    suspend fun getChapters(mangaId: String, offset: Int = 0, order: Order = Order.Asc): List<Chapter> {
        val chaptersRaw = service.fetchChapters(mangaId, offset, order)

//        val chapters: List<ChapterRaw> = chaptersRaw.mapNotNull { chapter ->
//            try{
//                val url = chapterPagesService.getChapterUrls(chapter.id)
//                if(url.isNotEmpty()) {
//                    ChapterRaw(
//                        id = chapter.id,
//                        attributes = chapter.attributes,
//                        title = chapter.title,
//                        volume = chapter.volume,
//                        chapter = chapter.chapter,
//                        pageNumbers = chapter.pageNumbers,
//                        pages = url
//                    )
//                } else {null}
//            } catch (e: Exception) {
//                null
//            }
//        }

        return mapChapters(chaptersRaw)
    }

//    suspend fun getChapterPages(chapterUrls: List<String>): List<ByteArray> {
//        return chapterPagesService.fetchChapterImages(chapterUrls)
//    }

    suspend fun getChapterIds(mangaId: String): List<String> {
        return service.fetchChapterIds(mangaId)
    }
    suspend fun getChaptersById(chapterIds: List<String>): List<Chapter> {
        val chaptersRaw = service.fetchChaptersById(chapterIds)
        return mapChapters(chaptersRaw)
    }

    suspend fun getChapterUrls(chapterId: String, highQuality: Boolean = false): List<String> {
        return chapterPagesService.getChapterUrls(chapterId, highQuality)
    }

    suspend fun downloadAllChapters(
        mangaId: String,
        updateDownload: (count: Int, total: Int, chapterId: String?, chapterDownloaded: Boolean) -> Unit
    ) {
        repo.downloadAllChapters(mangaId, {count, total, chapterId, chapterDownloaded -> updateDownload(count, total, chapterId, chapterDownloaded)})
    }

    suspend fun downloadChapter(chapter: Chapter, mangaId: String) {
        repo.downloadChapter(chapter, mangaId)
    }

    fun getDownloadedChapters(mangaId: String): List<Chapter> {
        return repo.getDownloadedChapters(mangaId)
    }


//    Queries for downloaded Chapters
    fun getLastReadPage(combinedId: String): Int{
        return dataSource.getLastReadPage(combinedId)
    }

    fun updateLastReadPage(combinedId: String, lastReadPage: Int) {
        dataSource.updateLastReadPage(combinedId, lastReadPage)
    }

    fun getIfChapterRead(combinedId: String): Boolean {
        return dataSource.getIfChapterRead(combinedId)
    }

    fun updateIfChapterRead(combinedId: String, read: Boolean) {
        dataSource.updateIfChapterRead(combinedId, read)
    }

    fun updateIfChapterFilesDownloaded(combinedId: String, filesDownloaded: Boolean) {
        dataSource.updateIfChapterFilesDownloaded(combinedId, filesDownloaded)
    }
//    End of queries for downloaded chapters

    fun checkDownloadedChapterById(mangaId: String, chapterId: String): Boolean {
        return repo.getDownloadedChapterById(mangaId, chapterId)
    }

    fun deleteDownloadedChapter(chapter: Chapter) {
        repo.deleteDownloadedChapter(chapter)
    }

    private fun mapChapters(chaptersRaw: List<ChapterRaw>): List<Chapter> = chaptersRaw.map { raw ->
        Chapter(
            id = raw.id,
            title = raw.title,
            volume = raw.volume,
            chapter = raw.chapter,
            scanlationGroup = raw.scanlationGroup,
            pageNumbers = raw.pageNumbers,
            pages = raw.pages,
            read = false,
            filesDownloaded = false
        )
    }
}