package com.mdr.offline.user.di

import com.mdr.offline.user.application.UserUseCase
import com.mdr.offline.user.data.Authentication
import com.mdr.offline.user.data.TokensDataSource
import com.mdr.offline.user.data.TokensRepository
import com.mdr.offline.user.data.UserService
import org.koin.dsl.module

val userModule = module {
    single<UserUseCase> { UserUseCase(get(), get()) }
    single<UserService> { UserService(get()) }
    single<TokensRepository> { TokensRepository(get(), get()) }
    single<TokensDataSource> { TokensDataSource(get()) }
    single<Authentication> { Authentication(get()) }
}