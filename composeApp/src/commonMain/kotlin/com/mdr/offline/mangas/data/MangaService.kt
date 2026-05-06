package com.mdr.offline.mangas.data

import com.mdr.offline.network.NetworkService
import com.mdr.offline.mangas.application.Manga
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readBytes
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class MangaService(
    private val httpClient: HttpClient,
    private val service: MangaDetailsService
): NetworkService() {
    @Serializable
    data class TotalChapters(
        @SerialName("total")
        val total: Int,
    )
    private suspend fun mangaTotalChapters(mangaId: String): Int {
        val response: TotalChapters = retryRequest { httpClient.get("https://api.mangadex.org/chapter?manga=${mangaId}&translatedLanguage%5B%5D=en&order%5Bchapter%5D=asc").body() }
        return response.total
    }

    suspend fun fetchMangas(manga: String): List<Manga> {

        val defaultUrl = "https://api.mangadex.org/manga?limit=12&includedTagsMode=AND&excludedTagsMode=OR&publicationDemographic%5B%5D=none&contentRating%5B%5D=safe&order%5BlatestUploadedChapter%5D=desc"
        val url = if(manga != "") "https://api.mangadex.org/manga?title=${manga}" else defaultUrl

        val response: MangaResponse = retryRequest{ httpClient.get(url).body() }

        var mangasRaw = response.mangas.filter { it.title != ""}
        mangasRaw = mangasRaw.filter { mangaTotalChapters(it.id) > 0}

        // Gets cover image url and author
        mangasRaw = mangasRaw.map { manga ->
            manga.populateAdditionalData(service)
        }

        return mapMangas(mangasRaw)
    }

    suspend fun getMDListMangas(mangaIds: List<String>): List<Manga> {
        println("MDList mangaIds: $mangaIds")

        val response: MangaResponse = retryRequest {
            httpClient.get("https://api.mangadex.org/manga"){
                mangaIds.forEach { id ->
                    parameter("ids[]", id)
                }
            }.body()
        }

        val mangasRaw = response.mangas.map { manga ->
            manga.populateAdditionalData(service)
        }

        return mapMangas(mangasRaw)
    }

    suspend fun getCoverImage(coverImageUrl: String): ByteArray {
        return httpClient.get(coverImageUrl).readBytes()
    }

    suspend fun mangaReadChapters(token: String, mangaId: String): List<String> {
        val response: MangaReadChapters.OK_200 = retryRequest {
            httpClient.get("https://api.mangadex.org/manga/$mangaId/read") {
                headers.append(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
            }
        }.body()

        return response.data.toList()
    }

    suspend fun updateMangaReadChapter(token: String, mangaId: String, chapterId: String, read: Boolean = true) {
        val body = if (read) {
            MangaReadUpdateRequest(
                chapterIdsRead = listOf(chapterId),
                chapterIdsUnread = emptyList()
            )
        } else {
            MangaReadUpdateRequest(
                chapterIdsRead = emptyList(),
                chapterIdsUnread = listOf(chapterId)
            )
        }

        println("Post body: $body")

        val status = retryRequest {
            client.post("https://api.mangadex.org/manga/$mangaId/read") {
                contentType(ContentType.Application.Json)
                headers.append(HttpHeaders.Authorization, "Bearer $token")
//                parameter("updateHistory", true)
                setBody(body)
            }
        }.status

        println("Post status: $status")
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