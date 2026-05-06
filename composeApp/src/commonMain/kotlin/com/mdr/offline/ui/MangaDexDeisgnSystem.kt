package com.mdr.offline.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

data class MangaDexColorScheme(
    val background: Color,
    val primary: Color,
    val secondary: Color,
    val mainButton: Color,
    val white: Color,
    val lightGray: Color,
    val statusCompleted: Color,
    val statusOnGoing: Color,
    val statusHiatus: Color,
    val statusCancelled: Color,
)

data class MangaDexTypography(
    val titleLarge: TextStyle,
    val titleNormal: TextStyle,
    val titleSmall: TextStyle,
    val body: TextStyle,
    val labelLarge: TextStyle,
    val labelNormal: TextStyle,
    val labelSmall: TextStyle
)

data class MangaDexShape(
    val container: Shape,
    val button: Shape
)

data class MangaDexSize(
    val large: Dp,
    val medium: Dp,
    val normal: Dp,
    val small: Dp
)

val LocalMangaDexColorScheme = staticCompositionLocalOf {
    MangaDexColorScheme(
        background = Color.Unspecified,
        primary = Color.Unspecified,
        secondary = Color.Unspecified,
        mainButton = Color.Unspecified,
        white = Color(0xFFFFFFFF),
        lightGray = Color(0xFF4F4F4F),
        statusCompleted = Color(0xFF00C9f5),
        statusOnGoing = Color(0xFF04D000),
        statusHiatus = Color(0xFFDA7500),
        statusCancelled = Color(0xFFFF4040),
    )
}

val LocalMangaDexTypography = staticCompositionLocalOf {
    MangaDexTypography(
        titleLarge = TextStyle.Default,
        titleNormal = TextStyle.Default,
        titleSmall = TextStyle.Default,
        body = TextStyle.Default,
        labelLarge = TextStyle.Default,
        labelNormal = TextStyle.Default,
        labelSmall = TextStyle.Default
    )
}

val LocalMangaDexShape = staticCompositionLocalOf {
    MangaDexShape(
        container = RectangleShape,
        button = RectangleShape
    )
}

val LocalMangaDexSize = staticCompositionLocalOf {
    MangaDexSize(
        large = Dp.Unspecified,
        medium = Dp.Unspecified,
        normal = Dp.Unspecified,
        small = Dp.Unspecified
    )
}