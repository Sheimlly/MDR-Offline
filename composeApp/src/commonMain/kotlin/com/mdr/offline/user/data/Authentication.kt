package com.mdr.offline.user.data

import com.mdr.offline.Secrets
import com.mdr.offline.user.application.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Authentication(
    private val httpClient: HttpClient,
    private val clientId: String = Secrets.CLIENT_ID,
    private val clientSecret: String = Secrets.CLIENT_SECRET
) {
    suspend fun logIn(login: String, password: String): TokenResponse {
        val tokens: TokenResponse =
            httpClient.post("https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    append("grant_type", "password")
                    append("username", login)
                    append("password", password)
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                }))
            }.body()

        return tokens
    }

    suspend fun refreshAccessToken(refreshToken: String): TokenResponse {
        val accessToken: TokenResponse =
            httpClient.post("https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                }))
            }.body()

        return accessToken
    }
}