package com.mdr.offline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mdr.offline.ui.MangaDexTheme
import com.mdr.offline.ui.navigation.AboutAppComponent

@Composable
fun AboutAppScreen(
    component: AboutAppComponent
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = Modifier
            .fillMaxSize()
            .background(MangaDexTheme.color.background)
    ) {
        AppBar(component)
        ScreenContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    component: AboutAppComponent
) {
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("About app", modifier = Modifier.weight(4f))
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                component.navigate("panel")
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back Button",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MangaDexTheme.color.primary,
            titleContentColor = MangaDexTheme.color.white,
            navigationIconContentColor = MangaDexTheme.color.white,
            actionIconContentColor = MangaDexTheme.color.white
        )
    )
}

@Composable
private fun ScreenContent() {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(
            text ="App is based on MangaDex api:",
            style = MangaDexTheme.typography.body
        )
        TextButton(onClick = { uriHandler.openUri("https://api.mangadex.org") }) {
            Text(
                text = "api.mangadex.org",
                style = MangaDexTheme.typography.body,
                textDecoration = TextDecoration.Underline
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "You can also read on MangaDex website:",
            style = MangaDexTheme.typography.body
        )
        TextButton(onClick = { uriHandler.openUri("https://mangadex.org") }) {
            Text(
                text = "mangadex.org",
                style = MangaDexTheme.typography.body,
                textDecoration = TextDecoration.Underline
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "If you find any bugs or want to contact me you can email me: sheimlly.dev@gmail.com",
            style = MangaDexTheme.typography.body
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Project is available on github:",
            style = MangaDexTheme.typography.body
        )
        TextButton(onClick = { uriHandler.openUri("https://github.com/Sheimlly/MDR-Offline") }) {
            Text(
                text = "MDR: Offline",
                style = MangaDexTheme.typography.body,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}