package com.mdr.offline.user.data

import com.mdr.offline.chapters.data.ChapterRaw
import com.mdr.offline.network.NetworkService
import com.mdr.offline.user.application.MDList
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class UserService(
    private val httpClient: HttpClient
): NetworkService() {
    @Serializable
    data class MDListsResponse(
        @SerialName("result")
        val result: String,
        @SerialName("data")
        val mdLists: List<MDListRaw>
    )

    suspend fun getMDLists(token: String): List<MDList> {
        val response: MDListsResponse = retryRequest {
            httpClient.get("https://api.mangadex.org/user/list") {
                contentType(ContentType.Application.Json)
                headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.body()

        val mdLists: List<MDList> = response.mdLists.map { mdList ->
            val mangaIds: List<String> = mdList.relationships.mapNotNull { relationship ->
                if (relationship.type == "manga") relationship.id
                else null
            }
            MDList(
                id = mdList.id,
                name = mdList.attributes.name,
                mangaIds = mangaIds
            )
        }
        return mdLists
    }
}