package com.mdr.offline.ui.screens.manga.chapter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.mdr.offline.ui.MangaDexTheme
import com.mdr.offline.ui.navigation.manga.chapter.ChapterComponent
import com.mdr.offline.ui.navigation.manga.chapter.ChapterPagesComponent

@Composable
fun ChapterPagesScreen(
    component: ChapterPagesComponent,
) {
    Column {
        if(component.showTopBar.value)
            AppBar(component = component.currentChapterComponent.value)

        PagesNavContent(component)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    component: ChapterComponent?
) {
    if(component != null) {
        val chapter = component.model.value.chapter

        TopAppBar(
            title = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.weight(6f)) {
                        Text(text = "Chapter: ${chapter.chapter}" )
                    }
                    Text("Page: ${component.currentPage.value}/${chapter.pageNumbers}", modifier = Modifier.weight(4f))
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    component.onBackPressed()
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
}

@Composable
private fun PagesNavContent(
    component: ChapterPagesComponent,
) {
    val childPages by component.pages.subscribeAsState()
    val pagerState = rememberPagerState(
        initialPage = childPages.selectedIndex,
        pageCount = { childPages.items.size }
    )


    LaunchedEffect(childPages.selectedIndex) {
        if (pagerState.currentPage != childPages.selectedIndex) {
            pagerState.animateScrollToPage(childPages.selectedIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            component.onSelectPage(page)
            childPages.items[page].instance?.let { chapterComponent -> component.setCurrentChapterComponent(chapterComponent)  }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .clipToBounds()
    ) {
        if(component.format == "Doujinshi") {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { index -> childPages.items[index].configuration.hashCode() }
            ) { pageIndex ->

                val currentChildInstance = childPages.items[pageIndex] as? Child.Created
                currentChildInstance?.instance?.let { chapterComponent ->

                    ChapterScreen(component = chapterComponent, appBarVisibility = { component.onShowTopBarChange() })

                } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading chapter ${pageIndex + 1}...")
                }
            }
        } else if (component.format == "Long Strip") {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { index -> childPages.items[index].configuration.hashCode() }
            ) { pageIndex ->
                val currentChildInstance = childPages.items[pageIndex] as? Child.Created
                currentChildInstance?.instance?.let { chapterComponent ->

                    ChapterScreen(component = chapterComponent, appBarVisibility = { component.onShowTopBarChange() })

                } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading chapter ${pageIndex + 1}...")
                }
            }
        }
    }
}