package com.mdr.offline.mangas.data

import com.mdr.offline.db.MDROfflineDatabase
import com.mdr.offline.mangas.application.Manga

class MangasDataSource(
    private val database: MDROfflineDatabase,
) {

    fun getFetchedMangas() : List<Manga> =
        database.mDROfflineDatabaseQueries.selectFetchedMangas(::mapToManga).executeAsList()

    fun insertFetchedMangas(mangas: List<Manga>) {
        database.mDROfflineDatabaseQueries.transaction {
            mangas.forEach { manga ->
                insertFetchedMangas(manga)
            }
        }
    }

    fun clearFetchedMangas() {
        database.mDROfflineDatabaseQueries.removeFetchedMangas()
    }

    private fun insertFetchedMangas(manga: Manga) {
        database.mDROfflineDatabaseQueries.insertFetchedManga(
            id = manga.id,
            title = manga.title,
            description = manga.description,
            author = manga.author,
            coverImageUrl = manga.coverImageUrl!!,
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


    private fun mapToManga(
        id: String,
        title: String,
        description: String,
        author: String,
        coverImageUrl: String,
        originalLanguage: String,
        status: String,
        year: String,
        state: String,
        format: String,
        publicationDemographic: String,
        contentRating: String,
        genres: List<String>
    ): Manga =
        Manga(
            id = id,
            title = title,
            description = description,
            author = author,
            coverImageUrl = coverImageUrl,
            originalLanguage = originalLanguage,
            status = status,
            year = year,
            state = state,
            format = format,
            publicationDemographic = publicationDemographic,
            contentRating = contentRating,
            genres = genres,
        )


//    Downloaded Manga

    fun getDownloadedMangas() : List<Manga> =
        database.mDROfflineDatabaseQueries.selectDownloadedMangas(::mapToDownloadedManga).executeAsList()

    fun getDownloadedMangaById(mangaId: String): Manga? =
        database.mDROfflineDatabaseQueries.selectDownloadedMangaById(mangaId, ::mapToDownloadedManga).executeAsOneOrNull()

    fun insertDownloadedManga(manga: Manga) {
        database.mDROfflineDatabaseQueries.insertDownloadedManga(
            manga.id,
            manga.title,
            manga.description,
            manga.author,
            manga.coverImage!!,
            manga.originalLanguage,
            manga.status,
            manga.year,
            manga.state,
            manga.format,
            manga.publicationDemographic,
            manga.contentRating,
            manga.genres
        )
    }

    fun deleteDownloadedManga(mangaId: String) {
        database.mDROfflineDatabaseQueries.deleteDownloadedManga(mangaId)
    }

    fun checkIfWholeMangaDownloaded(mangaId: String): Boolean? =
        database.mDROfflineDatabaseQueries.selectDownloadedMangaDownload(mangaId).executeAsOneOrNull()

    fun updateDownloadedMangaDownload(mangaId: String, downloadedWholeManga: Boolean) =
        database.mDROfflineDatabaseQueries.updateDownloadedMangaDownload(downloadedWholeManga, mangaId)

    fun getLastReadChapter(mangaId: String): String? =
        database.mDROfflineDatabaseQueries.selectLastReadChapter(mangaId).executeAsOneOrNull()?.lastReadChapter

    fun updateLastReadChapter(mangaId: String, combinedId: String) =
        database.mDROfflineDatabaseQueries.updateLastReadChapter(combinedId, mangaId)


    private fun mapToDownloadedManga(
        id: String,
        title: String,
        description: String,
        author: String,
        coverImage: ByteArray,
        originalLanguage: String,
        status: String,
        year: String,
        state: String,
        format: String,
        publicationDemographic: String,
        contentRating: String,
        genres: List<String>,
        lastReadChapter: String?,
        downloadedWholeManga: Boolean?
    ): Manga =
        Manga(
            id = id,
            title = title,
            description = description,
            author = author,
            coverImage = coverImage,
            originalLanguage = originalLanguage,
            status = status,
            year = year,
            state = state,
            format = format,
            publicationDemographic = publicationDemographic,
            contentRating = contentRating,
            genres = genres,
        )
}