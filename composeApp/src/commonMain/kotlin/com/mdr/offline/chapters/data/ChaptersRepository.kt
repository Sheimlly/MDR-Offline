package com.mdr.offline.chapters.data

import com.mdr.offline.ImageStorage
import com.mdr.offline.chapters.application.Chapter
import com.mdr.offline.data.Order

class ChaptersRepository(
    private val dataSource: ChaptersDataSource,
    private val service: ChapterService,
    private val pagesService: ChapterPagesService,
) {
    private suspend fun getAllChapters(mangaId: String, offset: Int = 0, order: Order, chapters: List<ChapterRaw> = emptyList()): List<ChapterRaw> {
        val response = service.fetchChapters(mangaId, offset, order)

        if(response.isEmpty()) {
            return chapters
        } else {
            return getAllChapters(mangaId, offset + 10, order, chapters + response)
        }
    }

    suspend fun downloadAllChapters(
        mangaId: String,
        updateDownload: (count: Int, total: Int, chapterId: String?, chapterDownloaded: Boolean) -> Unit
    ) {
        val downloadedChapters = dataSource.getDownloadedChapters(mangaId)
        val chapters = mapToChapters(getAllChapters(mangaId = mangaId, order = Order.Asc))

        var count = 1
        val total = chapters.size

        updateDownload(count, total, null, false)

        chapters.forEach { chapter ->
            if(!downloadedChapters.any { it.id == chapter.id }) {
                updateDownload(count, total, chapter.id, false)

                downloadChapter(chapter, mangaId)

                updateDownload(count, total, chapter.id, true)

            }
            count++
            updateDownload(count, total, null, false)
        }

    }

    suspend fun downloadChapter(chapter: Chapter, mangaId: String) {

        var index = 0
        val imagePaths: MutableList<String> = mutableListOf()
        var pages = chapter.pages ?: emptyList()


        if(pages.isEmpty()) {
            pages = pagesService.getChapterUrls(chapter.id, highQuality = true)
        }

        pages.forEach {imageUrl ->
            val filename = "$mangaId+${chapter.id}+$index.png"
            imagePaths += filename
            ImageStorage().saveImage(imageUrl, filename)
            index++
        }

        dataSource.insertDownloadedChapters(mapToDownloadedChapter(chapter, mangaId, imagePaths))
    }

    fun getDownloadedChapters(mangaId: String): List<Chapter> {
        return dataSource.getDownloadedChapters(mangaId)
    }

    fun getDownloadedChapterById(mangaId: String, chapterId: String): Boolean {
        return dataSource.getDownloadedChapterById(mangaId, chapterId) != null
    }
    
    fun deleteDownloadedChapter(chapter: Chapter) {
        chapter.imagesPath?.forEach { filename ->
            ImageStorage().deleteImage(filename)
        }

        dataSource.deleteDownloadedChapters(chapter.combinedId!!)
    }

    private fun mapToChapters(chaptersRaw: List<ChapterRaw>): List<Chapter> = chaptersRaw.map { raw ->
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

    private fun mapToDownloadedChapter(chapter: Chapter, mangaId: String, imagePaths: List<String>): Chapter =
        Chapter(
            combinedId = mangaId+chapter.id,
            mangaId = mangaId,
            id = chapter.id,
            title = chapter.title,
            volume = chapter.volume,
            chapter = chapter.chapter,
            scanlationGroup = chapter.scanlationGroup,
            pageNumbers = chapter.pageNumbers,
            imagesPath = imagePaths,
            read = chapter.read,
            filesDownloaded = chapter.filesDownloaded
        )
}