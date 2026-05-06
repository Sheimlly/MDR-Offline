package com.mdr.offline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.mdr.offline.ui.App
import com.mdr.offline.ui.navigation.RootComponent
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val rootComponentFactory: RootComponent.Factory = get()

        // Always create the root component outside Compose on the main thread
        val rootComponent = rootComponentFactory(defaultComponentContext())

        setContent {
            App(rootComponent)
        }
    }
}