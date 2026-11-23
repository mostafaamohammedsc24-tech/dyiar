package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DiyarMapType
import com.example.data.model.ListingSort
import com.example.data.model.MapPointOfInterest
import com.example.data.model.PropertyFilter
import com.example.data.model.SearchSuggestionSource
import com.example.data.model.SmartSearchSuggestion
import com.example.data.model.UiState
import com.example.i18n.LocalStrings
import com.example.ui.components.DiyarInteractiveMap
import com.example.ui.components.MapPropertyPreviewCard
import com.example.ui.components.MapRailCategory
import com.example.ui.components.PropertyReportCard
import com.example.ui.components.VerticalMapCategorySelector
import com.example.ui.components.formatCurrencyPrice
import com.example.ui.components.hasConfiguredMapsApiKey
import com.example.ui.viewmodel.DiyarViewModel
import com.google.android.gms.location.LocationServices
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: DiyarViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenPropertyDetail: (String) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val propertiesState by viewModel.filteredPropertiesState.collectAsStateWithLifecycle()
    val allActivePropsState by viewModel.activePropertiesState.collectAsStateWithLifecycle()
    val pois by viewModel.mapPointsOfInterest.collectAsStateWithLifecycle()
    val activePoiCat by viewModel.activePoiCategory.collectAsStateWithLifecycle()
    val mapFocusTarget by viewModel.mapFocusTarget.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val selectedPropertyId by viewModel.selectedPropertyId.collectAsStateWithLifecycle()
    val unreadAlerts by viewModel.unreadAlertsCount.collectAsStateWithLifecycle()
    val searchedPlaceMarker by viewModel.searchedPlaceMarker.collectAsStateWithLifecycle()
    val smartSuggestions by viewModel.smartSearchSuggestions.collectAsStateWithLifecycle()
    val isSearchingSuggestions by viewModel.isSearchingSuggestions.collectAsStateWithLifecycle()
    val isMapZoomedForPois by viewModel.isMapZoomedForPois.collectAsStateWithLifecycle()
    val requestedMapZoom by viewModel.requestedMapZoom.collectAsStateWithLifecycle()

    var showListMode by remember { mutableStateOf(false) }
    var useGoogleTiles by remember { mutableStateOf(hasConfiguredMapsApiKey()) }
    var mapType by remember { mutableStateOf(DiyarMapType.NORMAL) }
    var showMapTypeMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf(filter.query) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showSuggestionsDropdown by remember { mutableStateOf(false) }
    var selectedPoi by remember { mutableStateOf<MapPointOfInterest?>(null) }

    val totalResults = (propertiesState as? UiState.Success)?.data?.size ?: 0
    val allActiveProps = (allActivePropsState as? UiState.Success)?.data.orEmpty()
    val mortgageCount = allActiveProps.count { it.listingType == "MORTGAGE" && it.propertyType != "COMMERCIAL" }
    val saleCount = allActiveProps.count { it.listingType == "SALE" && it.propertyType != "COMMERCIAL" }
    val rentCount = allActiveProps.count { it.listingType == "RENT" && it.propertyType != "COMMERCIAL" }
    val commercialCount = allActiveProps.count { it.propertyType == "COMMERCIAL" }
    val activeRailCategory = when {
        filter.propertyType == "COMMERCIAL" -> MapRailCategory.COMMERCIAL
        filter.listingType == "MORTGAGE" -> MapRailCategory.MORTGAGE
        filter.listingType == "SALE" -> MapRailCategory.SALE
        filter.listingType == "RENT" -> MapRailCategory.RENT
        else -> null
    }

    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    fun centerOnCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                viewModel.updateGeoCenter(loc.latitude, loc.longitude, enableRadius = true)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) centerOnCurrentLocation()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isCompactPhone = maxWidth < 600.dp
        val isShortViewport = maxHeight < 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                // Minimal Mobile-First Top Bar: keeps map viewport maximized on phones
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(
                                horizontal = if (isCompactPhone) 10.dp else 14.dp,
                                vertical = if (isShortViewport) 4.dp else 6.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(if (isShortViewport) 4.dp else 5.dp)
                    ) {
                        // Compact Top Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = strings.appName,
                                style = if (isCompactPhone) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!showListMode) {
                                    Box {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .clickable { showMapTypeMenu = true }
                                                .testTag("map_type_button")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Layers,
                                                    contentDescription = strings.mapTypeNormal,
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = showMapTypeMenu,
                                            onDismissRequest = { showMapTypeMenu = false }
                                        ) {
                                            listOf(
                                                DiyarMapType.NORMAL to strings.mapTypeNormal,
                                                DiyarMapType.SATELLITE to strings.mapTypeSatellite,
                                                DiyarMapType.HYBRID to strings.mapTypeHybrid,
                                                DiyarMapType.TERRAIN to strings.mapTypeTerrain
                                            ).forEach { (mt, label) ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = label,
                                                            fontWeight = if (mapType == mt) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (mapType == mt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    onClick = {
                                                        mapType = mt
                                                        showMapTypeMenu = false
                                                    }
                                                )
                                            }
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = if (useGoogleTiles) strings.interactiveVectorMode else strings.googleMapMode,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                onClick = {
                                                    useGoogleTiles = !useGoogleTiles
                                                    showMapTypeMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { showListMode = !showListMode }
                                        .testTag("toggle_map_list_view")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (showListMode) Icons.Default.Map else Icons.Default.FormatListBulleted,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = "${if (showListMode) strings.mapView else strings.listView} ($totalResults)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onToggleTheme,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("header_theme_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = if (isDark) strings.lightMode else strings.darkMode,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(onClick = onToggleLanguage)
                                        .testTag("header_language_toggle")
                                ) {
                                    Text(
                                        text = if (strings.isArabic) "EN" else "ع",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onOpenAlerts,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("header_alerts_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadAlerts > 0) {
                                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                                    Text(unreadAlerts.toString(), color = Color.White)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = strings.navAlerts,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Compact 38dp Search Bar + Orange Filter CTA
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSearchFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchInput.isEmpty()) {
                                            Text(
                                                text = strings.searchPlaceholder,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        BasicTextField(
                                            value = searchInput,
                                            onValueChange = { txt ->
                                                searchInput = txt
                                                showSuggestionsDropdown = true
                                                viewModel.onSearchInputChanged(txt, strings.isArabic)
                                                if (txt.isBlank()) {
                                                    selectedPoi = null
                                                }
                                            },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(
                                                onSearch = {
                                                    showSuggestionsDropdown = false
                                                    keyboard?.hide()
                                                    focusManager.clearFocus()
                                                    viewModel.searchLandmarkOrPlace(searchInput, strings.isArabic)
                                                }
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusChanged { focusState ->
                                                    isSearchFocused = focusState.isFocused
                                                    if (focusState.isFocused) {
                                                        showSuggestionsDropdown = true
                                                        viewModel.onSearchInputChanged(searchInput, strings.isArabic)
                                                    }
                                                }
                                                .testTag("discover_search_input")
                                        )
                                    }
                                    if (isSearchingSuggestions) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    if (searchInput.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                searchInput = ""
                                                selectedPoi = null
                                                showSuggestionsDropdown = false
                                                viewModel.onSearchInputChanged("", strings.isArabic)
                                                viewModel.clearSearchedPlace()
                                            },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                showSuggestionsDropdown = false
                                                keyboard?.hide()
                                                focusManager.clearFocus()
                                                viewModel.searchLandmarkOrPlace(searchInput, strings.isArabic)
                                            },
                                            modifier = Modifier
                                                .size(26.dp)
                                                .testTag("landmark_search_go")
                                        ) {
                                            Icon(
                                                Icons.Default.Place,
                                                contentDescription = strings.landmarksLayer,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { showFilterSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("open_filters_button")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = strings.filters, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.filters, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            // Flexible Viewport Container: Map & List automatically scale cleanly to exact available bounds
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val state = propertiesState) {
                    is UiState.Loading -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(strings.loadingText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    is UiState.Error -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    is UiState.Success -> {
                        val properties = state.data
                        val selectedProperty = if (selectedPropertyId != null) {
                            properties.firstOrNull { it.id == selectedPropertyId }
                        } else {
                            null
                        }

                        if (showListMode) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    DiscoverFilterPillStrip(
                                        filter = filter,
                                        activePoiCat = activePoiCat,
                                        overlayOnMap = false,
                                        onQuickFilter = viewModel::updateQuickFilter,
                                        onResetAll = {
                                            viewModel.updateQuickFilter("ALL", "ALL")
                                            viewModel.setPoiCategory("ALL")
                                        },
                                        onTogglePoi = { cat ->
                                            viewModel.setPoiCategory(if (activePoiCat == cat) "ALL" else cat)
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                if (properties.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(22.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Home,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Text(
                                                    text = strings.emptyMarketplaceTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = strings.emptyMarketplaceBody,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .testTag("discover_list_view")
                                    ) {
                                        items(properties, key = { it.id }) { prop ->
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                PropertyReportCard(
                                                    property = prop,
                                                    isFavorite = favoriteIds.contains(prop.id),
                                                    onToggleFavorite = { viewModel.toggleFavorite(prop) },
                                                    onClick = { onOpenPropertyDetail(prop.id) },
                                                    modifier = Modifier.widthIn(max = 600.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val targetLat = mapFocusTarget?.first ?: filter.centerLat
                            val targetLng = mapFocusTarget?.second ?: filter.centerLng
                            val hasBottomOverlay = selectedProperty != null || (selectedPoi ?: searchedPlaceMarker) != null
                            val mapViewportPadding = PaddingValues(
                                top = 46.dp,
                                start = if (isCompactPhone) 68.dp else 76.dp,
                                end = 12.dp,
                                bottom = if (hasBottomOverlay) 136.dp else 64.dp
                            )

                            DiyarInteractiveMap(
                                properties = properties,
                                pointsOfInterest = pois,
                                selectedPropertyId = selectedProperty?.id,
                                centerLat = targetLat,
                                centerLng = targetLng,
                                radiusKm = filter.radiusKm,
                                showRadiusCircle = filter.geoRadiusEnabled,
                                isDark = isDark,
                                useGoogleMapTiles = useGoogleTiles,
                                mapType = mapType,
                                onSelectProperty = {
                                    selectedPoi = null
                                    viewModel.selectProperty(it.id)
                                },
                                onSelectPoi = { poi ->
                                    selectedPoi = poi
                                    viewModel.selectProperty(null)
                                    viewModel.focusOnPoi(poi)
                                },
                                onClearSelection = {
                                    selectedPoi = null
                                    viewModel.clearSearchedPlace()
                                    viewModel.selectProperty(null)
                                },
                                isExplicitFocus = mapFocusTarget != null,
                                showPois = isMapZoomedForPois,
                                targetZoom = requestedMapZoom,
                                onZoomConsumed = viewModel::consumeRequestedZoom,
                                onCameraChanged = { lat, lng, zoom ->
                                    viewModel.onMapCameraChanged(lat, lng, zoom, strings.isArabic)
                                },
                                viewportPadding = mapViewportPadding,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Translucent Quick Filter & POI Pill Strip floating over top edge of map
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp, start = 10.dp, end = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DiscoverFilterPillStrip(
                                    filter = filter,
                                    activePoiCat = activePoiCat,
                                    overlayOnMap = true,
                                    onQuickFilter = viewModel::updateQuickFilter,
                                    onResetAll = {
                                        viewModel.updateQuickFilter("ALL", "ALL")
                                        viewModel.setPoiCategory("ALL")
                                    },
                                    onTogglePoi = { cat ->
                                        viewModel.setPoiCategory(if (activePoiCat == cat) "ALL" else cat)
                                    }
                                )

                                if (activePoiCat != "ALL" && !isMapZoomedForPois) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                        shadowElevation = 4.dp,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { viewModel.zoomInForPois() }
                                            .testTag("zoom_for_places_hint")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = strings.zoomInForPlacesHint,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // Floating Vertical Category Selector positioned cleanly below top pill strip
                            VerticalMapCategorySelector(
                                activeCategory = activeRailCategory,
                                mortgageCount = mortgageCount,
                                saleCount = saleCount,
                                rentCount = rentCount,
                                commercialCount = commercialCount,
                                onSelectCategory = { cat ->
                                    viewModel.toggleMapCategory(cat.name)
                                },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 10.dp, top = 46.dp)
                            )

                            // Bottom Overlay: Locate Me FAB floats dynamically above POI Banner & Dismissible Property Card
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    FloatingActionButton(
                                        onClick = {
                                            val granted = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (granted) centerOnCurrentLocation()
                                            else locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .size(44.dp)
                                            .testTag("locate_me_button")
                                    ) {
                                        Icon(
                                            Icons.Default.MyLocation,
                                            contentDescription = strings.locateMe,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                val poiBanner = selectedPoi ?: searchedPlaceMarker
                                if (poiBanner != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(14.dp),
                                        shadowElevation = 6.dp,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = if (strings.isArabic) poiBanner.nameAr else poiBanner.nameEn,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${strings.localizeLandmarkCategory(poiBanner.category)} • ${if (strings.isArabic) poiBanner.subtitleAr else poiBanner.subtitleEn}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    selectedPoi = null
                                                    viewModel.clearSearchedPlace()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = selectedProperty != null,
                                    enter = slideInVertically { it } + fadeIn(),
                                    exit = slideOutVertically { it } + fadeOut()
                                ) {
                                    if (selectedProperty != null) {
                                        MapPropertyPreviewCard(
                                            property = selectedProperty,
                                            isFavorite = favoriteIds.contains(selectedProperty.id),
                                            onToggleFavorite = { viewModel.toggleFavorite(selectedProperty) },
                                            onDismiss = { viewModel.selectProperty(null) },
                                            onClick = { onOpenPropertyDetail(selectedProperty.id) },
                                            modifier = Modifier
                                                .widthIn(max = 520.dp)
                                                .pointerInput(selectedProperty.id) {
                                                    detectVerticalDragGestures { _, dragAmount ->
                                                        if (dragAmount > 14f) {
                                                            viewModel.selectProperty(null)
                                                        }
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Search Autocomplete floats over the viewport without resizing or squishing the map
                AnimatedVisibility(
                    visible = showSuggestionsDropdown && smartSuggestions.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(10f)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    SmartSearchSuggestionsCard(
                        suggestions = smartSuggestions,
                        isQueryEmpty = searchInput.isBlank(),
                        isLoading = isSearchingSuggestions,
                        onSelectSuggestion = { suggestion ->
                            showSuggestionsDropdown = false
                            keyboard?.hide()
                            focusManager.clearFocus()
                            selectedPoi = null
                            viewModel.selectSearchSuggestion(suggestion, strings.isArabic) { applied ->
                                searchInput = applied
                            }
                        },
                        onDismiss = {
                            showSuggestionsDropdown = false
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        val currentCount = (propertiesState as? UiState.Success)?.data?.size ?: 0
        AdvancedFiltersSheet(
            currentFilter = filter,
            liveMatchCount = currentCount,
            onDismiss = { showFilterSheet = false },
            onUpdateRealTime = { viewModel.updateFilter(it) },
            onReset = {
                searchInput = ""
                viewModel.resetFilters()
                showFilterSheet = false
            }
        )
    }
}

@Composable
private fun SmartSearchSuggestionsCard(
    suggestions: List<SmartSearchSuggestion>,
    isQueryEmpty: Boolean,
    isLoading: Boolean,
    onSelectSuggestion: (SmartSearchSuggestion) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_suggestions_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isQueryEmpty) strings.popularIraqTitle else strings.smartSuggestionsTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.8.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
            ) {
                items(suggestions, key = { it.id }) { suggestion ->
                    val primary = if (strings.isArabic) suggestion.primaryTextAr else suggestion.primaryTextEn
                    val secondary = if (strings.isArabic) suggestion.secondaryTextAr else suggestion.secondaryTextEn

                    val icon = when (suggestion.category) {
                        "MORTGAGE" -> Icons.Default.AccountBalance
                        "SALE", "RENT", "PROPERTY" -> Icons.Default.Home
                        "COMMERCIAL" -> Icons.Default.Storefront
                        "HOSPITAL" -> Icons.Default.LocalHospital
                        "RESTAURANT" -> Icons.Default.Restaurant
                        "SCHOOL" -> Icons.Default.School
                        "MOSQUE" -> Icons.Default.Mosque
                        "STREET" -> Icons.Default.Directions
                        "CITY", "DISTRICT" -> Icons.Default.LocationCity
                        else -> Icons.Default.Place
                    }

                    val sourceBadge = when (suggestion.source) {
                        SearchSuggestionSource.GOOGLE_PLACES,
                        SearchSuggestionSource.GEOCODER -> strings.sourceGoogleMapsBadge
                        SearchSuggestionSource.IRAQ_DISTRICT -> strings.sourceDistrictBadge
                        SearchSuggestionSource.LANDMARK -> strings.sourceLandmarkBadge
                        SearchSuggestionSource.PROPERTY -> strings.sourcePropertyBadge
                        SearchSuggestionSource.CATEGORY_FILTER -> strings.sourceCategoryBadge
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSuggestion(suggestion) }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("suggestion_item_${suggestion.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = primary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = secondary,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = sourceBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.NorthWest,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = strings.poweredByGoogleMaps,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DiscoverFilterPillStrip(
    filter: PropertyFilter,
    activePoiCat: String,
    overlayOnMap: Boolean,
    onQuickFilter: (String, String) -> Unit,
    onResetAll: () -> Unit,
    onTogglePoi: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val pillAlpha = if (overlayOnMap) 0.92f else 1f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickChip(
            label = strings.all,
            selected = filter.listingType == "ALL" && filter.propertyType == "ALL" && activePoiCat == "ALL",
            surfaceAlpha = pillAlpha,
            onClick = onResetAll
        )
        QuickChip(
            label = strings.forMortgage,
            selected = filter.listingType == "MORTGAGE",
            surfaceAlpha = pillAlpha,
            onClick = {
                onQuickFilter(if (filter.listingType == "MORTGAGE") "ALL" else "MORTGAGE", filter.propertyType)
            }
        )
        QuickChip(
            label = strings.forSale,
            selected = filter.listingType == "SALE",
            surfaceAlpha = pillAlpha,
            onClick = {
                onQuickFilter(if (filter.listingType == "SALE") "ALL" else "SALE", filter.propertyType)
            }
        )
        QuickChip(
            label = strings.forRent,
            selected = filter.listingType == "RENT",
            surfaceAlpha = pillAlpha,
            onClick = {
                onQuickFilter(if (filter.listingType == "RENT") "ALL" else "RENT", filter.propertyType)
            }
        )
        QuickChip(
            label = strings.villa,
            selected = filter.propertyType == "VILLA",
            surfaceAlpha = pillAlpha,
            onClick = {
                onQuickFilter(filter.listingType, if (filter.propertyType == "VILLA") "ALL" else "VILLA")
            }
        )
        QuickChip(
            label = strings.apartment,
            selected = filter.propertyType == "APARTMENT",
            surfaceAlpha = pillAlpha,
            onClick = {
                onQuickFilter(filter.listingType, if (filter.propertyType == "APARTMENT") "ALL" else "APARTMENT")
            }
        )
        QuickChip(
            label = strings.penthouse,
            selected = filter.propertyType == "PENTHOUSE",
            surfaceAlpha = pillAlpha,
            onClick = {
                onQuickFilter(filter.listingType, if (filter.propertyType == "PENTHOUSE") "ALL" else "PENTHOUSE")
            }
        )
        QuickChip(
            label = strings.commercial,
            selected = filter.propertyType == "COMMERCIAL",
            surfaceAlpha = pillAlpha,
            onClick = {
                onQuickFilter(filter.listingType, if (filter.propertyType == "COMMERCIAL") "ALL" else "COMMERCIAL")
            }
        )
        QuickChip(
            label = strings.land,
            selected = filter.propertyType == "LAND",
            surfaceAlpha = pillAlpha,
            onClick = {
                onQuickFilter(filter.listingType, if (filter.propertyType == "LAND") "ALL" else "LAND")
            }
        )

        VerticalDivider(
            modifier = Modifier.height(20.dp).padding(horizontal = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        PoiPill(strings.hospitals, Icons.Default.LocalHospital, activePoiCat == "HOSPITAL", pillAlpha) {
            onTogglePoi("HOSPITAL")
        }
        PoiPill(strings.restaurantsCat, Icons.Default.Restaurant, activePoiCat == "RESTAURANT", pillAlpha) {
            onTogglePoi("RESTAURANT")
        }
        PoiPill(strings.schools, Icons.Default.School, activePoiCat == "SCHOOL", pillAlpha) {
            onTogglePoi("SCHOOL")
        }
        PoiPill(strings.streetsCat, Icons.Default.Directions, activePoiCat == "STREET", pillAlpha) {
            onTogglePoi("STREET")
        }
        PoiPill(strings.mosques, Icons.Default.Mosque, activePoiCat == "MOSQUE", pillAlpha) {
            onTogglePoi("MOSQUE")
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    selected: Boolean,
    surfaceAlpha: Float = 1f,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface.copy(alpha = surfaceAlpha),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (surfaceAlpha < 1f) 3.dp else 0.dp,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(
            0.6.dp,
            MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun PoiPill(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    surfaceAlpha: Float = 1f,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface.copy(alpha = surfaceAlpha),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (surfaceAlpha < 1f) 3.dp else 0.dp,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(
            0.6.dp,
            MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AdvancedFiltersSheet(
    currentFilter: PropertyFilter,
    liveMatchCount: Int,
    onDismiss: () -> Unit,
    onUpdateRealTime: (PropertyFilter) -> Unit,
    onReset: () -> Unit
) {
    val strings = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.filters,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            // Listing Type (Mortgage above Sale & Rent)
            Text(
                text = if (strings.isArabic) "نوع العرض" else "Listing Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to strings.all,
                    "MORTGAGE" to strings.forMortgage,
                    "SALE" to strings.forSale,
                    "RENT" to strings.forRent
                ).forEach { (code, label) ->
                    QuickChip(
                        label = label,
                        selected = currentFilter.listingType == code,
                        onClick = { onUpdateRealTime(currentFilter.copy(listingType = code)) }
                    )
                }
            }

            // Property Type
            Text(
                text = if (strings.isArabic) "نوع العقار" else "Property Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to strings.all,
                    "VILLA" to strings.villa,
                    "APARTMENT" to strings.apartment,
                    "PENTHOUSE" to strings.penthouse,
                    "TOWNHOUSE" to strings.townhouse,
                    "COMMERCIAL" to strings.commercial,
                    "LAND" to strings.land
                ).forEach { (code, label) ->
                    QuickChip(
                        label = label,
                        selected = currentFilter.propertyType == code,
                        onClick = { onUpdateRealTime(currentFilter.copy(propertyType = code)) }
                    )
                }
            }

            // Min & Max Price RangeSlider (Real-time in IQD)
            Text(
                text = "${strings.priceRange}: ${formatCurrencyPrice(currentFilter.minPrice, "IQD", false, strings.isArabic)} — ${formatCurrencyPrice(currentFilter.maxPrice, "IQD", false, strings.isArabic)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            RangeSlider(
                value = currentFilter.minPrice.toFloat().coerceIn(0f, 2_000_000_000f)..currentFilter.maxPrice.toFloat().coerceIn(10_000_000f, 2_000_000_000f),
                onValueChange = { range ->
                    onUpdateRealTime(
                        currentFilter.copy(
                            minPrice = range.start.toDouble(),
                            maxPrice = range.endInclusive.toDouble()
                        )
                    )
                },
                valueRange = 0f..2_000_000_000f
            )

            // Min Area Slider (Real-time)
            Text(
                text = "${strings.minArea}: ${currentFilter.minAreaSqm.roundToInt()} ${strings.areaUnit}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = currentFilter.minAreaSqm.toFloat().coerceIn(0f, 800f),
                onValueChange = { onUpdateRealTime(currentFilter.copy(minAreaSqm = it.toDouble())) },
                valueRange = 0f..800f
            )

            // Number of Rooms / Bedrooms (Real-time)
            Text(text = strings.minBedrooms, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 1, 2, 3, 4, 5).forEach { count ->
                    QuickChip(
                        label = if (count == 0) strings.all else "$count+",
                        selected = currentFilter.minBedrooms == count,
                        onClick = { onUpdateRealTime(currentFilter.copy(minBedrooms = count)) }
                    )
                }
            }

            // Amenities Multi-Select Chips (Pool, Garden, Elevator, Smart Home, Parking, etc.)
            Text(text = strings.amenitiesTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DiyarViewModel.availableAmenityCodes.forEach { code ->
                    val isSelected = currentFilter.selectedAmenities.contains(code)
                    QuickChip(
                        label = strings.localizeAmenity(code),
                        selected = isSelected,
                        onClick = {
                            val updated = if (isSelected) {
                                currentFilter.selectedAmenities - code
                            } else {
                                currentFilter.selectedAmenities + code
                            }
                            onUpdateRealTime(currentFilter.copy(selectedAmenities = updated))
                        }
                    )
                }
            }

            // Year Built Chips (Real-time)
            Text(text = strings.yearBuiltLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 2020, 2022, 2024, 2025).forEach { yr ->
                    QuickChip(
                        label = if (yr == 0) strings.anyYear else "$yr+",
                        selected = currentFilter.minYearBuilt == yr,
                        onClick = { onUpdateRealTime(currentFilter.copy(minYearBuilt = yr)) }
                    )
                }
            }

            // Geo Radius Toggle & Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.geoRadius}: ${currentFilter.radiusKm.roundToInt()} km",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = currentFilter.geoRadiusEnabled,
                    onCheckedChange = { onUpdateRealTime(currentFilter.copy(geoRadiusEnabled = it)) }
                )
            }
            if (currentFilter.geoRadiusEnabled) {
                Slider(
                    value = currentFilter.radiusKm.toFloat().coerceIn(2f, 100f),
                    onValueChange = { onUpdateRealTime(currentFilter.copy(radiusKm = it.toDouble())) },
                    valueRange = 2f..100f
                )
            }

            FilterSwitchRow(strings.verifiedOnly, currentFilter.verifiedOnly) {
                onUpdateRealTime(currentFilter.copy(verifiedOnly = it))
            }
            FilterSwitchRow(strings.with360Only, currentFilter.virtualTourOnly) {
                onUpdateRealTime(currentFilter.copy(virtualTourOnly = it))
            }
            FilterSwitchRow(strings.withFloorPlanOnly, currentFilter.floorPlanOnly) {
                onUpdateRealTime(currentFilter.copy(floorPlanOnly = it))
            }

            Text(text = strings.sortBy, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ListingSort.NEWEST to strings.sortNewest,
                    ListingSort.PRICE_ASC to strings.sortPriceAsc,
                    ListingSort.PRICE_DESC to strings.sortPriceDesc,
                    ListingSort.AREA_DESC to strings.sortAreaDesc
                ).forEach { (sortMode, label) ->
                    QuickChip(
                        label = label,
                        selected = currentFilter.sort == sortMode,
                        onClick = { onUpdateRealTime(currentFilter.copy(sort = sortMode)) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text(strings.resetFilters)
                }
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("apply_filters_button")
                ) {
                    Text("${strings.applyFilters} ($liveMatchCount)")
                }
            }
        }
    }
}

@Composable
private fun FilterSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
