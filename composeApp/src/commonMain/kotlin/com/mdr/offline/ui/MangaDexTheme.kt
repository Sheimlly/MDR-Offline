package com.mdr.offline.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val colorScheme = MangaDexColorScheme(
    background = Color(0xFF191A1C),
    primary = Color(0xFF2C2C2C),
    secondary = Color(0xFF2E2A2E),
    mainButton = Color(0xFFFF6740),
    white = Color(0xFFFFFFFF),
    lightGray = Color(0xFF4F4F4F),
    statusCompleted = Color(0xFF00C9f5),
    statusOnGoing = Color(0xFF04D000),
    statusHiatus = Color(0xFFDA7500),
    statusCancelled = Color(0xFFFF4040),
)

private val typography = MangaDexTypography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = Color(0xFFFFFFFF)
    ),
    titleNormal = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = Color(0xFFFFFFFF)
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = Color(0xFFFFFFFF)
    ),
    body = TextStyle(
        fontSize = 16.sp,
        color = Color(0xFFFFFFFF)
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = Color(0xFFFFFFFF)
    ),
    labelNormal = TextStyle(
        fontSize = 14.sp,
        color = Color(0xFFFFFFFF)
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        color = Color(0xFFFFFFFF)
    )
)

private val shape = MangaDexShape(
    container = RoundedCornerShape(12.dp),
    button = RoundedCornerShape(50)
)

private val size = MangaDexSize(
    large = 24.dp,
    medium = 16.dp,
    normal = 14.dp,
    small = 12.dp
)


@Composable
fun MangaDexTheme(
    content: @Composable () -> Unit
) {

    CompositionLocalProvider(
        LocalMangaDexColorScheme provides colorScheme,
        LocalMangaDexTypography provides typography,
        LocalMangaDexShape provides shape,
        LocalMangaDexSize provides size,
        content = content
    )
}

object MangaDexTheme {
    val color: MangaDexColorScheme
        @Composable get() = LocalMangaDexColorScheme.current

    val typography: MangaDexTypography
        @Composable get() = LocalMangaDexTypography.current

    val shape: MangaDexShape
        @Composable get() = LocalMangaDexShape.current

    val size: MangaDexSize
        @Composable get() = LocalMangaDexSize.current
}
