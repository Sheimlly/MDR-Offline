package com.mdr.offline.user.application

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("error_description")
    val errorMessage: String? = null
)

data class Tokens(
    val accessToken: String,
    val refreshToken: String,
)