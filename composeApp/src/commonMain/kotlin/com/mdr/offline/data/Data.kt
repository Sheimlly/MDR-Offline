package com.mdr.offline.data

import kotlinx.serialization.Serializable

sealed class DownloadState {
    data object Idle : DownloadState()                // Nothing started
    data object Downloading : DownloadState()         // Download in progress
    data object Completed : DownloadState()           // Finished successfully
    data class Error(val message: String) : DownloadState() // Optional error state
}

sealed class ChapterFilters(val label: String) {
    data object All : ChapterFilters("All")        // All chapters
    data object Read : ChapterFilters("Read")      // Read chapters
    data object Unread : ChapterFilters("Unread")  // Unread chapters

    companion object {
        val values by lazy { listOf(All, Read, Unread) }
    }
}

@Serializable
sealed class ContentType {
    @Serializable
    data object Online: ContentType()
    @Serializable
    data object Logged: ContentType()
    @Serializable
    data object MDList: ContentType()
    @Serializable
    data object Downloaded: ContentType()
}

sealed class Order(val value: String) {
    data object Asc: Order("asc")
    data object Desc: Order("desc")
}