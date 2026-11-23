package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NearbyLandmark
import com.example.data.model.PropertyListing
import com.example.data.model.RoomSpec
import com.example.data.model.TourScene
import com.example.data.model.UiState
import com.example.i18n.LocalStrings
import com.example.ui.components.ArchitecturalFloorPlanCanvas
import com.example.ui.components.Panorama360Viewer
import com.example.ui.components.PropertyImageCarousel
import com.example.ui.components.PropertyNeighborhoodMiniMap
import com.example.ui.components.VirtualTour360Viewer
import com.example.ui.components.formatCurrencyPrice
import com.example.ui.components.openPropertyWhatsApp
import com.example.ui.components.parseColorHex
import com.example.ui.components.resolvePropertyGalleryItems
import com.example.ui.components.sharePropertyExternally
import com.example.ui.theme.DiyarActionBlue
import com.example.ui.theme.MortgageCyan
import com.example.ui.theme.RentViolet
import com.example.ui.theme.RestaurantOrange
import com.example.ui.theme.TealLandmark
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.viewmodel.DiyarViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropertyDetailScreen(
    propertyId: String,
    viewModel: DiyarViewModel,
    isDark: Boolean,
    onBack: () -> Unit,
    onOpenVirtualTour: (String) -> Unit,
    onOpenServices: () -> Unit
) {
    BackHandler { onBack() }

    val strings = LocalStrings.current
    val context = LocalContext.current
    val activeState by viewModel.activePropertiesState.collectAsStateWithLifecycle()
    val myState by viewModel.myPropertiesState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    val property: PropertyListing? = remember(activeState, myState, propertyId) {
        val activeList = (activeState as? UiState.Success)?.data ?: emptyList()
        val myList = (myState as? UiState.Success)?.data ?: emptyList()
        activeList.firstOrNull { it.id == propertyId } ?: myList.firstOrNull { it.id == propertyId }
    }

    if (property == null) {
        val isStillLoading = activeState is UiState.Loading || myState is UiState.Loading
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isStillLoading) {
                    CircularProgressIndicator()
                    Text(strings.loadingText)
                } else {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (strings.isArabic) "هذا العقار غير متاح أو تم حذفه" else "This property is unavailable or was removed.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.testTag("detail_missing_back_button")
                ) {
                    Text(strings.prevStep)
                }
            }
        }
        return
    }

    val isFav = favoriteIds.contains(property.id)
    val title = if (strings.isArabic) property.titleAr else property.titleEn
    val description = if (strings.isArabic) property.descriptionAr else property.descriptionEn
    val locationText = if (strings.isArabic) {
        "${property.neighborhoodAr}، ${property.cityAr}"
    } else {
        "${property.neighborhoodEn}, ${property.cityEn}"
    }
    val categoryColor = when {
        property.propertyType == "COMMERCIAL" -> RestaurantOrange
        property.listingType == "MORTGAGE" -> MortgageCyan
        property.listingType == "RENT" -> RentViolet
        else -> MaterialTheme.colorScheme.primary
    }

    var selectedTourSceneIdx by remember { mutableIntStateOf(0) }
    var selectedRoomIdx by remember { mutableIntStateOf(0) }
    var carousel360Token by remember { mutableStateOf<String?>(null) }

    val galleryItems = remember(property) { resolvePropertyGalleryItems(property) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Multi-Image Swipeable Carousel + Top Overlay Navigation
        Box(modifier = Modifier.fillMaxWidth()) {
            PropertyImageCarousel(
                images = galleryItems,
                contentDescription = title,
                showThumbnails = true,
                onOpenIn360 = { token -> carousel360Token = token },
                modifier = Modifier.fillMaxWidth(),
                carouselHeight = 290.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.prevStep,
                            tint = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    ) {
                        IconButton(
                            onClick = { sharePropertyExternally(context, property, strings) },
                            modifier = Modifier.testTag("detail_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = strings.shareProperty,
                                tint = Color.White
                            )
                        }
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite(property) },
                            modifier = Modifier.testTag("detail_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = strings.navFavorites,
                                tint = if (isFav) Color(0xFFFF4D6D) else Color.White
                            )
                        }
                    }
                }
            }
        }

        // Price, Title, Badges, WhatsApp Contact & External Share Headline Block
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (property.listingType == "MORTGAGE") {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = strings.localizeListingType(property.listingType),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = strings.localizePropertyType(property.propertyType),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = formatCurrencyPrice(
                        property.price,
                        property.currency,
                        property.listingType == "RENT",
                        strings.isArabic
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { openPropertyWhatsApp(context, property, strings) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VerifiedEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("detail_whatsapp_button")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${strings.contactWhatsapp} • 07740080310",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = { sharePropertyExternally(context, property, strings) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_share_outline_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = strings.shareProperty,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Report Container
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Trust & Deed Verification Certificate Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(VerifiedEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = strings.verifiedBadge,
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.verificationCardTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.publisherLabel}: ${property.ownerDisplayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${strings.deedRefLabel}: ${property.deedReference}",
                            style = MaterialTheme.typography.labelMedium,
                            color = VerifiedEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 2. Key Architectural Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(Icons.Default.Bed, "${property.bedrooms}", strings.bedsLabel, Modifier.weight(1f))
                MetricTile(Icons.Default.Bathtub, "${property.bathrooms}", strings.bathsLabel, Modifier.weight(1f))
                MetricTile(Icons.Default.SquareFoot, "${property.areaSqm.toInt()}", strings.areaUnit, Modifier.weight(1f))
                MetricTile(Icons.Default.CalendarMonth, "${property.yearBuilt}", strings.yearBuiltLabel, Modifier.weight(1f))
            }

            // Amenities chips if present
            if (property.amenities.isNotEmpty()) {
                ReportSectionCard(title = strings.amenitiesTitle) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        property.amenities.forEach { amenity ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = VerifiedEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = strings.localizeAmenity(amenity),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Professional Publisher Editorial Description
            ReportSectionCard(title = strings.professionalDescTitle) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 4. Interactive 360° Virtual Walkthrough Section
            if (property.tourScenes.isNotEmpty()) {
                ReportSectionCard(
                    title = strings.virtualTour360Title,
                    subtitle = strings.virtualTour360Subtitle
                ) {
                    VirtualTour360Viewer(
                        scenes = property.tourScenes,
                        activeSceneIndex = selectedTourSceneIdx.coerceIn(0, property.tourScenes.lastIndex),
                        onSelectScene = { selectedTourSceneIdx = it },
                        onExpandFullscreen = { onOpenVirtualTour(property.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(225.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onOpenVirtualTour(property.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("launch_360_tour_button")
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.launch360Tour)
                    }
                }
            }

            // 5. Engineering Floor Plan + Multi-Floor View + Room-by-Room Paint Colors & Dimensions
            if (property.rooms.isNotEmpty()) {
                val safeIdx = selectedRoomIdx.coerceIn(0, property.rooms.lastIndex)
                val activeRoom = property.rooms[safeIdx]
                val floors = remember(property.rooms) {
                    property.rooms.map { it.floor.coerceAtLeast(1) }.distinct().sorted()
                }
                var selectedFloorFilter by remember(property.id) {
                    mutableIntStateOf(floors.firstOrNull() ?: 1)
                }
                val visibleEntries = remember(property.rooms, selectedFloorFilter) {
                    property.rooms.mapIndexed { idx, room -> idx to room }
                        .filter { (_, room) ->
                            selectedFloorFilter == 0 || room.floor.coerceAtLeast(1) == selectedFloorFilter
                        }
                }
                val visibleCanvasRooms = remember(visibleEntries) { visibleEntries.map { it.second } }
                val selectedCanvasIdx = visibleEntries.indexOfFirst { it.first == safeIdx }

                ReportSectionCard(
                    title = strings.architecturalPlanTitle,
                    subtitle = strings.architecturalPlanSubtitle
                ) {
                    if (floors.size > 1) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            FilterChip(
                                selected = selectedFloorFilter == 0,
                                onClick = { selectedFloorFilter = 0 },
                                label = { Text(strings.all) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            floors.forEach { fl ->
                                val flLabel = if (fl == 1) strings.floorGround else "${strings.floorLabelPrefix} $fl"
                                FilterChip(
                                    selected = selectedFloorFilter == fl,
                                    onClick = {
                                        selectedFloorFilter = fl
                                        val firstOnFloor = property.rooms.indexOfFirst { it.floor.coerceAtLeast(1) == fl }
                                        if (firstOnFloor >= 0) selectedRoomIdx = firstOnFloor
                                    },
                                    label = { Text(flLabel) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    ArchitecturalFloorPlanCanvas(
                        rooms = visibleCanvasRooms,
                        totalWidthM = property.floorPlanWidthM,
                        totalLengthM = property.floorPlanLengthM,
                        selectedRoomIndex = selectedCanvasIdx,
                        onSelectRoom = { localIdx ->
                            visibleEntries.getOrNull(localIdx)?.let { (origIdx, _) ->
                                selectedRoomIdx = origIdx
                            }
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Highlighted Selected Room Specification Panel
                    RoomSpecificationPanel(room = activeRoom, showFloorBadge = floors.size > 1)

                    Spacer(Modifier.height(10.dp))

                    // Complete Room Index Table
                    Text(
                        text = strings.roomDetailsTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    property.rooms.forEachIndexed { idx, room ->
                        RoomListRow(
                            room = room,
                            selected = idx == safeIdx,
                            showFloorBadge = floors.size > 1,
                            onClick = {
                                selectedRoomIdx = idx
                                val fl = room.floor.coerceAtLeast(1)
                                if (selectedFloorFilter != 0 && selectedFloorFilter != fl) {
                                    selectedFloorFilter = fl
                                }
                            }
                        )
                        if (idx < property.rooms.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            // 6. Nearby Landmarks (Mosques, Schools, Health Centers, Services) + Map
            if (property.nearbyLandmarks.isNotEmpty()) {
                ReportSectionCard(title = strings.nearbyLandmarksTitle) {
                    PropertyNeighborhoodMiniMap(
                        property = property,
                        landmarks = property.nearbyLandmarks,
                        isDark = isDark
                    )
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        property.nearbyLandmarks.forEach { landmark ->
                            NearbyLandmarkRow(landmark)
                        }
                    }
                }
            }

            // 7. Turnkey Home Services CTA
            FilledTonalButton(
                onClick = onOpenServices,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("detail_book_service_button")
            ) {
                Icon(Icons.Default.Handyman, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.servicesHeroTitle)
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    val activeToken = carousel360Token
    if (activeToken != null) {
        val sceneFromToken = remember(activeToken, title) {
            if (activeToken.startsWith("preset:")) {
                TourScene(
                    sceneId = "carousel_360",
                    titleEn = "$title • 360°",
                    titleAr = "$title • ٣٦٠°",
                    presetKey = activeToken.removePrefix("preset:"),
                    initialYaw = 0.0
                )
            } else {
                TourScene(
                    sceneId = "carousel_360",
                    titleEn = "$title • 360°",
                    titleAr = "$title • ٣٦٠°",
                    presetKey = "pano_living",
                    initialYaw = 0.0,
                    customPhotoUri = activeToken
                )
            }
        }
        Dialog(
            onDismissRequest = { carousel360Token = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF060E1A))
                    .safeDrawingPadding()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xCC0D1B2A),
                        shape = CircleShape
                    ) {
                        IconButton(onClick = { carousel360Token = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.prevStep,
                                tint = Color.White
                            )
                        }
                    }
                    Text(
                        text = strings.tour360Badge,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.size(48.dp))
                }
                Panorama360Viewer(
                    scenes = listOf(sceneFromToken),
                    activeSceneIndex = 0,
                    onSelectScene = {},
                    onExpandFullscreen = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ReportSectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun RoomSpecificationPanel(room: RoomSpec, showFloorBadge: Boolean = false) {
    val strings = LocalStrings.current
    val swatchColor = parseColorHex(room.paintHex)
    val flNum = room.floor.coerceAtLeast(1)
    val floorLabel = if (flNum == 1) strings.floorGround else "${strings.floorLabelPrefix} $flNum"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val roomTitle = if (strings.isArabic) room.nameAr else room.nameEn
                Text(
                    text = if (showFloorBadge) "$roomTitle ($floorLabel)" else roomTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${room.widthM}×${room.lengthM}${strings.meterUnit} (${room.areaSqm().roundToInt()} ${strings.areaUnit}) • H ${room.ceilingM}${strings.meterUnit}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${strings.paintFinishLabel}: ${if (strings.isArabic) room.paintNameAr else room.paintNameEn} (${room.paintHex})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${strings.flooringLabel}: ${if (strings.isArabic) room.flooringAr else room.flooringEn}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomListRow(
    room: RoomSpec,
    selected: Boolean,
    showFloorBadge: Boolean = false,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    val flNum = room.floor.coerceAtLeast(1)
    val floorLabel = if (flNum == 1) strings.floorGround else "${strings.floorLabelPrefix} $flNum"
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(parseColorHex(room.paintHex))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    val roomName = if (strings.isArabic) room.nameAr else room.nameEn
                    Text(
                        text = if (showFloorBadge) "$roomName • $floorLabel" else roomName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${if (strings.isArabic) room.paintNameAr else room.paintNameEn} • ${if (strings.isArabic) room.flooringAr else room.flooringEn}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "${room.widthM}×${room.lengthM}${strings.meterUnit}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NearbyLandmarkRow(landmark: NearbyLandmark) {
    val strings = LocalStrings.current
    val (icon, tint, catLabel) = when (landmark.category) {
        "MOSQUE" -> Triple(Icons.Default.Mosque, VerifiedEmerald, strings.mosques)
        "SCHOOL" -> Triple(Icons.Default.School, DiyarActionBlue, strings.schools)
        "HOSPITAL" -> Triple(Icons.Default.LocalHospital, Color(0xFFEF4444), strings.hospitals)
        "RESTAURANT" -> Triple(Icons.Default.Restaurant, RestaurantOrange, strings.restaurantsCat)
        "STREET" -> Triple(Icons.Default.Directions, RentViolet, strings.streetsCat)
        else -> Triple(Icons.Default.Storefront, TealLandmark, strings.servicesCat)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = catLabel, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (strings.isArabic) landmark.nameAr else landmark.nameEn,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = catLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${landmark.distanceMeters} ${strings.metersAway}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${landmark.walkMinutes} ${strings.minWalk}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
