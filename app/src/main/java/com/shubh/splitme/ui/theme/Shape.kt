package com.shubh.splitme.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Material elevation for cards stays 0: its shadow would show through the translucent glass.
 * Cards get their lift from Modifier.glassShadow (Glass.kt), drawn only outside the card shape.
 */
val CardElevation = 0.dp
