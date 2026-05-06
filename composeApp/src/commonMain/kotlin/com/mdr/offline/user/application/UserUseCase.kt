package com.mdr.offline.user.application

import com.mdr.offline.user.data.TokensRepository
import com.mdr.offline.user.data.UserService

class UserUseCase(
    private val tokenRepo: TokensRepository,
    private val service: UserService
) {

    // Authentication
    suspend fun logIn(login: String, password: String): TokenResponse { return tokenRepo.authenticate(login, password) }
    fun getTokens(): Tokens? { return tokenRepo.getTokens() }
    fun setTokens(tokens: Tokens) {
        tokenRepo.clearTokens()
        tokenRepo.setTokens(tokens)
    }
    fun clearTokens() { tokenRepo.clearTokens() }
    suspend fun refreshTokens(refreshToken: String): TokenResponse { return tokenRepo.refreshTokens(refreshToken) }

    suspend fun getMDLists(token: String): List<MDList> {
        return service.getMDLists(token)
    }
}