package com.mdr.offline.ui.navigation.user

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.mdr.offline.user.application.TokenResponse
import com.mdr.offline.user.application.Tokens
import com.mdr.offline.user.application.UserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface AuthenticationComponent {
    val errorMessage : MutableState<String>
    fun logIn(login: String, password: String)

    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            rootNavigation: (String) -> Unit,
            tokens: MutableState<Tokens>,
            isUserLoggedIn: MutableValue<Boolean>,
            refreshToken: (String) -> Unit,
        ): AuthenticationComponent
    }
}

class DefaultAuthenticationComponent(
    private val componentContext: ComponentContext,
    private val useCase: UserUseCase,
    private val rootNavigation: (String) -> Unit,
    private val tokens: MutableState<Tokens>,
    private val isUserLoggedIn: MutableValue<Boolean>,
    private val refreshToken: (String) -> Unit
) : AuthenticationComponent, ComponentContext by componentContext {

    private val _errorMessage = mutableStateOf("")
    override val errorMessage: MutableState<String> get() = _errorMessage


    override fun logIn(login: String, password: String) {
        CoroutineScope(Dispatchers.Unconfined).launch {
            val fetchedTokens: TokenResponse = useCase.logIn(login, password)
            if(fetchedTokens.errorMessage != null) {
                _errorMessage.value = fetchedTokens.errorMessage
            } else {
                tokens.value = Tokens(
                    accessToken = fetchedTokens.accessToken!!,
                    refreshToken = fetchedTokens.refreshToken!!
                )
                refreshToken(fetchedTokens.refreshToken)

                useCase.setTokens(tokens.value)

                isUserLoggedIn.value = true

                withContext(Dispatchers.Main) {
                    rootNavigation("panel")
                }
            }
        }
    }

    class Factory(
        private val userUseCase: UserUseCase,
    ): AuthenticationComponent.Factory {
        override fun invoke(
            componentContext: ComponentContext,
            rootNavigation: (String) -> Unit,
            tokens: MutableState<Tokens>,
            isUserLoggedIn: MutableValue<Boolean>,
            refreshToken: (String) -> Unit,
        ): AuthenticationComponent {
            return DefaultAuthenticationComponent(
                componentContext = componentContext,
                useCase = userUseCase,
                rootNavigation = rootNavigation,
                tokens = tokens,
                isUserLoggedIn = isUserLoggedIn,
                refreshToken = refreshToken
            )
        }
    }
}