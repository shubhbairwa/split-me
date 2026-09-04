package com.shubh.splitme.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val PureWhite = Color(0xFFFFFFFF)
val PureBlack = Color(0xFF000000)

// White background, elevated cards, monochromatic (black/gray) text + orange accent.
val AppBackground = Color(0xFFFFFFFF)
val CardWhite = Color(0xFFFFFFFF)
val AccentOrange = Color(0xFFFFA733)
val AccentOrangeDeep = Color(0xFFFF7A45)
val TextPrimary = Color(0xFF161616)
val TextSecondary = Color(0xFF6E6E73)
val TrackGray = Color(0xFFF1F1F3)
val OutlineGray = Color(0xFFE4E4E7)

val PositiveGreen = Color(0xFF4CAF50)
val NegativeRed = Color(0xFFF44336)

// Light Palette
val PrimaryLight = AccentOrange
val OnPrimaryLight = PureWhite
val PrimaryContainerLight = Color(0xFFFFE3C2)
val OnPrimaryContainerLight = Color(0xFF7A4A00)

val SecondaryLight = TextSecondary
val OnSecondaryLight = PureWhite
val SecondaryContainerLight = TrackGray
val OnSecondaryContainerLight = TextPrimary

val TertiaryLight = TextSecondary
val OnTertiaryLight = PureWhite

val ErrorLight = NegativeRed
val OnErrorLight = PureWhite

val BackgroundLight = AppBackground
val OnBackgroundLight = TextPrimary
val SurfaceLight = CardWhite
val OnSurfaceLight = TextPrimary
val SurfaceVariantLight = TrackGray
val OnSurfaceVariantLight = TextSecondary

// Dark Palette (Defaults to light as requested)
val PrimaryDark = AccentOrange
val OnPrimaryDark = PureWhite
val BackgroundDark = AppBackground
val OnBackgroundDark = TextPrimary
val SurfaceDark = CardWhite
val OnSurfaceDark = TextPrimary

/** Icon + badge color for a bill category, used across Dashboard/GroupDetail/BillEntry. */
data class CategoryStyle(val icon: ImageVector, val badgeColor: Color)

val DefaultCategoryStyle = CategoryStyle(Icons.Filled.Category, Color(0xFF26A69A))

val CategoryStyles: Map<String, CategoryStyle> = mapOf(
    "Food" to CategoryStyle(Icons.Filled.Restaurant, Color(0xFFFF7043)),
    "Transport" to CategoryStyle(Icons.Filled.DirectionsCar, Color(0xFF42A5F5)),
    "Shopping" to CategoryStyle(Icons.Filled.ShoppingBag, Color(0xFFAB47BC)),
    "Entertainment" to CategoryStyle(Icons.Filled.Movie, Color(0xFFEC407A)),
    "General" to DefaultCategoryStyle
)

fun categoryStyleFor(category: String?): CategoryStyle =
    CategoryStyles[category?.ifBlank { "General" } ?: "General"] ?: DefaultCategoryStyle
