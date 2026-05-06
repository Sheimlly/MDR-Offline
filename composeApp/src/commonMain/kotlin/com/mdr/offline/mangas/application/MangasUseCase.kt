package com.mdr.offline.mangas.application

import com.mdr.offline.mangas.data.MangaRaw
import com.mdr.offline.mangas.data.MangaService
import com.mdr.offline.mangas.data.MangasDataSource
import com.mdr.offline.mangas.data.MangasRepository

class MangasUseCase(
    private val repo: MangasRepository,
    private val dataSource: MangasDataSource,
    private val service: MangaService
) {
    suspend fun getMangas(manga: String, forceFetch: Boolean): List<Manga> {
        return repo.getMangas(manga, forceFetch)
//        return mapMangas(articlesRaw)
    }

    suspend fun getMDListMangas(mangaIds: List<String>): List<Manga> {
        return service.getMDListMangas(mangaIds)
    }

    fun checkDownloadedMangaById(mangaId: String): Boolean {
        return repo.checkDownloadedMangaById(mangaId)
    }

    fun checkIfWholeMangaDownloaded(mangaId: String): Boolean {
        return repo.checkIfWholeMangaDownloaded(mangaId)
    }

    fun updateDownloadedMangaDownload(mangaId: String, downloaded: Boolean) {
        repo.updateDownloadedMangaDownload(mangaId, downloaded)
    }

    suspend fun downloadManga(manga: Manga) {
        repo.downloadManga(manga)
    }

    fun getDownloadedMangas(): List<Manga> {
        return repo.getDownloadedMangas()
    }

    fun deleteDownloadedManga(mangaId: String) {
        repo.deleteDownloadedManga(mangaId)
    }

    fun getLastReadChapter(mangaId: String): String? {
        return dataSource.getLastReadChapter(mangaId)
    }

    fun updateLastReadChapter(mangaId: String, combinedId: String) {
        dataSource.updateLastReadChapter(mangaId, combinedId)
    }

    suspend fun mangaReadChapters(token: String, mangaId: String): List<String> {
        return service.mangaReadChapters(token, mangaId)
    }

    suspend fun updateMangaReadChapter(token: String, mangaId: String, chapterId: String, read: Boolean = true) {
        service.updateMangaReadChapter(token, mangaId, chapterId, read)
    }

    private fun mapMangas(mangasRaw: List<MangaRaw>): List<Manga> = mangasRaw.map { raw ->
        Manga(
            id = raw.id,
            title = raw.title,
            description = raw.description,
            author = raw.author,
            coverImageUrl = raw.coverImageUrl,
            originalLanguage = raw.originalLanguage,
            status = raw.status,
            year = raw.year,
            state = raw.state,
            format = raw.format,
            publicationDemographic = raw.publicationDemographic,
            contentRating = raw.contentRating,
            genres = raw.genres,
        )
    }
}