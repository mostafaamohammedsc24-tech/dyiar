package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.remember
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
 * and allows tapping any room to inspect paint code & flooring specs.
 */
@Composable
fun ArchitecturalFloorPlanCanvas(
    rooms: List<RoomSpec>,
    totalWidthM: Double,
    totalLengthM: Double,
    selectedRoomIndex: Int,
    onSelectRoom: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val primaryBlue = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val blueprintBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)

    data class RoomRect(val index: Int, val left: Float, val top: Float, val w: Float, val h: Float)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(blueprintBg)
            .border(1.5.dp, primaryBlue.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .testTag("architectural_floor_plan_canvas")
    ) {
        val boxW = with(LocalDensity.current) { maxWidth.toPx() }
        val boxH = with(LocalDensity.current) { maxHeight.toPx() }
        val pad = with(LocalDensity.current) { 22.dp.toPx() }

        val roomRects = remember(rooms, boxW, boxH) {
            if (rooms.isEmpty()) emptyList()
            else {
                val innerW = (boxW - pad * 2f).coerceAtLeast(10f)
                val innerH = (boxH - pad * 2f).coerceAtLeast(10f)
                val cols = if (rooms.size <= 2) rooms.size else if (rooms.size <= 4) 2 else 3
                val rows = (rooms.size + cols - 1) / cols
                val cellW = innerW / cols
                val cellH = innerH / rows
                rooms.mapIndexed { idx, _ ->
                    val r = idx / cols
                    val c = idx % cols
                    RoomRect(
                        index = idx,
                        left = pad + c * cellW + 4f,
                        top = pad + r * cellH + 4f,
                        w = cellW - 8f,
                        h = cellH - 8f
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(roomRects) {
                    detectTapGestures { offset ->
                        val hit = roomRects.firstOrNull { rect ->
                            offset.x in rect.left..(rect.left + rect.w) &&
                                offset.y in rect.top..(rect.top + rect.h)
                        }
                        if (hit != null) onSelectRoom(hit.index)
                    }
                }
        ) {
            // Blueprint grid lines
            val step = 28.dp.toPx()
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
                color = primaryBlue.copy(alpha = 0.65f),
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
                    color = paintTint.copy(alpha = if (isSelected) 0.92f else 0.65f),
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.w, rect.h)
                )

                // Wall outline
                drawRect(
                    color = if (isSelected) primaryBlue else primaryBlue.copy(alpha = 0.55f),
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.w, rect.h),
                    style = Stroke(width = if (isSelected) 3.dp.toPx() else 1.5.dp.toPx())
                )

                // Architectural door swing arc indicator at bottom-left of room
                drawArc(
                    color = primaryBlue.copy(alpha = 0.6f),
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(rect.left + 8f, rect.top + rect.h - 24f),
                    size = Size(20f, 20f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Room text overlays aligned over rects
        val density = LocalDensity.current
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
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (strings.isArabic) room.nameAr else room.nameEn,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${room.widthM}×${room.lengthM}${strings.meterUnit} (${room.areaSqm().roundToInt()} ${strings.areaUnit})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1E3A5F),
                        maxLines = 1
                    )
                }
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
