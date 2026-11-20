package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NearbyLandmark
import com.example.data.model.PropertyListing
import com.example.data.model.RoomSpec
import com.example.data.model.TourScene
import com.example.data.model.UiState
import com.example.i18n.LocalStrings
import com.example.ui.components.ArchitecturalFloorPlanCanvas
import com.example.ui.components.EditableMultiImageCarousel
import com.example.ui.components.Panorama360Viewer
import com.example.ui.components.formatCurrencyPrice
import com.example.ui.components.parseColorHex
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.viewmodel.DiyarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    viewModel: DiyarViewModel,
    onPublished: (String) -> Unit
) {
    val strings = LocalStrings.current
    var activeTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                Text(
                    text = strings.publishTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                )
                PrimaryTabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text(strings.createListingTab) },
                        modifier = Modifier.testTag("publish_tab_create")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text(strings.myListingsTab) },
                        modifier = Modifier.testTag("publish_tab_mine")
                    )
                }
            }
        }

        if (activeTab == 0) {
            PublishWizard(viewModel = viewModel, onPublished = { propId ->
                onPublished(propId)
            })
        } else {
            MyListingsPane(viewModel = viewModel, onOpenDetail = onPublished)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PublishWizard(
    viewModel: DiyarViewModel,
    onPublished: (String) -> Unit
) {
    val strings = LocalStrings.current
    var step by remember { mutableIntStateOf(0) }

    // Step 1 fields
    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var listingType by remember { mutableStateOf("SALE") }
    var propertyType by remember { mutableStateOf("VILLA") }
    var priceInput by remember { mutableStateOf("450000000") }
    var areaInput by remember { mutableStateOf("380") }
    var bedsInput by remember { mutableStateOf("4") }
    var bathsInput by remember { mutableStateOf("4") }
    var yearBuiltInput by remember { mutableStateOf("2025") }
    val selectedAmenities = remember { mutableStateListOf("POOL", "GARDEN", "PARKING") }
    var cityInput by remember { mutableStateOf(if (strings.isArabic) "بغداد" else "Baghdad") }
    var neighborhoodInput by remember { mutableStateOf(if (strings.isArabic) "المنصور" else "Al-Mansour") }

    // Step 2: Rooms & Paint Specs
    val rooms = remember {
        mutableStateListOf(
            RoomSpec("Main Majlis", "المجلس الرئيسي", 6.0, 5.5, 3.6, "Jotun Warm Sand", "جوتن رمال دافئة", "#EAE0D0", "Travertine Marble", "رخام ترافرتين"),
            RoomSpec("Modern Kitchen", "المطبخ العصري", 4.8, 4.0, 3.4, "Pure Cotton", "قطني نقي", "#F5F6F8", "Porcelain Slab", "ألواح بورسلان")
        )
    }
    var newRoomName by remember { mutableStateOf("") }
    var newRoomW by remember { mutableStateOf("4.5") }
    var newRoomL by remember { mutableStateOf("5.0") }
    var newRoomPaintName by remember { mutableStateOf("Sky Slate") }
    var newRoomPaintHex by remember { mutableStateOf("#DCE6F2") }

    val context = LocalContext.current

    // Step 3: Multi-Image Carousel + 360° Spherical Panorama Upload & Preview
    val galleryImages = remember {
        mutableStateListOf("preset:villa_exterior", "preset:pano_living", "preset:pano_kitchen")
    }
    val scenes = remember {
        mutableStateListOf(
            TourScene("living_360", "Living Hall 360°", "الصالة الرئيسية ٣٦٠°", "pano_living", 0.0),
            TourScene("kitchen_360", "Kitchen 360°", "المطبخ ٣٦٠°", "pano_kitchen", 40.0)
        )
    }
    var previewSceneIdx by remember { mutableIntStateOf(0) }
    var newSceneTitle by remember { mutableStateOf("") }
    var pickedPhotoUri by remember { mutableStateOf("") }

    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(15)
    ) { uris ->
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            val uriStr = uri.toString()
            if (galleryImages.size < 20 && !galleryImages.contains(uriStr)) {
                // Insert user-picked images ahead of default presets for immediate visibility
                val firstPresetIdx = galleryImages.indexOfFirst { it.startsWith("preset:") }
                if (firstPresetIdx >= 0) galleryImages.add(firstPresetIdx, uriStr)
                else galleryImages.add(uriStr)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            pickedPhotoUri = uri.toString()
            if (scenes.size < 15) {
                val label = newSceneTitle.trim().ifBlank {
                    if (strings.isArabic) "مشهد ٣٦٠° مرفوع #${scenes.size + 1}"
                    else "Uploaded 360° #${scenes.size + 1}"
                }
                scenes.add(
                    0,
                    TourScene(
                        sceneId = "scene_${System.currentTimeMillis()}",
                        titleEn = label,
                        titleAr = label,
                        presetKey = "pano_living",
                        initialYaw = 0.0,
                        customPhotoUri = pickedPhotoUri
                    )
                )
                previewSceneIdx = 0
                newSceneTitle = ""
            }
        }
    }

    // Step 4: Landmarks & Verification
    val landmarks = remember {
        mutableStateListOf(
            NearbyLandmark("MOSQUE", "District Grand Mosque", "جامع المنطقة الكبير", 240, 3, 33.3195, 44.3435),
            NearbyLandmark("SCHOOL", "Baghdad International School", "مدرسة بغداد الدولية", 550, 7, 33.3210, 44.3410)
        )
    }
    var newLandmarkCategory by remember { mutableStateOf("HOSPITAL") }
    var newLandmarkName by remember { mutableStateOf("") }
    var newLandmarkDist by remember { mutableStateOf("650") }

    var deedNumber by remember { mutableStateOf("310118049210") }
    var licenseNumber by remember { mutableStateOf("IQ-TAPU-720041") }
    var contactPhone by remember { mutableStateOf("+9647701234567") }

    var submitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val stepTitles = listOf(
        strings.stepBasicInfo,
        strings.stepRoomsSpecs,
        strings.stepTourMedia,
        strings.stepVerifyPublish
    )

    Column(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Step Progress Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            stepTitles.forEachIndexed { idx, label ->
                FilterChip(
                    selected = step == idx,
                    onClick = { step = idx },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        when (step) {
            0 -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = listingType == "SALE",
                        onClick = { listingType = "SALE" },
                        label = { Text(strings.forSale) }
                    )
                    FilterChip(
                        selected = listingType == "RENT",
                        onClick = { listingType = "RENT" },
                        label = { Text(strings.forRent) }
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "VILLA" to strings.villa,
                        "APARTMENT" to strings.apartment,
                        "PENTHOUSE" to strings.penthouse,
                        "TOWNHOUSE" to strings.townhouse,
                        "COMMERCIAL" to strings.commercial,
                        "LAND" to strings.land
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = propertyType == code,
                            onClick = { propertyType = code },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(if (strings.isArabic) "عنوان العقار" else "Property Title") },
                    placeholder = { Text(if (strings.isArabic) "مثال: فيلا حديثة في النرجس" else "e.g., Modern Villa in Al Narjis") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("publish_title_input")
                )

                OutlinedTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = { Text(strings.professionalDescTitle) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("publish_desc_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text(if (strings.isArabic) "السعر (د.ع)" else "Price (IQD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = areaInput,
                        onValueChange = { areaInput = it },
                        label = { Text("${strings.areaUnit}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bedsInput,
                        onValueChange = { bedsInput = it },
                        label = { Text(strings.bedsLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bathsInput,
                        onValueChange = { bathsInput = it },
                        label = { Text(strings.bathsLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        label = { Text(if (strings.isArabic) "المحافظة / المدينة" else "Governorate / City") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = neighborhoodInput,
                        onValueChange = { neighborhoodInput = it },
                        label = { Text(if (strings.isArabic) "المنطقة / الحي" else "District / Neighborhood") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = yearBuiltInput,
                    onValueChange = { yearBuiltInput = it },
                    label = { Text(strings.yearBuiltLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = strings.amenitiesTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DiyarViewModel.availableAmenityCodes.forEach { code ->
                        val active = selectedAmenities.contains(code)
                        FilterChip(
                            selected = active,
                            onClick = {
                                if (active) selectedAmenities.remove(code) else selectedAmenities.add(code)
                            },
                            label = { Text(strings.localizeAmenity(code)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            border = null
                        )
                    }
                }
            }

            1 -> {
                Text(strings.architecturalPlanTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ArchitecturalFloorPlanCanvas(
                    rooms = rooms,
                    totalWidthM = 16.0,
                    totalLengthM = 22.0,
                    selectedRoomIndex = 0,
                    onSelectRoom = {}
                )

                val paletteOptions = listOf(
                    "#EAE0D0" to "Warm Sand",
                    "#F5F6F8" to "Pure Cotton",
                    "#DCE6F2" to "Slate Mist",
                    "#E4EDE6" to "Sage Whisper",
                    "#EDE4E0" to "Soft Terracotta"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    paletteOptions.forEach { (hex, name) ->
                        val selected = newRoomPaintHex == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(parseColorHex(hex))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable {
                                    newRoomPaintHex = hex
                                    newRoomPaintName = name
                                }
                        )
                    }
                }

                OutlinedTextField(
                    value = newRoomName,
                    onValueChange = { newRoomName = it },
                    label = { Text(if (strings.isArabic) "اسم الغرفة (مثال: غرفة نوم رئيسية)" else "Room Name (e.g., Master Suite)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newRoomW,
                        onValueChange = { newRoomW = it },
                        label = { Text(if (strings.isArabic) "العرض (م)" else "Width (m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newRoomL,
                        onValueChange = { newRoomL = it },
                        label = { Text(if (strings.isArabic) "الطول (م)" else "Length (m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (newRoomName.isNotBlank() && rooms.size < 20) {
                            rooms.add(
                                RoomSpec(
                                    nameEn = newRoomName.trim(),
                                    nameAr = newRoomName.trim(),
                                    widthM = newRoomW.toDoubleOrNull()?.coerceIn(1.0, 50.0) ?: 4.0,
                                    lengthM = newRoomL.toDoubleOrNull()?.coerceIn(1.0, 50.0) ?: 5.0,
                                    ceilingM = 3.4,
                                    paintNameEn = newRoomPaintName,
                                    paintNameAr = newRoomPaintName,
                                    paintHex = newRoomPaintHex
                                )
                            )
                            newRoomName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.addRoom)
                }
            }

            2 -> {
                EditableMultiImageCarousel(
                    images = galleryImages,
                    onAddManyClick = {
                        multiPhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveImage = { index ->
                        if (galleryImages.size > 1 && index in galleryImages.indices) {
                            galleryImages.removeAt(index)
                        }
                    },
                    onSetAs360Scene = { token ->
                        if (scenes.size < 15) {
                            val title = newSceneTitle.trim().ifBlank {
                                if (strings.isArabic) "مشهد ٣٦٠° #${scenes.size + 1}" else "360° Scene #${scenes.size + 1}"
                            }
                            val scene = if (token.startsWith("preset:")) {
                                TourScene(
                                    sceneId = "scene_${scenes.size + 1}",
                                    titleEn = title,
                                    titleAr = title,
                                    presetKey = token.removePrefix("preset:"),
                                    initialYaw = 0.0
                                )
                            } else {
                                pickedPhotoUri = token
                                TourScene(
                                    sceneId = "scene_${scenes.size + 1}",
                                    titleEn = title,
                                    titleAr = title,
                                    presetKey = "pano_living",
                                    initialYaw = 0.0,
                                    customPhotoUri = token
                                )
                            }
                            scenes.add(0, scene)
                            previewSceneIdx = 0
                            newSceneTitle = ""
                        }
                    }
                )

                Text(strings.virtualTour360Title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(strings.virtualTour360Subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (scenes.isNotEmpty()) {
                    Panorama360Viewer(
                        scenes = scenes,
                        activeSceneIndex = previewSceneIdx.coerceIn(0, scenes.lastIndex),
                        onSelectScene = { previewSceneIdx = it },
                        onExpandFullscreen = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(225.dp)
                            .testTag("publish_panorama_preview")
                    )
                }

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("publish_pick_photo_button")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (pickedPhotoUri.isBlank()) strings.upload360Spherical else "✓ ${strings.upload360Spherical}")
                }

                OutlinedTextField(
                    value = newSceneTitle,
                    onValueChange = { newSceneTitle = it },
                    label = { Text(if (strings.isArabic) "اسم زاوية ٣٦٠° (مثال: الحديقة الخلفية)" else "360° Angle Name (e.g., Rooftop Terrace)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        if (newSceneTitle.isNotBlank() && scenes.size < 15) {
                            scenes.add(
                                TourScene(
                                    sceneId = "scene_${scenes.size + 1}",
                                    titleEn = newSceneTitle.trim(),
                                    titleAr = newSceneTitle.trim(),
                                    presetKey = "pano_living",
                                    initialYaw = (scenes.size * 45.0) % 360.0,
                                    customPhotoUri = pickedPhotoUri
                                )
                            )
                            previewSceneIdx = scenes.lastIndex
                            newSceneTitle = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.addScene360)
                }

                scenes.forEachIndexed { idx, s ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (idx == previewSceneIdx) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { previewSceneIdx = idx }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (strings.isArabic) s.titleAr else s.titleEn, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            3 -> {
                Text(strings.nearbyLandmarksTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "MOSQUE" to strings.mosques,
                        "SCHOOL" to strings.schools,
                        "HOSPITAL" to strings.hospitals,
                        "RESTAURANT" to strings.restaurantsCat,
                        "STREET" to strings.streetsCat,
                        "SERVICE" to strings.servicesCat
                    ).forEach { (cat, label) ->
                        FilterChip(
                            selected = newLandmarkCategory == cat,
                            onClick = { newLandmarkCategory = cat },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newLandmarkName,
                        onValueChange = { newLandmarkName = it },
                        label = { Text(if (strings.isArabic) "اسم المعلم" else "Landmark Name") },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = newLandmarkDist,
                        onValueChange = { newLandmarkDist = it },
                        label = { Text(strings.metersAway) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (newLandmarkName.isNotBlank() && landmarks.size < 20) {
                            val dist = newLandmarkDist.toLongOrNull()?.coerceIn(10L, 50000L) ?: 350L
                            landmarks.add(
                                NearbyLandmark(
                                    category = newLandmarkCategory,
                                    nameEn = newLandmarkName.trim(),
                                    nameAr = newLandmarkName.trim(),
                                    distanceMeters = dist,
                                    walkMinutes = (dist / 80L).coerceAtLeast(1L),
                                    latitude = 33.3152 + (landmarks.size * 0.002),
                                    longitude = 44.3661 + (landmarks.size * 0.002)
                                )
                            )
                            newLandmarkName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.addLandmark)
                }

                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VerifiedEmerald)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.verificationCardTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(
                            value = deedNumber,
                            onValueChange = { deedNumber = it },
                            label = { Text(if (strings.isArabic) "رقم السند العقاري (الطابو)" else "Tapu / Registry Deed Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = licenseNumber,
                            onValueChange = { licenseNumber = it },
                            label = { Text(if (strings.isArabic) "رقم إجازة الدلالية / الوساطة" else "Brokerage / Registry License") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text(if (strings.isArabic) "رقم الهاتف العراقي للتحقق (خاص)" else "Verification Phone (Private)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (errorMsg != null) {
            Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step -= 1 },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text(strings.prevStep)
                }
            }
            if (step < 3) {
                Button(
                    onClick = { step += 1 },
                    modifier = Modifier.weight(1f).height(50.dp).testTag("publish_next_step")
                ) {
                    Text(strings.nextStep)
                }
            } else {
                Button(
                    onClick = {
                        val cleanTitle = titleInput.trim().ifBlank {
                            if (strings.isArabic) "عقار سكني حديث في $neighborhoodInput" else "Modern Residence in $neighborhoodInput"
                        }
                        val cleanDesc = descInput.trim().let {
                            if (it.length >= 10) it
                            else if (strings.isArabic) "عقار مميز موثق عبر ديار بتفاصيل الغرف وألوان الطلاء والجولة الافتراضية ٣٦٠°."
                            else "Verified Diyar residence complete with documented room dimensions, paint codes, and 360° virtual tour."
                        }
                        val listing = PropertyListing(
                            titleEn = cleanTitle,
                            titleAr = cleanTitle,
                            descriptionEn = cleanDesc,
                            descriptionAr = cleanDesc,
                            listingType = listingType,
                            propertyType = propertyType,
                            price = priceInput.toDoubleOrNull()?.coerceAtLeast(1000.0) ?: 350_000_000.0,
                            currency = "IQD",
                            areaSqm = areaInput.toDoubleOrNull()?.coerceAtLeast(20.0) ?: 280.0,
                            bedrooms = bedsInput.toLongOrNull()?.coerceIn(0L, 30L) ?: 3L,
                            bathrooms = bathsInput.toLongOrNull()?.coerceIn(0L, 30L) ?: 3L,
                            yearBuilt = yearBuiltInput.toLongOrNull()?.coerceIn(1950L, 2030L) ?: 2025L,
                            amenities = selectedAmenities.toList(),
                            cityEn = cityInput.ifBlank { "Baghdad" },
                            cityAr = cityInput.ifBlank { "بغداد" },
                            neighborhoodEn = neighborhoodInput.ifBlank { "Al-Mansour" },
                            neighborhoodAr = neighborhoodInput.ifBlank { "المنصور" },
                            latitude = 33.3185,
                            longitude = 44.3422,
                            heroPresetKey = if (propertyType == "PENTHOUSE") "penthouse_exterior" else "villa_exterior",
                            galleryImages = galleryImages.take(20).toList(),
                            ceilingHeightM = 3.5,
                            floorPlanWidthM = 16.0,
                            floorPlanLengthM = 22.0,
                            rooms = rooms.toList(),
                            tourScenes = scenes.toList(),
                            nearbyLandmarks = landmarks.toList()
                        )
                        submitting = true
                        errorMsg = null
                        viewModel.publishProperty(
                            listing = listing,
                            fullDeedNumber = deedNumber.ifBlank { "TAPU-1001" },
                            licenseNumber = licenseNumber.ifBlank { "IQ-TAPU-1001" },
                            contactPhone = contactPhone.ifBlank { "+9647700000000" }
                        ) { res ->
                            submitting = false
                            res.onSuccess { id -> onPublished(id) }
                            res.onFailure { err -> errorMsg = err.localizedMessage }
                        }
                    },
                    enabled = !submitting,
                    modifier = Modifier.weight(1f).height(50.dp).testTag("publish_submit_button")
                ) {
                    if (submitting) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(strings.submitListing)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MyListingsPane(
    viewModel: DiyarViewModel,
    onOpenDetail: (String) -> Unit
) {
    val strings = LocalStrings.current
    val myState by viewModel.myPropertiesState.collectAsStateWithLifecycle()

    when (val state = myState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.data, key = { it.id }) { prop ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.clickable { onOpenDetail(prop.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (strings.isArabic) prop.titleAr else prop.titleEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${formatCurrencyPrice(prop.price, prop.currency, prop.listingType == "RENT", strings.isArabic)} • ${strings.localizePropertyStatus(prop.status)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (prop.status == "ACTIVE") {
                                OutlinedButton(onClick = { viewModel.markPropertySold(prop.id) }) {
                                    Text(strings.markSold)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteProperty(prop.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = strings.deleteListing, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
