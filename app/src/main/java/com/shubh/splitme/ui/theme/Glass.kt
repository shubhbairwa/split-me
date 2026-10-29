package com.shubh.splitme.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Colors for the "glass" look: translucent frosted panels over a soft, colorful backdrop.
 * One set for light mode and one for dark mode, provided by [SplitMeTheme].
 */
@Immutable
data class GlassColors(
    /** Base backdrop gradient, top to bottom. */
    val backdropTop: Color,
    val backdropBottom: Color,
    /** Soft color blobs drawn on the backdrop; they show through the glass panels. */
    val blobs: List<Color>,
    /** Fill for cards and other panels. */
    val surface: Color,
    /** More opaque fill for bars that content scrolls under (bottom navigation). */
    val surfaceStrong: Color,
    /** Fill for a highlighted / selected panel. */
    val surfaceSelected: Color,
    /** Hairline edge: bright at the top-left, fading toward the bottom-right, like light on glass. */
    val edgeHighlight: Color,
    val edgeShadow: Color,
    /** Drop shadow color under raised glass panels. */
    val shadow: Color,
    /** Tint laid over the blurred content in the bottom navigation bar. */
    val navTint: Color
)

val LightGlassColors = GlassColors(
    backdropTop = Color(0xFFFFF6EC),
    backdropBottom = Color(0xFFF3F1FA),
    blobs = listOf(
        Color(0xFFFFC27A), // orange, the brand accent
        Color(0xFFFFB3C7), // pink
        Color(0xFFB9D7FF)  // blue
    ),
    surface = Color.White.copy(alpha = 0.62f),
    surfaceStrong = Color.White.copy(alpha = 0.80f),
    surfaceSelected = AccentOrange.copy(alpha = 0.22f),
    edgeHighlight = Color.White.copy(alpha = 0.90f),
    edgeShadow = Color.White.copy(alpha = 0.25f),
    shadow = Color(0xFF6B4A2E).copy(alpha = 0.18f),
    navTint = Color.White.copy(alpha = 0.55f)
)

val DarkGlassColors = GlassColors(
    backdropTop = Color(0xFF15121A),
    backdropBottom = Color(0xFF0C0C10),
    blobs = listOf(
        Color(0xFF8A4B12), // deep orange
        Color(0xFF5B2350), // plum
        Color(0xFF1C3D66)  // navy
    ),
    surface = Color.White.copy(alpha = 0.07f),
    surfaceStrong = Color(0xFF1A1820).copy(alpha = 0.88f),
    surfaceSelected = AccentOrange.copy(alpha = 0.22f),
    edgeHighlight = Color.White.copy(alpha = 0.22f),
    edgeShadow = Color.White.copy(alpha = 0.04f),
    shadow = Color.Black.copy(alpha = 0.55f),
    navTint = Color(0xFF1A1820).copy(alpha = 0.55f)
)

val LocalGlassColors = staticCompositionLocalOf { LightGlassColors }

/**
 * Height a screen must keep free at its bottom for the floating bottom bar (0 when there is none).
 * Tab screens pass [tabScreenInsets] to their Scaffold and add the Scaffold's bottom padding as
 * list content padding, so content scrolls under the glass bar instead of stopping above it.
 */
val LocalBottomBarSpace = compositionLocalOf { 0.dp }

/** Scaffold insets for a screen shown under the floating bottom bar. */
@Composable
fun tabScreenInsets(): WindowInsets {
    val space = LocalBottomBarSpace.current
    return if (space == 0.dp) ScaffoldDefaults.contentWindowInsets
    else WindowInsets.statusBars.add(WindowInsets(bottom = space))
}

/** The active glass colors (light or dark). */
val glass: GlassColors
    @Composable @ReadOnlyComposable get() = LocalGlassColors.current

/** Card colors for a glass panel. Pass `selected = true` for a highlighted card. */
@Composable
fun glassCardColors(selected: Boolean = false): CardColors = CardDefaults.cardColors(
    containerColor = if (selected) glass.surfaceSelected else glass.surface,
    // The translucent glass colors aren't theme colors, so set the text color explicitly
    // (otherwise it falls back to black, even in dark mode).
    contentColor = MaterialTheme.colorScheme.onSurface
)

/** The light-catching hairline edge for glass panels. */
@Composable
fun glassBorder(width: Dp = 1.dp): BorderStroke = BorderStroke(
    width,
    Brush.linearGradient(listOf(glass.edgeHighlight, glass.edgeShadow))
)

/** Glass fill + edge for things that aren't Cards (bars, tracks, chips). */
@Composable
fun Modifier.glassPanel(shape: Shape, strong: Boolean = false): Modifier {
    val colors = glass
    return this
        .clip(shape)
        .background(if (strong) colors.surfaceStrong else colors.surface, shape)
        .border(glassBorder(), shape)
}

/**
 * Soft drop shadow for a glass panel, drawn only outside [shape] so it never shows through the
 * translucent fill. Needs Android 9+ for blurred shadows; older versions simply draw no shadow.
 */
@Composable
fun Modifier.glassShadow(shape: Shape, blur: Dp = 18.dp, offsetY: Dp = 6.dp): Modifier {
    val shadowColor = glass.shadow
    return this.drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }
        clipPath(path, clipOp = ClipOp.Difference) {
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.WHITE
                    setShadowLayer(blur.toPx(), 0f, offsetY.toPx(), shadowColor.toArgb())
                }
                canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
            }
        }
    }
}

/** Full-screen backdrop: a soft gradient with large blurred-looking color blobs. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = glass
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.backdropTop, colors.backdropBottom)))
            .drawBehind {
                // Radial gradients fading to transparent read as out-of-focus light behind the glass,
                // without needing a real blur (which isn't available below Android 12).
                val w = size.width
                val h = size.height
                val spots = listOf(
                    Triple(Offset(w * 0.05f, h * 0.10f), w * 0.75f, 0),
                    Triple(Offset(w * 0.95f, h * 0.42f), w * 0.70f, 1),
                    Triple(Offset(w * 0.20f, h * 0.88f), w * 0.80f, 2)
                )
                spots.forEach { (center, radius, i) ->
                    val c = colors.blobs[i % colors.blobs.size]
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(c.copy(alpha = 0.70f), c.copy(alpha = 0.25f), Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
            },
        content = content
    )
}
