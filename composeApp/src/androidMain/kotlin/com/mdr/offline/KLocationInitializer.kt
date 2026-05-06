package com.mdr.offline

import android.content.Context
import androidx.startup.Initializer

internal lateinit var applicationContext: Context

public object KLocationContext

public class KLocationInitializer: Initializer<KLocationContext> {
    override fun create(context: Context): KLocationContext {
        applicationContext = context.applicationContext
        return KLocationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}