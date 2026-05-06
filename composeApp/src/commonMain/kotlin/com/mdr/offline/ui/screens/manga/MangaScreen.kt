package com.mdr.offline.ui.screens.manga

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.mdr.offline.data.DownloadState
import com.mdr.offline.data.ChapterFilters
import com.mdr.offline.ImageStorage
import com.mdr.offline.chapters.application.Chapter
import com.mdr.offline.data.ContentType
import com.mdr.offline.data.Order
import com.mdr.offline.mangas.application.Manga
import com.mdr.offline.ui.MangaDexTheme
import com.mdr.offline.ui.navigation.manga.MangaComponent
import com.mdr.offline.ui.screens.elements.ErrorMessage
import com.mdr.offline.ui.screens.elements.LoadingScreen
import com.mdr.offline.utilities.toLanguageName
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.http.Url
import mangadexoffline.composeapp.generated.resources.Res
import mangadexoffline.composeapp.generated.resources.ic_check
import mangadexoffline.composeapp.generated.resources.ic_circle
import mangadexoffline.composeapp.generated.resources.ic_delete
import mangadexoffline.composeapp.generated.resources.ic_done
import mangadexoffline.composeapp.generated.resources.ic_download
import mangadexoffline.composeapp.generated.resources.ic_filter
import mangadexoffline.composeapp.generated.resources.ic_syncarrowup
import org.jetbrains.compose.resources.painterResource

@Composable
fun MangaScreen(
    component: MangaComponent
) {
    val chaptersState = component.model.subscribeAsState()

    val scrollState = rememberLazyListState()

    Column {
        AppBar(component)
        ChaptersListView(component, chaptersState, scrollState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    component: MangaComponent,
) {
    var expandedFilters by remember { mutableStateOf(false) }
    var expandedSync by remember { mutableStateOf(false) }
    val filters = ChapterFilters.values
    val currentFilter = component.model.subscribeAsState().value.filter

    TopAppBar(
        title = { component.model.value.manga.title },
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
        ),
        actions = {
            if(component.model.value.contentType is ContentType.Downloaded) {
                if (component.isUserLogged.value) {
                    Button(
                        onClick = { component.syncAllChapters() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MangaDexTheme.color.lightGray,
                            contentColor = MangaDexTheme.color.white
                        ),
                    ) {
                        Text(
                            text = "Sync",
                            style = MangaDexTheme.typography.labelLarge
                        )
                    }
                }
            }

            when(component.model.value.contentType) {
                is ContentType.Downloaded, ContentType.Logged, ContentType.MDList -> {
                    Spacer(Modifier.width(8.dp))

                    IconButton(onClick = { expandedFilters = !expandedFilters }) {
                        Icon(painter = painterResource(Res.drawable.ic_filter), contentDescription = "Filter")
                    }
                    DropdownMenu(
                        expanded = expandedFilters,
                        onDismissRequest = { expandedFilters = false },
                        modifier = Modifier
                            .background(MangaDexTheme.color.lightGray)
                            .padding(start = 8.dp, end = 8.dp)
                    ) {
                        for (filter in filters.filterNotNull()) {
                            Spacer(modifier = Modifier.height(4.dp))

                            DropdownMenuItem(
                                text = { Text(text = filter.label, style = MangaDexTheme.typography.labelLarge) },
                                trailingIcon = {
                                    if(currentFilter == filter)
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_check),
                                            contentDescription = "Selected",
                                            tint = MangaDexTheme.color.white,
                                            modifier = Modifier.size(MangaDexTheme.size.medium)
                                        )
                                },
                                onClick = { component.onFilterChanged(filter) },
                                modifier = Modifier
                                    .background(MangaDexTheme.color.primary)
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
                is ContentType.Online -> null
            }
        }
    )
}

@Composable
fun ChaptersListView(
    component: MangaComponent,
    chapterModel: State<MangaComponent.Model>,
    scrollState: LazyListState
) {
    Box {
        val model = component.model.subscribeAsState()
        val chapters = model.value.chapters
        val filter = model.value.filter
        val loading = model.value.loading
        val loadingMore = model.value.loadingMoreChapters

        val order = component.order.subscribeAsState().value
        val filterOrder = component.filterOrder.subscribeAsState().value

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
        ) {
            item {
                MangaDetails(component)
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                if(model.value.contentType !is ContentType.Online) {
                    Button(
                        onClick = {component.changeOrder()},
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MangaDexTheme.color.lightGray),
                        modifier = Modifier
                            .padding(8.dp)
                    ) {
                        when(model.value.contentType) {
                            is ContentType.Logged, ContentType.MDList -> {
                                when(filter) {
                                    is ChapterFilters.All -> {
                                        when(filterOrder.all) {
                                            is Order.Asc -> Text("Desc")
                                            is Order.Desc -> Text("Asc")
                                        }
                                    }
                                    is ChapterFilters.Read -> {
                                        when(filterOrder.read) {
                                            is Order.Asc -> Text("Desc")
                                            is Order.Desc -> Text("Asc")
                                        }
                                    }
                                    is ChapterFilters.Unread -> {
                                        when(filterOrder.unread) {
                                            is Order.Asc -> Text("Desc")
                                            is Order.Desc -> Text("Asc")
                                        }
                                    }
                                }
                            }
                            is ContentType.Downloaded -> {
                                when (order) {
                                    is Order.Desc -> Text("Asc")
                                    is Order.Asc -> Text("Desc")
                                }
                            }
                            is ContentType.Online -> null
                        }
                    }
                }
            }

            if(loading) {
                item{ LoadingScreen() }
            } else if (model.value.error != null) {
                item{ ErrorMessage(model.value.error!!) }
            } else {
                items(chapters) { chapter ->
                    when(model.value.contentType) {
                        // I'll add read and unread to content type mdlist later
                        is ContentType.Online, ContentType.Logged, ContentType.MDList -> {
                            ChapterItemView(
                                chapter,
                                component,
                                onChapterClicked = { component.onChapterClicked(it) }
                            )
                        }
                        is ContentType.Downloaded -> {
                            when (filter) {
                                is ChapterFilters.All -> {
                                    ChapterItemView(
                                        chapter,
                                        component,
                                        onChapterClicked = { component.onChapterClicked(it) }
                                    )
                                }
                                is ChapterFilters.Read -> {
                                    if (chapter.read) {
                                        ChapterItemView(
                                            chapter,
                                            component,
                                            onChapterClicked = { component.onChapterClicked(it) }
                                        )
                                    }
                                }

                                is ChapterFilters.Unread -> {
                                    if (!chapter.read) {
                                        ChapterItemView(
                                            chapter,
                                            component,
                                            onChapterClicked = { component.onChapterClicked(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            when(component.model.value.contentType) {
                is ContentType.Online, ContentType.Logged, ContentType.MDList -> {
                    if(!loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(align = Alignment.CenterHorizontally)
                                    .padding(bottom = 16.dp)
                            ) {
                                when(loadingMore) {
                                    true -> {
                                        CircularProgressIndicator(
                                            color = MangaDexTheme.color.mainButton
                                        )
                                    }
                                    false -> {
                                        Button(
                                            onClick = { component.loadMoreChapters() },
                                            colors = ButtonDefaults.buttonColors(
                                                contentColor = MangaDexTheme.color.white
                                            )
                                        ) {
                                            Text("Load more")
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
                is ContentType.Downloaded -> null
            }
        }
    }
}

@Composable
fun MangaDetails(
    component: MangaComponent,
) {
    BoxWithConstraints {

        val manga = component.model.value.manga
        val contentType = component.model.value.contentType

        if (maxWidth < 512.dp) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .heightIn(min=450.dp)
                    .padding(8.dp)
            ) {
                when(contentType) {
                    is ContentType.Online, ContentType.Logged, ContentType.MDList -> {
                        KamelImage(
                            resource = asyncPainterResource(data = Url(manga.coverImageUrl!!)),
                            contentDescription = "Manga cover image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                    is ContentType.Downloaded -> {
                        Image(
                            bitmap = ImageStorage().convertToImageBitmap(manga.coverImage!!),
                            contentDescription = "Manga cover image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }

                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MangaDexTheme.color.primary)

                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceAround,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .heightIn(min=418.dp),
                    ) {
                        Text(
                            text = manga.title,
                            style = MangaDexTheme.typography.titleLarge
                        )

                        Text(
                            text = manga.description,
                            style = MangaDexTheme.typography.body
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Author: ${manga.author}",
                            style = MangaDexTheme.typography.labelLarge
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(align = Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp)
                    ){
                        when(contentType){
                            is ContentType.Online -> DownloadButton(component)
                            is ContentType.Downloaded -> ContinueReadingButton(component)
                            is ContentType.Logged, ContentType.MDList -> {DownloadButton(component); ContinueReadingButton(component)}
                        }
                    }
                }
                MangaTags(manga)
            }
        } else {
            Column(modifier = Modifier
                .heightIn(min=450.dp)
                .padding(8.dp)
            ) {

                Row {
                    when(contentType) {
                        is ContentType.Online, ContentType.Logged, ContentType.MDList -> {
                            KamelImage(
                                resource = asyncPainterResource(data = Url(manga.coverImageUrl!!)),
                                contentDescription = "Manga cover image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .height(450.dp)
                                    .aspectRatio(0.703f)
                            )
                        }
                        is ContentType.Downloaded -> {
                            Image(
                                bitmap = ImageStorage().convertToImageBitmap(manga.coverImage!!),
                                contentDescription = "Manga cover image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .height(450.dp)
                                    .aspectRatio(0.703f)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .background(MangaDexTheme.color.primary)

                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceAround,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .heightIn(min = 418.dp),
                        ) {
                            Text(
                                text = manga.title,
                                style = MangaDexTheme.typography.titleLarge
                            )

                            Text(
                                text = manga.description,
                                style = MangaDexTheme.typography.body
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Author: ${manga.author}",
                                style = MangaDexTheme.typography.labelLarge
                            )

                            when(contentType){
                                is ContentType.Online -> DownloadButton(component)
                                is ContentType.Downloaded -> ContinueReadingButton(component)
                                is ContentType.Logged, ContentType.MDList -> {
                                    Column{
                                        ContinueReadingButton(component)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        DownloadButton(component)
                                    }
                                }
                            }
                        }
                    }
                }
                MangaTags(manga)
            }
        }
    }
}

@Composable
fun DownloadButton(component: MangaComponent) {
    val state by component.downloadState
    val progress by component.progress

    Box(
        modifier = Modifier.padding(top = 24.dp)
    ){
        Button(
            onClick = { component.downloadAll() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MangaDexTheme.color.mainButton,
                contentColor = MangaDexTheme.color.white
            )
        ) {
            Text("Download All")
        }
//        when (state) {
//            is DownloadState.Idle -> {
//                Button(
//                    onClick = { component.downloadAll() },
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = MangaDexTheme.color.mainButton,
//                        contentColor = MangaDexTheme.color.white
//                    )
//                ) {
//                    Text("Download All")
//                }
//            }
//            is DownloadState.Downloading -> {
//                CircularProgressIndicator(
//                    progress = {progress},
//                    color = MangaDexTheme.color.mainButton
//                )
//            }
//            is DownloadState.Completed -> {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.Center,
//                ){
//                    Text("Manga downloaded", color = MangaDexTheme.color.white, style = MangaDexTheme.typography.body)
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Icon(painter = painterResource(Res.drawable.ic_done), contentDescription = "Download completed", tint = MangaDexTheme.color.white, modifier = Modifier.size(36.dp))
//                }
//            }
//            is DownloadState.Error -> Text("Error: ${(state as DownloadState.Error).message}", color = Color.Red)
//        }
    }
}

@Composable
fun ContinueReadingButton(
    component: MangaComponent
) {
    val lastRead = component.model.subscribeAsState().value.lastReadChapter

    if(lastRead != null) {
        Button(
            onClick = { component.continueReading() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MangaDexTheme.color.mainButton,
                contentColor = MangaDexTheme.color.white
            ),
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Continue Reading")
        }
    }
}

@Composable
fun getContentRatingColor(contentRating: String): Color {
    return when (contentRating.lowercase()) {
        "safe" -> Color(0xFF339900)
        "suggestive" -> Color(0xFFFFCC00)
        "erotica" -> Color(0xFFFF0000)
        "pornographic" -> Color(0xFF000000)
        else -> MangaDexTheme.color.primary
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MangaTags(
    manga: Manga,
) {
    Spacer(modifier = Modifier.height(16.dp))

    Column{
        Column {
            var color: Color = MangaDexTheme.color.white

            when(manga.status) {
                "ongoing" -> color = MangaDexTheme.color.statusOnGoing
                "completed" -> color = MangaDexTheme.color.statusCompleted
                "hiatus" -> color = MangaDexTheme.color.statusHiatus
                "cancelled" -> color = MangaDexTheme.color.statusCancelled
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ){
                Icon(painter = painterResource(Res.drawable.ic_circle), contentDescription = "Status", tint = color, modifier = Modifier.size(14.dp))
                Text("Publication: ${manga.year}, ${manga.status}", style = MangaDexTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Content Rating & Demographics:",
                style = MangaDexTheme.typography.titleNormal
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier
                    .background(getContentRatingColor(manga.contentRating)),
                    contentAlignment = Alignment.Center
                ){
                    Text(
                        text = manga.contentRating,
                        style = MangaDexTheme.typography.labelLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(modifier = Modifier
                    .background(MangaDexTheme.color.primary),
                    contentAlignment = Alignment.Center
                ){
                    Text(
                        text = manga.publicationDemographic,
                        style = MangaDexTheme.typography.labelLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Genres:",
                style = MangaDexTheme.typography.titleNormal
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                manga.genres.forEach {genre ->
                    Box(modifier = Modifier
                        .background(MangaDexTheme.color.primary),
                        contentAlignment = Alignment.Center
                    ){
                        Text(
                            text = genre,
                            style = MangaDexTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Original language",
                style = MangaDexTheme.typography.titleNormal
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box(modifier = Modifier
                    .background(MangaDexTheme.color.primary),
                    contentAlignment = Alignment.Center
                ){
                    Text(
                        text = manga.originalLanguage.toLanguageName(),
                        style = MangaDexTheme.typography.labelLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterItemView(
    chapter: Chapter,
    component: MangaComponent,
    onChapterClicked: (Chapter) -> Unit
) {
    val chapterIndicator = component.model.subscribeAsState().value.chapterDownloadIndicators.find { it.chapterId == chapter.id }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .padding(8.dp)
            .background(MangaDexTheme.color.primary)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable { onChapterClicked(chapter) }
                .padding(end = 55.dp)
                .weight(9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {

                if (chapter.volume.isNullOrEmpty() || chapter.volume == "") {
                    Text(
                        text = "Chapter ${chapter.chapter}",
                        style = MangaDexTheme.typography.labelLarge
                    )
                } else {
                    Text(
                        text = "Chapter ${chapter.chapter}, Volume: ${chapter.volume}",
                        style = MangaDexTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (component.model.value.contentType is ContentType.Downloaded || component.isUserLogged.value) {
                        IconButton(
                            onClick = { component.changeReadChapter(chapter) },
                            modifier = Modifier.size(MangaDexTheme.size.medium)
                        ) {
                            if (chapter.read) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_check),
                                    contentDescription = "Chapter read",
                                    tint = MangaDexTheme.color.mainButton,
                                    modifier = Modifier.size(MangaDexTheme.size.medium)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_check),
                                    contentDescription = "Chapter unread",
                                    tint = MangaDexTheme.color.lightGray,
                                    modifier = Modifier.size(MangaDexTheme.size.medium)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (chapter.title.isNullOrEmpty() || chapter.title == "") {
                        Text(
                            text = "Unknown title",
                            style = MangaDexTheme.typography.titleNormal
                        )
                    } else {
                        Text(
                            text = chapter.title,
                            style = MangaDexTheme.typography.titleNormal
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Pages: ${chapter.pageNumbers}",
                    style = MangaDexTheme.typography.body
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Scanlation group: ${chapter.scanlationGroup}",
                    style = MangaDexTheme.typography.labelNormal
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(55.dp)
                .weight(1f)
        ){
            when(component.model.value.contentType) {
                is ContentType.Online, ContentType.Logged, ContentType.MDList -> {
                    when(chapterIndicator!!.downloadState){
                        is DownloadState.Idle -> {
                            IconButton(
                                onClick = {
                                    component.downloadChapter(chapter)
                                },
                                modifier = Modifier.size(55.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_download),
                                    contentDescription = "Download",
                                    tint = MangaDexTheme.color.white,
                                    modifier = Modifier.size(55.dp)
                                )
                            }
                        }
                        is DownloadState.Downloading -> {
                            CircularProgressIndicator(color = MangaDexTheme.color.mainButton)
                        }
                        is DownloadState.Completed -> {
                            Icon(
                                painter = painterResource(Res.drawable.ic_done),
                                contentDescription = "Download completed",
                                tint = MangaDexTheme.color.white,
                                modifier = Modifier.size(55.dp)
                            )
                        }
                        is DownloadState.Error -> {
                            Text("Error: ${(chapterIndicator.downloadState as DownloadState.Error).message}", color = Color.Red)
                        }
                    }
                }
                is ContentType.Downloaded -> {
                    IconButton(
                        onClick = {
                            component.deleteChapter(chapter)
                        },
                        modifier = Modifier .size(45.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_delete),
                            contentDescription = "Delete download",
                            tint = MangaDexTheme.color.white,
                            modifier = Modifier.size(55.dp)
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
}