package com.mdr.offline.user.data

import com.mdr.offline.chapters.data.ChapterRawSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MDListRaw (
    @SerialName("id")
    val id: String,
    @SerialName("attributes")
    val attributes: MDListAttributes,
    @Serializable
    val relationships: List<Relationships>
)

@Serializable
data class MDListAttributes (
    @SerialName("name")
    val name: String
)

@Serializable
data class Relationships(
    @SerialName("type")
    val type: String,
    @SerialName("id")
    val id: String
)