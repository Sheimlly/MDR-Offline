package com.mdr.offline.user.data

import com.mdr.offline.user.application.TokenResponse
import com.mdr.offline.user.application.Tokens
import com.plusmobileapps.konnectivity.Konnectivity

class TokensRepository(
    private val dataSource: TokensDataSource,
    private val service: Authentication,
    private val konnectivity: Konnectivity = Konnectivity()
) {
    suspend fun authenticate(login: String, password: String): TokenResponse {
        if(!konnectivity.isConnected) {
            return TokenResponse(
                accessToken = null,
                refreshToken = null,
                errorMessage = "No internet connection"
            )
        }

        return service.logIn(login, password)
    }

    fun getTokens(): Tokens? {
        return dataSource.getTokens()
    }

    fun setTokens(tokens: Tokens) {
        dataSource.insertTokens(tokens)
    }

    fun clearTokens() {
        dataSource.clearTokens()
    }

    suspend fun refreshTokens(refreshToken: String): TokenResponse { return service.refreshAccessToken(refreshToken) }
}