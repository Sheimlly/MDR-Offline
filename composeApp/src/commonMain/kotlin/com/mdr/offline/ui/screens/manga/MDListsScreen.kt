package com.mdr.offline.ui.screens.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.mdr.offline.ui.MangaDexTheme
import com.mdr.offline.ui.navigation.user.MDListsComponent
import com.mdr.offline.ui.screens.elements.ErrorMessage
import com.mdr.offline.ui.screens.elements.LoadingScreen

@Composable
fun MDListsScreen(
    component: MDListsComponent
) {
    val mdListComponent = component.model.subscribeAsState()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = Modifier
            .fillMaxSize()
            .background(MangaDexTheme.color.background)
    ) {
        AppBar(component)

        if (mdListComponent.value.error != null)
            ErrorMessage(mdListComponent.value.error!!)
        if (mdListComponent.value.loading)
            LoadingScreen()
        else MDListsView(component)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    component: MDListsComponent
) {
    TopAppBar(
        title = { Text("MDLists") },
        navigationIcon = {
            IconButton(onClick = {
                component.goBack()
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Up Button",
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
private fun MDListsView(
    component: MDListsComponent
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(component.model.value.mdLists) {mdList ->
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable{ component.selectList(mdList) }
                        .background(MangaDexTheme.color.primary)
                        .fillMaxWidth()
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = mdList.name,
                        color = MangaDexTheme.color.white,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}