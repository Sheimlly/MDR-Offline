package com.mdr.offline.chapters.data

import com.mdr.offline.mangas.data.MangaAttributes
import com.mdr.offline.mangas.data.MangaDetailsService
import com.mdr.offline.mangas.data.MangaRaw
import com.mdr.offline.mangas.data.Relationships
import com.mdr.offline.mangas.data.Tag
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.util.valuesOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable(with = ChapterRawSerializer::class)
data class ChapterRaw (
    @SerialName("id")
    val id: String,
    @SerialName("attributes")
    val attributes: ChapterAttributes,
    @SerialName("relationships")
    val relationships: List<ChapterRelationships>,
    val title: String? = null,
    val volume: String? = null,
    val chapter: String,
    val pageNumbers: Int,
    val scanlationGroup: String,
    val pages: List<String>
)

@Serializable(with = ChapterAttributesRawSerializer::class)
//@Serializable
data class ChapterAttributes (
    @SerialName("title")
    val title: String? = null,
    @SerialName("volume")
    val volume: String? = null,
    @SerialName("chapter")
    val chapter: String,
    @SerialName("pages")
    val pages: Int,
)

@Serializable
data class ChapterRelationships (
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: String
)

class ChapterDetailsService(val httpClient: HttpClient) {
    @Serializable
    data class ScanlationGroupResponse(
        @SerialName("data")
        val scanlationGroup: ScanlationGroup
    )

    @Serializable
    data class ScanlationGroup(
        @SerialName("attributes")
        val attributes: ScanlationGroupAttributes
    )

    @Serializable
    data class ScanlationGroupAttributes(
        @SerialName("name")
        val name: String
    )

    suspend fun getScanlationGroupName(scanlationGroupId: String): String {
        val scanlationGroupResponse: ScanlationGroupResponse = httpClient.get("https://api.mangadex.org/group/${scanlationGroupId}").body()
        return scanlationGroupResponse.scanlationGroup.attributes.name
    }
}

object ChapterAttributesRawSerializer : KSerializer<ChapterAttributes> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ChapterAttributes") {
        element<String>("title")
        element<String>("volume")
        element<String>("chapter")
        element<Int>("pages")
    }

    override fun serialize(encoder: Encoder, value: ChapterAttributes) {
        val jsonObject = buildJsonObject {
            put("title", value.title)
            put("volume", value.volume)
            put("chapter", value.chapter)
            put("pages", value.pages)
        }
    }

    override fun deserialize(decoder: Decoder): ChapterAttributes {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        val jsonObject = input.decodeJsonElement().jsonObject

        val title = jsonObject["title"]?.jsonPrimitive?.contentOrNull
        val volume = jsonObject["volume"]?.jsonPrimitive?.contentOrNull
//        val chapter = jsonObject["chapter"]?.jsonPrimitive?.content ?: throw SerializationException("Missing chapter")

        val chapter = jsonObject["chapter"]?.jsonPrimitive?.content ?: "" // It don't work, and I don't know why
        val pages = jsonObject["pages"]?.jsonPrimitive?.content?.toInt() ?: 0

        return ChapterAttributes(
            title = title,
            volume = volume,
            chapter = chapter,
            pages = pages
        )
    }
}

object ChapterRawSerializer : KSerializer<ChapterRaw> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ChapterRaw") {
        element<String>("id")
        element<ChapterAttributes>("attributes")
        element<List<ChapterRelationships>>("relationships")
    }

    override fun serialize(encoder: Encoder, value: ChapterRaw) {
        val jsonObject = buildJsonObject {
            put("id", value.id)
            put("attributes", Json.encodeToJsonElement(ChapterAttributes.serializer(), value.attributes))
            put("relationships", Json.encodeToJsonElement(ListSerializer(ChapterRelationships.serializer()), value.relationships))
        }
        encoder.encodeSerializableValue(JsonObject.serializer(), jsonObject)
    }

    override fun deserialize(decoder: Decoder): ChapterRaw {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        val jsonObject = input.decodeJsonElement().jsonObject

        val id = jsonObject["id"]?.jsonPrimitive?.content ?: throw SerializationException("Missing chapter id")
        val chapterAttributes = jsonObject["attributes"]?.let {
            input.json.decodeFromJsonElement(ChapterAttributes.serializer(), it)
        } ?: throw SerializationException("Missing attributes")

        val title = chapterAttributes.title
        val volume = chapterAttributes.volume
        val chapter = chapterAttributes.chapter
        val pageNumbers = chapterAttributes.pages

        val relationships = jsonObject["relationships"]?.let {
            input.json.decodeFromJsonElement(ListSerializer(ChapterRelationships.serializer()), it)
        } ?: throw SerializationException("Missing relations")


        return ChapterRaw(
            id = id,
            attributes = chapterAttributes,
            relationships = relationships,
            title = title,
            volume = volume,
            chapter = chapter,
            pageNumbers = pageNumbers,
            scanlationGroup = "",
            pages = emptyList()
        )
    }
}

suspend fun ChapterRaw.populateAdditionalData(service: ChapterDetailsService): ChapterRaw {
    val scanlationGroupId = this.relationships
        .filter{it.type == "scanlation_group"}
        .map{ it.id }[0] //There could be only one instance where type is author

    val scanlationGroupName = service.getScanlationGroupName(scanlationGroupId)

    return this.copy(scanlationGroup = scanlationGroupName)
}