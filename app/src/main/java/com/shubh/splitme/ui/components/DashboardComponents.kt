package com.shubh.splitme.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shubh.splitme.ui.dashboard.ChartPoint
import com.shubh.splitme.ui.theme.AccentOrange
import com.shubh.splitme.ui.theme.AccentOrangeDeep
import com.shubh.splitme.ui.theme.CardElevation
import com.shubh.splitme.ui.theme.CardWhite
import com.shubh.splitme.ui.theme.CategoryStyle
import com.shubh.splitme.ui.theme.OutlineGray
import com.shubh.splitme.ui.theme.Poppins
import com.shubh.splitme.ui.theme.PureWhite
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary
import com.shubh.splitme.ui.theme.TrackGray
import kotlin.math.roundToInt

/** Hashes a name into a soft, stable avatar color. */
fun colorForName(name: String): Color {
    val hash = name.hashCode()
    return Color(
        red = (hash and 0xFF0000 shr 16) % 200 + 50,
        green = (hash and 0x00FF00 shr 8) % 200 + 50,
        blue = (hash and 0x0000FF) % 200 + 50,
        alpha = 255
    )
}

@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
    color: Color = colorForName(name)
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 2.5).sp
        )
    }
}

/** Pill-shaped segmented control with an animated sliding highlight (Day/Week/Month/Year style). */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(TrackGray)
    ) {
        val tabWidth = maxWidth / options.size
        val offset by animateDpAsState(targetValue = tabWidth * selectedIndex, label = "segmentedTabOffset")

        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(tabWidth)
                .fillMaxHeight()
                .padding(3.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(AccentOrange, AccentOrangeDeep)))
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (index == selectedIndex) PureWhite else TextSecondary
                    )
                }
            }
        }
    }
}

private fun nearestIndex(x: Float, width: Float, count: Int): Int {
    if (count <= 1) return 0
    val step = width / (count - 1)
    val idx = (x / step).roundToInt()
    return idx.coerceIn(0, count - 1)
}

/** Custom-drawn gradient area/line chart with a drag/tap tooltip, matching the reference design. */
@Composable
fun SpendLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    valueFormatter: (Double) -> String = { "%.2f".format(it) }
) {
    if (points.size < 2) {
        Box(
            modifier = modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Not enough data yet", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember(points) { mutableStateOf(points.size - 1) }
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
    val maxValue = points.maxOf { it.value }.let { if (it <= 0.0) 1.0 else it }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        selectedIndex = nearestIndex(offset.x, size.width.toFloat(), points.size)
                    }
                }
                .pointerInput(points) {
                    detectDragGestures { change, _ ->
                        selectedIndex = nearestIndex(change.position.x, size.width.toFloat(), points.size)
                    }
                }
        ) {
            val topReserve = 34.dp.toPx()
            val chartHeight = size.height - topReserve
            val chartWidth = size.width
            val stepX = chartWidth / (points.size - 1)

            fun xFor(i: Int) = stepX * i
            fun yFor(value: Double): Float {
                val ratio = (value / maxValue).toFloat().coerceIn(0f, 1f)
                return topReserve + chartHeight * (1f - ratio)
            }

            val coords = points.mapIndexed { i, p -> Offset(xFor(i), yFor(p.value)) }

            val linePath = Path()
            val fillPath = Path()
            coords.forEachIndexed { i, c ->
                if (i == 0) {
                    linePath.moveTo(c.x, c.y)
                    fillPath.moveTo(c.x, topReserve + chartHeight)
                    fillPath.lineTo(c.x, c.y)
                } else {
                    val prev = coords[i - 1]
                    val midX = (prev.x + c.x) / 2f
                    linePath.cubicTo(midX, prev.y, midX, c.y, c.x, c.y)
                    fillPath.cubicTo(midX, prev.y, midX, c.y, c.x, c.y)
                }
            }
            fillPath.lineTo(coords.last().x, topReserve + chartHeight)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(AccentOrange.copy(alpha = 0.35f), Color.Transparent),
                    startY = topReserve,
                    endY = topReserve + chartHeight
                )
            )
            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(listOf(AccentOrange, AccentOrangeDeep)),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            val selected = coords.getOrNull(selectedIndex)
            if (selected != null) {
                drawLine(
                    color = AccentOrangeDeep.copy(alpha = 0.4f),
                    start = Offset(selected.x, selected.y),
                    end = Offset(selected.x, topReserve + chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
                drawCircle(color = PureWhite, radius = 6.dp.toPx(), center = selected)
                drawCircle(
                    color = AccentOrangeDeep,
                    radius = 6.dp.toPx(),
                    center = selected,
                    style = Stroke(width = 2.dp.toPx())
                )

                val label = valueFormatter(points[selectedIndex].value)
                val textLayout = textMeasurer.measure(
                    AnnotatedString(label),
                    style = TextStyle(fontFamily = Poppins, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                val paddingH = 8.dp.toPx()
                val paddingV = 5.dp.toPx()
                val bubbleWidth = textLayout.size.width + paddingH * 2
                val bubbleHeight = textLayout.size.height + paddingV * 2
                val bubbleX = (selected.x - bubbleWidth / 2).coerceIn(0f, chartWidth - bubbleWidth)
                val bubbleY = (selected.y - bubbleHeight - 10.dp.toPx()).coerceAtLeast(0f)

                drawRoundRect(
                    color = PureWhite,
                    topLeft = Offset(bubbleX, bubbleY),
                    size = Size(bubbleWidth, bubbleHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                drawRoundRect(
                    color = OutlineGray,
                    topLeft = Offset(bubbleX, bubbleY),
                    size = Size(bubbleWidth, bubbleHeight),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(bubbleX + paddingH, bubbleY + paddingV)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { p ->
                Text(p.label, style = labelStyle, maxLines = 1)
            }
        }
    }
}

@Composable
fun CategorySpendCard(
    style: CategoryStyle,
    category: String,
    percent: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(style.badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(style.icon, contentDescription = null, tint = style.badgeColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${percent.roundToInt()}%",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(category, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
fun BalanceHeaderCard(
    label: String,
    amountText: String,
    modifier: Modifier = Modifier,
    amountColor: Color = TextPrimary,
    trailing: (@Composable () -> Unit)? = null,
    subContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(amountText, style = MaterialTheme.typography.headlineLarge, color = amountColor)
                }
                trailing?.invoke()
            }
            if (subContent != null) {
                Spacer(modifier = Modifier.height(16.dp))
                subContent()
            }
        }
    }
}
