package com.mdr.offline.ui.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.mdr.offline.ui.navigation.manga.MangaPanelComponent
import com.mdr.offline.ui.navigation.user.AuthenticationComponent
import com.mdr.offline.ui.navigation.user.MDListsComponent
import com.mdr.offline.user.application.MDList
import com.mdr.offline.user.application.Tokens
import com.mdr.offline.user.application.UserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable // Required for state preservation

interface RootComponent {
    val slot: Value<ChildSlot<*, Child>>

    val tokens: MutableState<Tokens>

//    val mainPanelSlot: Value<ChildSlot<*, Child.MainPanel>>
//    val authSlot: Value<ChildSlot<*, Child.Authentication>>

    fun refreshTokens(refreshToken: String)

    fun logOut()

    sealed interface Child {
        data class MangaPanel(val component: MangaPanelComponent) : Child
        data class Authentication(val component: AuthenticationComponent) : Child

        data class MDLists(val component: MDListsComponent): Child
        data class AboutApp(val component: AboutAppComponent): Child
        // You could add other children here later, e.g., Settings, Profile, etc.
        // data class Settings(val component: SettingsComponent): Child
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext): RootComponent
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val mangaPanelComponentFactory: MangaPanelComponent.Factory,
    private val authenticationComponentFactory: AuthenticationComponent.Factory,
    private val mdListsComponentFactory: MDListsComponent.Factory,
    private val aboutAppComponentFactory: AboutAppComponent.Factory,
    private val authUseCase: UserUseCase,
) : RootComponent, ComponentContext by componentContext {

    private val isUserLoggedIn = MutableValue(false)
    private val _tokens = mutableStateOf(
        Tokens(
            accessToken = "",
            refreshToken = ""
        )
    )

    override val tokens: MutableState<Tokens> get() = _tokens

    override fun refreshTokens(refreshToken: String) {
        CoroutineScope(Dispatchers.Default).launch {
            delay(870000) // Refreshing after 14min 30s
            val fetchedTokens = authUseCase.refreshTokens(refreshToken)
            if(fetchedTokens.errorMessage != null) {
                isUserLoggedIn.value = false
                authUseCase.clearTokens()
            }
            else {
                tokens.value = Tokens(
                    accessToken = fetchedTokens.accessToken!!,
                    refreshToken = fetchedTokens.refreshToken!!
                )

                authUseCase.setTokens(tokens.value)

                refreshTokens(fetchedTokens.refreshToken)
            }
        }
    }

    override fun logOut() {
        isUserLoggedIn.value = false
        authUseCase.clearTokens()
        _tokens.value = Tokens("", "")
    }

    init {
        val initTokens = authUseCase.getTokens()

        if(initTokens != null) {
            CoroutineScope(Dispatchers.Default).launch {
                val fetchedTokens = authUseCase.refreshTokens(initTokens.refreshToken)

                if(fetchedTokens.errorMessage == null) {
                    tokens.value = Tokens(
                        accessToken = fetchedTokens.accessToken!!,
                        refreshToken = fetchedTokens.refreshToken!!
                    )

                    authUseCase.setTokens(tokens.value)

                    refreshTokens(fetchedTokens.refreshToken)

                    isUserLoggedIn.value = true
                    println("Is user logged in: Root coroutine ${isUserLoggedIn.value}")
                }
            }
        }

        println("Is user logged in: Root ${isUserLoggedIn.value}")
    }

    private val slotNavigation = SlotNavigation<Config>()

    override val slot by lazy {
        childSlot(
            source = slotNavigation,
            serializer = Config.serializer(),
            initialConfiguration = { Config.MangaPanelConfig(null) },
            childFactory = { config, context ->
                when (config) {
                    is Config.MangaPanelConfig ->
                        RootComponent.Child.MangaPanel(mangaPanelComponentFactory(
                            context,
                            isUserLoggedIn = isUserLoggedIn,
                            logOut = { logOut() },
                            rootNavigation = { whereTo -> navigate(whereTo) },
                            token = tokens,
                            mdList = config.mdList,
                        ))

                    is Config.AuthorizationConfig ->
                        RootComponent.Child.Authentication(authenticationComponentFactory(
                            context,
                            rootNavigation = { whereTo -> navigate(whereTo) },
                            tokens,
                            isUserLoggedIn,
                            refreshToken = { refreshToken -> refreshTokens(refreshToken)}
                        ))

                    is Config.MDListsConfig ->
                        RootComponent.Child.MDLists(mdListsComponentFactory(
                            context,
                            tokens.value.accessToken,
                            rootNavigation = { whereTo -> navigate(whereTo) },
                            onListSelected = { mdList -> slotNavigation.activate(Config.MangaPanelConfig(mdList)) }
                        ))
                    is Config.AboutAppConfig ->
                        RootComponent.Child.AboutApp(aboutAppComponentFactory(
                            context,
                            rootNavigation = { whereTo -> navigate(whereTo) },
                        ))
                }
            }
        )
    }

    private fun navigate(whereTo: String) {
        if(whereTo == "panel") slotNavigation.activate(Config.MangaPanelConfig(null))
        if(whereTo == "login") slotNavigation.activate(Config.AuthorizationConfig)
        if(whereTo == "mdlists") slotNavigation.activate(Config.MDListsConfig)
        if(whereTo == "aboutapp") slotNavigation.activate(Config.AboutAppConfig)
    }


//    override val mainPanelSlot: Value<ChildSlot<*, RootComponent.Child.MainPanel>> =
//        childSlot(
//            source = mainPanelNavigation,
//            serializer = Config.MainPanelConfig.serializer(),
//            initialConfiguration = { Config.MainPanelConfig },
//            childFactory = { config, context ->
//                RootComponent.Child.MainPanel(panelComponentFactory(context))
//            }
//        )
//
//    override val authSlot: Value<ChildSlot<*, RootComponent.Child.Authentication>> =
//        childSlot(
//            source = authNavigation,
//            serializer = Config.AuthorizationConfig.serializer(),
//            initialConfiguration = { Config.AuthorizationConfig },
//            childFactory = { config, context ->
//                RootComponent.Child.Authentication(authenticationComponentFactory(context))
//            }
//        )


            // You can add navigation functions here if needed, e.g.
    // fun navigateToSettings() { navigation.push(Config.SettingsConfig) }

    @Serializable // Configurations must be serializable for state preservation
    sealed interface Config {
        @Serializable
        data class MangaPanelConfig(val mdList: MDList?) : Config

        @Serializable
        data object AuthorizationConfig : Config

        @Serializable
        data object MDListsConfig: Config

        @Serializable
        data object AboutAppConfig: Config
    }

    class Factory(
        private val mangaPanelComponentFactory: MangaPanelComponent.Factory,
        private val authenticationComponentFactory: AuthenticationComponent.Factory,
        private val mdListsComponentFactory: MDListsComponent.Factory,
        private val aboutAppComponentFactory: AboutAppComponent.Factory,
        private val authUseCase: UserUseCase,
    ) : RootComponent.Factory {
        override fun invoke(componentContext: ComponentContext): RootComponent {
            return DefaultRootComponent(
                componentContext = componentContext,
                mangaPanelComponentFactory = mangaPanelComponentFactory,
                authenticationComponentFactory = authenticationComponentFactory,
                mdListsComponentFactory = mdListsComponentFactory,
                aboutAppComponentFactory = aboutAppComponentFactory,
                authUseCase = authUseCase,
            )
        }
    }
}