package com.mdr.offline.user.data

import com.mdr.offline.db.MDROfflineDatabase
import com.mdr.offline.user.application.Tokens

class TokensDataSource(
    private val database: MDROfflineDatabase
) {
    fun getTokens(): Tokens? =
        database.mDROfflineDatabaseQueries.selectTokens(::mapToTokens).executeAsOneOrNull()

    fun insertTokens(tokens: Tokens) {
        database.mDROfflineDatabaseQueries.insertTokens(
            tokens.accessToken,
            tokens.refreshToken
        )
    }

    fun clearTokens() {
        database.mDROfflineDatabaseQueries.removeTokens()
    }

    private fun mapToTokens(
        accessToken: String,
        refreshToken: String
    ): Tokens =
        Tokens(
            accessToken,
            refreshToken
    )
}