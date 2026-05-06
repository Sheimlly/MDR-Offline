package com.mdr.offline.mangas.data

import com.mdr.offline.mangas.application.Manga
import com.plusmobileapps.konnectivity.Konnectivity

class MangasRepository(
    private val dataSource: MangasDataSource,
    private val service: MangaService,
    private val konnectivity: Konnectivity = Konnectivity()
) {
    suspend fun getMangas(manga: String, forceFetch: Boolean): List<Manga> {
        if(forceFetch && konnectivity.isConnected) {
            dataSource.clearFetchedMangas()

            return fetchMangas(manga)
        }

        val mangasDb = dataSource.getFetchedMangas()

        return if(mangasDb.isEmpty() && konnectivity.isConnected) {
            fetchMangas(manga)
        } else {
            mangasDb
        }
    }

    private suspend fun fetchMangas(manga: String): List<Manga> {
        val fetchedMangas = service.fetchMangas(manga)
        dataSource.insertFetchedMangas(fetchedMangas)
        return fetchedMangas
    }

    fun checkDownloadedMangaById(mangaId: String): Boolean {
        return dataSource.getDownloadedMangaById(mangaId) != null
    }

    fun updateDownloadedMangaDownload(mangaId: String, downloaded: Boolean) {
        dataSource.updateDownloadedMangaDownload(mangaId, downloaded)
    }

    fun checkIfWholeMangaDownloaded(mangaId: String): Boolean {
        val manga = dataSource.checkIfWholeMangaDownloaded(mangaId)
        if(manga == null) {
            return manga == true
        }

        return false
    }

    suspend fun downloadManga(manga: Manga) {
        val downloadManga = mapToDownloadedManga(manga)
        dataSource.insertDownloadedManga(downloadManga)
    }

    fun getDownloadedMangas(): List<Manga> {
        return dataSource.getDownloadedMangas()
    }

    fun deleteDownloadedManga(mangaId: String) {
        dataSource.deleteDownloadedManga(mangaId)
    }

    private suspend fun mapToDownloadedManga(manga: Manga): Manga =
        Manga(
            id = manga.id,
            title = manga.title,
            description = manga.description,
            author = manga.author,
            coverImage = service.getCoverImage(manga.coverImageUrl!!),
            originalLanguage = manga.originalLanguage,
            status = manga.status,
            year = manga.year,
            state = manga.state,
            format = manga.format,
            publicationDemographic = manga.publicationDemographic,
            contentRating = manga.contentRating,
            genres = manga.genres
        )
}