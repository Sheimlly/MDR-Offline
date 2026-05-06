package com.mdr.offline.ui.navigation.manga

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.mdr.offline.chapters.application.ChaptersUseCase
import com.mdr.offline.data.ContentType
import com.mdr.offline.mangas.application.Manga
import com.mdr.offline.mangas.application.MangasUseCase
import com.mdr.offline.user.application.MDList
import com.mdr.offline.user.application.UserUseCase
import io.ktor.util.reflect.Type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface MangaListComponent {
    val model: Value<Model>

    data class Model(
        val mangas: List<Manga>,
        val contentType: ContentType,
        val loading: Boolean = false,
        val error: String? = null
    )

    fun onMangaClicked(manga: Manga)

    fun navigate(route: String)

    fun switchMangasType(contentType: ContentType)

//    Online functions
    val mangaSearch: MutableState<String>
    fun onMangaSearchChange(search: String)

    val isUserLogged: MutableValue<Boolean>

    fun signOut()

    fun getMangas(forceFetch: Boolean)

    // MDLists
    fun getMDListMangas()

//    Downloaded functions

    fun deleteManga(mangaId: String)

    fun getDownloadedMangas()

    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            onMangaSelected: (manga: Manga, contentType: ContentType) -> Unit,
            isUserLoggedIn: MutableValue<Boolean>,
            logOut: () -> Unit,
            rootNavigation: (String) -> Unit,
            mdList: MDList?
        ): MangaListComponent
    }
}

class DefaultMangaListComponent(
    private val componentContext: ComponentContext,
    private val mangaUseCase: MangasUseCase,
    private val chapterUseCase: ChaptersUseCase,
    private val authUseCase: UserUseCase,
    private val onMangaSelected: (manga: Manga, contentType: ContentType) -> Unit,
    private val isUserLoggedIn: MutableValue<Boolean>,
    private val logOut: () -> Unit,
    private val rootNavigation: (String) -> Unit,
    private val mdList: MDList?,
) : MangaListComponent, ComponentContext by componentContext {

    private val _model = MutableValue(
        MangaListComponent.Model(
            mangas = emptyList(),
            contentType = ContentType.Online,
            loading = true
        )
    )
    override val model: Value<MangaListComponent.Model> = _model

    override fun onMangaClicked(manga: Manga) {
        // Later to change content type functions in manga component
        if(_model.value.contentType is ContentType.MDList) onMangaSelected(manga, ContentType.Logged)
        else if(isUserLogged.value && _model.value.contentType !is ContentType.Downloaded) onMangaSelected(manga, ContentType.Logged)
        else onMangaSelected(manga, _model.value.contentType)
    }

    override fun switchMangasType(contentType: ContentType) {
        _model.value = _model.value.copy(contentType = contentType)
        when(contentType) {
            is ContentType.Online -> getMangas(false)
            is ContentType.Downloaded -> getDownloadedMangas()
            is ContentType.Logged -> getMangas(false)
            is ContentType.MDList -> getMDListMangas()
        }
    }


//    Online functions
    private val _mangaSearch = mutableStateOf("")
    override val mangaSearch: MutableState<String> get() = _mangaSearch

    override fun onMangaSearchChange(search: String) {
        _mangaSearch.value = search
    }

    private val _isUserLogged = MutableValue(isUserLoggedIn.value)
    override val isUserLogged: MutableValue<Boolean> = _isUserLogged

    override fun signOut() {
        logOut()
    }

    init {
        isUserLoggedIn.subscribe { loggedIn ->
            _isUserLogged.value = loggedIn

            if(loggedIn && _model.value.contentType !is ContentType.Downloaded) {
                if(mdList is MDList) _model.value = MangaListComponent.Model(mangas = _model.value.mangas, contentType = ContentType.MDList, loading = _model.value.loading)
                else _model.value = MangaListComponent.Model(mangas = _model.value.mangas, contentType = ContentType.Logged, loading = _model.value.loading)
            }
        }

        if(mdList is MDList) getMDListMangas()
        else getMangas(true)

    }

    override fun navigate(route: String) {
        rootNavigation(route)
    }

    override fun getMangas(forceFetch: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            _model.value = MangaListComponent.Model(loading = true, mangas = _model.value.mangas, contentType = _model.value.contentType)

            val fetchedMangas = mangaUseCase.getMangas(_mangaSearch.value, forceFetch)

            if (fetchedMangas.isEmpty()) {
                _model.value = MangaListComponent.Model(loading = false, error = "Cannot find any mangas", mangas = fetchedMangas, contentType = _model.value.contentType)
            } else {
                _model.value = MangaListComponent.Model(loading = false, mangas = fetchedMangas, contentType = _model.value.contentType)
            }
        }
    }

    override fun getMDListMangas() {
        CoroutineScope(Dispatchers.Main).launch {
            _model.value = MangaListComponent.Model(loading = true, mangas = _model.value.mangas, contentType = ContentType.MDList)

            if(mdList!!.mangaIds.isNotEmpty()) {
                // It's checking if MDList exists in init and from init this functions is executable
                val fetchedMangas = mangaUseCase.getMDListMangas(mdList!!.mangaIds)

                if (fetchedMangas.isEmpty() ) {
                    _model.value = MangaListComponent.Model(error = "Cannot find any mangas", mangas = fetchedMangas, contentType = _model.value.contentType)
                } else {
                    _model.value = MangaListComponent.Model(mangas = fetchedMangas, contentType = _model.value.contentType)
                }
            } else {
                _model.value = MangaListComponent.Model(error = "Cannot find any mangas in MDList", mangas = _model.value.mangas, contentType = _model.value.contentType)
            }


        }
    }
//    Downloaded functions

    override fun deleteManga(mangaId: String) {
        mangaUseCase.deleteDownloadedManga(mangaId)

        chapterUseCase.getDownloadedChapters(mangaId).forEach { chapter ->
            chapterUseCase.deleteDownloadedChapter(chapter)
        }

        getDownloadedMangas()
    }

    override fun getDownloadedMangas() {
        _model.value =
            MangaListComponent.Model(mangas = mangaUseCase.getDownloadedMangas(), contentType = ContentType.Downloaded)
    }

    class Factory(
        private val mangaUseCase: MangasUseCase,
        private val chapterUseCase: ChaptersUseCase,
        private val authUseCase: UserUseCase
    ) : MangaListComponent.Factory {
        override fun invoke(
            componentContext: ComponentContext,
            onMangaSelected: (manga: Manga, contentType: ContentType) -> Unit,
            isUserLoggedIn: MutableValue<Boolean>,
            logOut: () -> Unit,
            rootNavigation: (String) -> Unit,
            mdList: MDList?
        ): MangaListComponent {
            return DefaultMangaListComponent(
                componentContext = componentContext,
                mangaUseCase = mangaUseCase,
                chapterUseCase = chapterUseCase,
                authUseCase = authUseCase,
                onMangaSelected = onMangaSelected,
                isUserLoggedIn = isUserLoggedIn,
                logOut = logOut,
                rootNavigation = rootNavigation,
                mdList = mdList
            )
        }
    }
}