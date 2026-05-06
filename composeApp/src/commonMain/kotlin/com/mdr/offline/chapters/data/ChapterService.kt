package com.mdr.offline.chapters.data

import com.mdr.offline.chapters.application.Chapter
import com.mdr.offline.data.Order
import com.mdr.offline.network.NetworkService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ChapterService(
    private val httpClient: HttpClient,
    private val service: ChapterDetailsService
): NetworkService() {

    suspend fun fetchChapters(mangaId: String, offset: Int = 0, order: Order): List<ChapterRaw> {
        val response: ChapterResponse = retryRequest {
            httpClient.get("https://api.mangadex.org/chapter") {
                url {
                    parameters.append("limit", "10")
                    parameters.append("offset", offset.toString())
                    parameters.append("manga", mangaId)
                    parameters.append("translatedLanguage[]", "en")
                    parameters.append("order[chapter]", order.value)
                }
            }.body()
        }

        // I don't know how to handle it during serialization and just skip it
        var chapters = response.chapters.filter { it.pageNumbers > 0 }
        chapters = chapters.filter { it.chapter != "null"} // It should be "" instead of "null" but I don't know why it doesn't work in serialization

        chapters = chapters.map { chapter ->
            chapter.populateAdditionalData(service)
        }
        return chapters

    }

    suspend fun fetchChapterIds(mangaId: String, offset: Int = 0, chapterIds: List<String> = emptyList()): List<String> {
        val response: ChapterResponse = retryRequest {
            httpClient.get("https://api.mangadex.org/chapter") {
                url {
                    parameters.append("limit", "50")
                    parameters.append("offset", offset.toString())
                    parameters.append("manga", mangaId)
                    parameters.append("translatedLanguage[]", "en")
                    parameters.append("order[chapter]", Order.Asc.value)
                }
            }.body()
        }

        if(response.chapters.isNotEmpty()) {
            // I don't know how to handle it during serialization and just skip it
            var chapters = response.chapters.filter { it.pageNumbers > 0 }
            chapters = chapters.filter { it.chapter != "null"} // It should be "" instead of "null" but I don't know why it doesn't work in serialization

            val ids: List<String> = chapters.map { it.id }

            return fetchChapterIds(mangaId, offset+50, chapterIds.plus(ids))
        } else  {
            return chapterIds
        }

    }

    suspend fun fetchChaptersById(chapterIds: List<String>): List<ChapterRaw> {

        var chapters: List<ChapterRaw> = chapterIds.mapNotNull { chapterId ->
            val response: SingleChapterResponse = retryRequest {
                httpClient.get("https://api.mangadex.org/chapter/$chapterId") {
                    url {
                        parameters.append("translatedLanguage[]", "en")
                    }
                }.body()
            }

            if(response.chapter.pageNumbers > 0 || response.chapter.chapter != "null") response.chapter
            else null
        }

        chapters = chapters.map { chapter ->
            chapter.populateAdditionalData(service)
        }
        return chapters
    }
}

class ChapterPagesService(private val httpClient: HttpClient): NetworkService() {
    suspend fun fetchChapterImages(urls: List<String>): List<ByteArray> {
        val images: List<ByteArray> = urls.map { url ->
            httpClient.get(url).readBytes()
        }

        return images
    }

//    That should be working perfectly but server has problems
    @Serializable
    data class ChapterImagesUrlNormalResponse(
        @SerialName("baseUrl")
        val baseUrl: String,
        @SerialName("chapter")
        val chapter: ChapterImagesUrlsNormalData
    )

    @Serializable
    data class ChapterImagesUrlsNormalData(
        @SerialName("hash")
        val hash: String,
        @SerialName("dataSaver")
        val images: List<String>
    )

    private suspend fun getChapterUrlsNormal(chapterId: String): List<String> {
        val response: ChapterImagesUrlNormalResponse = retryRequest200 { httpClient.get("https://api.mangadex.org/at-home/server/$chapterId?forcePort433=true"){
            accept(ContentType.Application.Json)
        } }

        val urls: List<String> = response.chapter.images.map {image ->
            "${response.baseUrl}/data/${response.chapter.hash}/${image}"
        }

        return urls
    }

//    And that should work perfectly and it does
    @Serializable
    data class ChapterImagesUrlHighResponse(
        @SerialName("baseUrl")
        val baseUrl: String,
        @SerialName("chapter")
        val chapter: ChapterImagesUrlsHighData
    )

    @Serializable
    data class ChapterImagesUrlsHighData(
        @SerialName("hash")
        val hash: String,
        @SerialName("data")
        val images: List<String>
    )

    private suspend fun getChapterUrlsHigh(chapterId: String): List<String> {
        val response: ChapterImagesUrlHighResponse = retryRequest200 { httpClient.get("https://api.mangadex.org/at-home/server/$chapterId?forcePort433=true"){
            accept(ContentType.Application.Json)
        } }

        val urls: List<String> = response.chapter.images.map {image ->
            "${response.baseUrl}/data/${response.chapter.hash}/${image}"
        }

        return urls
    }

    suspend fun getChapterUrls(chapterId: String, highQuality: Boolean = true): List<String> {
        return when(highQuality) {
            true ->  getChapterUrlsHigh(chapterId)
            false -> getChapterUrlsNormal(chapterId)
        }
    }
}