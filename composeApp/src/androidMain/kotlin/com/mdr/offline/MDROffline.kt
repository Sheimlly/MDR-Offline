package com.mdr.offline

import android.app.Application
import com.mdr.offline.di.databaseModule
import com.mdr.offline.di.sharedKoinModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MDROffline : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()

        com.mdr.offline.applicationContext = applicationContext
    }

    private fun initKoin() {
        val modules = sharedKoinModules + databaseModule

        startKoin {
            androidContext(this@MDROffline)
            modules(modules)
        }
    }

}