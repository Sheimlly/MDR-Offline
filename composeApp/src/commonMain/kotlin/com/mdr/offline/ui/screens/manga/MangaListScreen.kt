package com.mdr.offline.ui.screens.manga

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.mdr.offline.ImageStorage
import com.mdr.offline.data.ContentType
import com.mdr.offline.mangas.application.Manga
import com.mdr.offline.ui.MangaDexTheme
import com.mdr.offline.ui.navigation.manga.MangaListComponent
import com.mdr.offline.ui.screens.elements.ErrorMessage
import com.mdr.offline.ui.screens.elements.LoadingScreen
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mangadexoffline.composeapp.generated.resources.Res
import mangadexoffline.composeapp.generated.resources.ic_clear
import mangadexoffline.composeapp.generated.resources.ic_close
import mangadexoffline.composeapp.generated.resources.ic_delete
import mangadexoffline.composeapp.generated.resources.ic_menu
import org.jetbrains.compose.resources.painterResource

@Composable
fun MangaListScreen(
    component: MangaListComponent
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val contentType = component.model.subscribeAsState().value.contentType
    val isUserLogged = component.isUserLogged.subscribeAsState().value

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        ModalNavigationDrawer(
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        drawerContainerColor = MangaDexTheme.color.primary,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Menu",
                                        modifier = Modifier.padding(16.dp),
                                        style = MangaDexTheme.typography.titleLarge
                                    )

                                    IconButton(
                                        onClick = { scope.launch { drawerState.close() } }
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_close),
                                            contentDescription = "Close menu",
                                            tint = MangaDexTheme.color.white
                                        )
                                    }
                                }
                                HorizontalDivider()

                                when (contentType) {
                                    is ContentType.Online, ContentType.Logged -> {
                                        NavigationDrawerItem(
                                            label = {
                                                Text(
                                                    text = "Downloads",
                                                    style = MangaDexTheme.typography.labelLarge
                                                )
                                            },
                                            selected = false,
                                            onClick = {
                                                scope.launch{ drawerState.close() }
                                                component.switchMangasType(ContentType.Downloaded)
                                            },
                                            colors = NavigationDrawerItemDefaults.colors(
                                                selectedIconColor = MangaDexTheme.color.white,
                                                unselectedIconColor = MangaDexTheme.color.white,
                                                selectedTextColor = MangaDexTheme.color.white,
                                                unselectedTextColor = MangaDexTheme.color.white,
                                            ),
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                    }

                                    is ContentType.Downloaded -> {
                                        NavigationDrawerItem(
                                            label = {
                                                Text(
                                                    text = "Home",
                                                    style = MangaDexTheme.typography.labelLarge
                                                )
                                            },
                                            selected = false,
                                            onClick = {
                                                scope.launch{ drawerState.close() }
                                                component.switchMangasType(ContentType.Online)
                                            },
                                            colors = NavigationDrawerItemDefaults.colors(
                                                selectedIconColor = MangaDexTheme.color.white,
                                                unselectedIconColor = MangaDexTheme.color.white,
                                                selectedTextColor = MangaDexTheme.color.white,
                                                unselectedTextColor = MangaDexTheme.color.white,
                                            ),
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                    }
                                    // There is better way to do it so I'll do it later
                                    is ContentType.MDList -> {
                                        NavigationDrawerItem(
                                            label = {
                                                Text(
                                                    text = "Downloads",
                                                    style = MangaDexTheme.typography.labelLarge
                                                )
                                            },
                                            selected = false,
                                            onClick = {
                                                scope.launch{ drawerState.close() }
                                                component.switchMangasType(ContentType.Downloaded)
                                            },
                                            colors = NavigationDrawerItemDefaults.colors(
                                                selectedIconColor = MangaDexTheme.color.white,
                                                unselectedIconColor = MangaDexTheme.color.white,
                                                selectedTextColor = MangaDexTheme.color.white,
                                                unselectedTextColor = MangaDexTheme.color.white,
                                            ),
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                        NavigationDrawerItem(
                                            label = {
                                                Text(
                                                    text = "Home",
                                                    style = MangaDexTheme.typography.labelLarge
                                                )
                                            },
                                            selected = false,
                                            onClick = {
                                                scope.launch{ drawerState.close() }
                                                component.switchMangasType(ContentType.Online)
                                            },
                                            colors = NavigationDrawerItemDefaults.colors(
                                                selectedIconColor = MangaDexTheme.color.white,
                                                unselectedIconColor = MangaDexTheme.color.white,
                                                selectedTextColor = MangaDexTheme.color.white,
                                                unselectedTextColor = MangaDexTheme.color.white,
                                            ),
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                    }
                                }

                                if(isUserLogged && contentType !is ContentType.MDList ) {
                                    NavigationDrawerItem(
                                        label = {
                                            Text(
                                                text = "MDLists",
                                                style = MangaDexTheme.typography.labelLarge
                                            )
                                        },
                                        selected = false,
                                        onClick = { component.navigate("mdlists") },
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedIconColor = MangaDexTheme.color.white,
                                            unselectedIconColor = MangaDexTheme.color.white,
                                            selectedTextColor = MangaDexTheme.color.white,
                                            unselectedTextColor = MangaDexTheme.color.white,
                                        ),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                }

                                HorizontalDivider()

                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            text = "About app",
                                            style = MangaDexTheme.typography.labelLarge
                                        )
                                    },
                                    selected = false,
                                    onClick = { component.navigate("aboutapp") },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedIconColor = MangaDexTheme.color.white,
                                        unselectedIconColor = MangaDexTheme.color.white,
                                        selectedTextColor = MangaDexTheme.color.white,
                                        unselectedTextColor = MangaDexTheme.color.white,
                                    ),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                if(!isUserLogged) {
                                    Button(
                                        onClick = { component.navigate("login") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MangaDexTheme.color.mainButton,
                                            contentColor = MangaDexTheme.color.white
                                        )
                                    ) {
                                        Text("Sign in")
                                    }
                                } else {
                                    Text(
                                        modifier = Modifier.clickable {
                                            component.signOut()
                                        },
                                        text = "Log out",
                                        style = MangaDexTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            drawerState = drawerState,
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                val focusManager = LocalFocusManager.current

                Column(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus()
                            })
                        }
                ) {
                    val mangaComponent = component.model.subscribeAsState()
                    val scrollState = rememberLazyGridState()

                    AppBar(component, drawerState)

                    if (mangaComponent.value.error != null)
                        ErrorMessage(mangaComponent.value.error!!)
                    if (mangaComponent.value.loading)
                        LoadingScreen()
                    else MangasListView(component, mangaComponent, scrollState)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    component: MangaListComponent,
    drawerState: DrawerState,
    scope: CoroutineScope = rememberCoroutineScope()
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val contentType = component.model.subscribeAsState().value.contentType

    TopAppBar(
        {
            when(contentType) {
                is ContentType.Online, is ContentType.Logged -> {
                    Row {
                        TextField(
                            modifier = Modifier
                                .padding(vertical = 2.dp),
                            value = component.mangaSearch.value,
                            onValueChange = { component.onMangaSearchChange(it) },
                            placeholder = {
                                Text(text = "Search manga")
                            },
                            colors = TextFieldDefaults.textFieldColors(
                                focusedIndicatorColor = MangaDexTheme.color.mainButton,
                                unfocusedIndicatorColor = MangaDexTheme.color.mainButton,
                                textColor = MangaDexTheme.color.white,
                                backgroundColor = Color.Transparent,
                                cursorColor = MangaDexTheme.color.white,
                            ),
                            trailingIcon = {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    IconButton(
                                        onClick = {component.onMangaSearchChange("")},
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_close),
                                            contentDescription = "Clear search",
                                            tint = MangaDexTheme.color.lightGray
                                        )
                                    }

//                                    IconButton(onClick = {
//                                        keyboardController?.hide()
//                                        focusManager.clearFocus()
//                                        component.getMangas(forceFetch = true)
//                                    }) {
//                                        Icon(
//                                            imageVector = Icons.Filled.Search,
//                                            contentDescription = "Search manga"
//                                        )
//                                    }
                                }
                            },
                            maxLines = 1,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                component.getMangas(forceFetch = true)
                            }),
                        )
                    }
                }
                is ContentType.Downloaded -> Text(text = "Downloaded Mangas")
                is ContentType.MDList -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ){
                        IconButton(
                            onClick = { component.navigate("mdlists") },
                            modifier = Modifier.padding(end = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Up Button",
                            )
                        }

                        Text("MDLists")
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MangaDexTheme.color.primary,
            titleContentColor = MangaDexTheme.color.white,
            navigationIconContentColor = MangaDexTheme.color.white,
            actionIconContentColor = MangaDexTheme.color.white
        ),
        actions = {
            IconButton( onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                scope.launch { drawerState.open() }
            }) {
                Icon(
                    painter = painterResource(Res.drawable.ic_menu),
                    contentDescription = "Menu"
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MangasListView(
    component: MangaListComponent,
    mangaModel: State<MangaListComponent.Model>,
    scrollState: LazyGridState
) {
    val state = rememberPullRefreshState(
        refreshing = mangaModel.value.loading,
        onRefresh = {component.getMangas(forceFetch = true)}
    )

    val focusManager = LocalFocusManager.current

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                focusManager.clearFocus()

                return Offset.Zero
            }
        }
    }


    Box( modifier = Modifier.pullRefresh(state = state) ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 256.dp),
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            items(mangaModel.value.mangas) { manga ->
                MangaItemView(
                    manga = manga,
                    component = component,
                    contentType = component.model.value.contentType,
                    onMangaClicked = { component.onMangaClicked(it) }
                )
            }
        }

        if(component.model.value.contentType is ContentType.Online) {
            PullRefreshIndicator(
                refreshing = mangaModel.value.loading,
                state = state,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun MangaItemView(
    manga: Manga,
    component: MangaListComponent,
    contentType: ContentType,
    onMangaClicked: (Manga) -> Unit,
) {

//    val image = ImageStorage().convertToImageBitmap(article.image)

    val m = component.model.subscribeAsState().value.mangas.filter { it.id == manga.id }[0]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .background(MangaDexTheme.color.primary)
            .heightIn(min = 500.dp)
            .clickable {
                onMangaClicked(manga)
            }
    ) {
        when(contentType){
            is ContentType.Online, ContentType.Logged, ContentType.MDList -> {
                if(m.coverImageUrl != null) {
                    KamelImage(
                        resource = asyncPainterResource(data = Url(m.coverImageUrl!!)),
                        contentDescription = "Manga cover image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.aspectRatio(0.703f)
                    )
                } else {
                    LoadingScreen()
                }
            }
            is ContentType.Downloaded -> {
                if(m.coverImage != null) {
                    Box {
                        Image(
                            bitmap = ImageStorage().convertToImageBitmap(m.coverImage),
                            contentDescription = "Manga cover image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.aspectRatio(0.703f)
                        )
                        IconButton(
                            onClick = { component.deleteManga(manga.id) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_delete),
                                contentDescription = "Delete download",
                                tint = MangaDexTheme.color.white
                            )
                        }
                    }
                } else {
                    LoadingScreen()
                }

            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                text = manga.title,
                style = MangaDexTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Status: ${manga.status}",
                style = MangaDexTheme.typography.body
            )
        }
        Spacer(modifier = Modifier.height(50.dp))
    }
}

val LazyGridState.isScrolled: Boolean
    get() = firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0