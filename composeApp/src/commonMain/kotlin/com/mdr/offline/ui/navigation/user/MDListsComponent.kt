package com.mdr.offline.ui.navigation.user

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.mdr.offline.ui.navigation.manga.MangaListComponent
import com.mdr.offline.user.application.MDList
import com.mdr.offline.user.application.UserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface MDListsComponent {
    val model: Value<Model>

    data class Model(
        val mdLists: List<MDList>,
        val loading: Boolean = false,
        val error: String? = null
    )

    fun selectList(list: MDList)

    fun goBack()

    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            token: String,
            onListSelected: (list: MDList) -> Unit,
            rootNavigation: (String) -> Unit
        ): MDListsComponent
    }
}

class DefaultMDListsComponent (
    private val componentContext: ComponentContext,
    private val token: String,
    private val userUseCase: UserUseCase,
    private val onListSelected: (MDList) -> Unit,
    private val rootNavigation: (String) -> Unit
): MDListsComponent, ComponentContext by componentContext {
    private val _model = MutableValue(
        MDListsComponent.Model(
            mdLists = emptyList(),
            loading = true
        )
    )
    override val model: Value<MDListsComponent.Model> = _model

    init {
        CoroutineScope(Dispatchers.Default).launch {
            _model.value = MDListsComponent.Model(mdLists = _model.value.mdLists, loading = true)

            val fetchedMDLists = userUseCase.getMDLists(token)

            if(fetchedMDLists.isNotEmpty()) {
                _model.value = MDListsComponent.Model(mdLists = fetchedMDLists, loading = false)
                println("MDList response: $fetchedMDLists")
            } else {
                _model.value = MDListsComponent.Model(mdLists = _model.value.mdLists, loading = false, error = "Cannot find any MDLists")
            }
        }
    }

    override fun selectList(list: MDList) {
        onListSelected(list)
    }

    override fun goBack() {
        rootNavigation("panel")
    }

    class Factory(
        private val userUseCase: UserUseCase
    ) : MDListsComponent.Factory {
        override fun invoke(
            componentContext: ComponentContext,
            token: String,
            onListSelected: (list: MDList) -> Unit,
            rootNavigation: (String) -> Unit
        ): MDListsComponent {
            return DefaultMDListsComponent(
                componentContext = componentContext,
                userUseCase = userUseCase,
                token = token,
                onListSelected = onListSelected,
                rootNavigation = rootNavigation
            )
        }
    }
}