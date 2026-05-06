package com.mdr.offline.user.application

import kotlinx.serialization.Serializable

@Serializable
data class MDList(
    val id: String,
    val name: String,
    val mangaIds: List<String>
)