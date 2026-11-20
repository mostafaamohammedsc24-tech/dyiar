package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.DiyarMapType
import com.example.data.model.MapPointOfInterest
import com.example.data.model.NearbyLandmark
import com.example.data.model.PropertyListing
import com.example.i18n.LocalStrings
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DiyarActionBlue
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
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    if (useGoogleMapTiles) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 11.4f)
        }
        LaunchedEffect(centerLat, centerLng) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLng), 12.2f),
                durationMs = 550
            )
        }

        val googleMapType = when (mapType) {
            DiyarMapType.NORMAL -> MapType.NORMAL
            DiyarMapType.SATELLITE -> MapType.SATELLITE
            DiyarMapType.HYBRID -> MapType.HYBRID
            DiyarMapType.TERRAIN -> MapType.TERRAIN
        }

        val mapProperties = remember(isDark, googleMapType) {
            MapProperties(
                mapType = googleMapType,
                mapStyleOptions = if (isDark && googleMapType == MapType.NORMAL) {
                    MapStyleOptions(DARK_MAP_STYLE_JSON)
                } else null
            )
        }
        val uiSettings = remember {
            MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            )
        }

        Box(modifier = modifier.testTag("google_map_container")) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings
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

                // Points of Interest markers (Hospitals, Schools, Restaurants, Mosques, Streets)
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

                // Property Listing Markers with distinct Sale / Rent / Land icons
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 148.dp)
                ) {
                    Text(
                        text = strings.mapsKeyNotice,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
            onSelectProperty = onSelectProperty,
            onSelectPoi = onSelectPoi,
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
    onSelectProperty: (PropertyListing) -> Unit,
    onSelectPoi: (MapPointOfInterest) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
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

    BoxWithConstraints(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
            .testTag("vector_geo_map")
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 44.dp.toPx()
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
            // Major arterial corridors
            drawLine(
                color = roadColor,
                start = Offset(0f, size.height * 0.34f),
                end = Offset(size.width, size.height * 0.56f),
                strokeWidth = 7.dp.toPx()
            )
            drawLine(
                color = roadColor,
                start = Offset(size.width * 0.46f, 0f),
                end = Offset(size.width * 0.54f, size.height),
                strokeWidth = 8.dp.toPx()
            )
            drawCircle(
                color = DiyarActionBlue.copy(alpha = 0.06f),
                radius = size.minDimension * 0.36f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
            drawCircle(
                color = DiyarActionBlue.copy(alpha = 0.35f),
                radius = size.minDimension * 0.36f,
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            )
        }

        val lats = properties.map { it.latitude } + pointsOfInterest.map { it.latitude } + centerLat
        val lngs = properties.map { it.longitude } + pointsOfInterest.map { it.longitude } + centerLng
        val minLat = (lats.minOrNull() ?: 33.27) - 0.025
        val maxLat = (lats.maxOrNull() ?: 33.36) + 0.025
        val minLng = (lngs.minOrNull() ?: 44.32) - 0.025
        val maxLng = (lngs.maxOrNull() ?: 44.45) + 0.025
        val latSpan = (maxLat - minLat).coerceAtLeast(0.04)
        val lngSpan = (maxLng - minLng).coerceAtLeast(0.04)
        val density = LocalDensity.current

        // Render POI markers underneath property price badges
        pointsOfInterest.take(14).forEach { poi ->
            val nx = ((poi.longitude - minLng) / lngSpan).toFloat().coerceIn(0.08f, 0.90f)
            val ny = (1f - ((poi.latitude - minLat) / latSpan).toFloat()).coerceIn(0.14f, 0.78f)
            val xDp = with(density) { (nx * widthPx).toDp() - 15.dp }
            val yDp = with(density) { (ny * heightPx).toDp() - 15.dp }

            Box(
                modifier = Modifier
                    .offset(x = xDp, y = yDp)
                    .clickable { onSelectPoi(poi) }
                    .testTag("poi_pin_${poi.id}")
            ) {
                LandmarkPinBadge(category = poi.category)
            }
        }

        // Render Property Pins
        properties.forEach { prop ->
            val nx = ((prop.longitude - minLng) / lngSpan).toFloat().coerceIn(0.10f, 0.85f)
            val ny = (1f - ((prop.latitude - minLat) / latSpan).toFloat()).coerceIn(0.15f, 0.76f)
            val xDp = with(density) { (nx * widthPx).toDp() - 42.dp }
            val yDp = with(density) { (ny * heightPx).toDp() - 18.dp }
            val isSelected = prop.id == selectedPropertyId

            Box(
                modifier = Modifier
                    .offset(x = xDp, y = yDp)
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
        listingType == "RENT" -> Icons.Default.Key
        else -> Icons.Default.Home
    }
    val typeTint = when {
        selected -> Color.White
        propertyType == "COMMERCIAL" -> AmberGold
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
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
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
