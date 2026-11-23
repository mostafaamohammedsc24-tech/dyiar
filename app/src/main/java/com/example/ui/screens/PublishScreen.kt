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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.components.openPropertyWhatsApp
import com.example.ui.components.parseColorHex
import com.example.ui.components.parseLocalizedDouble
import com.example.ui.components.parseLocalizedLong
import com.example.ui.components.sharePropertyExternally
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.viewmodel.DiyarViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class PaintPaletteOption(
    val hex: String,
    val nameEn: String,
    val nameAr: String
)

private data class FlooringOption(
    val nameEn: String,
    val nameAr: String
)

private data class QuickRoomPreset(
    val nameEn: String,
    val nameAr: String,
    val widthM: Double,
    val lengthM: Double,
    val paintHex: String,
    val paintEn: String,
    val paintAr: String
)

private val designPaintPalette = listOf(
    PaintPaletteOption("#EAE0D0", "Warm Sand", "رمال دافئة"),
    PaintPaletteOption("#F5F6F8", "Pure Cotton", "قطني نقي"),
    PaintPaletteOption("#DCE6F2", "Sky Slate", "أزرق سماوي"),
    PaintPaletteOption("#E4EDE6", "Calm Sage", "أخضر هادئ"),
    PaintPaletteOption("#EDE4E0", "Soft Terracotta", "تيراكوتا ناعم"),
    PaintPaletteOption("#F7F1E3", "Warm Cream", "كريمي دافئ"),
    PaintPaletteOption("#E2E8F0", "Modern Grey", "رمادي عصري")
)

private val designFlooringOptions = listOf(
    FlooringOption("Travertine Marble", "رخام ترافرتين"),
    FlooringOption("Porcelain Slab", "ألواح بورسلان"),
    FlooringOption("Oak Parquet", "باركيه خشبي"),
    FlooringOption("Italian Ceramic", "سيراميك إيطالي"),
    FlooringOption("Polished Granite", "جرانيت مصقول")
)

private val quickRoomPresets = listOf(
    QuickRoomPreset("Reception", "استقبال", 5.5, 6.0, "#EAE0D0", "Warm Sand", "رمال دافئة"),
    QuickRoomPreset("Living Hall", "صالة", 5.0, 5.5, "#F7F1E3", "Warm Cream", "كريمي دافئ"),
    QuickRoomPreset("Bedroom", "غرفة نوم", 4.0, 4.5, "#DCE6F2", "Sky Slate", "أزرق سماوي"),
    QuickRoomPreset("Kitchen", "مطبخ", 4.0, 4.2, "#F5F6F8", "Pure Cotton", "قطني نقي"),
    QuickRoomPreset("Bathroom", "حمام", 2.5, 3.0, "#E4EDE6", "Calm Sage", "أخضر هادئ"),
    QuickRoomPreset("Garage / Garden", "كراج / حديقة", 4.0, 6.0, "#E2E8F0", "Modern Grey", "رمادي عصري")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    viewModel: DiyarViewModel,
    onPublished: (String) -> Unit
) {
    val strings = LocalStrings.current
    var activeTab by remember { mutableIntStateOf(0) }
    val openMyListings by viewModel.openMyListingsRequest.collectAsStateWithLifecycle()

    LaunchedEffect(openMyListings) {
        if (openMyListings) {
            activeTab = 1
            viewModel.consumeOpenMyListingsRequest()
        }
    }

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
            PublishWizard(
                viewModel = viewModel,
                onPublished = { propId ->
                    onPublished(propId)
                }
            )
        } else {
            MyListingsPane(
                viewModel = viewModel,
                onOpenDetail = onPublished,
                onCreateNew = { activeTab = 0 }
            )
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
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }

    // Step 1 fields (Mortgage above Sale & Rent)
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

    // Step 2: Effortless Plot Size, Multi-Floor, Draw-to-Divide, Paint & Flooring Designer
    var plotWidthM by remember { mutableDoubleStateOf(16.0) }
    var plotLengthM by remember { mutableDoubleStateOf(22.0) }
    var ceilingHeightM by remember { mutableDoubleStateOf(3.5) }
    val availableFloors = remember { mutableStateListOf(1) }
    var activeFloor by remember { mutableIntStateOf(1) }
    var isDrawMode by remember { mutableStateOf(true) }
    var selectedRoomIdx by remember { mutableIntStateOf(0) }

    val rooms = remember {
        mutableStateListOf(
            RoomSpec(
                nameEn = "Main Reception",
                nameAr = "الاستقبال الرئيسي",
                widthM = 6.0,
                lengthM = 5.5,
                ceilingM = 3.5,
                paintNameEn = "Warm Sand",
                paintNameAr = "رمال دافئة",
                paintHex = "#EAE0D0",
                flooringEn = "Travertine Marble",
                flooringAr = "رخام ترافرتين",
                floor = 1,
                xNorm = 0.02,
                yNorm = 0.04,
                wNorm = 0.46,
                hNorm = 0.56
            ),
            RoomSpec(
                nameEn = "Modern Kitchen",
                nameAr = "المطبخ العصري",
                widthM = 4.8,
                lengthM = 4.0,
                ceilingM = 3.5,
                paintNameEn = "Pure Cotton",
                paintNameAr = "قطني نقي",
                paintHex = "#F5F6F8",
                flooringEn = "Porcelain Slab",
                flooringAr = "ألواح بورسلان",
                floor = 1,
                xNorm = 0.52,
                yNorm = 0.04,
                wNorm = 0.46,
                hNorm = 0.44
            )
        )
    }
    var newRoomName by remember { mutableStateOf("") }
    var newRoomW by remember { mutableStateOf("4.5") }
    var newRoomL by remember { mutableStateOf("5.0") }
    var activePaint by remember { mutableStateOf(designPaintPalette[2]) }
    var activeFlooring by remember { mutableStateOf(designFlooringOptions[1]) }

    fun floorDisplayLabel(fl: Int): String =
        if (fl <= 1) strings.floorGround else "${strings.floorLabelPrefix} $fl"

    fun nextAutoSlotForFloor(fl: Int, wM: Double, lM: Double): List<Double> {
        val countOnFloor = rooms.count { it.floor == fl }
        val col = countOnFloor % 2
        val row = (countOnFloor / 2) % 2
        val wNorm = (wM / plotWidthM).coerceIn(0.22, 0.46)
        val hNorm = (lM / plotLengthM).coerceIn(0.22, 0.44)
        val xNorm = if (col == 0) 0.02 else (0.98 - wNorm).coerceAtLeast(0.50)
        val yNorm = if (row == 0) 0.04 else (0.96 - hNorm).coerceAtLeast(0.50)
        return listOf(xNorm, yNorm, wNorm, hNorm)
    }

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
        if (uris.isNotEmpty()) {
            // Clear preset placeholder tokens when the user uploads real gallery photos
            if (galleryImages.all { it.startsWith("preset:") }) {
                galleryImages.clear()
            }
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                val uriStr = uri.toString()
                if (galleryImages.size < 20 && !galleryImages.contains(uriStr)) {
                    galleryImages.add(0, uriStr)
                }
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
            } catch (_: Exception) {
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
    var contactPhone by remember { mutableStateOf("07740080310") }

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
                    onClick = {
                        errorMsg = null
                        step = idx
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        when (step) {
            0 -> {
                // Listing Category: Mortgage ABOVE Sale & Rent
                Text(
                    text = if (strings.isArabic) "نوع العرض" else "Listing Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "MORTGAGE" to strings.forMortgage,
                        "SALE" to strings.forSale,
                        "RENT" to strings.forRent
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = listingType == code,
                            onClick = { listingType = code },
                            label = { Text(label, fontWeight = if (listingType == code) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("publish_listing_${code.lowercase()}")
                        )
                    }
                }

                // Pure Arabic Property Types
                Text(
                    text = if (strings.isArabic) "نوع العقار" else "Property Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(if (strings.isArabic) "عنوان العقار" else "Property Title") },
                    placeholder = { Text(if (strings.isArabic) "مثال: دار حديثة في المنصور" else "e.g., Modern Residence in Al-Mansour") },
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
                        label = { Text(strings.areaUnit) },
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
                Text(
                    text = strings.architecturalPlanTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // 1. Plot Dimensions Card (Easy Presets + Steppers)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${strings.plotSizeTitle}: ${plotWidthM}×${plotLengthM}${strings.meterUnit} (${(plotWidthM * plotLengthM).roundToInt()} ${strings.areaUnit})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple(10.0, 20.0, "10×20م"),
                                Triple(12.5, 20.0, "12.5×20م"),
                                Triple(15.0, 20.0, "15×20م"),
                                Triple(20.0, 25.0, "20×25م")
                            ).forEach { (w, l, lbl) ->
                                FilterChip(
                                    selected = plotWidthM == w && plotLengthM == l,
                                    onClick = {
                                        plotWidthM = w
                                        plotLengthM = l
                                    },
                                    label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactStepper(
                                label = if (strings.isArabic) "العرض" else "Width",
                                valueText = "${plotWidthM}${strings.meterUnit}",
                                onMinus = { plotWidthM = (plotWidthM - 1.0).coerceAtLeast(4.0) },
                                onPlus = { plotWidthM = (plotWidthM + 1.0).coerceAtMost(80.0) },
                                modifier = Modifier.weight(1f)
                            )
                            CompactStepper(
                                label = if (strings.isArabic) "الطول" else "Length",
                                valueText = "${plotLengthM}${strings.meterUnit}",
                                onMinus = { plotLengthM = (plotLengthM - 1.0).coerceAtLeast(4.0) },
                                onPlus = { plotLengthM = (plotLengthM + 1.0).coerceAtMost(80.0) },
                                modifier = Modifier.weight(1f)
                            )
                            CompactStepper(
                                label = if (strings.isArabic) "السقف" else "Ceiling",
                                valueText = "${ceilingHeightM}${strings.meterUnit}",
                                onMinus = { ceilingHeightM = (((ceilingHeightM - 0.1) * 10).roundToInt() / 10.0).coerceAtLeast(2.4) },
                                onPlus = { ceilingHeightM = (((ceilingHeightM + 0.1) * 10).roundToInt() / 10.0).coerceAtMost(6.0) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 2. Floors Selector & Add Floor Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    availableFloors.forEach { fl ->
                        FilterChip(
                            selected = activeFloor == fl,
                            onClick = {
                                activeFloor = fl
                                val firstOnFloor = rooms.indexOfFirst { it.floor == fl }
                                if (firstOnFloor >= 0) selectedRoomIdx = firstOnFloor
                            },
                            label = { Text(floorDisplayLabel(fl), fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            if (availableFloors.size < 8) {
                                val nextFloor = (availableFloors.maxOrNull() ?: 1) + 1
                                availableFloors.add(nextFloor)
                                activeFloor = nextFloor
                            }
                        },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_floor_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addFloorLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }

                // Quick Duplicate Ground Floor button if active floor has no rooms yet
                val activeFloorRoomsWithIdx = rooms.mapIndexed { idx, r -> idx to r }
                    .filter { (_, r) -> r.floor == activeFloor }
                if (activeFloor > 1 && activeFloorRoomsWithIdx.isEmpty() && rooms.any { it.floor == 1 }) {
                    OutlinedButton(
                        onClick = {
                            val copied = rooms.filter { it.floor == 1 }.map { r ->
                                r.copy(floor = activeFloor)
                            }
                            rooms.addAll(copied)
                            selectedRoomIdx = rooms.lastIndex
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (strings.isArabic) "نسخ تصميم الطابق الأرضي لهذا الطابق" else "Copy Ground Floor Layout Here")
                    }
                }

                // 3. Draw vs Select Mode + Quick Room Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isDrawMode,
                        onClick = { isDrawMode = true },
                        leadingIcon = { Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(strings.drawModeLabel, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).testTag("floorplan_draw_mode_chip")
                    )
                    FilterChip(
                        selected = !isDrawMode,
                        onClick = { isDrawMode = false },
                        leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(strings.selectModeLabel, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).testTag("floorplan_select_mode_chip")
                    )
                }

                if (isDrawMode) {
                    Text(
                        text = strings.drawHintText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 1-Tap Quick Room Presets Row
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickRoomPresets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    if (rooms.size < 30) {
                                        val slot = nextAutoSlotForFloor(activeFloor, preset.widthM, preset.lengthM)
                                        rooms.add(
                                            RoomSpec(
                                                nameEn = preset.nameEn,
                                                nameAr = preset.nameAr,
                                                widthM = preset.widthM,
                                                lengthM = preset.lengthM,
                                                ceilingM = ceilingHeightM,
                                                paintNameEn = preset.paintEn,
                                                paintNameAr = preset.paintAr,
                                                paintHex = preset.paintHex,
                                                flooringEn = activeFlooring.nameEn,
                                                flooringAr = activeFlooring.nameAr,
                                                floor = activeFloor,
                                                xNorm = slot[0],
                                                yNorm = slot[1],
                                                wNorm = slot[2],
                                                hNorm = slot[3]
                                            )
                                        )
                                        selectedRoomIdx = rooms.lastIndex
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = if (strings.isArabic) preset.nameAr else preset.nameEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // 4. Live Interactive Canvas (Drag to Draw/Divide OR Tap to Select)
                val canvasSelectedLocalIdx = activeFloorRoomsWithIdx.indexOfFirst { it.first == selectedRoomIdx }
                ArchitecturalFloorPlanCanvas(
                    rooms = activeFloorRoomsWithIdx.map { it.second },
                    totalWidthM = plotWidthM,
                    totalLengthM = plotLengthM,
                    selectedRoomIndex = canvasSelectedLocalIdx,
                    drawModeEnabled = isDrawMode,
                    onSelectRoom = { localIdx ->
                        activeFloorRoomsWithIdx.getOrNull(localIdx)?.let { (globalIdx, _) ->
                            selectedRoomIdx = globalIdx
                        }
                    },
                    onDrawRoomRect = { xN, yN, wN, hN ->
                        if (rooms.size < 30) {
                            val wMeters = (((wN * plotWidthM) * 10.0).roundToInt() / 10.0).coerceAtLeast(1.0)
                            val lMeters = (((hN * plotLengthM) * 10.0).roundToInt() / 10.0).coerceAtLeast(1.0)
                            val roomNum = activeFloorRoomsWithIdx.size + 1
                            val labelAr = "غرفة $roomNum"
                            val labelEn = "Room $roomNum"
                            rooms.add(
                                RoomSpec(
                                    nameEn = labelEn,
                                    nameAr = labelAr,
                                    widthM = wMeters,
                                    lengthM = lMeters,
                                    ceilingM = ceilingHeightM,
                                    paintNameEn = activePaint.nameEn,
                                    paintNameAr = activePaint.nameAr,
                                    paintHex = activePaint.hex,
                                    flooringEn = activeFlooring.nameEn,
                                    flooringAr = activeFlooring.nameAr,
                                    floor = activeFloor,
                                    xNorm = xN.toDouble(),
                                    yNorm = yN.toDouble(),
                                    wNorm = wN.toDouble(),
                                    hNorm = hN.toDouble()
                                )
                            )
                            selectedRoomIdx = rooms.lastIndex
                        }
                    }
                )

                // 5. Active Floor Rooms Quick Pills
                if (activeFloorRoomsWithIdx.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeFloorRoomsWithIdx.forEach { (globalIdx, room) ->
                            FilterChip(
                                selected = globalIdx == selectedRoomIdx,
                                onClick = { selectedRoomIdx = globalIdx },
                                label = {
                                    Text(
                                        text = "${if (strings.isArabic) room.nameAr else room.nameEn} (${room.widthM}×${room.lengthM})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }

                // 6. Selected Room Visual Editor (Rename, Step Dimensions, 1-Tap Split, Paint, Flooring)
                val editRoom = rooms.getOrNull(selectedRoomIdx)?.takeIf { it.floor == activeFloor }
                    ?: activeFloorRoomsWithIdx.firstOrNull()?.let { (gIdx, r) ->
                        selectedRoomIdx = gIdx
                        r
                    }

                if (editRoom != null && selectedRoomIdx in rooms.indices) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (strings.isArabic) "تعديل الغرفة المحددة (${floorDisplayLabel(editRoom.floor)})"
                                    else "Edit Selected Room (${floorDisplayLabel(editRoom.floor)})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        if (rooms.size > 1) {
                                            rooms.removeAt(selectedRoomIdx)
                                            selectedRoomIdx = rooms.indexOfFirst { it.floor == activeFloor }.coerceAtLeast(0)
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = strings.deleteListing, modifier = Modifier.size(18.dp))
                                }
                            }

                            OutlinedTextField(
                                value = if (strings.isArabic) editRoom.nameAr else editRoom.nameEn,
                                onValueChange = { txt ->
                                    rooms[selectedRoomIdx] = editRoom.copy(nameEn = txt, nameAr = txt)
                                },
                                label = { Text(if (strings.isArabic) "اسم الغرفة" else "Room Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Dimensions +/- 0.5m Steppers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompactStepper(
                                    label = if (strings.isArabic) "عرض الغرفة" else "Width",
                                    valueText = "${editRoom.widthM}${strings.meterUnit}",
                                    onMinus = {
                                        val nw = (((editRoom.widthM - 0.5) * 10).roundToInt() / 10.0).coerceAtLeast(1.0)
                                        val newWNorm = if (editRoom.xNorm >= 0) (nw / plotWidthM).coerceIn(0.08, 1.0 - editRoom.xNorm) else editRoom.wNorm
                                        rooms[selectedRoomIdx] = editRoom.copy(widthM = nw, wNorm = newWNorm)
                                    },
                                    onPlus = {
                                        val nw = (((editRoom.widthM + 0.5) * 10).roundToInt() / 10.0).coerceAtMost(plotWidthM)
                                        val newWNorm = if (editRoom.xNorm >= 0) (nw / plotWidthM).coerceIn(0.08, 1.0 - editRoom.xNorm) else editRoom.wNorm
                                        rooms[selectedRoomIdx] = editRoom.copy(widthM = nw, wNorm = newWNorm)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                CompactStepper(
                                    label = if (strings.isArabic) "طول الغرفة" else "Length",
                                    valueText = "${editRoom.lengthM}${strings.meterUnit}",
                                    onMinus = {
                                        val nl = (((editRoom.lengthM - 0.5) * 10).roundToInt() / 10.0).coerceAtLeast(1.0)
                                        val newHNorm = if (editRoom.yNorm >= 0) (nl / plotLengthM).coerceIn(0.08, 1.0 - editRoom.yNorm) else editRoom.hNorm
                                        rooms[selectedRoomIdx] = editRoom.copy(lengthM = nl, hNorm = newHNorm)
                                    },
                                    onPlus = {
                                        val nl = (((editRoom.lengthM + 0.5) * 10).roundToInt() / 10.0).coerceAtMost(plotLengthM)
                                        val newHNorm = if (editRoom.yNorm >= 0) (nl / plotLengthM).coerceIn(0.08, 1.0 - editRoom.yNorm) else editRoom.hNorm
                                        rooms[selectedRoomIdx] = editRoom.copy(lengthM = nl, hNorm = newHNorm)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 1-Tap Split / Divide Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (rooms.size < 30 && editRoom.widthM >= 2.0) {
                                            val halfW = (((editRoom.widthM / 2.0) * 10).roundToInt() / 10.0).coerceAtLeast(1.0)
                                            val baseX = if (editRoom.xNorm >= 0.0) editRoom.xNorm else 0.04
                                            val baseY = if (editRoom.yNorm >= 0.0) editRoom.yNorm else 0.04
                                            val baseWN = if (editRoom.wNorm > 0.0) editRoom.wNorm else 0.90
                                            val baseHN = if (editRoom.hNorm > 0.0) editRoom.hNorm else 0.44
                                            val halfWN = (baseWN / 2.0).coerceAtLeast(0.08)
                                            rooms[selectedRoomIdx] = editRoom.copy(
                                                widthM = halfW,
                                                xNorm = baseX,
                                                yNorm = baseY,
                                                wNorm = (halfWN - 0.01).coerceAtLeast(0.07),
                                                hNorm = baseHN
                                            )
                                            val suffixEn = "${editRoom.nameEn} 2"
                                            val suffixAr = "${editRoom.nameAr} ٢"
                                            rooms.add(
                                                selectedRoomIdx + 1,
                                                editRoom.copy(
                                                    nameEn = suffixEn,
                                                    nameAr = suffixAr,
                                                    widthM = halfW,
                                                    xNorm = (baseX + halfWN + 0.01).coerceAtMost(0.92),
                                                    yNorm = baseY,
                                                    wNorm = (halfWN - 0.01).coerceAtLeast(0.07),
                                                    hNorm = baseHN
                                                )
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("split_room_horizontal_btn")
                                ) {
                                    Icon(Icons.Default.ViewColumn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.splitHorizLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (rooms.size < 30 && editRoom.lengthM >= 2.0) {
                                            val halfL = (((editRoom.lengthM / 2.0) * 10).roundToInt() / 10.0).coerceAtLeast(1.0)
                                            val baseX = if (editRoom.xNorm >= 0.0) editRoom.xNorm else 0.04
                                            val baseY = if (editRoom.yNorm >= 0.0) editRoom.yNorm else 0.04
                                            val baseWN = if (editRoom.wNorm > 0.0) editRoom.wNorm else 0.44
                                            val baseHN = if (editRoom.hNorm > 0.0) editRoom.hNorm else 0.90
                                            val halfHN = (baseHN / 2.0).coerceAtLeast(0.08)
                                            rooms[selectedRoomIdx] = editRoom.copy(
                                                lengthM = halfL,
                                                xNorm = baseX,
                                                yNorm = baseY,
                                                wNorm = baseWN,
                                                hNorm = (halfHN - 0.01).coerceAtLeast(0.07)
                                            )
                                            val suffixEn = "${editRoom.nameEn} 2"
                                            val suffixAr = "${editRoom.nameAr} ٢"
                                            rooms.add(
                                                selectedRoomIdx + 1,
                                                editRoom.copy(
                                                    nameEn = suffixEn,
                                                    nameAr = suffixAr,
                                                    lengthM = halfL,
                                                    xNorm = baseX,
                                                    yNorm = (baseY + halfHN + 0.01).coerceAtMost(0.92),
                                                    wNorm = baseWN,
                                                    hNorm = (halfHN - 0.01).coerceAtLeast(0.07)
                                                )
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("split_room_vertical_btn")
                                ) {
                                    Icon(Icons.Default.Splitscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.splitVertLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }

                            // Visual Paint Picker (Live updates selected room on blueprint!)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "${strings.paintFinishLabel}: ${if (strings.isArabic) editRoom.paintNameAr else editRoom.paintNameEn}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                designPaintPalette.forEach { option ->
                                    val selected = editRoom.paintHex.equals(option.hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(parseColorHex(option.hex))
                                            .border(
                                                width = if (selected) 3.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                activePaint = option
                                                rooms[selectedRoomIdx] = editRoom.copy(
                                                    paintHex = option.hex,
                                                    paintNameEn = option.nameEn,
                                                    paintNameAr = option.nameAr
                                                )
                                            }
                                    )
                                }
                            }

                            // Flooring Chips
                            Text(
                                text = strings.flooringLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                designFlooringOptions.forEach { flOpt ->
                                    val selected = editRoom.flooringEn == flOpt.nameEn
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            activeFlooring = flOpt
                                            rooms[selectedRoomIdx] = editRoom.copy(
                                                flooringEn = flOpt.nameEn,
                                                flooringAr = flOpt.nameAr
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = if (strings.isArabic) flOpt.nameAr else flOpt.nameEn,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. Manual Exact Room Entry Option
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OutlinedTextField(
                    value = newRoomName,
                    onValueChange = { newRoomName = it },
                    label = { Text(if (strings.isArabic) "إضافة غرفة مخصصة (الاسم)" else "Add Custom Room (Name)") },
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
                        if (newRoomName.isNotBlank() && rooms.size < 30) {
                            val wVal = parseLocalizedDouble(newRoomW)?.coerceIn(1.0, 50.0) ?: 4.0
                            val lVal = parseLocalizedDouble(newRoomL)?.coerceIn(1.0, 50.0) ?: 5.0
                            val slot = nextAutoSlotForFloor(activeFloor, wVal, lVal)
                            rooms.add(
                                RoomSpec(
                                    nameEn = newRoomName.trim(),
                                    nameAr = newRoomName.trim(),
                                    widthM = wVal,
                                    lengthM = lVal,
                                    ceilingM = ceilingHeightM,
                                    paintNameEn = activePaint.nameEn,
                                    paintNameAr = activePaint.nameAr,
                                    paintHex = activePaint.hex,
                                    flooringEn = activeFlooring.nameEn,
                                    flooringAr = activeFlooring.nameAr,
                                    floor = activeFloor,
                                    xNorm = slot[0],
                                    yNorm = slot[1],
                                    wNorm = slot[2],
                                    hNorm = slot[3]
                                )
                            )
                            selectedRoomIdx = rooms.lastIndex
                            newRoomName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("${strings.addRoom} (${floorDisplayLabel(activeFloor)})")
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
                        if (index in galleryImages.indices) {
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
                                    sceneId = "scene_${System.currentTimeMillis()}_${scenes.size + 1}",
                                    titleEn = title,
                                    titleAr = title,
                                    presetKey = token.removePrefix("preset:"),
                                    initialYaw = 0.0
                                )
                            } else {
                                pickedPhotoUri = token
                                TourScene(
                                    sceneId = "scene_${System.currentTimeMillis()}_${scenes.size + 1}",
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
                                    sceneId = "scene_${System.currentTimeMillis()}_${scenes.size + 1}",
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (strings.isArabic) s.titleAr else s.titleEn,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(18.dp))
                                if (scenes.size > 1) {
                                    IconButton(
                                        onClick = {
                                            scenes.removeAt(idx)
                                            previewSceneIdx = previewSceneIdx.coerceIn(0, (scenes.size - 1).coerceAtLeast(0))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = strings.deleteListing,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
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
                            val dist = parseLocalizedLong(newLandmarkDist)?.coerceIn(10L, 50000L) ?: 350L
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

                landmarks.forEachIndexed { idx, lm ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (strings.isArabic) lm.nameAr else lm.nameEn} • ${lm.distanceMeters} ${strings.metersAway}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = { landmarks.removeAt(idx) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = strings.deleteListing, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
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
                    onClick = {
                        errorMsg = null
                        step -= 1
                    },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text(strings.prevStep)
                }
            }
            if (step < 3) {
                Button(
                    onClick = {
                        errorMsg = null
                        step += 1
                    },
                    modifier = Modifier.weight(1f).height(50.dp).testTag("publish_next_step")
                ) {
                    Text(strings.nextStep)
                }
            } else {
                Button(
                    onClick = {
                        val parsedPrice = parseLocalizedDouble(priceInput)
                        val parsedArea = parseLocalizedDouble(areaInput)
                        val cleanDeed = deedNumber.trim()
                        val cleanLicense = licenseNumber.trim()
                        val cleanPhone = contactPhone.trim()

                        if (parsedPrice == null || parsedPrice <= 0.0) {
                            errorMsg = if (strings.isArabic) "يرجى إدخال سعر صحيح للعقار" else "Please enter a valid property price."
                            return@Button
                        }
                        if (parsedArea == null || parsedArea <= 0.0) {
                            errorMsg = if (strings.isArabic) "يرجى إدخال مساحة صحيحة" else "Please enter a valid area."
                            return@Button
                        }
                        if (cleanDeed.length < 3 || cleanLicense.length < 3 || cleanPhone.length < 5) {
                            errorMsg = if (strings.isArabic) "يرجى التحقق من رقم الطابو وإجازة الدلالية ورقم الهاتف"
                            else "Please verify Tapu deed, license, and contact phone."
                            return@Button
                        }

                        val cleanNeighborhood = neighborhoodInput.trim().ifBlank {
                            if (strings.isArabic) "المنصور" else "Al-Mansour"
                        }
                        val cleanCity = cityInput.trim().ifBlank {
                            if (strings.isArabic) "بغداد" else "Baghdad"
                        }
                        val cleanTitle = titleInput.trim().let {
                            if (it.length >= 3) it
                            else if (strings.isArabic) "${strings.localizePropertyType(propertyType)} ${strings.localizeListingType(listingType)} في $cleanNeighborhood"
                            else "${strings.localizePropertyType(propertyType)} ${strings.localizeListingType(listingType)} in $cleanNeighborhood"
                        }
                        val cleanDesc = descInput.trim().let {
                            if (it.length >= 10) it
                            else if (strings.isArabic) "عقار مميز موثق عبر شبكة دلال العراق بتفاصيل الغرف والطوابق وألوان الطلاء والجولة الافتراضية ٣٦٠°."
                            else "Verified Iraq Dallal Network property complete with floor plans, room specs, paint codes, and 360° virtual tour."
                        }
                        val safeGallery = galleryImages.ifEmpty { listOf("preset:villa_exterior") }.take(20).toList()

                        submitting = true
                        errorMsg = null
                        coroutineScope.launch {
                            val (resolvedLat, resolvedLng) = viewModel.resolveCityNeighborhoodCoords(
                                city = cleanCity,
                                neighborhood = cleanNeighborhood,
                                isArabic = strings.isArabic
                            )
                            val listing = PropertyListing(
                                titleEn = cleanTitle,
                                titleAr = cleanTitle,
                                descriptionEn = cleanDesc,
                                descriptionAr = cleanDesc,
                                listingType = listingType,
                                propertyType = propertyType,
                                price = parsedPrice.coerceAtLeast(1000.0),
                                currency = "IQD",
                                areaSqm = parsedArea.coerceAtLeast(20.0),
                                bedrooms = parseLocalizedLong(bedsInput)?.coerceIn(0L, 30L) ?: 3L,
                                bathrooms = parseLocalizedLong(bathsInput)?.coerceIn(0L, 30L) ?: 3L,
                                yearBuilt = parseLocalizedLong(yearBuiltInput)?.coerceIn(1950L, 2030L) ?: 2025L,
                                amenities = selectedAmenities.toList(),
                                cityEn = cleanCity,
                                cityAr = cleanCity,
                                neighborhoodEn = cleanNeighborhood,
                                neighborhoodAr = cleanNeighborhood,
                                latitude = resolvedLat,
                                longitude = resolvedLng,
                                heroPresetKey = if (propertyType == "PENTHOUSE") "penthouse_exterior" else "villa_exterior",
                                galleryImages = safeGallery,
                                ceilingHeightM = ceilingHeightM,
                                floorPlanWidthM = plotWidthM,
                                floorPlanLengthM = plotLengthM,
                                rooms = rooms.toList(),
                                tourScenes = scenes.toList(),
                                nearbyLandmarks = landmarks.toList()
                            )
                            viewModel.publishProperty(
                                listing = listing,
                                fullDeedNumber = cleanDeed,
                                licenseNumber = cleanLicense,
                                contactPhone = cleanPhone
                            ) { res ->
                                submitting = false
                                res.onSuccess { id -> onPublished(id) }
                                res.onFailure { err -> errorMsg = err.localizedMessage }
                            }
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
private fun CompactStepper(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onMinus)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlus)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MyListingsPane(
    viewModel: DiyarViewModel,
    onOpenDetail: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val myState by viewModel.myPropertiesState.collectAsStateWithLifecycle()

    when (val state = myState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = strings.emptyMyListingsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(onClick = onCreateNew) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.createListingTab)
                        }
                    }
                }
            } else {
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
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (strings.isArabic) prop.titleAr else prop.titleEn,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${strings.localizeListingType(prop.listingType)} • ${formatCurrencyPrice(prop.price, prop.currency, prop.listingType == "RENT", strings.isArabic)} • ${strings.localizePropertyStatus(prop.status)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteProperty(prop.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = strings.deleteListing, tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { openPropertyWhatsApp(context, prop, strings) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VerifiedEmerald,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.contactWhatsapp, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { sharePropertyExternally(context, prop, strings) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.shareProperty, style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (prop.status == "ACTIVE") {
                                        OutlinedButton(
                                            onClick = { viewModel.markPropertySold(prop.id) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(strings.markSold, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
