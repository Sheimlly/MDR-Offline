package com.mdr.offline.ui.navigation.manga.chapter

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.mdr.offline.chapters.application.Chapter
import com.mdr.offline.chapters.application.ChaptersUseCase
import com.mdr.offline.data.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface ChapterComponent {
    val model: Value<Model>

    data class Model(
        val chapter: Chapter,
        val pages: List<String>,
        val contentType: ContentType,
        val loading: Boolean = false,
        val error: String? = null
    )

    val format: String

    fun onBackPressed()

    val currentPage: MutableState<Int>
    fun onPageChange(page: Int)



    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            chapter: Chapter,
            format: String,
            contentType: ContentType,
            isUserLoggedIn: Value<Boolean>,
            updateChapter: (Chapter, Boolean) -> Unit?,
            onBack: () -> Unit,
        ): ChapterComponent
    }
}

class DefaultChapterComponent(
    private val componentContext: ComponentContext,
    private val chapterUseCase: ChaptersUseCase,
    private val chapter: Chapter,
    override val format: String,
    private val contentType: ContentType,
    private val isUserLoggedIn: Value<Boolean>,
    private val updateChapter: (Chapter, Boolean) -> Unit?,
    private val onBack: () -> Unit,
) : ChapterComponent, ComponentContext by componentContext {

    private val _model = MutableValue(
        ChapterComponent.Model(
            chapter = chapter,
            pages = emptyList(),
            contentType = contentType
        )
    )
    override val model: Value<ChapterComponent.Model> = _model

    init {
        if(contentType !is ContentType.Downloaded) getPages()
//        when(contentType) {
//            is ContentType.Online, ContentType.Logged, ContentType.MDList -> getPages()
//            is ContentType.Downloaded -> null
//        }
    }

    override fun onBackPressed() {
        onBack()
    }

    private val _currentPage = mutableStateOf<Int>(1)
    override val currentPage: MutableState<Int> get() = _currentPage

    override fun onPageChange(page: Int) {
        _currentPage.value = page

        when(contentType) {
            is ContentType.Downloaded -> {
                var read: Boolean = false

                if(_model.value.chapter.read) {
                    read = true
                } else if(page == chapter.pageNumbers) {
                    read = true
                    chapterUseCase.updateIfChapterRead(chapter.combinedId!!, page == chapter.pageNumbers)
                }


                _model.value = ChapterComponent.Model(
                    chapter = Chapter(
                        combinedId = _model.value.chapter.combinedId!!,
                        mangaId = _model.value.chapter.mangaId!!,
                        id = _model.value.chapter.id,
                        title = _model.value.chapter.title,
                        volume = _model.value.chapter.volume,
                        chapter = _model.value.chapter.chapter,
                        scanlationGroup = _model.value.chapter.scanlationGroup,
                        pages = _model.value.chapter.pages,
                        pageNumbers = _model.value.chapter.pageNumbers,
                        imagesPath = _model.value.chapter.imagesPath!!,
                        lastReadPage = page,
                        read = read,
                        filesDownloaded = _model.value.chapter.filesDownloaded // Should be always true here
                    ),
                    contentType = _model.value.contentType,
                    pages = _model.value.pages,
                    loading = _model.value.loading
                )

                updateChapter(_model.value.chapter, false)

                chapterUseCase.updateLastReadPage(_model.value.chapter.combinedId!!, page)
            }
            is ContentType.Logged, ContentType.MDList -> {
                var read: Boolean = false

                if(_model.value.chapter.read) {
                    read = true
                } else if(page == chapter.pageNumbers) {
                    read = true
                }

                _model.value = ChapterComponent.Model(
                    chapter = Chapter(
                        id = _model.value.chapter.id,
                        title = _model.value.chapter.title,
                        volume = _model.value.chapter.volume,
                        chapter = _model.value.chapter.chapter,
                        scanlationGroup = _model.value.chapter.scanlationGroup,
                        pages = _model.value.chapter.pages,
                        pageNumbers = _model.value.chapter.pageNumbers,
                        read = read,
                        filesDownloaded = _model.value.chapter.filesDownloaded
                    ),
                    contentType = _model.value.contentType,
                    pages = _model.value.pages,
                    loading = _model.value.loading
                )

                updateChapter(_model.value.chapter, false)
            }
            is ContentType.Online -> null
        }
    }

    private fun getPages(){
        CoroutineScope(Dispatchers.Main).launch{
            _model.value = ChapterComponent.Model(
                chapter = _model.value.chapter,
                pages = _model.value.pages,
                contentType = _model.value.contentType,
                loading = true
            )

            val fetchedPages = chapterUseCase.getChapterUrls(chapter.id, true)
            
            if(fetchedPages.isEmpty()) {
                _model.value = ChapterComponent.Model(
                    chapter = _model.value.chapter,
                    pages = fetchedPages,
                    contentType = _model.value.contentType,
                    error = "Cannot load pages"
                )
            } else {
                _model.value =
                    ChapterComponent.Model(chapter = _model.value.chapter, pages = fetchedPages, contentType = _model.value.contentType)
            }
        }
    }

    class Factory(
        private val chapterUseCase: ChaptersUseCase
    ) : ChapterComponent.Factory {
        override fun invoke(
            componentContext: ComponentContext,
            chapter: Chapter,
            format: String,
            contentType: ContentType,
            isUserLoggedIn: Value<Boolean>,
            updateChapter: (Chapter, Boolean) -> Unit?,
            onBack: () -> Unit,
        ): ChapterComponent {
            return DefaultChapterComponent(
                componentContext = componentContext,
                chapterUseCase = chapterUseCase,
                chapter = chapter,
                format = format,
                contentType = contentType,
                isUserLoggedIn = isUserLoggedIn,
                updateChapter = updateChapter,
                onBack = onBack
            )
        }
    }
}