package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.TourScene
import com.example.i18n.LocalStrings
import com.example.ui.theme.DiyarRoyalBlue
import com.example.ui.theme.DiyarSkyBlue
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Dedicated Compose 360-degree Spherical Panorama Viewer.
 * Supports smooth 360° horizontal yaw pan, vertical pitch tilt, pinch-to-zoom,
 * double-tap zoom, HUD +/- zoom controls, auto-orbit, and seamless 3x spherical wrapping
 * for both uploaded equirectangular/spherical images (content/http URIs) and preset scenes.
 */
@Composable
fun Panorama360Viewer(
    scenes: List<TourScene>,
    activeSceneIndex: Int,
    onSelectScene: (Int) -> Unit,
    onExpandFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val currentScene = scenes.getOrNull(activeSceneIndex) ?: return

    var manualYawDegrees by remember(activeSceneIndex, currentScene.customPhotoUri) {
        mutableFloatStateOf(currentScene.initialYaw.toFloat())
    }
    var pitchOffsetFraction by remember(activeSceneIndex, currentScene.customPhotoUri) {
        mutableFloatStateOf(0f)
    }
    var targetZoom by remember(activeSceneIndex, currentScene.customPhotoUri) {
        mutableFloatStateOf(1.0f)
    }
    var autoOrbit by remember(currentScene.customPhotoUri) {
        mutableStateOf(true)
    }

    val animatedZoom by animateFloatAsState(
        targetValue = targetZoom,
        animationSpec = spring(),
        label = "panoZoom"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "tour360Orbit")
    val autoDelta by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )

    val effectiveYaw = ((manualYawDegrees + (if (autoOrbit) autoDelta else 0f)) % 360f + 360f) % 360f
    val pitchDegrees = (pitchOffsetFraction * 45f).roundToInt()

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF070D17))
            .testTag("virtual_tour_360_viewport")
    ) {
        val density = LocalDensity.current
        val viewportWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
        val viewportHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
        // Each 360° spherical strip spans 2x viewport width for wide immersive field of view
        val sphereStripWidthPx = viewportWidthPx * 2f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeSceneIndex, currentScene.customPhotoUri) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        autoOrbit = false
                        val nextZoom = (targetZoom * zoomChange).coerceIn(1.0f, 3.2f)
                        targetZoom = nextZoom

                        // Pan yaw smoothly across 360° (adjusted by zoom sensitivity)
                        val yawDelta = -(pan.x / (viewportWidthPx * nextZoom)) * 180f
                        manualYawDegrees += yawDelta

                        // Allow vertical pitch tilt especially when zoomed in or exploring spherical poles
                        val maxPitchFrac = if (nextZoom > 1.05f) 0.48f else 0.16f
                        val pitchDelta = (pan.y / viewportHeightPx) * 0.65f
                        pitchOffsetFraction = (pitchOffsetFraction + pitchDelta).coerceIn(-maxPitchFrac, maxPitchFrac)
                    }
                }
                .pointerInput(activeSceneIndex, currentScene.customPhotoUri) {
                    detectTapGestures(
                        onDoubleTap = {
                            autoOrbit = false
                            if (targetZoom > 1.25f) {
                                targetZoom = 1.0f
                                pitchOffsetFraction = 0f
                            } else {
                                targetZoom = 2.0f
                            }
                        }
                    )
                }
        ) {
            val normalizedYaw = effectiveYaw / 360f
            val baseShiftPx = -(normalizedYaw * sphereStripWidthPx)
            val verticalShiftPx = pitchOffsetFraction * viewportHeightPx * 0.45f
            val effectiveScale = animatedZoom * 1.18f

            // Seamless 2-tile loop with unbounded width so 360° wraparound never clamps
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = effectiveScale
                        scaleY = effectiveScale
                        translationY = verticalShiftPx
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .wrapContentWidth(
                            align = Alignment.Start,
                            unbounded = true
                        )
                        .offset { IntOffset(baseShiftPx.roundToInt(), 0) }
                ) {
                    repeat(2) { copyIdx ->
                        SphericalTileImage(
                            scene = currentScene,
                            contentDescription = if (copyIdx == 0) {
                                if (strings.isArabic) currentScene.titleAr else currentScene.titleEn
                            } else null,
                            modifier = Modifier
                                .width(with(density) { sphereStripWidthPx.toDp() })
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }

        // Subtle vignette & spherical horizon compass guide
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.44f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.68f)
                        )
                    )
                )
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        ) {
            val ticks = 24
            val step = size.width / ticks
            for (i in 0..ticks) {
                val x = ((i * step) - (effectiveYaw / 360f) * size.width + size.width) % size.width
                val tall = i % 3 == 0
                drawLine(
                    color = Color.White.copy(alpha = if (tall) 0.45f else 0.2f),
                    start = Offset(x, 0f),
                    end = Offset(x, if (tall) 8.dp.toPx() else 4.dp.toPx()),
                    strokeWidth = if (tall) 2f else 1f
                )
            }
        }

        // Top HUD: 360° Azimuth + Pitch + Zoom Indicator & Action Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xCC0D1B2A),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, DiyarSkyBlue.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = strings.tour360Badge,
                        tint = DiyarSkyBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${effectiveYaw.roundToInt()}° • ${if (pitchDegrees >= 0) "+$pitchDegrees" else "$pitchDegrees"}°",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = DiyarRoyalBlue.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", targetZoom)}×",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HudCircleButton(
                    icon = if (autoOrbit) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = strings.autoOrbit,
                    testTag = "tour_auto_orbit_toggle",
                    onClick = { autoOrbit = !autoOrbit }
                )
                HudCircleButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = strings.resetView,
                    testTag = "tour_reset_view",
                    onClick = {
                        autoOrbit = false
                        manualYawDegrees = currentScene.initialYaw.toFloat()
                        pitchOffsetFraction = 0f
                        targetZoom = 1.0f
                    }
                )
                if (onExpandFullscreen != null) {
                    Surface(
                        color = DiyarRoyalBlue.copy(alpha = 0.92f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onExpandFullscreen)
                            .testTag("tour_fullscreen_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = strings.launch360Tour,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Right-Edge Vertical Zoom Controls (+ / -) for accessible pan & zoom
        Surface(
            color = Color(0xCC0D1B2A),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable {
                            autoOrbit = false
                            targetZoom = (targetZoom + 0.4f).coerceAtMost(3.2f)
                        }
                        .testTag("tour_zoom_in"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.zoomIn,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable {
                            autoOrbit = false
                            val next = (targetZoom - 0.4f).coerceAtLeast(1.0f)
                            targetZoom = next
                            if (next <= 1.05f) pitchOffsetFraction = 0f
                        }
                        .testTag("tour_zoom_out"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = strings.zoomOut,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom HUD: Gestures Hint + Interactive Hotspot Room Switcher
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            Text(
                text = strings.pinchOrDrag360,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            if (scenes.size > 1 || scenes.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    scenes.forEachIndexed { index, scene ->
                        val selected = index == activeSceneIndex
                        Surface(
                            color = if (selected) DiyarRoyalBlue else Color(0xB30F172A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selected) DiyarSkyBlue else Color.White.copy(alpha = 0.28f)
                            ),
                            modifier = Modifier
                                .clickable { onSelectScene(index) }
                                .testTag("tour_scene_pill_$index")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = if (selected) Color.White else DiyarSkyBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (strings.isArabic) scene.titleAr else scene.titleEn,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SphericalTileImage(
    scene: TourScene,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    if (scene.customPhotoUri.isNotBlank()) {
        AsyncImage(
            model = Uri.parse(scene.customPhotoUri),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = resolvePresetDrawable(scene.presetKey)),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
private fun HudCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xCC0D1B2A),
        shape = CircleShape,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
