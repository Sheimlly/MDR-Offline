package com.mdr.offline.ui.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.mdr.offline.ui.navigation.user.AuthenticationComponent
import com.mdr.offline.user.application.TokenResponse
import com.mdr.offline.user.application.Tokens
import com.mdr.offline.user.application.UserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface AboutAppComponent {
    fun navigate(route: String)

    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            rootNavigation: (String) -> Unit,
        ): AboutAppComponent
    }
}

class DefaultAboutAppComponent(
    private val componentContext: ComponentContext,
    private val rootNavigation: (String) -> Unit,
) : AboutAppComponent, ComponentContext by componentContext {

    override fun navigate(route: String) {
        rootNavigation(route)
    }

    class Factory(
    ): AboutAppComponent.Factory {
        override fun invoke(
            componentContext: ComponentContext,
            rootNavigation: (String) -> Unit,
        ): AboutAppComponent {
            return DefaultAboutAppComponent(
                componentContext = componentContext,
                rootNavigation = rootNavigation,
            )
        }
    }
}