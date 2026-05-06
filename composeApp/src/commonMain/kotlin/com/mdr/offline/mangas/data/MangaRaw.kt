package com.mdr.offline.mangas.data

import io.ktor.client.HttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

import io.ktor.client.call.body
import io.ktor.client.request.get
import org.koin.core.component.getScopeName

@Serializable(with = MangaRawSerializer::class)
data class MangaRaw(
    @SerialName("id")
    val id: String,
    @SerialName("attributes")
    val attributes: MangaAttributes,
    @SerialName("relationships")
    val relationships: List<Relationships>,
    val title: String,
    val description: String,
    val author: String,
    val coverImageUrl: String,
    val originalLanguage: String,
    val status: String,
    val year: String,
    val state: String,
    val format: String,
    val publicationDemographic: String,
    val contentRating: String,
    val genres: List<String>
)

@Serializable(with = MangaAttributesSerializer::class)
data class MangaAttributes(
    @SerialName("title")
//    val title: GetEn,
    val title: JsonObject,
    @SerialName("description")
    val description: GetEn,
    @SerialName("originalLanguage")
    val originalLanguage: String,
    @SerialName("status")
    val status: String,
    @SerialName("year")
    val year: String,
    @SerialName("state")
    val state: String,
    @SerialName("contentRating")
    val contentRating: String,
    @SerialName("publicationDemographic")
    val publicationDemographic: String?,
    @SerialName("tags")
    val tags: List<Tag>
)

@Serializable(with = TagSerializer::class)
data class Tag(
    @SerialName("attributes")
    val attributes: TagAttributes
)

@Serializable(with = TagAttributesSerializer::class)
data class TagAttributes (
    @SerialName("name")
    val name: GetEn,
    @SerialName("group")
    val group: String
)

@Serializable
data class Relationships(
    @SerialName("type")
    val type: String,
    @SerialName("id")
    val id: String
)

//@Serializable
@Serializable(with = GetEnSerializer::class)
data class GetEn(
    @SerialName("en")
    val en: String
)

class MangaDetailsService(private val httpClient: HttpClient) {
    @Serializable
    data class CoverResponse(
        @SerialName("data")
        val cover: Cover
    )

    @Serializable
    data class Cover(
        @SerialName("attributes")
        val attributes: CoverAttributes
    )

    @Serializable
    data class CoverAttributes(
        @SerialName("fileName")
        val filename: String
    )

    @Serializable
    data class AuthorResponse(
        @SerialName("data")
        val author: Author
    )

    @Serializable
    data class Author(
        @SerialName("attributes")
        val attributes: AuthorAttributes
    )

    @Serializable
    data class AuthorAttributes(
        @SerialName("name")
        val name: String
    )

    suspend fun getAuthorName(authorId: String): String {
        val authorResponse: AuthorResponse = httpClient.get("https://api.mangadex.org/author/${authorId}").body()
        return authorResponse.author.attributes.name
    }
    suspend fun getCoverUrl(coverId: String, mangaId: String): String {
        val coverResponse: CoverResponse = httpClient.get("https://api.mangadex.org/cover/${coverId}").body()
//        val fileName = coverResponse.cover.attributes.filename.substringBeforeLast(".")
//        val fileExtension = coverResponse.cover.attributes.filename.substringAfterLast(".")
        return "https://uploads.mangadex.org/covers/${mangaId}/${coverResponse.cover.attributes.filename}.512.jpg"
    }

}

object GetEnSerializer: KSerializer<GetEn> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Attributes") {
        element<String>("en")
    }

    override fun serialize(encoder: Encoder, value: GetEn) {
        val jsonObject = buildJsonObject {
            put("en", value.en)

        }
        encoder.encodeSerializableValue(JsonObject.serializer(), jsonObject)
    }

    override fun deserialize(decoder: Decoder): GetEn {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        val jsonObject = input.decodeJsonElement().jsonObject

        val en = jsonObject["en"]?.jsonPrimitive?.content ?: ""

        return GetEn(en)
    }
}

object TagAttributesSerializer: KSerializer<TagAttributes> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TagAttributes") {
        element<GetEn>("name")
        element<String>("group")
    }

    override fun serialize(encoder: Encoder, value: TagAttributes) {
        val jsonObject = buildJsonObject {
            put("name", Json.encodeToJsonElement(GetEn.serializer(), value.name))
            put("group", value.group)
        }
        encoder.encodeSerializableValue(JsonObject.serializer(), jsonObject)
    }

    override fun deserialize(decoder: Decoder): TagAttributes {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        val jsonObject = input.decodeJsonElement().jsonObject

        val name = jsonObject["name"]?.let {
            input.json.decodeFromJsonElement(GetEn.serializer(), it)
        } ?: throw SerializationException("Missing tag name")

        val group = jsonObject["group"]?.jsonPrimitive?.content ?: throw SerializationException("Missing tag group")

        return TagAttributes(name, group)
    }
}

object TagSerializer: KSerializer<Tag> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Attributes") {
        element<TagAttributes>("attributes")
    }

    override fun serialize(encoder: Encoder, value: Tag) {
        val jsonObject = buildJsonObject {
            put("attributes", Json.encodeToJsonElement(TagAttributes.serializer(), value.attributes))
        }
        encoder.encodeSerializableValue(JsonObject.serializer(), jsonObject)
    }

    override fun deserialize(decoder: Decoder): Tag {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        val jsonObject = input.decodeJsonElement().jsonObject

        val tagAttributes = jsonObject["attributes"]?.let {
            input.json.decodeFromJsonElement(TagAttributes.serializer(), it)
        } ?: throw SerializationException("Missing tag attributes")

        return Tag(tagAttributes)
    }
}

object MangaAttributesSerializer: KSerializer<MangaAttributes> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MangaAttributes") {
        element<GetEn>("title")
        element<GetEn>("description")
        element<String>("originalLanguage")
        element<String>("status")
        element<String>("year")
        element<String>("state")
        element<String?>("publicationDemographic")
        element<List<Tag>>("tags")
    }

    override fun serialize(encoder: Encoder, value: MangaAttributes) {
        val jsonObject = buildJsonObject {
            put("title", value.title)
            put("description", Json.encodeToJsonElement(GetEn.serializer(), value.description))
            put("status", value.status)
            put("status", value.year)
            put("state", value.state)
            put("publicationDemographic", value.publicationDemographic)
            put("contentRating", value.publicationDemographic)
            put("tags", Json.encodeToJsonElement(ListSerializer(Tag.serializer()), value.tags))

        }
        encoder.encodeSerializableValue(JsonObject.serializer(), jsonObject)
    }

    override fun deserialize(decoder: Decoder): MangaAttributes {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        val jsonObject = input.decodeJsonElement().jsonObject

//        val title = jsonObject["title"]?.let {
//            input.json.decodeFromJsonElement(GetEn.serializer(), it)
//        } ?: throw SerializationException("Missing title")

        val title = jsonObject["title"]?.jsonObject ?: throw SerializationException("Missing title")

        val description = jsonObject["description"]?.let {
            input.json.decodeFromJsonElement(GetEn.serializer(), it)
        } ?: throw SerializationException("Missing description")

        val originalLanguage = jsonObject["originalLanguage"]?.jsonPrimitive?.content ?: throw SerializationException("Missing original language")
        val status = jsonObject["status"]?.jsonPrimitive?.content ?: throw SerializationException("Missing status")
        val year = jsonObject["year"]?.jsonPrimitive?.content ?: throw SerializationException("Missing year")
        val state = jsonObject["state"]?.jsonPrimitive?.content ?: throw SerializationException("Missing state")
        val publicationDemographic = jsonObject["publicationDemographic"]?.jsonPrimitive?.contentOrNull
        val contentRating = jsonObject["contentRating"]?.jsonPrimitive?.content ?: throw SerializationException("Missing content rating")

        val tags = jsonObject["tags"]?.let {
            input.json.decodeFromJsonElement(ListSerializer(Tag.serializer()), it)
        } ?: throw SerializationException("Missing tags")

        return MangaAttributes(
            title = title,
            description = description,
            originalLanguage = originalLanguage,
            status = status,
            year = year,
            state = state,
            publicationDemographic = publicationDemographic,
            contentRating = contentRating,
            tags = tags
        )
    }
}

object MangaRawSerializer: KSerializer<MangaRaw> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ArticleRaw") {
        element<String>("id")
        element<MangaAttributes>("attributes")
        element<List<Relationships>>("relationships")
    }

    override fun serialize(encoder: Encoder, value: MangaRaw) {
        val jsonObject = buildJsonObject {
            put("id", value.id)
            put("attributes", Json.encodeToJsonElement(MangaAttributes.serializer(), value.attributes))
            put("relationships", Json.encodeToJsonElement(ListSerializer(Relationships.serializer()), value.relationships))
        }
        encoder.encodeSerializableValue(JsonObject.serializer(), jsonObject)
    }

    override fun deserialize(decoder: Decoder): MangaRaw {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        val jsonObject = input.decodeJsonElement().jsonObject

        val id = jsonObject["id"]?.jsonPrimitive?.content ?: throw SerializationException("Missing id")

        val mangaAttributes = jsonObject["attributes"]?.let {
            input.json.decodeFromJsonElement(MangaAttributes.serializer(), it)
        } ?: throw SerializationException("Missing manga attributes")
        val relationships = jsonObject["relationships"]?.let {
            input.json.decodeFromJsonElement(ListSerializer(Relationships.serializer()), it)
        } ?: throw SerializationException("Missing relations")

        val title = mangaAttributes.title.jsonObject.values.first().toString()
        val description = if(mangaAttributes.description.en == "") "Missing description" else mangaAttributes.description.en

        val originalLanguage = mangaAttributes.originalLanguage
        val status = mangaAttributes.status
        val year = mangaAttributes.year
        val state = mangaAttributes.state
        val publicationDemographic = mangaAttributes.publicationDemographic  ?: "Unknown"
        val contentRating = mangaAttributes.contentRating

        val format = getFormat(mangaAttributes.tags)

        val genres: List<String> = mangaAttributes.tags
            .filter {
                it.attributes.group == "genre"
            }.map {it.attributes.name.en}

        val author = ""
        val coverImageUrl = ""

        return MangaRaw(
            id = id,
            attributes = mangaAttributes,
            relationships = relationships,
            title = title.drop(1).dropLast(1),   // removing '"' from title ex. "title" -> title
            description = description,
            originalLanguage = originalLanguage,
            status = status,
            year = year,
            state = state,
            publicationDemographic = publicationDemographic,
            author = author,
            coverImageUrl = coverImageUrl,
            contentRating = contentRating,
            format = format,
            genres = genres
        )
    }
}

fun getFormat(tags: List<Tag>): String {
    var format = "Doujinshi"
    tags.forEach {
        if(it.attributes.group == "format") {
            if(it.attributes.name.en == "Long Strip" || it.attributes.name.en == "Doujinshi") format = it.attributes.name.en
        }
    }

    return format
}

suspend fun MangaRaw.populateAdditionalData(service: MangaDetailsService): MangaRaw {
    val authorId = this.relationships
        .filter{it.type == "author"}
        .map{ it.id }[0] //There could be only one instance where type is author

    val coverId = this.relationships
        .filter{it.type == "cover_art"}
        .map{ it.id }[0] //There could be only one instance where type is author

    val authorName = service.getAuthorName(authorId)
    val coverUrl = service.getCoverUrl(coverId, this.id)

    return this.copy(author = authorName, coverImageUrl = coverUrl)
}