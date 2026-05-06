package com.mdr.offline.ui.screens.manga.chapter

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.mdr.offline.ImageStorage
import com.mdr.offline.data.ContentType
import com.mdr.offline.ui.navigation.manga.chapter.ChapterComponent
import com.mdr.offline.ui.screens.elements.ErrorMessage
import com.mdr.offline.ui.screens.elements.LoadingScreen
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun ChapterScreen(
    component: ChapterComponent,
    appBarVisibility: () -> Unit
) {
    val chapterPagesState = component.model.subscribeAsState()

    Column {

        if (chapterPagesState.value.error != null)
            ErrorMessage(chapterPagesState.value.error!!)
        if (chapterPagesState.value.loading)
            LoadingScreen()
        else PagesListView(component, appBarVisibility)
    }
}

@Composable
fun PagesListView(
    component: ChapterComponent,
    appBarVisibility: () -> Unit
) {
    val zoomState = rememberZoomState(maxScale = 5f)

    val pagerState = rememberPagerState(pageCount = {
        when(component.model.value.contentType) {
            is ContentType.Online, ContentType.Logged, ContentType.MDList -> component.model.value.pages!!.size
            is ContentType.Downloaded -> component.model.value.chapter.imagesPath!!.size
        }
    })

    val lastReadPage = component.model.value.chapter.lastReadPage
    if (lastReadPage != null) {
        if(lastReadPage > 1) {
            LaunchedEffect(Unit) {
                pagerState.scrollToPage(lastReadPage-1)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            component.onPageChange(page+1)
        }
    }

    if (component.format == "Doujinshi") {
        HorizontalPager(
            state = pagerState,
        ) { page ->
            when(component.model.value.contentType) {
                is ContentType.Online, ContentType.Logged, ContentType.MDList -> {
                    KamelImage(
                        resource = asyncPainterResource(data = component.model.value.pages!![page]),
                        contentDescription = "Page nr. $page",
                        onLoading = { LoadingScreen() },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .zoomable(
                                zoomState = zoomState,
                                enableOneFingerZoom = false,
                                onDoubleTap = { position ->
                                    val targetScale = when {
                                        zoomState.scale > 1f -> 1f
                                        else -> 2f
                                    }
                                    zoomState.changeScale(targetScale, position)
                                },
                                onTap = { appBarVisibility() }
                            )
                    )
                }
                is ContentType.Downloaded -> {
                    Image(
                        bitmap = ImageStorage().convertToImageBitmap(ImageStorage().getImage(component.model.value.chapter.imagesPath!![page])),
                        contentDescription = "Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .zoomable(
                                zoomState = zoomState,
                                enableOneFingerZoom = false,
                                onDoubleTap = { position ->
                                    val targetScale = when {
                                        zoomState.scale > 1f -> 1f
                                        else -> 2f
                                    }
                                    zoomState.changeScale(targetScale, position)
                                },
                                onTap = { appBarVisibility() }
                            )
                    )
                }
            }
        }
    } else if (component.format == "Long Strip") {
        val listState = rememberLazyListState()

        if (lastReadPage != null) {
            if(lastReadPage > 1) {
                LaunchedEffect(Unit) {
                    listState.scrollToItem(lastReadPage-1)
                }
            }
        }

        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                .collect { visibleItems ->
                    if (visibleItems.isNotEmpty()) {
                        val largestItem = visibleItems.maxByOrNull { item ->
                            val viewportTop = listState.layoutInfo.viewportStartOffset
                            val viewportBottom = listState.layoutInfo.viewportEndOffset
                            val itemTop = item.offset
                            val itemBottom = item.offset + item.size

                            // clamp to viewport
                            val visibleTop = maxOf(itemTop, viewportTop)
                            val visibleBottom = minOf(itemBottom, viewportBottom)
                            val visibleHeight = (visibleBottom - visibleTop).coerceAtLeast(0)

                            visibleHeight
                        }

                        largestItem?.let {
                            // notify your component with 1-based page number
                            component.onPageChange(it.index + 1)
                        }
                    }
                }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .zoomable(
                    zoomState = zoomState,
                    enableOneFingerZoom = false,
                    onDoubleTap = { position ->
                        val targetScale = when {
                            zoomState.scale > 1f -> 1f
                            else -> 2f
                        }

                        zoomState.changeScale(targetScale, position)
                    },
                    onTap = { appBarVisibility() }
                )
        ) {
            when(component.model.value.contentType) {
                is ContentType.Online, ContentType.Logged, ContentType.MDList -> {
                    itemsIndexed(component.model.value.pages!!) {index, page ->
                        KamelImage(
                            resource = asyncPainterResource(data = page),
                            contentDescription = "Page nr. ${index+1}",
                            onLoading = { LoadingScreen() },
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is ContentType.Downloaded -> {
                    itemsIndexed(component.model.value.chapter.imagesPath!!) {index, page ->
                        Image(
                            bitmap = ImageStorage().convertToImageBitmap(ImageStorage().getImage(page)),
                            contentDescription = "Page nr. ${index + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

            }
        }
    }
}