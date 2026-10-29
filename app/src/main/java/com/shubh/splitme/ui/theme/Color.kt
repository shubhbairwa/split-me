package com.shubh.splitme.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val PureWhite = Color(0xFFFFFFFF)
val PureBlack = Color(0xFF000000)

// Brand accent, the same in light and dark mode.
val AccentOrange = Color(0xFFFFA733)
val AccentOrangeDeep = Color(0xFFFF7A45)

val PositiveGreen = Color(0xFF4CAF50)
val NegativeRed = Color(0xFFF44336)

// Light palette: white background, elevated cards, black/gray text + orange accent.
private val LightBackgroundRaw = Color(0xFFFFFFFF)
private val LightSurfaceRaw = Color(0xFFFFFFFF)
private val LightTextPrimaryRaw = Color(0xFF161616)
private val LightTextSecondaryRaw = Color(0xFF6E6E73)
private val LightTrackRaw = Color(0xFFF1F1F3)
private val LightOutlineRaw = Color(0xFFE4E4E7)

val PrimaryLight = AccentOrange
val OnPrimaryLight = PureWhite
val PrimaryContainerLight = Color(0xFFFFE3C2)
val OnPrimaryContainerLight = Color(0xFF7A4A00)
val SecondaryLight = LightTextSecondaryRaw
val OnSecondaryLight = PureWhite
val SecondaryContainerLight = LightTrackRaw
val OnSecondaryContainerLight = LightTextPrimaryRaw
val TertiaryLight = LightTextSecondaryRaw
val OnTertiaryLight = PureWhite
val ErrorLight = NegativeRed
val OnErrorLight = PureWhite
val BackgroundLight = LightBackgroundRaw
val OnBackgroundLight = LightTextPrimaryRaw
val SurfaceLight = LightSurfaceRaw
val OnSurfaceLight = LightTextPrimaryRaw
val SurfaceVariantLight = LightTrackRaw
val OnSurfaceVariantLight = LightTextSecondaryRaw
val OutlineLight = Color(0xFFC9C9CE)
val OutlineVariantLight = LightOutlineRaw

// Dark palette: near-black background, slightly lighter cards, light text, same orange accent.
val PrimaryDark = AccentOrange
val OnPrimaryDark = PureWhite
val PrimaryContainerDark = Color(0xFF5A3600)
val OnPrimaryContainerDark = Color(0xFFFFDDB3)
val SecondaryDark = Color(0xFFA1A1A6)
val OnSecondaryDark = Color(0xFF161616)
val SecondaryContainerDark = Color(0xFF2C2C2E)
val OnSecondaryContainerDark = Color(0xFFF2F2F2)
val TertiaryDark = Color(0xFFA1A1A6)
val OnTertiaryDark = Color(0xFF161616)
val ErrorDark = Color(0xFFFF6B5E)
val OnErrorDark = Color(0xFF161616)
val BackgroundDark = Color(0xFF121212)
val OnBackgroundDark = Color(0xFFF2F2F2)
val SurfaceDark = Color(0xFF1E1E20)
val OnSurfaceDark = Color(0xFFF2F2F2)
val SurfaceVariantDark = Color(0xFF2C2C2E)
val OnSurfaceVariantDark = Color(0xFFA1A1A6)
val OutlineDark = Color(0xFF5A5A60)
val OutlineVariantDark = Color(0xFF3A3A3C)

// Theme-aware aliases used across the screens. They read the active MaterialTheme
// color scheme, so they switch automatically between light and dark mode.
val AppBackground: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
val CardWhite: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
val TextPrimary: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
val TextSecondary: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val TrackGray: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
val OutlineGray: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant

/** Icon + badge color for a bill category, used across Dashboard/GroupDetail/BillEntry. */
data class CategoryStyle(val icon: ImageVector, val badgeColor: Color)

val DefaultCategoryStyle = CategoryStyle(Icons.Outlined.Category, Color(0xFF26A69A))

val CategoryStyles: Map<String, CategoryStyle> = mapOf(
    "Food" to CategoryStyle(Icons.Outlined.Restaurant, Color(0xFFFF7043)),
    "Transport" to CategoryStyle(Icons.Outlined.DirectionsCar, Color(0xFF42A5F5)),
    "Shopping" to CategoryStyle(Icons.Outlined.ShoppingBag, Color(0xFFAB47BC)),
    "Entertainment" to CategoryStyle(Icons.Outlined.Movie, Color(0xFFEC407A)),
    "General" to DefaultCategoryStyle
)

fun categoryStyleFor(category: String?): CategoryStyle =
    CategoryStyles[category?.ifBlank { "General" } ?: "General"] ?: DefaultCategoryStyle
