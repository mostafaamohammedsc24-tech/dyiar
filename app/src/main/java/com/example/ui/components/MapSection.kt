package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLngBounds
import kotlin.math.roundToInt
import com.example.BuildConfig
import com.example.data.model.DiyarMapType
import com.example.data.model.MapPointOfInterest
import com.example.data.model.NearbyLandmark
import com.example.data.model.PropertyListing
import com.example.i18n.LocalStrings
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DiyarActionBlue
import com.example.ui.theme.MortgageCyan
import com.example.ui.theme.RentViolet
import com.example.ui.theme.RestaurantOrange
import com.example.ui.theme.TealLandmark
import com.example.ui.theme.VerifiedEmerald
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private const val DARK_MAP_STYLE_JSON = """
[
  {"elementType":"geometry","stylers":[{"color":"#11151f"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#94a3b8"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#0b0e14"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#1e2536"}]},
  {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#2a344c"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#090d14"}]},
  {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#132421"}]}
]
"""

private const val DARK_HIDE_POI_STYLE_JSON = """
[
  {"elementType":"geometry","stylers":[{"color":"#11151f"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#94a3b8"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#0b0e14"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#1e2536"}]},
  {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#2a344c"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#090d14"}]},
  {"featureType":"poi","stylers":[{"visibility":"off"}]}
]
"""

private const val LIGHT_HIDE_POI_STYLE_JSON = """
[
  {"featureType":"poi","stylers":[{"visibility":"off"}]}
]
"""

fun hasConfiguredMapsApiKey(): Boolean {
    val key = BuildConfig.MAPS_API_KEY
    return key.isNotBlank() && key != "YOUR_GOOGLE_MAPS_API_KEY"
}

@Composable
fun DiyarInteractiveMap(
    properties: List<PropertyListing>,
    pointsOfInterest: List<MapPointOfInterest>,
    selectedPropertyId: String?,
    centerLat: Double,
    centerLng: Double,
    radiusKm: Double,
    showRadiusCircle: Boolean,
    isDark: Boolean,
    useGoogleMapTiles: Boolean,
    mapType: DiyarMapType,
    onSelectProperty: (PropertyListing) -> Unit,
    onSelectPoi: (MapPointOfInterest) -> Unit,
    onClearSelection: () -> Unit = {},
    isExplicitFocus: Boolean = false,
    showPois: Boolean = false,
    targetZoom: Float? = null,
    onZoomConsumed: () -> Unit = {},
    onCameraChanged: (Double, Double, Float) -> Unit = { _, _, _ -> },
    viewportPadding: PaddingValues = PaddingValues(top = 48.dp, start = 64.dp, end = 12.dp, bottom = 68.dp),
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    if (useGoogleMapTiles) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 11.4f)
        }
        val boundsSignature = remember(properties) {
            properties.joinToString(",") { it.id }
        }
        LaunchedEffect(centerLat, centerLng, isExplicitFocus, boundsSignature) {
            try {
                if (isExplicitFocus || properties.isEmpty()) {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(centerLat, centerLng),
                            if (isExplicitFocus) 13.0f else 11.4f
                        ),
                        durationMs = 500
                    )
                } else if (properties.size == 1) {
                    val only = properties.first()
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(LatLng(only.latitude, only.longitude), 13.2f),
                        durationMs = 500
                    )
                } else {
                    val builder = LatLngBounds.Builder()
                    properties.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(builder.build(), 64),
                        durationMs = 550
                    )
                }
            } catch (_: Exception) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLng), 11.4f),
                    durationMs = 450
                )
            }
        }

        LaunchedEffect(cameraPositionState.isMoving) {
            if (!cameraPositionState.isMoving) {
                val pos = cameraPositionState.position
                onCameraChanged(pos.target.latitude, pos.target.longitude, pos.zoom)
            }
        }

        val liveZoomedThreshold = cameraPositionState.position.zoom >= 13.5f
        LaunchedEffect(liveZoomedThreshold) {
            val pos = cameraPositionState.position
            onCameraChanged(pos.target.latitude, pos.target.longitude, pos.zoom)
        }

        LaunchedEffect(targetZoom) {
            if (targetZoom != null) {
                val dest = if (isExplicitFocus) {
                    LatLng(centerLat, centerLng)
                } else {
                    cameraPositionState.position.target
                }
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(dest, targetZoom),
                        durationMs = 450
                    )
                } catch (_: Exception) {
                }
                onZoomConsumed()
            }
        }

        val googleMapType = when (mapType) {
            DiyarMapType.NORMAL -> MapType.NORMAL
            DiyarMapType.SATELLITE -> MapType.SATELLITE
            DiyarMapType.HYBRID -> MapType.HYBRID
            DiyarMapType.TERRAIN -> MapType.TERRAIN
        }

        val effectiveShowPois = showPois || liveZoomedThreshold
        val mapProperties = remember(isDark, googleMapType, effectiveShowPois) {
            val styleJson = when {
                googleMapType != MapType.NORMAL -> null
                isDark && !effectiveShowPois -> DARK_HIDE_POI_STYLE_JSON
                isDark && effectiveShowPois -> DARK_MAP_STYLE_JSON
                !isDark && !effectiveShowPois -> LIGHT_HIDE_POI_STYLE_JSON
                else -> null
            }
            MapProperties(
                mapType = googleMapType,
                mapStyleOptions = styleJson?.let { MapStyleOptions(it) }
            )
        }
        val uiSettings = remember {
            MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            )
        }

        Box(modifier = modifier.clipToBounds().testTag("google_map_container")) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings,
                contentPadding = viewportPadding,
                onMapClick = { onClearSelection() }
            ) {
                if (showRadiusCircle) {
                    Circle(
                        center = LatLng(centerLat, centerLng),
                        radius = radiusKm * 1000.0,
                        fillColor = DiyarActionBlue.copy(alpha = 0.08f),
                        strokeColor = DiyarActionBlue.copy(alpha = 0.65f),
                        strokeWidth = 3f
                    )
                }

                // Points of Interest markers appear ONLY when zoomed in
                if (effectiveShowPois) {
                    pointsOfInterest.forEach { poi ->
                        MarkerComposable(
                            keys = arrayOf<Any>(poi.id, poi.category),
                            state = MarkerState(position = LatLng(poi.latitude, poi.longitude)),
                            onClick = {
                                onSelectPoi(poi)
                                true
                            }
                        ) {
                            LandmarkPinBadge(category = poi.category)
                        }
                    }
                }

                // Property Listing Markers with distinct Sale / Rent / Commercial / Land icons
                properties.forEach { prop ->
                    val isSelected = prop.id == selectedPropertyId
                    MarkerComposable(
                        keys = arrayOf<Any>(prop.id, isSelected, strings.isArabic, prop.listingType, prop.propertyType),
                        state = MarkerState(position = LatLng(prop.latitude, prop.longitude)),
                        onClick = {
                            onSelectProperty(prop)
                            true
                        }
                    ) {
                        PricePinBadge(
                            priceText = formatCompactPrice(prop.price, strings.isArabic),
                            listingType = prop.listingType,
                            propertyType = prop.propertyType,
                            selected = isSelected,
                            verified = prop.verified
                        )
                    }
                }
            }

            if (!hasConfiguredMapsApiKey()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp),
                    tonalElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, end = 72.dp, bottom = 14.dp)
                ) {
                    Text(
                        text = strings.mapsKeyNotice,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    } else {
        VectorGeoMap(
            properties = properties,
            pointsOfInterest = pointsOfInterest,
            selectedPropertyId = selectedPropertyId,
            centerLat = centerLat,
            centerLng = centerLng,
            isDark = isDark,
            mapType = mapType,
            showPois = showPois,
            targetZoom = targetZoom,
            onZoomConsumed = onZoomConsumed,
            onCameraChanged = onCameraChanged,
            viewportPadding = viewportPadding,
            onSelectProperty = onSelectProperty,
            onSelectPoi = onSelectPoi,
            onClearSelection = onClearSelection,
            modifier = modifier
        )
    }
}

@Composable
private fun VectorGeoMap(
    properties: List<PropertyListing>,
    pointsOfInterest: List<MapPointOfInterest>,
    selectedPropertyId: String?,
    centerLat: Double,
    centerLng: Double,
    isDark: Boolean,
    mapType: DiyarMapType,
    showPois: Boolean,
    targetZoom: Float?,
    onZoomConsumed: () -> Unit,
    onCameraChanged: (Double, Double, Float) -> Unit,
    viewportPadding: PaddingValues,
    onSelectProperty: (PropertyListing) -> Unit,
    onSelectPoi: (MapPointOfInterest) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val layoutDir = LocalLayoutDirection.current
    val density = LocalDensity.current
    val isSatView = mapType == DiyarMapType.SATELLITE || mapType == DiyarMapType.HYBRID
    val bgTop = when {
        isSatView -> Color(0xFF141B18)
        isDark -> Color(0xFF0E121B)
        else -> Color(0xFFF3F5F8)
    }
    val bgBottom = when {
        isSatView -> Color(0xFF1D2621)
        isDark -> Color(0xFF131926)
        else -> Color(0xFFE9EDF2)
    }
    val roadColor = when {
        isSatView -> Color(0xFF2D3831)
        isDark -> Color(0xFF21293B)
        else -> Color(0xFFFFFFFF)
    }
    val gridColor = when {
        isSatView -> Color(0xFF222B26)
        isDark -> Color(0xFF181F2E)
        else -> Color(0xFFDDE3EC)
    }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(centerLat, centerLng, properties.size) {
        if (targetZoom == null) {
            zoomScale = 1f
            panOffset = Offset.Zero
        }
    }

    LaunchedEffect(targetZoom) {
        if (targetZoom != null) {
            zoomScale = if (targetZoom >= 13.5f) 1.55f else 1.0f
            panOffset = Offset.Zero
            onCameraChanged(centerLat, centerLng, targetZoom)
            onZoomConsumed()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClearSelection() })
            }
            .pointerInput(centerLat, centerLng) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextZoom = (zoomScale * zoom).coerceIn(0.85f, 2.6f)
                    val maxPanX = size.width * 0.45f * nextZoom
                    val maxPanY = size.height * 0.45f * nextZoom
                    zoomScale = nextZoom
                    panOffset = Offset(
                        x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                        y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                    )
                    val simulatedZoom = if (nextZoom >= 1.35f) 14.2f else 11.4f
                    onCameraChanged(centerLat, centerLng, simulatedZoom)
                }
            }
            .testTag("vector_geo_map")
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val padLeftPx = with(density) { viewportPadding.calculateLeftPadding(layoutDir).toPx() }
        val padRightPx = with(density) { viewportPadding.calculateRightPadding(layoutDir).toPx() }
        val padTopPx = with(density) { viewportPadding.calculateTopPadding().toPx() }
        val padBottomPx = with(density) { viewportPadding.calculateBottomPadding().toPx() }

        val poiHalfPx = with(density) { 15.dp.toPx() }
        val pinHalfWidthPx = with(density) { 46.dp.toPx() }
        val pinHalfHeightPx = with(density) { 16.dp.toPx() }

        val safeMinX = (padLeftPx + 8f).coerceAtMost(widthPx * 0.4f)
        val safeMaxX = (widthPx - padRightPx - 8f).coerceAtLeast(safeMinX + 40f)
        val safeMinY = (padTopPx + 8f).coerceAtMost(heightPx * 0.4f)
        val safeMaxY = (heightPx - padBottomPx - 8f).coerceAtLeast(safeMinY + 40f)
        val safeSpanX = (safeMaxX - safeMinX).coerceAtLeast(1f)
        val safeSpanY = (safeMaxY - safeMinY).coerceAtLeast(1f)
        val safeCenterX = (safeMinX + safeMaxX) / 2f
        val safeCenterY = (safeMinY + safeMaxY) / 2f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = (44.dp.toPx() * zoomScale).coerceAtLeast(18f)
            val startX = ((panOffset.x % step) + step) % step - step
            var x = startX
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            val startY = ((panOffset.y % step) + step) % step - step
            var y = startY
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
            // Major arterial corridors scaled with zoom & pan
            drawLine(
                color = roadColor,
                start = Offset(0f, size.height * 0.34f + panOffset.y),
                end = Offset(size.width, size.height * 0.56f + panOffset.y),
                strokeWidth = (6.dp.toPx() * zoomScale).coerceIn(4f, 14f)
            )
            drawLine(
                color = roadColor,
                start = Offset(size.width * 0.46f + panOffset.x, 0f),
                end = Offset(size.width * 0.54f + panOffset.x, size.height),
                strokeWidth = (7.dp.toPx() * zoomScale).coerceIn(5f, 16f)
            )
            val circleCenter = Offset(safeCenterX + panOffset.x, safeCenterY + panOffset.y)
            val circleRadius = (minOf(safeSpanX, safeSpanY) * 0.42f * zoomScale).coerceAtLeast(24f)
            drawCircle(
                color = DiyarActionBlue.copy(alpha = 0.06f),
                radius = circleRadius,
                center = circleCenter
            )
            drawCircle(
                color = DiyarActionBlue.copy(alpha = 0.35f),
                radius = circleRadius,
                center = circleCenter,
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            )
        }

        val lats = properties.map { it.latitude } + pointsOfInterest.map { it.latitude } + centerLat
        val lngs = properties.map { it.longitude } + pointsOfInterest.map { it.longitude } + centerLng
        val minLat = (lats.minOrNull() ?: 33.27) - 0.015
        val maxLat = (lats.maxOrNull() ?: 33.36) + 0.015
        val minLng = (lngs.minOrNull() ?: 44.32) - 0.015
        val maxLng = (lngs.maxOrNull() ?: 44.45) + 0.015
        val latSpan = (maxLat - minLat).coerceAtLeast(0.03)
        val lngSpan = (maxLng - minLng).coerceAtLeast(0.03)

        fun projectPoint(lat: Double, lng: Double): Offset {
            val nx = ((lng - minLng) / lngSpan).toFloat().coerceIn(0f, 1f)
            val ny = (1f - ((lat - minLat) / latSpan).toFloat()).coerceIn(0f, 1f)
            val baseX = safeMinX + nx * safeSpanX
            val baseY = safeMinY + ny * safeSpanY
            val zoomedX = safeCenterX + (baseX - safeCenterX) * zoomScale + panOffset.x
            val zoomedY = safeCenterY + (baseY - safeCenterY) * zoomScale + panOffset.y
            return Offset(zoomedX, zoomedY)
        }

        // Render POI markers ONLY when zoomed in, strictly clamped inside safe viewport
        if (showPois || zoomScale >= 1.35f) {
            pointsOfInterest.take(18).forEach { poi ->
                val p = projectPoint(poi.latitude, poi.longitude)
                val clampedX = (p.x - poiHalfPx).coerceIn(
                    safeMinX,
                    (safeMaxX - poiHalfPx * 2f).coerceAtLeast(safeMinX)
                )
                val clampedY = (p.y - poiHalfPx).coerceIn(
                    safeMinY,
                    (safeMaxY - poiHalfPx * 2f).coerceAtLeast(safeMinY)
                )

                Box(
                    modifier = Modifier
                        .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                        .clickable { onSelectPoi(poi) }
                        .testTag("poi_pin_${poi.id}")
                ) {
                    LandmarkPinBadge(category = poi.category)
                }
            }
        }

        // Render Property Pins, strictly clamped so price pills fit cleanly inside viewport bounds
        properties.forEach { prop ->
            val p = projectPoint(prop.latitude, prop.longitude)
            val clampedX = (p.x - pinHalfWidthPx).coerceIn(
                safeMinX,
                (safeMaxX - pinHalfWidthPx * 2f).coerceAtLeast(safeMinX)
            )
            val clampedY = (p.y - pinHalfHeightPx).coerceIn(
                safeMinY,
                (safeMaxY - pinHalfHeightPx * 2f).coerceAtLeast(safeMinY)
            )
            val isSelected = prop.id == selectedPropertyId

            Box(
                modifier = Modifier
                    .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                    .clickable { onSelectProperty(prop) }
                    .testTag("map_pin_${prop.id}")
            ) {
                PricePinBadge(
                    priceText = formatCompactPrice(prop.price, strings.isArabic),
                    listingType = prop.listingType,
                    propertyType = prop.propertyType,
                    selected = isSelected,
                    verified = prop.verified
                )
            }
        }
    }
}

@Composable
fun PricePinBadge(
    priceText: String,
    listingType: String,
    propertyType: String,
    selected: Boolean,
    verified: Boolean
) {
    val typeIcon = when {
        propertyType == "COMMERCIAL" -> Icons.Default.Storefront
        propertyType == "LAND" -> Icons.Default.Landscape
        listingType == "MORTGAGE" -> Icons.Default.AccountBalance
        listingType == "RENT" -> Icons.Default.Key
        else -> Icons.Default.Home
    }
    val typeTint = when {
        selected -> Color.White
        propertyType == "COMMERCIAL" -> AmberGold
        listingType == "MORTGAGE" -> MortgageCyan
        listingType == "RENT" -> RentViolet
        propertyType == "LAND" -> TealLandmark
        else -> DiyarActionBlue
    }

    Surface(
        color = if (selected) DiyarActionBlue else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(50),
        shadowElevation = if (selected) 8.dp else 4.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.2.dp,
            color = if (selected) Color.White else typeTint.copy(alpha = 0.75f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = listingType,
                tint = typeTint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = priceText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            if (verified && !selected) {
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(VerifiedEmerald)
                )
            }
        }
    }
}

@Composable
fun PropertyNeighborhoodMiniMap(
    property: PropertyListing,
    landmarks: List<NearbyLandmark>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var showGoogleTiles by remember { mutableStateOf(hasConfiguredMapsApiKey()) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (strings.isArabic) "${property.neighborhoodAr} • ${property.cityAr}"
                else "${property.neighborhoodEn} • ${property.cityEn}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { showGoogleTiles = !showGoogleTiles }
            ) {
                Text(
                    text = if (showGoogleTiles) strings.interactiveVectorMode else strings.googleMapMode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
        ) {
            if (showGoogleTiles) {
                val cameraState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(
                        LatLng(property.latitude, property.longitude),
                        14.2f
                    )
                }
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraState,
                    properties = MapProperties(
                        mapStyleOptions = if (isDark) MapStyleOptions(DARK_MAP_STYLE_JSON) else null
                    ),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false)
                ) {
                    MarkerComposable(
                        state = MarkerState(position = LatLng(property.latitude, property.longitude))
                    ) {
                        PricePinBadge(
                            priceText = formatCompactPrice(property.price, strings.isArabic),
                            listingType = property.listingType,
                            propertyType = property.propertyType,
                            selected = true,
                            verified = property.verified
                        )
                    }
                    landmarks.forEach { lm ->
                        MarkerComposable(
                            state = MarkerState(position = LatLng(lm.latitude, lm.longitude))
                        ) {
                            LandmarkPinBadge(category = lm.category)
                        }
                    }
                }
            } else {
                val ringColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isDark) Color(0xFF0F131D) else Color(0xFFF1F5F9)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        listOf(0.22f, 0.36f, 0.48f).forEach { factor ->
                            drawCircle(
                                color = ringColor,
                                radius = size.minDimension * factor,
                                center = c,
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }
                    PricePinBadge(
                        priceText = if (strings.isArabic) "العقار" else "Home",
                        listingType = property.listingType,
                        propertyType = property.propertyType,
                        selected = true,
                        verified = property.verified
                    )
                    val offsets = listOf(
                        Offset(-85f, -55f),
                        Offset(88f, -45f),
                        Offset(-75f, 56f),
                        Offset(82f, 52f),
                        Offset(0f, -72f)
                    )
                    landmarks.take(5).forEachIndexed { idx, lm ->
                        val off = offsets[idx % offsets.size]
                        Box(
                            modifier = Modifier.offset(x = off.x.dp, y = off.y.dp)
                        ) {
                            LandmarkPinBadge(category = lm.category)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandmarkPinBadge(category: String) {
    val (icon, tint) = when (category) {
        "MOSQUE" -> Icons.Default.Mosque to VerifiedEmerald
        "SCHOOL" -> Icons.Default.School to DiyarActionBlue
        "HOSPITAL" -> Icons.Default.LocalHospital to Color(0xFFEF4444)
        "RESTAURANT" -> Icons.Default.Restaurant to RestaurantOrange
        "STREET" -> Icons.Default.Directions to RentViolet
        else -> Icons.Default.Storefront to TealLandmark
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, tint),
        modifier = Modifier.size(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = category,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
