package com.mdr.offline.ui.navigation.manga

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.mdr.offline.DownloadManager
import com.mdr.offline.ImageStorage
import com.mdr.offline.KotlinPlatform
import com.mdr.offline.data.DownloadState
import com.mdr.offline.chapters.application.Chapter
import com.mdr.offline.chapters.application.ChaptersUseCase
import com.mdr.offline.currentPlatform
import com.mdr.offline.data.ChapterFilters
import com.mdr.offline.data.ContentType
import com.mdr.offline.data.Order
import com.mdr.offline.mangas.application.Manga
import com.mdr.offline.mangas.application.MangasUseCase
import com.mdr.offline.user.application.Tokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


interface MangaComponent {
    val model: Value<Model>

    data class Model(
        val manga: Manga,
        val chapters: List<Chapter>,
        val contentType: ContentType,
        val lastReadChapter: String? = null,    // Online and Logged
        val filter: ChapterFilters,             // Downloaded, Logged and MDList
        val loading: Boolean = false,
        val error: String? = null,
        val chapterDownloadIndicators: List<ChapterDownloadIndicator> = emptyList(),
        val loadingMoreChapters: Boolean = false
    )

    val filterOrder: Value<FilterOrder>

    data class FilterOrder(
        val all: Order,
        val read: Order,
        val unread: Order,
    )

    fun onBackPressed()

    val order: MutableValue<Order>
    fun changeOrder()

    fun changeReadChapter(chapter: Chapter)

//    Online functions
    fun onChapterClicked(chapter: Chapter)

    fun downloadAll()
    fun downloadChapter(chapter: Chapter)

    fun loadMoreChapters()

    val isUserLogged: Value<Boolean>

    val downloadState: MutableState<DownloadState>
    val progress: MutableState<Float>

    data class ChapterDownloadIndicator(
        val chapterId: String,
        val downloadState: DownloadState
    )

//    Downloaded functions

    fun updateLastReadChapter(combinedId: String)

    fun continueReading()

    fun updateChapter(updatedChapter: Chapter, lastRead: Boolean)

    fun deleteChapter(chapter: Chapter)

    fun onFilterChanged(filter: ChapterFilters)

    fun syncReadChapters()
    fun syncUnreadChapters()
    fun syncAllChapters()

    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            manga: Manga,
            contentType: ContentType,
            isUserLoggedIn: Value<Boolean>,
            token: State<Tokens>,
            onChapterSelected: (initialChapter: Chapter, allChapters: List<Chapter>, format: String, contentType: ContentType, updateChapter: (Chapter, Boolean) -> Unit?) -> Unit,
            onBack: () -> Unit,
        ): MangaComponent
    }
}

class DefaultMangaComponent(
    private val componentContext: ComponentContext,
    private val mangaUseCase: MangasUseCase,
    private val chapterUseCase: ChaptersUseCase,
    private val downloadManager: DownloadManager,
    private val manga: Manga,
    private val contentType: ContentType,
    private val isUserLoggedIn: Value<Boolean>,
    private val token: State<Tokens>,
    private val onChapterSelected: (initialChapter: Chapter, allChapters: List<Chapter>, format: String, contentType: ContentType, updateChapter: (Chapter, Boolean) -> Unit?) -> Unit,
    private val onBack: () -> Unit,
) : MangaComponent, ComponentContext by componentContext {
    private val _model = MutableValue(MangaComponent.Model(manga = manga, chapters = emptyList(), contentType = contentType, filter = ChapterFilters.All, loading = true))
    override val model: Value<MangaComponent.Model> = _model

    private val _filterModel = MutableValue(MangaComponent.FilterOrder(all = Order.Asc, read = Order.Asc, unread = Order.Asc))
    override val filterOrder: Value<MangaComponent.FilterOrder> = _filterModel

    private fun <T> List<T>.get10(offset: Int): List<T> =
        this.drop(offset).take(10)

    private val readChapterIds = mutableListOf<String>()
    private val unreadChapterIds = mutableListOf<String>()

    private val _order = MutableValue<Order>(Order.Asc)
    override val order: MutableValue<Order> = _order

    override fun changeOrder() {
        when(_model.value.contentType) {
            is ContentType.Logged, ContentType.MDList -> {
                when(_model.value.filter) {
                    is ChapterFilters.All -> {
                        when(_filterModel.value.all) {
                            is Order.Asc -> {
                                _filterModel.value = _filterModel.value.copy(all = Order.Desc, read = _filterModel.value.read, unread = _filterModel.value.unread)
                                getOnlineChapters()
                            }
                            is Order.Desc -> {
                                _filterModel.value = _filterModel.value.copy(all = Order.Asc, read = _filterModel.value.read, unread = _filterModel.value.unread)
                                getOnlineChapters()
                            }
                        }
                    }
                    is ChapterFilters.Read -> {
                        when(_filterModel.value.read) {
                            is Order.Asc -> {
                                _filterModel.value = _filterModel.value.copy(all = _filterModel.value.all, read = Order.Desc, unread = _filterModel.value.unread)
                                readChapterIds.reverse()
                                getReadChapters()
                            }
                            is Order.Desc -> {
                                _filterModel.value = _filterModel.value.copy(all = _filterModel.value.all, read = Order.Asc, unread = _filterModel.value.unread)
                                readChapterIds.reverse()
                                getReadChapters()
                            }
                        }
                    }
                    is ChapterFilters.Unread -> {
                        when(_filterModel.value.unread) {
                            is Order.Asc -> {
                                _filterModel.value = _filterModel.value.copy(all = _filterModel.value.all, read = Order.Desc, unread = Order.Desc)
                                unreadChapterIds.reverse()
                                getUnreadChapters()
                            }
                            is Order.Desc -> {
                                _filterModel.value = _filterModel.value.copy(all = _filterModel.value.all, read = _filterModel.value.unread, unread = Order.Asc)
                                unreadChapterIds.reverse()
                                getUnreadChapters()
                            }
                        }
                    }
                }
            }
            is ContentType.Downloaded -> {
                when(_order.value) {
                    is Order.Asc -> {
                        _order.value = Order.Desc
                        _model.value = MangaComponent.Model(chapters = _model.value.chapters.reversed(), manga = manga, contentType = contentType, lastReadChapter = _model.value.lastReadChapter, loading = false, filter = _model.value.filter)
                    }
                    is Order.Desc -> {
                        _order.value = Order.Asc
                        _model.value = MangaComponent.Model(chapters = _model.value.chapters.reversed(), manga = manga, contentType = contentType, lastReadChapter = _model.value.lastReadChapter, loading = false, filter = _model.value.filter)
                    }
                }
            }
            is ContentType.Online -> null
        }

//        when (_order.value) {
//            is Order.Asc -> {
//                _order.value = Order.Desc
//                when(contentType) {
//                    is ContentType.Online -> getOnlineChapters()
//                    is ContentType.Logged, ContentType.MDList -> {
//                        when(_model.value.filter) {
//                            is ChapterFilters.All -> getOnlineChapters()
//                            is ChapterFilters.Read -> {
//                                readChapterIds.reverse()
//                                getReadChapters()
//                            }
//                            is ChapterFilters.Unread -> {
//                                unreadChapterIds.reverse()
//                                getUnreadChapters()
//                            }
//                        }
//                    }
//                    is ContentType.Downloaded -> _model.value = MangaComponent.Model(chapters = _model.value.chapters.reversed(), manga = manga, contentType = contentType, lastReadChapter = _model.value.lastReadChapter, loading = false, filter = _model.value.filter)
//                }
//            }
//            is Order.Desc -> {
//                _order.value = Order.Asc
//                when(contentType) {
//                    is ContentType.Online -> getOnlineChapters()
//                    is ContentType.Logged, ContentType.MDList -> {
//                        when(_model.value.filter) {
//                            is ChapterFilters.All -> getOnlineChapters()
//                            is ChapterFilters.Read -> {
//                                readChapterIds.reverse()
//                                getReadChapters()
//                            }
//                            is ChapterFilters.Unread -> {
//                                unreadChapterIds.reverse()
//                                getUnreadChapters()
//                            }
//                        }
//                    }
//                    is ContentType.Downloaded -> _model.value = MangaComponent.Model(chapters = _model.value.chapters.reversed(), manga = manga, contentType = contentType, lastReadChapter = _model.value.lastReadChapter, loading = false, filter = _model.value.filter)
//                }
//            }
//        }
    }

    private val _isUserLogged = MutableValue(isUserLoggedIn.value)
    override val isUserLogged: Value<Boolean> = _isUserLogged

    init {
        isUserLoggedIn.subscribe { loggedIn ->
            _isUserLogged.value = loggedIn

            if(loggedIn && _model.value.contentType !is ContentType.Downloaded) {
                CoroutineScope(Dispatchers.Main).launch {
                    readChapterIds.addAll(mangaUseCase.mangaReadChapters(token.value.accessToken, manga.id))

                    val chapterIds: List<String> = chapterUseCase.getChapterIds(manga.id)

                    val unreadIds = chapterIds.mapNotNull {
                        if (it in readChapterIds) null
                        else it
                    }
                    unreadChapterIds.addAll(unreadIds)
                    _model.value = _model.value.copy(lastReadChapter = unreadChapterIds[0])
                }
            }
        }

        when(contentType) {
            is ContentType.Online, ContentType.Logged, ContentType.MDList -> getOnlineChapters()
            is ContentType.Downloaded -> {
                getDownloadedChapters()

                _model.value = _model.value.copy(lastReadChapter = mangaUseCase.getLastReadChapter(manga.id))
            }
        }
    }
    override fun onChapterClicked(chapter: Chapter) {
        var chaptersToPass = _model.value.chapters
        when(contentType) {
            is ContentType.Online -> {
                onChapterSelected(
                    chapter,
                    chaptersToPass,
                    manga.format,
                    contentType,
                    {_, _ ->}
                )
            }
            is ContentType.Logged, ContentType.MDList -> {
                when(_model.value.filter) {
                    is ChapterFilters.All -> if(_filterModel.value.all is  Order.Desc) chaptersToPass = chaptersToPass.reversed()
                    is ChapterFilters.Read -> if(_filterModel.value.read is  Order.Desc) chaptersToPass = chaptersToPass.reversed()
                    is ChapterFilters.Unread -> if(_filterModel.value.unread is  Order.Desc) chaptersToPass = chaptersToPass.reversed()
                }
                onChapterSelected(
                    chapter,
                    chaptersToPass,
                    manga.format,
                    contentType,
                    {updatedChapter, lastRead -> updateChapter(updatedChapter, lastRead)}
                )
            }
            is ContentType.Downloaded -> {
                if (_order.value is Order.Desc) {
                    chaptersToPass = chaptersToPass.reversed()
                }

                onChapterSelected(
                    chapter,
                    chaptersToPass,
                    manga.format,
                    contentType,
                    {updatedChapter, lastRead -> updateChapter(updatedChapter, lastRead)}
                )
            }
        }
    }

    override fun changeReadChapter(chapter: Chapter) {
        when(contentType){
            is ContentType.Downloaded -> {
                chapterUseCase.updateIfChapterRead(chapter.combinedId!!, !chapter.read)
                _model.value = _model.value.copy(
                    chapters = _model.value.chapters.map {
                        if (it.id == chapter.id) it.copy(read = !chapter.read) else it
                    }
                )

                if(isUserLoggedIn.value && token.value.accessToken != "") {
                    CoroutineScope(Dispatchers.Default).launch {
                        if(!chapter.read) mangaUseCase.updateMangaReadChapter(token.value.accessToken, manga.id, chapter.id)
                        else mangaUseCase.updateMangaReadChapter(token.value.accessToken, manga.id, chapter.id, false)
                    }
                }
            }
            is ContentType.Online, ContentType.Logged, ContentType.MDList -> {
                if(isUserLoggedIn.value && token.value.accessToken != "") {
                    CoroutineScope(Dispatchers.Default).launch {
                        _model.value = _model.value.copy(
                            chapters = _model.value.chapters.map {
                                if (it.id == chapter.id) it.copy(read = !chapter.read) else it
                            }
                        )

                        if(!chapter.read) mangaUseCase.updateMangaReadChapter(token.value.accessToken, manga.id, chapter.id)
                        else mangaUseCase.updateMangaReadChapter(token.value.accessToken, manga.id, chapter.id, false)
                    }
                }
            }
        }
    }

    // Online functions

    override fun onBackPressed() {
        onBack()
    }

    private val _downloadState = mutableStateOf<DownloadState>(DownloadState.Idle)
    override val downloadState: MutableState<DownloadState> = _downloadState

    private val _progress = mutableStateOf(0f) // 0.0 -> 1.0
    override val progress: MutableState<Float> = _progress

    private val count = mutableStateOf<Int>(1)

    private fun downloadProgress(
        count: Int,
        total: Int,
        chapterId: String?,
        chapterDownloaded: Boolean = false
    ) {

        if(chapterId != null) {
            if(chapterId in model.value.chapterDownloadIndicators.map { it.chapterId }) {
                if(!chapterDownloaded) {
                    _model.value = _model.value.copy(
                        chapterDownloadIndicators =
                            _model.value.chapterDownloadIndicators.map {
                                if (it.chapterId == chapterId)
                                    it.copy(downloadState = DownloadState.Downloading)
                                else it
                            }
                    )
                } else {
                    _model.value = _model.value.copy(
                        chapterDownloadIndicators =
                            _model.value.chapterDownloadIndicators.map {
                                if (it.chapterId == chapterId)
                                    it.copy(downloadState = DownloadState.Completed)
                                else it
                            }
                    )
                }
            }
        }

        _progress.value = count.toFloat() / total
    }

    override fun downloadAll() {
        downloadManager.downloadAllChapters(manga)

//        CoroutineScope(Dispatchers.Default).launch {
//            if(!mangaUseCase.checkDownloadedMangaById(manga.id)) {
//                downloadManager.downloadManga(manga)
//            }
//
//            _downloadState.value = DownloadState.Downloading
//            _progress.value = 0f
//            val total = _model.value.chapters.size
//
//            downloadManager.downloadAllChapters(manga)
//
//            _model.value.chapters.forEach { chapter ->
//                _model.value = _model.value.copy(
//                    chapterDownloadIndicators =
//                        _model.value.chapterDownloadIndicators.map {
//                            if (it.chapterId == chapter.id)
//                                it.copy(downloadState = DownloadState.Downloading)
//                            else it
//                        }
//                )
//
//                while(!chapterUseCase.checkDownloadedChapterById(manga.id, chapter.id)) {
//                    delay(1000)
//                    if(chapterUseCase.checkDownloadedChapterById(manga.id, chapter.id)) {
//                        _model.value = _model.value.copy(
//                            chapterDownloadIndicators =
//                                _model.value.chapterDownloadIndicators.map {
//                                    if (it.chapterId == chapter.id)
//                                        it.copy(downloadState = DownloadState.Completed)
//                                    else it
//                                }
//                        )
//
//                        _progress.value = count.value.toFloat() / total
//                        count.value++
//
//                        if(count.value == total) _downloadState.value = DownloadState.Completed
//                    }
//                }
//            }
//        }
//        CoroutineScope(Dispatchers.Default).launch {
//            _downloadState.value = DownloadState.Downloading
//            _progress.value = 0f
//
//            try {
//                if (!mangaUseCase.checkDownloadedMangaById(manga.id)) {
//                    mangaUseCase.downloadManga(manga)
//                }
//
//                chapterUseCase.downloadAllChapters(manga.id, { count, total, chapterId, chapterDownloaded -> downloadProgress(count, total, chapterId, chapterDownloaded) })
//
//                mangaUseCase.updateDownloadedMangaDownload(manga.id, true)
//                _downloadState.value = DownloadState.Completed
//            } catch (e: Exception) {
//                println("Downloading error: ${e.message}")
//                _downloadState.value = DownloadState.Error("Download failed")
//            }
//        }
    }


    override fun downloadChapter(chapter: Chapter) {
        downloadManager.downloadChapter(manga, chapter)
//        CoroutineScope(Dispatchers.Default).launch {
//            if(!mangaUseCase.checkDownloadedMangaById(manga.id)) {
//                downloadManager.downloadManga(manga)
//            }
//
//            downloadManager.enqueueChapter(chapter, manga.id)
//
//            _model.value = _model.value.copy(
//                chapterDownloadIndicators =
//                    _model.value.chapterDownloadIndicators.map {
//                        if (it.chapterId == chapter.id)
//                            it.copy(downloadState = DownloadState.Downloading)
//                        else it
//                    }
//            )
//
//            while(!chapterUseCase.checkDownloadedChapterById(manga.id, chapter.id)) {
//                delay(1000)
//                if(chapterUseCase.checkDownloadedChapterById(manga.id, chapter.id)) {
//                    _model.value = _model.value.copy(
//                        chapterDownloadIndicators =
//                            _model.value.chapterDownloadIndicators.map {
//                                if (it.chapterId == chapter.id)
//                                    it.copy(downloadState = DownloadState.Completed)
//                                else it
//                            }
//                    )
//                }
//            }
//        }
//        CoroutineScope(Dispatchers.Default).launch {
//            val chapter = chaptersToDownload.peek()
//
//            if(chapter != null) {
//                try {
//                    if(!mangaUseCase.checkDownloadedMangaById(manga.id)) {
//                        mangaUseCase.downloadManga(manga)
//                    }
//
//                    chapterUseCase.downloadChapter(chapter, manga.id)
//
//                    _model.value = _model.value.copy(
//                        chapterDownloadIndicators =
//                            _model.value.chapterDownloadIndicators.map {
//                                if (it.chapterId == chapter.id)
//                                    it.copy(downloadState = DownloadState.Completed)
//                                else it
//                            }
//                    )
//                    chaptersToDownload.dequeue()
//                } catch (e: Exception) {
//                    println("Error: ${e.message}")
//                    _model.value = _model.value.copy(
//                        chapterDownloadIndicators =
//                            _model.value.chapterDownloadIndicators.map {
//                                if (it.chapterId == chapter.id)
//                                    it.copy(downloadState = DownloadState.Error("Download failed"))
//                                else it
//                            }
//                    )
//                }
//            }
//
//            if(chaptersToDownload.peek() != null) downloadChapter()
//        }
    }

    override fun loadMoreChapters() {
        _model.value = _model.value.copy(loadingMoreChapters = true)

        when(_model.value.filter) {
            is ChapterFilters.All -> {
                CoroutineScope(Dispatchers.Main).launch {
                    var fetchedChapters = chapterUseCase.getChapters(
                        manga.id,
                        _model.value.chapters.size,
                        _order.value
                    )

                    val updatedChapters = fetchedChapters.toMutableList()

                    fetchedChapters.mapIndexed { index, chapter ->
                        if(chapter.id in readChapterIds) updatedChapters[index] = chapter.copy(read = true)
                        else updatedChapters[index] = chapter.copy(read = false)
                    }

                    fetchedChapters = updatedChapters

                    if (fetchedChapters.isNotEmpty()) {
                        _model.value = _model.value.copy(
                            chapters = _model.value.chapters + fetchedChapters,
                            chapterDownloadIndicators = _model.value.chapterDownloadIndicators,
                            loadingMoreChapters = false
                        )

                        checkDownloadedChapters(fetchedChapters)
                    } else {
                        _model.value = _model.value.copy(
                            loadingMoreChapters = false
                        )
                    }
                }
            }
            is ChapterFilters.Read  ->  {
                CoroutineScope(Dispatchers.Main).launch {
                    val readChapters = chapterUseCase.getChaptersById(readChapterIds.get10(_model.value.chapters.size))

                    val chapters: List<Chapter> = readChapters.map{it.copy(read = true)}

                    if (readChapters.isNotEmpty()) {
                        _model.value = _model.value.copy(
                            chapters = _model.value.chapters + chapters,
                            chapterDownloadIndicators = _model.value.chapterDownloadIndicators,
                            loadingMoreChapters = false
                        )

                        checkDownloadedChapters(readChapters)
                    } else {
                        _model.value = _model.value.copy(
                            loadingMoreChapters = false
                        )
                    }
                }
            }
            is ChapterFilters.Unread -> {
                CoroutineScope(Dispatchers.Main).launch {
                    val unreadChapters = chapterUseCase.getChaptersById(unreadChapterIds.get10(_model.value.chapters.size))

                    if (unreadChapters.isNotEmpty()) {
                        _model.value = _model.value.copy(
                            chapters = _model.value.chapters + unreadChapters,
                            chapterDownloadIndicators = _model.value.chapterDownloadIndicators,
                            loadingMoreChapters = false
                        )

                        checkDownloadedChapters(unreadChapters)
                    } else {
                        _model.value = _model.value.copy(
                            loadingMoreChapters = false
                        )
                    }
                }
            }
        }
    }

    private fun checkDownloadedChapters(chapters: List<Chapter>) {
        var completed = 0

        chapters.forEach { chapter ->
            if(chapterUseCase.checkDownloadedChapterById(manga.id, chapter.id)){
                _model.value = MangaComponent.Model(
                    manga = _model.value.manga,
                    contentType = _model.value.contentType,
                    loading = true,
                    chapters = _model.value.chapters,
                    chapterDownloadIndicators = model.value.chapterDownloadIndicators + MangaComponent.ChapterDownloadIndicator(
                        chapter.id,
                        DownloadState.Completed
                    ),
                    filter = _model.value.filter
                )

                completed++
            } else {
                _model.value = MangaComponent.Model(
                    manga = _model.value.manga,
                    contentType = _model.value.contentType,
                    loading = true,
                    chapters = _model.value.chapters,
                    chapterDownloadIndicators = model.value.chapterDownloadIndicators + MangaComponent.ChapterDownloadIndicator(
                        chapter.id,
                        DownloadState.Idle
                    ),
                    filter = _model.value.filter
                )

            }
        }

        _model.value = MangaComponent.Model(
            loading = false,
            manga = _model.value.manga,
            contentType = _model.value.contentType,
            chapters = _model.value.chapters,
            chapterDownloadIndicators = model.value.chapterDownloadIndicators,
            filter = _model.value.filter
        )

        if(completed == _model.value.chapters.size) _downloadState.value = DownloadState.Completed
    }

    private fun getReadChapters() {
        CoroutineScope(Dispatchers.Main).launch {
            _model.value = MangaComponent.Model(
                manga = _model.value.manga,
                contentType = _model.value.contentType,
                chapters = _model.value.chapters,
                loading = true,
                filter = _model.value.filter
            )

            if(readChapterIds.isNotEmpty()) {
                val readChapters = chapterUseCase.getChaptersById(readChapterIds.get10(0))

                if(readChapters.isNotEmpty()) {
                    val chapters: List<Chapter> = readChapters.map{it.copy(read = true)}
                    _model.value = MangaComponent.Model(
                        manga = _model.value.manga,
                        contentType = _model.value.contentType,
                        lastReadChapter = _model.value.lastReadChapter,
                        loading = true,
                        chapters = chapters,
                        filter = _model.value.filter
                    )

                    if(mangaUseCase.checkIfWholeMangaDownloaded(manga.id)) _downloadState.value = DownloadState.Completed
                    checkDownloadedChapters(_model.value.chapters)

                } else {
                    _model.value = MangaComponent.Model(
                        manga = _model.value.manga,
                        contentType = _model.value.contentType,
                        lastReadChapter = _model.value.lastReadChapter,
                        loading = false,
                        chapters = _model.value.chapters,
                        error = "Cannot get read chapters",
                        filter = _model.value.filter
                    )
                }

            } else {
                _model.value = MangaComponent.Model(
                    manga = _model.value.manga,
                    contentType = _model.value.contentType,
                    lastReadChapter = _model.value.lastReadChapter,
                    loading = false,
                    chapters = _model.value.chapters,
                    error = "Cannot get read chapters",
                    filter = _model.value.filter
                )
            }
        }
    }
    private fun getUnreadChapters() {
        CoroutineScope(Dispatchers.Main).launch {
            _model.value = MangaComponent.Model(
                manga = _model.value.manga,
                contentType = _model.value.contentType,
                chapters = _model.value.chapters,
                loading = true,
                filter = _model.value.filter
            )

            if(unreadChapterIds.isNotEmpty()) {
                val unreadChapters = chapterUseCase.getChaptersById(unreadChapterIds.get10(0))

                if(unreadChapters.isNotEmpty()) {
                    _model.value = MangaComponent.Model(
                        manga = _model.value.manga,
                        contentType = _model.value.contentType,
                        lastReadChapter = _model.value.lastReadChapter,
                        loading = true,
                        chapters = unreadChapters,
                        filter = _model.value.filter
                    )

                    if(mangaUseCase.checkIfWholeMangaDownloaded(manga.id)) _downloadState.value = DownloadState.Completed
                    checkDownloadedChapters(_model.value.chapters)

                } else {
                    _model.value = MangaComponent.Model(
                        manga = _model.value.manga,
                        contentType = _model.value.contentType,
                        lastReadChapter = _model.value.lastReadChapter,
                        loading = false,
                        chapters = _model.value.chapters,
                        error = "Cannot get read chapters",
                        filter = _model.value.filter
                    )
                }

            } else {
                _model.value = MangaComponent.Model(
                    manga = _model.value.manga,
                    contentType = _model.value.contentType,
                    lastReadChapter = _model.value.lastReadChapter,
                    loading = false,
                    chapters = _model.value.chapters,
                    error = "Cannot get read chapters",
                    filter = _model.value.filter
                )
            }
        }
    }

    private fun getOnlineChapters() {
        CoroutineScope(Dispatchers.Default).launch {
            _model.value = MangaComponent.Model(
                manga = _model.value.manga,
                contentType = _model.value.contentType,
                chapters = _model.value.chapters,
                loading = true,
                filter = _model.value.filter
            )

            var fetchedChapters = chapterUseCase.getChapters(mangaId = manga.id, order = _filterModel.value.all)
            if(fetchedChapters.isEmpty()) {
                _model.value = MangaComponent.Model(
                    manga = _model.value.manga,
                    contentType = _model.value.contentType,
                    chapters = fetchedChapters,
                    error = "Cannot find any chapters",
                    filter = _model.value.filter
                )
            } else {
                if(isUserLoggedIn.value && token.value.accessToken != "") {
                    CoroutineScope(Dispatchers.Default).launch {
                        val chapterRead = mangaUseCase.mangaReadChapters(token.value.accessToken, manga.id)

                        if(chapterRead != emptyList<String>()) {
                            val updatedChapters = fetchedChapters.toMutableList()

                            fetchedChapters.mapIndexed { index, chapter ->
                                if(chapter.id in chapterRead) updatedChapters[index] = chapter.copy(read = true)
                                else updatedChapters[index] = chapter.copy(read = false)
                            }

                            fetchedChapters = updatedChapters
                        }

                        _model.value = MangaComponent.Model(
                            manga = _model.value.manga,
                            contentType = _model.value.contentType,
                            lastReadChapter = _model.value.lastReadChapter,
                            loading = true,
                            chapters = fetchedChapters,
                            filter = _model.value.filter
                        )

                        if(mangaUseCase.checkIfWholeMangaDownloaded(manga.id)) _downloadState.value = DownloadState.Completed
                        checkDownloadedChapters(_model.value.chapters)
                    }
                } else {
                    _model.value = MangaComponent.Model(
                        manga = _model.value.manga,
                        contentType = _model.value.contentType,
                        loading = true,
                        chapters = fetchedChapters,
                        filter = _model.value.filter
                    )

                    if(mangaUseCase.checkIfWholeMangaDownloaded(manga.id)) _downloadState.value = DownloadState.Completed
                    checkDownloadedChapters(_model.value.chapters)
                }
            }
        }
    }

//    Downloaded functions

    override fun updateLastReadChapter(combinedId: String) {
        mangaUseCase.updateLastReadChapter(manga.id, combinedId)
        _model.value = _model.value.copy(lastReadChapter = combinedId)
    }

    override fun continueReading() {
        when(contentType) {
            is ContentType.Downloaded -> {
                val chapter = _model.value.chapters.find { it.combinedId == model.value.lastReadChapter }
                if(chapter != null) onChapterClicked(chapter)
            }
            is ContentType.Logged, ContentType.MDList -> {
                val chapter = _model.value.chapters.find { it.id == model.value.lastReadChapter }
                if(chapter != null) onChapterClicked(chapter)
            }
            is ContentType.Online -> null
        }

    }

    override fun updateChapter(updatedChapter: Chapter, lastRead: Boolean) {
        val chapterPreUpdate: Chapter = _model.value.chapters.filter { chapter -> chapter.id == updatedChapter.id}[0]

        if(chapterPreUpdate.id in unreadChapterIds) {
            unreadChapterIds.remove(chapterPreUpdate.id)
            readChapterIds.add(chapterPreUpdate.id)
        }
        if(!chapterPreUpdate.read && updatedChapter.read) {
            CoroutineScope(Dispatchers.Default).launch {
                mangaUseCase.updateMangaReadChapter(token.value.accessToken, manga.id, updatedChapter.id)
            }
        }
        _model.value = _model.value.copy(
            chapters = _model.value.chapters.map { chapter ->
                if (chapter.id == updatedChapter.id) updatedChapter else chapter
            }
        )

        if(lastRead) {
            updateLastReadChapter(updatedChapter.combinedId!!)
            _model.value = _model.value.copy(lastReadChapter = updatedChapter.combinedId)
        }
    }

    override fun deleteChapter(chapter: Chapter) {
        chapterUseCase.deleteDownloadedChapter(chapter)

        getDownloadedChapters()
    }

    override fun onFilterChanged(filter: ChapterFilters) {
        _order.value = Order.Asc
        when(_model.value.contentType) {
            is ContentType.Downloaded -> _model.value = _model.value.copy(filter = filter)
            is ContentType.Logged, ContentType.MDList -> {
                _model.value = _model.value.copy(filter = filter)
                when(filter) {
                    is ChapterFilters.All -> getOnlineChapters()
                    is ChapterFilters.Read -> getReadChapters()
                    is ChapterFilters.Unread -> getUnreadChapters()
                }
            }
            is ContentType.Online -> null
        }
    }

    // Syncing chapters with mangadex
    override fun syncReadChapters() {
        if(isUserLoggedIn.value && token.value.accessToken != "") {
            CoroutineScope(Dispatchers.Default).launch {
                _model.value.chapters.forEach { chapter ->
                    if(chapter.read) mangaUseCase.updateMangaReadChapter(token.value.accessToken, manga.id, chapter.id)
                }
            }
        } 
    }

    override fun syncUnreadChapters() {
        if(isUserLoggedIn.value && token.value.accessToken != "") {
            CoroutineScope(Dispatchers.Default).launch {
                _model.value.chapters.forEach { chapter ->
                    if(!chapter.read) mangaUseCase.updateMangaReadChapter(token.value.accessToken, manga.id, chapter.id, false)
                }
            }
        }
    }

    override fun syncAllChapters() {
        if(isUserLoggedIn.value && token.value.accessToken != "") {
            CoroutineScope(Dispatchers.Default).launch {
                _model.value.chapters.forEach { chapter ->
                    if(chapter.read) mangaUseCase.updateMangaReadChapter(token.value.accessToken, manga.id, chapter.id)
                    else mangaUseCase.updateMangaReadChapter(token.value.accessToken, manga.id, chapter.id, false)
                }
            }
        }
    }

    private fun getDownloadedChapters() {
        _model.value = MangaComponent.Model(
            manga = _model.value.manga,
            contentType = _model.value.contentType,
            chapters = _model.value.chapters,
            loading = true,
            filter = _model.value.filter
        )

        var downloadedChapters = chapterUseCase.getDownloadedChapters(manga.id).sortedBy { it.chapter.toFloat() } // Chapter isn't name, it's chapter nr like 1, 23, 54, 64 etc.


        if (downloadedChapters.isEmpty()) {
            _model.value = MangaComponent.Model(
                loading = false,
                manga = _model.value.manga,
                contentType = _model.value.contentType,
                chapters = downloadedChapters,
                error = "Cannot find any chapters",
                filter = _model.value.filter,
            )
        } else {
            if(currentPlatform == KotlinPlatform.IOS) {
                downloadedChapters = downloadedChapters.mapNotNull { chapter ->
                    val images = chapter.imagesPath // It always exists

                    val allImagesExists = images!!.all { image ->
                        ImageStorage().checkIfImageExists(image)
                    }

                    if(allImagesExists) {
                        chapterUseCase.updateIfChapterFilesDownloaded(chapter.combinedId!!, true)
                        chapter
                    }
                    else null
                }
            }

            // It will be separated function later
            if(isUserLoggedIn.value && token.value.accessToken != "") {
                CoroutineScope(Dispatchers.Main).launch {
                    if(readChapterIds != emptyList<String>()) {
                        val updatedChapters = downloadedChapters.toMutableList()

                        downloadedChapters.mapIndexed { index, chapter ->
                            if(chapter.id in readChapterIds) {
                                chapterUseCase.updateIfChapterRead(chapter.combinedId!!, true)

                                updatedChapters[index] = chapter.copy(read = true)

                                val lastReadId = if (index == downloadedChapters.lastIndex)
                                    downloadedChapters.last().combinedId!!
                                else
                                    downloadedChapters[index+1].combinedId!!

                                mangaUseCase.updateLastReadChapter(manga.id, lastReadId)
                                _model.value = _model.value.copy(lastReadChapter = lastReadId)
                            }
                        }

                       downloadedChapters = updatedChapters
                    }

                    _model.value = MangaComponent.Model(
                        loading = false,
                        manga = _model.value.manga,
                        contentType = _model.value.contentType,
                        lastReadChapter = _model.value.lastReadChapter,
                        chapters = downloadedChapters,
                        filter = _model.value.filter,
                    )
                }
            } else {
                _model.value = MangaComponent.Model(
                    loading = false,
                    manga = _model.value.manga,
                    contentType = _model.value.contentType,
                    lastReadChapter = _model.value.lastReadChapter,
                    chapters = downloadedChapters,
                    filter = _model.value.filter,
                )
            }
        }
    }

    class Factory(
        private val mangaUseCase: MangasUseCase,
        private val chapterUseCase: ChaptersUseCase,
        private val downloadManager: DownloadManager
    ) : MangaComponent.Factory {
        override fun invoke(
            componentContext: ComponentContext,
            manga: Manga,
            contentType: ContentType,
            isUserLoggedIn: Value<Boolean>,
            token: State<Tokens>,
            onChapterSelected: (initialChapter: Chapter, allChapters: List<Chapter>, format: String, contentType: ContentType, updateChapter: (Chapter, Boolean) -> Unit?) -> Unit,
            onBack: () -> Unit,
        ): MangaComponent {
            return DefaultMangaComponent(
                componentContext = componentContext,
                mangaUseCase = mangaUseCase,
                chapterUseCase = chapterUseCase,
                downloadManager = downloadManager,
                manga = manga,
                contentType = contentType,
                isUserLoggedIn = isUserLoggedIn,
                token = token,
                onChapterSelected = onChapterSelected,
                onBack = onBack,
            )
        }
    }
}