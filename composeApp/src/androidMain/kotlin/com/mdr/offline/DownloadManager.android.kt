package com.mdr.offline
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mdr.offline.chapters.application.Chapter
import com.mdr.offline.db.MDROfflineDatabase
import com.mdr.offline.mangas.application.Manga
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual class DownloadManager actual constructor(
    private val database: MDROfflineDatabase
) {

    private val context: Context = applicationContext

    actual fun downloadChapter(manga: Manga, chapter: Chapter) {
        startDownloadService(manga, "Single", chapter)
    }

    actual fun downloadAllChapters(manga: Manga) {
        startDownloadService(manga, "All")
    }

    private fun startDownloadService(manga: Manga, downloadType: String, chapter: Chapter? = null) {
        val intent = Intent(context, DownloadService::class.java).apply {
            putExtra("mangaJson", Json.encodeToString(manga))
            putExtra("downloadType", downloadType)
            putExtra("chapterJson", Json.encodeToString(chapter))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}