package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.RoomSpec
import com.example.data.model.TourScene
import com.example.i18n.LocalStrings
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

fun resolvePresetDrawable(presetKey: String): Int {
    return when (presetKey) {
        "penthouse_exterior" -> R.drawable.img_penthouse_exterior
        "pano_living" -> R.drawable.img_pano_living
        "pano_kitchen" -> R.drawable.img_pano_kitchen
        "services_hero" -> R.drawable.img_services_hero
        else -> R.drawable.img_villa_exterior
    }
}

fun formatCurrencyPrice(price: Double, currency: String, isRent: Boolean, isArabic: Boolean): String {
    val nf = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
    val numStr = nf.format(price)
    val curr = when {
        isArabic && (currency == "IQD" || currency == "SAR") -> "د.ع"
        currency == "SAR" -> "IQD"
        else -> currency
    }
    val suffix = if (isRent) (if (isArabic) " /سنوياً" else " /yr") else ""
    return "$numStr $curr$suffix"
}

fun formatCompactPrice(price: Double, isArabic: Boolean): String {
    return when {
        price >= 1_000_000_000 -> {
            val b = price / 1_000_000_000.0
            val formatted = String.format(Locale.US, "%.2f", b).trimEnd('0').trimEnd('.')
            if (isArabic) "${formatted}مليار" else "${formatted}B"
        }
        price >= 1_000_000 -> {
            val m = price / 1_000_000.0
            val formatted = String.format(Locale.US, "%.1f", m).trimEnd('0').trimEnd('.')
            if (isArabic) "${formatted}م" else "${formatted}M"
        }
        price >= 1_000 -> {
            val k = (price / 1_000.0).roundToInt()
            if (isArabic) "${k}ألف" else "${k}K"
        }
        else -> price.roundToInt().toString()
    }
}

fun parseColorHex(hex: String, fallback: Color = Color(0xFFE8EEF5)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun PropertyHeroImage(
    presetKey: String,
    customUri: String = "",
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (customUri.isNotBlank()) {
        AsyncImage(
            model = Uri.parse(customUri),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = resolvePresetDrawable(presetKey)),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

/**
 * Interactive 360° Panoramic Room Explorer.
 * Delegates to the dedicated [Panorama360Viewer] component with pan, pitch tilt,
 * pinch-to-zoom, HUD zoom controls, and seamless spherical wraparound.
 */
@Composable
fun VirtualTour360Viewer(
    scenes: List<TourScene>,
    activeSceneIndex: Int,
    onSelectScene: (Int) -> Unit,
    onExpandFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Panorama360Viewer(
        scenes = scenes,
        activeSceneIndex = activeSceneIndex,
        onSelectScene = onSelectScene,
        onExpandFullscreen = onExpandFullscreen,
        modifier = modifier
    )
}

/**
 * Interactive Architectural Blueprint Canvas.
 * Renders scaled rooms, walls, dimensions in meters, paint color tints,
 * supports normalized drawn/split coordinates and drag-to-draw rectangle creation.
 */
@Composable
fun ArchitecturalFloorPlanCanvas(
    rooms: List<RoomSpec>,
    totalWidthM: Double,
    totalLengthM: Double,
    selectedRoomIndex: Int,
    onSelectRoom: (Int) -> Unit,
    drawModeEnabled: Boolean = false,
    onDrawRoomRect: ((Float, Float, Float, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val blueprintBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)

    data class RoomRect(val index: Int, val left: Float, val top: Float, val w: Float, val h: Float)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(270.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(blueprintBg)
            .border(
                width = if (drawModeEnabled) 2.dp else 1.5.dp,
                color = if (drawModeEnabled) primaryColor else primaryColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("architectural_floor_plan_canvas")
    ) {
        val density = LocalDensity.current
        val boxW = with(density) { maxWidth.toPx() }
        val boxH = with(density) { maxHeight.toPx() }
        val pad = with(density) { 22.dp.toPx() }
        val innerW = (boxW - pad * 2f).coerceAtLeast(10f)
        val innerH = (boxH - pad * 2f).coerceAtLeast(10f)

        var dragStart by remember { mutableStateOf<Offset?>(null) }
        var dragCurrent by remember { mutableStateOf<Offset?>(null) }

        val roomRects = remember(rooms, boxW, boxH) {
            if (rooms.isEmpty()) emptyList()
            else {
                val unpackedIndices = rooms.mapIndexedNotNull { idx, r ->
                    if (r.xNorm >= 0.0 && r.wNorm > 0.0 && r.hNorm > 0.0) null else idx
                }
                val cols = if (unpackedIndices.size <= 2) unpackedIndices.size.coerceAtLeast(1)
                else if (unpackedIndices.size <= 4) 2 else 3
                val rows = ((unpackedIndices.size + cols - 1) / cols).coerceAtLeast(1)
                val cellW = innerW / cols
                val cellH = innerH / rows

                rooms.mapIndexed { idx, room ->
                    if (room.xNorm >= 0.0 && room.wNorm > 0.0 && room.hNorm > 0.0) {
                        val xN = room.xNorm.toFloat().coerceIn(0f, 0.94f)
                        val yN = room.yNorm.toFloat().coerceIn(0f, 0.94f)
                        val wN = room.wNorm.toFloat().coerceIn(0.06f, 1f - xN)
                        val hN = room.hNorm.toFloat().coerceIn(0.06f, 1f - yN)
                        RoomRect(
                            index = idx,
                            left = pad + xN * innerW + 2f,
                            top = pad + yN * innerH + 2f,
                            w = (wN * innerW - 4f).coerceAtLeast(12f),
                            h = (hN * innerH - 4f).coerceAtLeast(12f)
                        )
                    } else {
                        val gridPos = unpackedIndices.indexOf(idx).coerceAtLeast(0)
                        val r = gridPos / cols
                        val c = gridPos % cols
                        RoomRect(
                            index = idx,
                            left = pad + c * cellW + 4f,
                            top = pad + r * cellH + 4f,
                            w = (cellW - 8f).coerceAtLeast(12f),
                            h = (cellH - 8f).coerceAtLeast(12f)
                        )
                    }
                }
            }
        }

        fun clampInsideInner(offset: Offset): Offset {
            return Offset(
                x = offset.x.coerceIn(pad, pad + innerW),
                y = offset.y.coerceIn(pad, pad + innerH)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(drawModeEnabled, roomRects, innerW, innerH) {
                    if (drawModeEnabled && onDrawRoomRect != null) {
                        detectDragGestures(
                            onDragStart = { start ->
                                val clamped = clampInsideInner(start)
                                dragStart = clamped
                                dragCurrent = clamped
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                dragCurrent = clampInsideInner(change.position)
                            },
                            onDragEnd = {
                                val s = dragStart
                                val e = dragCurrent
                                dragStart = null
                                dragCurrent = null
                                if (s != null && e != null) {
                                    val leftPx = minOf(s.x, e.x)
                                    val topPx = minOf(s.y, e.y)
                                    val wPx = abs(e.x - s.x)
                                    val hPx = abs(e.y - s.y)
                                    val xN = ((leftPx - pad) / innerW).coerceIn(0f, 0.94f)
                                    val yN = ((topPx - pad) / innerH).coerceIn(0f, 0.94f)
                                    val wN = (wPx / innerW).coerceIn(0f, 1f - xN)
                                    val hN = (hPx / innerH).coerceIn(0f, 1f - yN)
                                    if (wN >= 0.06f && hN >= 0.06f) {
                                        onDrawRoomRect(xN, yN, wN, hN)
                                    }
                                }
                            },
                            onDragCancel = {
                                dragStart = null
                                dragCurrent = null
                            }
                        )
                    } else {
                        detectTapGestures { offset ->
                            val hit = roomRects.lastOrNull { rect ->
                                offset.x in rect.left..(rect.left + rect.w) &&
                                    offset.y in rect.top..(rect.top + rect.h)
                            }
                            if (hit != null) onSelectRoom(hit.index)
                        }
                    }
                }
        ) {
            // Blueprint grid lines
            val step = 26.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }

            // Outer structural perimeter with architectural dash
            drawRect(
                color = primaryColor.copy(alpha = 0.65f),
                topLeft = Offset(pad - 4f, pad - 4f),
                size = Size(size.width - (pad - 4f) * 2f, size.height - (pad - 4f) * 2f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                )
            )

            // Rooms
            roomRects.forEach { rect ->
                val room = rooms[rect.index]
                val isSelected = rect.index == selectedRoomIndex
                val paintTint = parseColorHex(room.paintHex, surfaceColor)

                drawRect(
                    color = paintTint.copy(alpha = if (isSelected) 0.94f else 0.72f),
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.w, rect.h)
                )

                // Wall outline
                drawRect(
                    color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.58f),
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.w, rect.h),
                    style = Stroke(width = if (isSelected) 3.dp.toPx() else 1.5.dp.toPx())
                )

                // Architectural door swing arc indicator scaled to room size
                val arcSize = min(20f, min(rect.w, rect.h) * 0.35f).coerceAtLeast(8f)
                if (rect.w > 22f && rect.h > 22f) {
                    drawArc(
                        color = primaryColor.copy(alpha = 0.6f),
                        startAngle = 270f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(rect.left + 6f, rect.top + rect.h - arcSize - 4f),
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // Live Drag-to-Draw Rectangle Overlay
            val dStart = dragStart
            val dCur = dragCurrent
            if (dStart != null && dCur != null) {
                val left = minOf(dStart.x, dCur.x)
                val top = minOf(dStart.y, dCur.y)
                val w = abs(dCur.x - dStart.x)
                val h = abs(dCur.y - dStart.y)
                drawRect(
                    color = primaryColor.copy(alpha = 0.28f),
                    topLeft = Offset(left, top),
                    size = Size(w, h)
                )
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                )
            }
        }

        // Room text overlays aligned over rects
        roomRects.forEach { rect ->
            val room = rooms[rect.index]
            val isSelected = rect.index == selectedRoomIndex
            val xDp = with(density) { rect.left.toDp() }
            val yDp = with(density) { rect.top.toDp() }
            val wDp = with(density) { rect.w.toDp() }
            val hDp = with(density) { rect.h.toDp() }

            Box(
                modifier = Modifier
                    .offset(x = xDp, y = yDp)
                    .size(width = wDp, height = hDp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (strings.isArabic) room.nameAr else room.nameEn,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${room.widthM}×${room.lengthM}${strings.meterUnit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1E3A5F),
                        maxLines = 1
                    )
                }
            }
        }

        // Live Dimension Pill while Dragging
        val dStart = dragStart
        val dCur = dragCurrent
        if (dStart != null && dCur != null) {
            val wM = (((abs(dCur.x - dStart.x) / innerW) * totalWidthM) * 10.0).roundToInt() / 10.0
            val lM = (((abs(dCur.y - dStart.y) / innerH) * totalLengthM) * 10.0).roundToInt() / 10.0
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "${wM.coerceAtLeast(0.5)} × ${lM.coerceAtLeast(0.5)} ${strings.meterUnit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Total plot footprint badge top-start & North indicator top-end
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = "${strings.plotDimensionsLabel}: ${totalWidthM}×${totalLengthM}${strings.meterUnit}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "N",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
