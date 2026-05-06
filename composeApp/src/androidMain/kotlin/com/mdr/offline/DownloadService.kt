package com.mdr.offline

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mdr.offline.chapters.application.Chapter
import com.mdr.offline.chapters.data.ChapterRaw
import com.mdr.offline.chapters.data.ChapterResponse
import com.mdr.offline.data.Order
import com.mdr.offline.db.MDROfflineDatabase
import com.mdr.offline.mangas.application.Manga
import com.mdr.offline.network.NetworkService
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.readBytes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject
import androidx.core.content.edit

class DownloadService : Service() {

    private var scope: CoroutineScope? = null
    private lateinit var httpClient: HttpClient
    private val database: MDROfflineDatabase by inject()

    private var totalChapters = 0
    private var progress = 0
    private val channelId = "manga_downloads"
    private val notificationId = 42069

    private var currentMangaTitle = ""

    companion object {
        const val ACTION_CANCEL_DOWNLOAD = "com.mdr.offline.CANCEL_DOWNLOAD"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        httpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = false
                    isLenient = true
                })
            }
        }

        createNotificationChannel()
        Log.d("DownloadService", "Foreground service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Try to recover previous state if intent is null
        val prefs = getSharedPreferences("download_state", Context.MODE_PRIVATE)

        val mangaJson = intent?.getStringExtra("mangaJson")
            ?: prefs.getString("mangaJson", null)
        val downloadType = intent?.getStringExtra("downloadType")
            ?: prefs.getString("downloadType", null)
        val chapterJson = intent?.getStringExtra("chapterJson")
            ?: prefs.getString("chapterJson", null)

        if (mangaJson == null || downloadType == null) {
            Log.w("DownloadService", "No manga info, waiting for new task")
            stopSelf()
            return START_NOT_STICKY
        }

        val manga = Json.decodeFromString<Manga>(mangaJson)
        currentMangaTitle = manga.title

        // Start as foreground
        val notification = createNotification("Preparing ${manga.title}...")
        startForeground(notificationId, notification)

        if (intent?.action == ACTION_CANCEL_DOWNLOAD) {
            cancelDownload()
            return START_NOT_STICKY
        }

        Log.d("DownloadService", "${prefs.getBoolean("cancelled", false)}")

        if (prefs.getBoolean("cancelled", false)) {
            Log.d("DownloadService", "Download was cancelled earlier — not resuming.")
            prefs.edit() { clear() }
            stopSelf()
            return START_NOT_STICKY
        } else {
            // Save state so we can resume if process restarts
            prefs.edit {
                putString("mangaJson", mangaJson)
                putString("chapterJson", chapterJson)
                putString("downloadType", downloadType)
                putBoolean("cancelled", false)
            }

            // Start coroutine only once
            scope?.launch {
                try {
                    if(chapterJson != null) handleDownload(manga, downloadType, Json.decodeFromString<Chapter>(chapterJson))
                    else handleDownload(manga, downloadType)
                } catch (e: Exception) {
                    Log.e("DownloadService", "Download failed", e)
                } finally {
                    val prefs2 = getSharedPreferences("download_state", Context.MODE_PRIVATE)
                    val cancelled = prefs2.getBoolean("cancelled", false)
                    prefs2.edit { clear() }

                    if (!cancelled) {
                        Log.d("DownloadService", "Completed successfully — showing done notification")
                        stopForeground(false)
                        showCompletionNotification()
                        stopSelf()
                    } else {
                        Log.d("DownloadService", "Cancelled before completion — showing cancelled notification")
                        stopForeground(false)
                        showCancelledNotification()
                        stopSelf()
                    }

                }
            }

            return START_STICKY
        }
    }


    override fun onDestroy() {
        scope?.cancel()
        scope = null
        super.onDestroy()
        Log.d("DownloadService", "Foreground service destroyed")
    }

    private fun cancelDownload() {
        // Stop any running coroutines
        scope?.cancel()
        scope = null

        val prefs = getSharedPreferences("download_state", Context.MODE_PRIVATE)
        prefs.edit {
            clear()
            putBoolean("cancelled", true)
        }

        // Cancel foreground service and keep the ongoing notification
        stopForeground(true)

        Log.d("DownloadService", "Download cancelled")

        // Stop the service
        stopSelf()
    }

    // -------------------- Notifications --------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Manga Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows manga download progress"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
        }

        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Preparing download")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, 0, true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun updateNotificationProgress(current: Int, total: Int) {
        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
        }

        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progressPercent = if (total > 0) (current * 100 / total) else 0
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Downloading $currentMangaTitle")
            .setContentText("Chapter $current of $total")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progressPercent, false)
            .setOngoing(current < total)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }

    private fun showCompletionNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val doneNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Download complete")
            .setContentText("$currentMangaTitle: $progress / $totalChapters chapters")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(false)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(notificationId, doneNotification)
    }

    private fun showCancelledNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val doneNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Download cancelled")
            .setContentText(currentMangaTitle)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(false)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(notificationId, doneNotification)
    }

    // -------------------- Download Logic --------------------

    private suspend fun handleDownload(manga: Manga, downloadType: String, chapter: Chapter? = null) {
        val prefs = getSharedPreferences("download_state", Context.MODE_PRIVATE)
        if (prefs.getBoolean("cancelled", false)) return

        // Insert manga if missing
        val existing = database.mDROfflineDatabaseQueries
            .selectDownloadedMangaById(manga.id)
            .executeAsOneOrNull()

        if (existing == null) {
            database.mDROfflineDatabaseQueries.insertDownloadedManga(
                manga.id,
                manga.title,
                manga.description,
                manga.author,
                httpClient.get(manga.coverImageUrl!!).readBytes(),
                manga.originalLanguage,
                manga.status,
                manga.year,
                manga.state,
                manga.format,
                manga.publicationDemographic,
                manga.contentRating,
                manga.genres
            )
        }

        if (downloadType == "All") downloadAllChapters(manga)
        if (downloadType == "Single" && chapter != null) downloadChapter(chapter, manga.id)

        stopForeground(true)
        stopSelf()
    }

    private suspend fun downloadAllChapters(manga: Manga) {
        val chaptersRaw = getAllChapters(manga.id, 0, Order.Asc)
        val chapters = mapChapters(chaptersRaw)
        totalChapters = chapters.size
        progress = 0

        updateNotificationProgress(progress, totalChapters)

        chapters.forEach { chapter ->
            if (getSharedPreferences("download_state", Context.MODE_PRIVATE).getBoolean("cancelled", false)) return
            if(scope?.isActive == false) return

            try {
                if (database.mDROfflineDatabaseQueries
                        .selectDownloadedChapterById("${manga.id}${chapter.id}")
                        .executeAsOneOrNull() == null
                ) {
                    downloadChapter(chapter, manga.id)
                }
                progress++
                updateNotificationProgress(progress, totalChapters)
            } catch (e: Exception) {
                Log.e("DownloadService", "Error downloading ${chapter.chapter}", e)
                progress++
                updateNotificationProgress(progress, totalChapters)
            }
        }
    }

    private suspend fun downloadChapter(chapter: Chapter, mangaId: String) {
        val urls = getChapterUrls(chapter.id)
        val paths = mutableListOf<String>()

        urls.forEachIndexed { index, url ->
            val filename = "${chapter.id}_$index.png"
            ImageStorage().saveImage(url, filename)
            paths += filename
        }

        database.mDROfflineDatabaseQueries.insertDownloadedChapter(
            "$mangaId${chapter.id}",
            mangaId,
            chapter.id,
            chapter.title,
            chapter.volume,
            chapter.chapter,
            chapter.scanlationGroup,
            chapter.pageNumbers.toLong(),
            paths
        )
    }

    // -------------------- Networking --------------------

    @Serializable
    data class ChapterImagesUrlResponse(
        @SerialName("baseUrl") val baseUrl: String,
        @SerialName("chapter") val chapter: ChapterImagesUrlData
    )

    @Serializable
    data class ChapterImagesUrlData(
        @SerialName("hash") val hash: String,
        @SerialName("data") val images: List<String>
    )

    private suspend fun getChapterUrls(chapterId: String): List<String> {
        try {
            val response: ChapterImagesUrlResponse = NetworkService().retryRequest200 {
                httpClient.get("https://api.mangadex.org/at-home/server/$chapterId") {
                    accept(ContentType.Application.Json)
                }
            }
            return response.chapter.images.map {
                "${response.baseUrl}/data/${response.chapter.hash}/$it"
            }
        }
        catch (e: Exception) {
            Log.e("DownloadService", "Error getting chapter urls for $chapterId", e)
            return emptyList()
        }
    }

    private suspend fun fetchChapters(mangaId: String, offset: Int, order: Order): List<ChapterRaw> {
        val response: ChapterResponse = NetworkService().retryRequest {
            httpClient.get("https://api.mangadex.org/chapter") {
                url {
                    parameters.append("limit", "100")
                    parameters.append("offset", offset.toString())
                    parameters.append("manga", mangaId)
                    parameters.append("translatedLanguage[]", "en")
                    parameters.append("order[chapter]", order.value)
                }
            }.body()
        }
        return response.chapters.filter { it.pageNumbers > 0 && it.chapter != "null" }
    }

    private suspend fun getAllChapters(mangaId: String, offset: Int, order: Order): List<ChapterRaw> {
        val all = mutableListOf<ChapterRaw>()
        var currentOffset = 0
        while (true) {
            val batch = fetchChapters(mangaId, currentOffset, order)
            if (batch.isEmpty()) break
            all += batch
            currentOffset += 100
        }
        return all
    }

    private fun mapChapters(chaptersRaw: List<ChapterRaw>) = chaptersRaw.map {
        Chapter(
            id = it.id,
            title = it.title,
            volume = it.volume,
            chapter = it.chapter,
            scanlationGroup = it.scanlationGroup,
            pageNumbers = it.pageNumbers,
            pages = it.pages,
            read = false,
            filesDownloaded = false
        )
    }
}