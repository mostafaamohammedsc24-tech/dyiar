package com.example.ui.viewmodel

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlertRule
import com.example.data.model.FavoriteProperty
import com.example.data.model.ListingSort
import com.example.data.model.MapPointOfInterest
import com.example.data.model.PropertyAlert
import com.example.data.model.PropertyFilter
import com.example.data.model.PropertyListing
import com.example.data.model.SearchSuggestionSource
import com.example.data.model.ServiceRequest
import com.example.data.model.SmartSearchSuggestion
import com.example.data.model.UiState
import com.example.data.remote.GooglePlacesAutocompleteService
import com.example.data.repository.DiyarRepository
import com.example.notifications.DiyarNotificationHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DiyarViewModel(
    private val repository: DiyarRepository,
    private val currentUserId: String,
    private val appContext: Context,
    private val placesService: GooglePlacesAutocompleteService = GooglePlacesAutocompleteService(appContext)
) : ViewModel() {

    private val _isArabicNotifications = MutableStateFlow(true)

    private val _filter = MutableStateFlow(PropertyFilter())
    val filter: StateFlow<PropertyFilter> = _filter.asStateFlow()

    private val _selectedPropertyId = MutableStateFlow<String?>(null)
    val selectedPropertyId: StateFlow<String?> = _selectedPropertyId.asStateFlow()

    private val _activePoiCategory = MutableStateFlow("ALL")
    val activePoiCategory: StateFlow<String> = _activePoiCategory.asStateFlow()

    private val _searchedPlaceMarker = MutableStateFlow<MapPointOfInterest?>(null)
    val searchedPlaceMarker: StateFlow<MapPointOfInterest?> = _searchedPlaceMarker.asStateFlow()

    private val _mapFocusTarget = MutableStateFlow<Pair<Double, Double>?>(null)
    val mapFocusTarget: StateFlow<Pair<Double, Double>?> = _mapFocusTarget.asStateFlow()

    private val _liveSearchText = MutableStateFlow("")
    private val _searchSuggestionsArabic = MutableStateFlow(true)
    private val _remotePlaceSuggestions = MutableStateFlow<List<SmartSearchSuggestion>>(emptyList())
    private val _isSearchingSuggestions = MutableStateFlow(false)
    val isSearchingSuggestions: StateFlow<Boolean> = _isSearchingSuggestions.asStateFlow()

    val activePropertiesState: StateFlow<UiState<List<PropertyListing>>> =
        repository.observeActiveProperties()
            .map<List<PropertyListing>, UiState<List<PropertyListing>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFirestoreError(error, "observeActiveProperties")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val filteredPropertiesState: StateFlow<UiState<List<PropertyListing>>> =
        combine(activePropertiesState, _filter) { state, currentFilter ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> state
                is UiState.Success -> UiState.Success(applyFilter(state.data, currentFilter))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val mapPointsOfInterest: StateFlow<List<MapPointOfInterest>> =
        combine(activePropertiesState, _activePoiCategory, _searchedPlaceMarker) { propsState, poiCat, searchedMarker ->
            val fromListings = (propsState as? UiState.Success)?.data.orEmpty().flatMap { prop ->
                prop.nearbyLandmarks.mapIndexed { idx, lm ->
                    MapPointOfInterest(
                        id = "${prop.id}_lm_$idx",
                        category = lm.category,
                        nameEn = lm.nameEn,
                        nameAr = lm.nameAr,
                        subtitleEn = prop.neighborhoodEn,
                        subtitleAr = prop.neighborhoodAr,
                        latitude = lm.latitude,
                        longitude = lm.longitude
                    )
                }
            }
            val combined = (curatedIraqPois + fromListings)
                .distinctBy { "${it.nameEn}_${it.category}" }
                .filter { poiCat == "ALL" || it.category == poiCat }
            if (searchedMarker != null) {
                listOf(searchedMarker) + combined
            } else {
                combined
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = curatedIraqPois
        )

    val smartSearchSuggestions: StateFlow<List<SmartSearchSuggestion>> =
        combine(
            _liveSearchText,
            activePropertiesState,
            _remotePlaceSuggestions
        ) { rawQuery, propsState, remoteSuggestions ->
            val q = rawQuery.trim()
            val props = (propsState as? UiState.Success)?.data.orEmpty()
            buildMergedSearchSuggestions(q, props, remoteSuggestions)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = popularDefaultSuggestions
        )

    val myPropertiesState: StateFlow<UiState<List<PropertyListing>>> =
        repository.observeMyProperties(currentUserId)
            .map<List<PropertyListing>, UiState<List<PropertyListing>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFirestoreError(error, "observeMyProperties")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val favoritesState: StateFlow<UiState<List<FavoriteProperty>>> =
        repository.observeFavorites(currentUserId)
            .map<List<FavoriteProperty>, UiState<List<FavoriteProperty>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFirestoreError(error, "observeFavorites")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val favoriteIds: StateFlow<Set<String>> = favoritesState.map { state ->
        if (state is UiState.Success) state.data.map { it.propertyId }.toSet() else emptySet()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptySet()
    )

    val alertRulesState: StateFlow<UiState<List<AlertRule>>> =
        repository.observeAlertRules(currentUserId)
            .map<List<AlertRule>, UiState<List<AlertRule>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFirestoreError(error, "observeAlertRules")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val alertsState: StateFlow<UiState<List<PropertyAlert>>> =
        repository.observeAlerts(currentUserId)
            .map<List<PropertyAlert>, UiState<List<PropertyAlert>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFirestoreError(error, "observeAlerts")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val unreadAlertsCount: StateFlow<Int> = alertsState.map { state ->
        if (state is UiState.Success) state.data.count { !it.read } else 0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = 0
    )

    val serviceRequestsState: StateFlow<UiState<List<ServiceRequest>>> =
        repository.observeServiceRequests(currentUserId)
            .map<List<ServiceRequest>, UiState<List<ServiceRequest>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFirestoreError(error, "observeServiceRequests")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile("ar")
            repository.seedInitialMarketplaceIfEmpty()
        }

        // 1. Real-time watch-rule alert matcher + device push notification trigger
        viewModelScope.launch {
            combine(activePropertiesState, alertRulesState) { propsState, rulesState ->
                if (propsState is UiState.Success && rulesState is UiState.Success) {
                    propsState.data to rulesState.data.filter { it.active }
                } else null
            }.collect { pair ->
                if (pair != null) {
                    evaluateAlertMatches(pair.first, pair.second)
                }
            }
        }

        // 2. Real-time favorited-property update detector (price change or status change -> push alert)
        viewModelScope.launch {
            combine(activePropertiesState, myPropertiesState, favoritesState) { activeS, myS, favS ->
                if (favS is UiState.Success) {
                    val allProps = ((activeS as? UiState.Success)?.data.orEmpty() +
                        (myS as? UiState.Success)?.data.orEmpty()).associateBy { it.id }
                    allProps to favS.data
                } else null
            }.collect { pair ->
                if (pair != null) {
                    evaluateFavoriteUpdates(pair.first, pair.second)
                }
            }
        }

        // 3. Debounced live Google Maps Places Autocomplete + Iraq Geocoder pipeline
        viewModelScope.launch {
            _liveSearchText
                .map { it.trim() }
                .debounce(220L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.length < 2) {
                        _remotePlaceSuggestions.value = emptyList()
                        _isSearchingSuggestions.value = false
                    } else {
                        _isSearchingSuggestions.value = true
                        val live = placesService.fetchLivePlaceSuggestions(
                            query = query,
                            isArabic = _searchSuggestionsArabic.value
                        )
                        _remotePlaceSuggestions.value = live
                        _isSearchingSuggestions.value = false
                    }
                }
        }
    }

    fun syncLanguagePreference(langCode: String) {
        _isArabicNotifications.value = (langCode == "ar")
        viewModelScope.launch {
            repository.ensureUserProfile(langCode)
        }
    }

    private suspend fun evaluateAlertMatches(
        properties: List<PropertyListing>,
        rules: List<AlertRule>
    ) {
        val isAr = _isArabicNotifications.value
        for (rule in rules) {
            for (prop in properties) {
                val matchesListing = rule.listingType == "ALL" || prop.listingType == rule.listingType
                val matchesType = rule.propertyType == "ALL" || prop.propertyType == rule.propertyType
                val matchesPrice = rule.maxPrice <= 0.0 || prop.price <= rule.maxPrice
                val matchesArea = rule.minAreaSqm <= 0.0 || prop.areaSqm >= rule.minAreaSqm
                val matchesCity = rule.cityFilter.isBlank() ||
                    prop.cityEn.contains(rule.cityFilter, ignoreCase = true) ||
                    prop.cityAr.contains(rule.cityFilter, ignoreCase = true) ||
                    prop.neighborhoodEn.contains(rule.cityFilter, ignoreCase = true) ||
                    prop.neighborhoodAr.contains(rule.cityFilter, ignoreCase = true)

                if (matchesListing && matchesType && matchesPrice && matchesArea && matchesCity) {
                    val alertId = "${rule.id}_${prop.id}".take(120)
                    val titleEn = prop.titleEn
                    val titleAr = prop.titleAr
                    val msgEn = "Matches \"${rule.nameEn}\" • ${prop.cityEn}"
                    val msgAr = "يطابق تنبيه \"${rule.nameAr}\" • ${prop.cityAr}"
                    val createdRes = repository.createAlertIfNotExists(
                        alertId = alertId,
                        alert = PropertyAlert(
                            propertyId = prop.id,
                            ruleId = rule.id,
                            titleEn = titleEn,
                            titleAr = titleAr,
                            messageEn = msgEn,
                            messageAr = msgAr,
                            price = prop.price,
                            read = false
                        )
                    )
                    if (createdRes.getOrDefault(false)) {
                        DiyarNotificationHelper.showPropertyNotification(
                            context = appContext,
                            alertId = alertId,
                            propertyId = prop.id,
                            title = if (isAr) titleAr else titleEn,
                            message = if (isAr) msgAr else msgEn,
                            isArabic = isAr
                        )
                    }
                }
            }
        }
    }

    private suspend fun evaluateFavoriteUpdates(
        allProps: Map<String, PropertyListing>,
        favorites: List<FavoriteProperty>
    ) {
        val isAr = _isArabicNotifications.value
        for (fav in favorites) {
            val latest = allProps[fav.propertyId] ?: continue
            val priceChanged = kotlin.math.abs(latest.price - fav.price) >= 1.0
            val statusChanged = latest.status != fav.status
            if (priceChanged || statusChanged) {
                val alertId = "fav_${latest.id}_${latest.price.toLong()}_${latest.status}".take(120)
                val msgEn = when {
                    statusChanged && latest.status == "SOLD" -> "Saved property was marked SOLD."
                    priceChanged -> "Saved property price updated from ${fav.price.toLong()} to ${latest.price.toLong()} IQD."
                    else -> "Saved property details were updated."
                }
                val msgAr = when {
                    statusChanged && latest.status == "SOLD" -> "تم تحديث حالة العقار المحفوظ إلى مباع."
                    priceChanged -> "تغير سعر العقار المحفوظ من ${fav.price.toLong()} إلى ${latest.price.toLong()} د.ع."
                    else -> "تم تحديث بيانات العقار المحفوظ."
                }
                val createdRes = repository.createAlertIfNotExists(
                    alertId = alertId,
                    alert = PropertyAlert(
                        propertyId = latest.id,
                        ruleId = "favorite_watch",
                        titleEn = "Saved Update: ${latest.titleEn}",
                        titleAr = "تحديث المفضلة: ${latest.titleAr}",
                        messageEn = msgEn,
                        messageAr = msgAr,
                        price = latest.price,
                        read = false
                    )
                )
                repository.updateFavoriteSnapshot(latest)
                if (createdRes.getOrDefault(false)) {
                    DiyarNotificationHelper.showPropertyNotification(
                        context = appContext,
                        alertId = alertId,
                        propertyId = latest.id,
                        title = if (isAr) "تحديث المفضلة: ${latest.titleAr}" else "Saved Update: ${latest.titleEn}",
                        message = if (isAr) msgAr else msgEn,
                        isArabic = isAr
                    )
                }
            }
        }
    }

    fun setPoiCategory(category: String) {
        _activePoiCategory.value = category
    }

    fun clearSearchedPlace() {
        _searchedPlaceMarker.value = null
    }

    fun focusOnPoi(poi: MapPointOfInterest) {
        _mapFocusTarget.value = poi.latitude to poi.longitude
        _searchedPlaceMarker.value = poi
    }

    fun searchLandmarkOrPlace(query: String, isArabic: Boolean) {
        val q = query.trim()
        if (q.isEmpty()) {
            _searchedPlaceMarker.value = null
            return
        }

        // First check matching curated Iraq POIs & cities or property landmarks
        val localHit = (mapPointsOfInterest.value + iraqCityPois).firstOrNull { poi ->
            poi.nameEn.contains(q, ignoreCase = true) ||
                poi.nameAr.contains(q, ignoreCase = true) ||
                poi.subtitleEn.contains(q, ignoreCase = true) ||
                poi.subtitleAr.contains(q, ignoreCase = true)
        }
        if (localHit != null) {
            _searchedPlaceMarker.value = localHit
            _mapFocusTarget.value = localHit.latitude to localHit.longitude
            // If exact text query filtered out all properties (e.g. searching a hospital/street), reset text filter so map shows surrounding listings cleanly
            val curProps = (filteredPropertiesState.value as? UiState.Success)?.data
            if (curProps != null && curProps.isEmpty() && _filter.value.query.isNotBlank()) {
                _filter.value = _filter.value.copy(query = "")
            }
            return
        }

        // Next check Android/Google Geocoder scoped to Iraq for streets, hospitals, restaurants, districts
        viewModelScope.launch {
            val geocoded = geocodeLandmark(q, isArabic)
            if (geocoded != null) {
                val (lat, lng, label) = geocoded
                val poi = MapPointOfInterest(
                    id = "geocoded_${q.hashCode()}",
                    category = inferPoiCategory(q),
                    nameEn = label,
                    nameAr = label,
                    subtitleEn = "Google Maps Search Result • Iraq",
                    subtitleAr = "نتيجة بحث خرائط Google • العراق",
                    latitude = lat,
                    longitude = lng
                )
                _searchedPlaceMarker.value = poi
                _mapFocusTarget.value = lat to lng
                val curProps = (filteredPropertiesState.value as? UiState.Success)?.data
                if (curProps != null && curProps.isEmpty() && _filter.value.query.isNotBlank()) {
                    _filter.value = _filter.value.copy(query = "")
                }
            }
        }
    }

    private fun inferPoiCategory(q: String): String {
        val lower = q.lowercase()
        return when {
            lower.contains("hospital") || lower.contains("clinic") || lower.contains("مستشفى") || lower.contains("مركز صحي") -> "HOSPITAL"
            lower.contains("school") || lower.contains("university") || lower.contains("مدرس") || lower.contains("جامعة") -> "SCHOOL"
            lower.contains("restaurant") || lower.contains("cafe") || lower.contains("مطعم") || lower.contains("مقهى") || lower.contains("كافيه") -> "RESTAURANT"
            lower.contains("mosque") || lower.contains("جامع") || lower.contains("مسجد") -> "MOSQUE"
            lower.contains("street") || lower.contains("road") || lower.contains("شارع") || lower.contains("طريق") -> "STREET"
            else -> "SERVICE"
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun geocodeLandmark(query: String, isArabic: Boolean): Triple<Double, Double, String>? {
        return withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) return@withContext null
                val geocoder = Geocoder(appContext, Locale(if (isArabic) "ar" else "en"))
                val hasIraqKeyword = listOf(
                    "iraq", "baghdad", "erbil", "basra", "najaf", "karbala", "mosul",
                    "العراق", "بغداد", "أربيل", "اربيل", "البصرة", "النجف", "كربلاء", "الموصل"
                ).any { query.contains(it, ignoreCase = true) }
                val fullQuery = if (hasIraqKeyword) query else "$query, Baghdad, Iraq"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocationName(fullQuery, 1) { addresses ->
                            val first = addresses.firstOrNull()
                            if (cont.isActive) {
                                cont.resume(
                                    first?.let {
                                        Triple(it.latitude, it.longitude, it.featureName ?: query)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    val addresses = geocoder.getFromLocationName(fullQuery, 1)
                    addresses?.firstOrNull()?.let {
                        Triple(it.latitude, it.longitude, it.featureName ?: query)
                    }
                }
            } catch (e: Exception) {
                Log.w("DiyarViewModel", "Geocoder lookup failed for $query", e)
                null
            }
        }
    }

    fun onSearchInputChanged(query: String, isArabic: Boolean) {
        _searchSuggestionsArabic.value = isArabic
        _liveSearchText.value = query
        _filter.value = _filter.value.copy(query = query)
        if (query.isBlank()) {
            _searchedPlaceMarker.value = null
            _remotePlaceSuggestions.value = emptyList()
        }
    }

    fun selectSearchSuggestion(
        suggestion: SmartSearchSuggestion,
        isArabic: Boolean,
        onAppliedLabel: (String) -> Unit = {}
    ) {
        val chosenLabel = if (isArabic) suggestion.primaryTextAr else suggestion.primaryTextEn
        _liveSearchText.value = chosenLabel
        onAppliedLabel(chosenLabel)

        when (suggestion.source) {
            SearchSuggestionSource.PROPERTY -> {
                val propId = suggestion.propertyId
                if (propId != null) {
                    _selectedPropertyId.value = propId
                }
                val lat = suggestion.latitude
                val lng = suggestion.longitude
                if (lat != null && lng != null) {
                    _mapFocusTarget.value = lat to lng
                }
                _searchedPlaceMarker.value = null
                _filter.value = _filter.value.copy(query = chosenLabel)
            }

            SearchSuggestionSource.IRAQ_DISTRICT,
            SearchSuggestionSource.LANDMARK,
            SearchSuggestionSource.GEOCODER -> {
                val lat = suggestion.latitude
                val lng = suggestion.longitude
                if (lat != null && lng != null) {
                    applySearchedLocation(
                        id = suggestion.id,
                        category = suggestion.category,
                        nameEn = suggestion.primaryTextEn,
                        nameAr = suggestion.primaryTextAr,
                        subtitleEn = suggestion.secondaryTextEn,
                        subtitleAr = suggestion.secondaryTextAr,
                        lat = lat,
                        lng = lng,
                        districtFilterHint = suggestion.filterQueryText.takeIf {
                            suggestion.source == SearchSuggestionSource.IRAQ_DISTRICT
                        }
                    )
                } else {
                    searchLandmarkOrPlace(chosenLabel, isArabic)
                }
            }

            SearchSuggestionSource.GOOGLE_PLACES -> {
                viewModelScope.launch {
                    _isSearchingSuggestions.value = true
                    val coords = placesService.resolvePlaceCoordinates(
                        placeId = suggestion.placeId,
                        fallbackQuery = chosenLabel,
                        isArabic = isArabic
                    )
                    _isSearchingSuggestions.value = false
                    if (coords != null) {
                        applySearchedLocation(
                            id = suggestion.id,
                            category = suggestion.category,
                            nameEn = suggestion.primaryTextEn,
                            nameAr = suggestion.primaryTextAr,
                            subtitleEn = suggestion.secondaryTextEn,
                            subtitleAr = suggestion.secondaryTextAr,
                            lat = coords.first,
                            lng = coords.second,
                            districtFilterHint = suggestion.filterQueryText.takeIf {
                                suggestion.category == "DISTRICT" || suggestion.category == "CITY"
                            }
                        )
                    } else {
                        searchLandmarkOrPlace(chosenLabel, isArabic)
                    }
                }
            }
        }
    }

    private fun applySearchedLocation(
        id: String,
        category: String,
        nameEn: String,
        nameAr: String,
        subtitleEn: String,
        subtitleAr: String,
        lat: Double,
        lng: Double,
        districtFilterHint: String?
    ) {
        val poi = MapPointOfInterest(
            id = id,
            category = if (category == "DISTRICT" || category == "CITY") "SERVICE" else category,
            nameEn = nameEn,
            nameAr = nameAr,
            subtitleEn = subtitleEn,
            subtitleAr = subtitleAr,
            latitude = lat,
            longitude = lng
        )
        _searchedPlaceMarker.value = poi
        _mapFocusTarget.value = lat to lng

        val activeProps = (activePropertiesState.value as? UiState.Success)?.data.orEmpty()
        if (!districtFilterHint.isNullOrBlank()) {
            val probeFilter = _filter.value.copy(query = districtFilterHint)
            val matched = applyFilter(activeProps, probeFilter)
            _filter.value = if (matched.isNotEmpty()) {
                probeFilter
            } else {
                // Keep surrounding properties visible when exploring another Iraqi city/district
                _filter.value.copy(query = "")
            }
        } else {
            // Landmark / hospital / street / restaurant: show nearby properties cleanly
            val curFiltered = applyFilter(activeProps, _filter.value)
            if (curFiltered.isEmpty() && _filter.value.query.isNotBlank()) {
                _filter.value = _filter.value.copy(query = "")
            }
        }
    }

    fun selectProperty(propertyId: String?) {
        _selectedPropertyId.value = propertyId
    }

    fun updateSearchQuery(query: String) {
        _liveSearchText.value = query
        _filter.value = _filter.value.copy(query = query)
    }

    fun updateQuickFilter(listingType: String, propertyType: String) {
        _filter.value = _filter.value.copy(listingType = listingType, propertyType = propertyType)
    }

    fun toggleMapCategory(category: String) {
        val current = _filter.value
        _filter.value = when (category) {
            "SALE" -> {
                val isCurrentlySale = current.listingType == "SALE" && current.propertyType != "COMMERCIAL"
                if (isCurrentlySale) current.copy(listingType = "ALL", propertyType = "ALL")
                else current.copy(listingType = "SALE", propertyType = "ALL")
            }
            "RENT" -> {
                val isCurrentlyRent = current.listingType == "RENT" && current.propertyType != "COMMERCIAL"
                if (isCurrentlyRent) current.copy(listingType = "ALL", propertyType = "ALL")
                else current.copy(listingType = "RENT", propertyType = "ALL")
            }
            "COMMERCIAL" -> {
                val isCurrentlyComm = current.propertyType == "COMMERCIAL"
                if (isCurrentlyComm) current.copy(listingType = "ALL", propertyType = "ALL")
                else current.copy(listingType = "ALL", propertyType = "COMMERCIAL")
            }
            else -> current.copy(listingType = "ALL", propertyType = "ALL")
        }
    }

    fun updateFilter(newFilter: PropertyFilter) {
        _filter.value = newFilter
    }

    fun updateGeoCenter(lat: Double, lng: Double, enableRadius: Boolean = true) {
        _filter.value = _filter.value.copy(
            centerLat = lat,
            centerLng = lng,
            geoRadiusEnabled = enableRadius
        )
        _mapFocusTarget.value = lat to lng
    }

    fun resetFilters() {
        _liveSearchText.value = ""
        _searchedPlaceMarker.value = null
        _remotePlaceSuggestions.value = emptyList()
        _filter.value = PropertyFilter()
    }

    fun toggleFavorite(listing: PropertyListing) {
        viewModelScope.launch {
            val saved = favoriteIds.value.contains(listing.id)
            repository.toggleFavorite(listing, saved)
        }
    }

    fun publishProperty(
        listing: PropertyListing,
        fullDeedNumber: String,
        licenseNumber: String,
        contactPhone: String,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.publishProperty(listing, fullDeedNumber, licenseNumber, contactPhone)
            onResult(result)
        }
    }

    fun markPropertySold(propertyId: String) {
        viewModelScope.launch {
            repository.markPropertySold(propertyId)
        }
    }

    fun deleteProperty(propertyId: String) {
        viewModelScope.launch {
            repository.deleteProperty(propertyId)
        }
    }

    fun createAlertRule(rule: AlertRule, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = repository.createAlertRule(rule)
            onResult(result)
        }
    }

    fun toggleAlertRule(ruleId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleAlertRule(ruleId, active)
        }
    }

    fun deleteAlertRule(ruleId: String) {
        viewModelScope.launch {
            repository.deleteAlertRule(ruleId)
        }
    }

    fun markAlertRead(alertId: String) {
        viewModelScope.launch {
            repository.markAlertRead(alertId)
        }
    }

    fun bookService(request: ServiceRequest, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = repository.bookServiceRequest(request)
            onResult(result)
        }
    }

    fun cancelServiceRequest(requestId: String) {
        viewModelScope.launch {
            repository.cancelServiceRequest(requestId)
        }
    }

    private fun applyFilter(list: List<PropertyListing>, f: PropertyFilter): List<PropertyListing> {
        val q = f.query.trim()
        val filtered = list.filter { prop ->
            val matchesQuery = q.isEmpty() ||
                prop.titleEn.contains(q, ignoreCase = true) ||
                prop.titleAr.contains(q, ignoreCase = true) ||
                prop.cityEn.contains(q, ignoreCase = true) ||
                prop.cityAr.contains(q, ignoreCase = true) ||
                prop.neighborhoodEn.contains(q, ignoreCase = true) ||
                prop.neighborhoodAr.contains(q, ignoreCase = true) ||
                prop.nearbyLandmarks.any {
                    it.nameEn.contains(q, ignoreCase = true) || it.nameAr.contains(q, ignoreCase = true)
                }

            val matchesListing = f.listingType == "ALL" || prop.listingType == f.listingType
            val matchesType = f.propertyType == "ALL" || prop.propertyType == f.propertyType
            val matchesPrice = prop.price >= f.minPrice && prop.price <= f.maxPrice
            val matchesArea = prop.areaSqm >= f.minAreaSqm
            val matchesBeds = prop.bedrooms >= f.minBedrooms
            val matchesAmenities = f.selectedAmenities.isEmpty() ||
                f.selectedAmenities.all { reqAmenity -> prop.amenities.contains(reqAmenity) }
            val matchesYear = f.minYearBuilt <= 0 || prop.yearBuilt >= f.minYearBuilt
            val matchesVerified = !f.verifiedOnly || prop.verified
            val matchesTour = !f.virtualTourOnly || prop.hasVirtualTour
            val matchesPlan = !f.floorPlanOnly || prop.hasFloorPlan
            val matchesGeo = !f.geoRadiusEnabled ||
                haversineKm(f.centerLat, f.centerLng, prop.latitude, prop.longitude) <= f.radiusKm

            matchesQuery && matchesListing && matchesType && matchesPrice &&
                matchesArea && matchesBeds && matchesAmenities && matchesYear &&
                matchesVerified && matchesTour && matchesPlan && matchesGeo
        }

        return when (f.sort) {
            ListingSort.NEWEST -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
            ListingSort.PRICE_ASC -> filtered.sortedBy { it.price }
            ListingSort.PRICE_DESC -> filtered.sortedByDescending { it.price }
            ListingSort.AREA_DESC -> filtered.sortedByDescending { it.areaSqm }
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun <T> unwrapFirestoreError(error: Throwable, tag: String): UiState<T> {
        val fsError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fsError == null && error is CancellationException) throw error
        val root = fsError ?: error
        Log.w("DiyarViewModel", "Error in $tag", root)
        return UiState.Error(root.localizedMessage ?: "Sync error")
    }

    private fun buildMergedSearchSuggestions(
        query: String,
        properties: List<PropertyListing>,
        remoteSuggestions: List<SmartSearchSuggestion>
    ): List<SmartSearchSuggestion> {
        if (query.isBlank()) {
            return popularDefaultSuggestions
        }

        val normQ = normalizeSearchText(query)
        val out = mutableListOf<SmartSearchSuggestion>()

        // 1. Live Google Maps Places & Geocoder suggestions (first for comprehensive map search)
        for (remote in remoteSuggestions) {
            if (out.none {
                    normalizeSearchText(it.primaryTextEn) == normalizeSearchText(remote.primaryTextEn) ||
                        normalizeSearchText(it.primaryTextAr) == normalizeSearchText(remote.primaryTextAr)
                }
            ) {
                out.add(remote)
            }
        }

        // 2. Comprehensive Iraqi Cities & Baghdad/Governorate Districts (instant prefix/contains)
        val matchingDistricts = iraqDistrictsAndCities.filter { item ->
            normalizeSearchText(item.primaryTextAr).contains(normQ) ||
                normalizeSearchText(item.primaryTextEn).contains(normQ) ||
                normalizeSearchText(item.secondaryTextAr).contains(normQ) ||
                normalizeSearchText(item.secondaryTextEn).contains(normQ)
        }.sortedByDescending { item ->
            normalizeSearchText(item.primaryTextAr).startsWith(normQ) ||
                normalizeSearchText(item.primaryTextEn).startsWith(normQ)
        }
        for (d in matchingDistricts.take(6)) {
            if (out.none {
                    normalizeSearchText(it.primaryTextAr) == normalizeSearchText(d.primaryTextAr)
                }
            ) {
                out.add(d)
            }
        }

        // 3. Curated Iraqi Landmarks & Active Listing Landmarks (Hospitals, Restaurants, Schools, Streets, Mosques)
        val dynamicLandmarks = properties.flatMap { prop ->
            prop.nearbyLandmarks.mapIndexed { idx, lm ->
                SmartSearchSuggestion(
                    id = "lm_${prop.id}_$idx",
                    primaryTextEn = lm.nameEn,
                    primaryTextAr = lm.nameAr,
                    secondaryTextEn = "${prop.neighborhoodEn}, ${prop.cityEn}",
                    secondaryTextAr = "${prop.neighborhoodAr}، ${prop.cityAr}",
                    category = lm.category,
                    source = SearchSuggestionSource.LANDMARK,
                    latitude = lm.latitude,
                    longitude = lm.longitude,
                    filterQueryText = lm.nameAr
                )
            }
        }
        val allLandmarks = (curatedIraqLandmarks + dynamicLandmarks)
            .filter { lm ->
                normalizeSearchText(lm.primaryTextAr).contains(normQ) ||
                    normalizeSearchText(lm.primaryTextEn).contains(normQ) ||
                    normalizeSearchText(lm.secondaryTextAr).contains(normQ) ||
                    normalizeSearchText(lm.secondaryTextEn).contains(normQ)
            }
            .sortedByDescending { lm ->
                normalizeSearchText(lm.primaryTextAr).startsWith(normQ) ||
                    normalizeSearchText(lm.primaryTextEn).startsWith(normQ)
            }
        for (lm in allLandmarks.take(5)) {
            if (out.none {
                    normalizeSearchText(it.primaryTextAr) == normalizeSearchText(lm.primaryTextAr)
                }
            ) {
                out.add(lm)
            }
        }

        // 4. Matching Active Properties from Firestore
        val matchingProps = properties.filter { prop ->
            normalizeSearchText(prop.titleAr).contains(normQ) ||
                normalizeSearchText(prop.titleEn).contains(normQ) ||
                normalizeSearchText(prop.neighborhoodAr).contains(normQ) ||
                normalizeSearchText(prop.neighborhoodEn).contains(normQ) ||
                normalizeSearchText(prop.cityAr).contains(normQ) ||
                normalizeSearchText(prop.cityEn).contains(normQ)
        }.take(4).map { prop ->
            SmartSearchSuggestion(
                id = "prop_${prop.id}",
                primaryTextEn = prop.titleEn,
                primaryTextAr = prop.titleAr,
                secondaryTextEn = "${prop.neighborhoodEn}, ${prop.cityEn}",
                secondaryTextAr = "${prop.neighborhoodAr}، ${prop.cityAr}",
                category = "PROPERTY",
                source = SearchSuggestionSource.PROPERTY,
                propertyId = prop.id,
                latitude = prop.latitude,
                longitude = prop.longitude,
                filterQueryText = prop.titleAr
            )
        }
        out.addAll(matchingProps)

        return out.take(12)
    }

    private fun normalizeSearchText(input: String): String {
        return input.trim().lowercase()
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5000L

        val availableAmenityCodes = listOf(
            "POOL", "GARDEN", "ELEVATOR", "SMART_HOME", "PARKING", "MAID_ROOM", "GYM"
        )

        private val curatedIraqPois = listOf(
            MapPointOfInterest("poi_hosp_1", "HOSPITAL", "Al-Yarmouk Teaching Hospital", "مستشفى اليرموك التعليمي", "Baghdad • Al-Yarmouk", "بغداد • اليرموك", 33.3048, 44.3468),
            MapPointOfInterest("poi_hosp_2", "HOSPITAL", "Medical City Teaching Complex", "مدينة الطب التعليمية", "Baghdad • Bab Al-Muadham", "بغداد • باب المعظم", 33.3472, 44.3775),
            MapPointOfInterest("poi_hosp_3", "HOSPITAL", "Ibn Al-Bitar Cardiac Center", "مستشفى ابن البيطار", "Baghdad • Al-Karkh", "بغداد • الكرخ", 33.3292, 44.3694),
            MapPointOfInterest("poi_sch_1", "SCHOOL", "University of Baghdad", "جامعة بغداد", "Baghdad • Al-Jadriya", "بغداد • الجادرية", 33.2795, 44.3812),
            MapPointOfInterest("poi_sch_2", "SCHOOL", "Al-Mansour International School", "مدرسة المنصور الدولية", "Baghdad • Al-Mansour", "بغداد • المنصور", 33.3210, 44.3455),
            MapPointOfInterest("poi_rest_1", "RESTAURANT", "Samad Restaurant Al-Mansour", "مطعم صمد - المنصور", "Al-Amirat Street", "شارع الأميرات", 33.3172, 44.3478),
            MapPointOfInterest("poi_rest_2", "RESTAURANT", "Zarzour Karrada Restaurant", "مطعم زرزور الكرادة", "Al-Karrada", "الكرادة", 33.3065, 44.4125),
            MapPointOfInterest("poi_str_1", "STREET", "Al-Amirat Street", "شارع الأميرات", "Al-Mansour", "المنصور", 33.3168, 44.3405),
            MapPointOfInterest("poi_str_2", "STREET", "Palestine Street", "شارع فلسطين", "East Baghdad", "شرق بغداد", 33.3502, 44.4060),
            MapPointOfInterest("poi_str_3", "STREET", "Abu Nuwas Street", "شارع أبو نؤاس", "Tigris Corniche", "كورنيش دجلة", 33.3065, 44.3950),
            MapPointOfInterest("poi_mos_1", "MOSQUE", "Umm Al-Tubul Grand Mosque", "جامع أم الطبول", "Al-Yarmouk / Mansour", "اليرموك / المنصور", 33.3075, 44.3315)
        )

        private val iraqCityPois = listOf(
            MapPointOfInterest("iq_city_bgd", "SERVICE", "Baghdad", "بغداد", "Capital of Iraq", "عاصمة العراق", 33.3152, 44.3661),
            MapPointOfInterest("iq_city_ebl", "SERVICE", "Erbil", "أربيل", "Kurdistan Region, Iraq", "إقليم كردستان العراق", 36.1911, 44.0091),
            MapPointOfInterest("iq_city_bsr", "SERVICE", "Basra", "البصرة", "Southern Iraq", "جنوب العراق", 30.5085, 47.7804),
            MapPointOfInterest("iq_city_njf", "SERVICE", "Najaf", "النجف", "Al-Najaf Governorate", "محافظة النجف الأشرف", 32.0259, 44.3463),
            MapPointOfInterest("iq_city_krb", "SERVICE", "Karbala", "كربلاء", "Karbala Governorate", "محافظة كربلاء", 32.6160, 44.0249),
            MapPointOfInterest("iq_city_msl", "SERVICE", "Mosul", "الموصل", "Nineveh Governorate", "محافظة نينوى", 36.3350, 43.1189),
            MapPointOfInterest("iq_city_sul", "SERVICE", "Sulaymaniyah", "السليمانية", "Kurdistan Region, Iraq", "إقليم كردستان العراق", 35.5558, 45.4351)
        )

        private val iraqDistrictsAndCities = listOf(
            SmartSearchSuggestion("dist_mansour", "Al-Mansour", "المنصور", "Baghdad Governorate", "بغداد • الكرخ", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3189, longitude = 44.3435, filterQueryText = "المنصور"),
            SmartSearchSuggestion("dist_jadriya", "Al-Jadriya", "الجادرية", "Baghdad Governorate", "بغداد • الرصافة", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.2858, longitude = 44.3824, filterQueryText = "الجادرية"),
            SmartSearchSuggestion("dist_karrada", "Al-Karrada", "الكرادة", "Baghdad Governorate", "بغداد • الرصافة", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3068, longitude = 44.4162, filterQueryText = "الكرادة"),
            SmartSearchSuggestion("dist_zayouna", "Zayouna", "زيونة", "Baghdad Governorate", "بغداد • الرصافة", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3372, longitude = 44.4552, filterQueryText = "زيونة"),
            SmartSearchSuggestion("dist_yarmouk", "Al-Yarmouk", "اليرموك", "Baghdad Governorate", "بغداد • الكرخ", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3036, longitude = 44.3422, filterQueryText = "اليرموك"),
            SmartSearchSuggestion("dist_qadisiyah", "Al-Qadisiyah", "القادسية", "Baghdad Governorate", "بغداد • الكرخ", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.2981, longitude = 44.3618, filterQueryText = "القادسية"),
            SmartSearchSuggestion("dist_harthiya", "Al-Harthiya", "الحارثية", "Baghdad Governorate", "بغداد • الكرخ", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3112, longitude = 44.3548, filterQueryText = "الحارثية"),
            SmartSearchSuggestion("dist_adhamiya", "Al-Adhamiya", "الأعظمية", "Baghdad Governorate", "بغداد • الرصافة", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3712, longitude = 44.3621, filterQueryText = "الأعظمية"),
            SmartSearchSuggestion("dist_kadhimiya", "Al-Kadhimiya", "الكاظمية", "Baghdad Governorate", "بغداد • الكرخ", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3792, longitude = 44.3379, filterQueryText = "الكاظمية"),
            SmartSearchSuggestion("dist_jamia", "Hay Al-Jami'a", "حي الجامعة", "Baghdad Governorate", "بغداد • الكرخ", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3218, longitude = 44.3168, filterQueryText = "حي الجامعة"),
            SmartSearchSuggestion("dist_amiriya", "Al-Amiriya", "العامرية", "Baghdad Governorate", "بغداد • الكرخ", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3049, longitude = 44.2941, filterQueryText = "العامرية"),
            SmartSearchSuggestion("dist_ghazaliya", "Al-Ghazaliya", "الغزالية", "Baghdad Governorate", "بغداد • الكرخ", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3372, longitude = 44.2815, filterQueryText = "الغزالية"),
            SmartSearchSuggestion("dist_saidiya", "Al-Saidiya", "السيدية", "Baghdad Governorate", "بغداد • الكرخ", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.2614, longitude = 44.3532, filterQueryText = "السيدية"),
            SmartSearchSuggestion("dist_dora", "Al-Dora", "الدورة", "Baghdad Governorate", "بغداد • الكرخ", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.2412, longitude = 44.3885, filterQueryText = "الدورة"),
            SmartSearchSuggestion("dist_waziriya", "Al-Waziriya", "الوزيرية", "Baghdad Governorate", "بغداد • الرصافة", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3568, longitude = 44.3865, filterQueryText = "الوزيرية"),
            SmartSearchSuggestion("dist_palestine", "Palestine Street District", "شارع فلسطين", "Baghdad Governorate", "بغداد • الرصافة", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3502, longitude = 44.4060, filterQueryText = "شارع فلسطين"),
            SmartSearchSuggestion("dist_newbgd", "New Baghdad", "بغداد الجديدة", "Baghdad Governorate", "بغداد • الرصافة", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3075, longitude = 44.4622, filterQueryText = "بغداد الجديدة"),
            SmartSearchSuggestion("dist_bismayah", "Bismayah New City", "مدينة بسماية", "Baghdad Governorate", "جنوب شرق بغداد", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.1752, longitude = 44.5768, filterQueryText = "بسماية"),
            SmartSearchSuggestion("city_baghdad", "Baghdad", "بغداد", "Capital of Iraq", "العراق", "CITY", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3152, longitude = 44.3661, filterQueryText = "بغداد"),
            SmartSearchSuggestion("city_erbil", "Erbil", "أربيل", "Kurdistan Region, Iraq", "العراق • إقليم كردستان", "CITY", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 36.1911, longitude = 44.0091, filterQueryText = "أربيل"),
            SmartSearchSuggestion("dist_ankawa", "Ankawa, Erbil", "عينكاوا - أربيل", "Erbil Governorate", "أربيل • العراق", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 36.2248, longitude = 43.9932, filterQueryText = "عينكاوا"),
            SmartSearchSuggestion("dist_empire_erbil", "Empire World, Erbil", "إمباير وورلد - أربيل", "Erbil Governorate", "أربيل • العراق", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 36.1945, longitude = 43.9722, filterQueryText = "إمباير"),
            SmartSearchSuggestion("city_basra", "Basra", "البصرة", "Southern Iraq", "العراق", "CITY", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 30.5085, longitude = 47.7804, filterQueryText = "البصرة"),
            SmartSearchSuggestion("dist_ashar_basra", "Al-Ashar, Basra", "العشار - البصرة", "Basra Governorate", "البصرة • العراق", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 30.5112, longitude = 47.8385, filterQueryText = "العشار"),
            SmartSearchSuggestion("dist_jazair_basra", "Al-Jaza'ir, Basra", "الجزائر - البصرة", "Basra Governorate", "البصرة • العراق", "DISTRICT", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 30.5061, longitude = 47.8265, filterQueryText = "الجزائر"),
            SmartSearchSuggestion("city_najaf", "Al-Najaf", "النجف الأشرف", "Najaf Governorate", "العراق", "CITY", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 32.0259, longitude = 44.3463, filterQueryText = "النجف"),
            SmartSearchSuggestion("city_karbala", "Karbala", "كربلاء المقدسة", "Karbala Governorate", "العراق", "CITY", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 32.6160, longitude = 44.0249, filterQueryText = "كربلاء"),
            SmartSearchSuggestion("city_mosul", "Mosul", "الموصل", "Nineveh Governorate", "العراق", "CITY", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 36.3350, longitude = 43.1189, filterQueryText = "الموصل"),
            SmartSearchSuggestion("city_sulaymaniyah", "Sulaymaniyah", "السليمانية", "Kurdistan Region, Iraq", "العراق", "CITY", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 35.5558, longitude = 45.4351, filterQueryText = "السليمانية"),
            SmartSearchSuggestion("city_kirkuk", "Kirkuk", "كركوك", "Kirkuk Governorate", "العراق", "CITY", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 35.4681, longitude = 44.3922, filterQueryText = "كركوك"),
            SmartSearchSuggestion("city_hillah", "Hillah / Babylon", "الحلة / بابل", "Babylon Governorate", "العراق", "CITY", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 32.4637, longitude = 44.4196, filterQueryText = "الحلة"),
            SmartSearchSuggestion("city_duhok", "Duhok", "دهوك", "Duhok Governorate", "العراق", "CITY", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 36.8669, longitude = 42.9503, filterQueryText = "دهوك"),
            SmartSearchSuggestion("city_ramadi", "Ramadi", "الرمادي", "Al-Anbar Governorate", "العراق", "CITY", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.4258, longitude = 43.2992, filterQueryText = "الرمادي"),
            SmartSearchSuggestion("city_fallujah", "Fallujah", "الفلوجة", "Al-Anbar Governorate", "العراق", "CITY", SearchSuggestionSource.IRAQ_DISTRICT, latitude = 33.3491, longitude = 43.7860, filterQueryText = "الفلوجة")
        )

        private val curatedIraqLandmarks = listOf(
            SmartSearchSuggestion("lm_yarmouk_hosp", "Al-Yarmouk Teaching Hospital", "مستشفى اليرموك التعليمي", "Hospital • Baghdad", "مستشفى • اليرموك، بغداد", "HOSPITAL", SearchSuggestionSource.LANDMARK, latitude = 33.3048, longitude = 44.3468),
            SmartSearchSuggestion("lm_med_city", "Medical City Teaching Complex", "مدينة الطب التعليمية", "Hospital • Bab Al-Muadham, Baghdad", "مستشفى • باب المعظم، بغداد", "HOSPITAL", SearchSuggestionSource.LANDMARK, latitude = 33.3472, longitude = 44.3775),
            SmartSearchSuggestion("lm_ibn_bitar", "Ibn Al-Bitar Cardiac Hospital", "مستشفى ابن البيطار", "Hospital • Al-Salhiya, Baghdad", "مستشفى • الصالحية، بغداد", "HOSPITAL", SearchSuggestionSource.LANDMARK, latitude = 33.3292, longitude = 44.3694),
            SmartSearchSuggestion("lm_alkindi", "Al-Kindi Teaching Hospital", "مستشفى الكندي التعليمي", "Hospital • Al-Nahda, Baghdad", "مستشفى • النهضة، بغداد", "HOSPITAL", SearchSuggestionSource.LANDMARK, latitude = 33.3418, longitude = 44.4052),
            SmartSearchSuggestion("lm_andalus", "Al-Andalus Specialist Hospital", "مستشفى الأندلس التخصصي", "Hospital • Al-Jadriya, Baghdad", "مستشفى • الجادرية، بغداد", "HOSPITAL", SearchSuggestionSource.LANDMARK, latitude = 33.2921, longitude = 44.3848),
            SmartSearchSuggestion("lm_uob", "University of Baghdad", "جامعة بغداد", "University • Al-Jadriya", "جامعة • الجادرية، بغداد", "SCHOOL", SearchSuggestionSource.LANDMARK, latitude = 33.2795, longitude = 44.3812),
            SmartSearchSuggestion("lm_mustansiriya", "Al-Mustansiriya University", "الجامعة المستنصرية", "University • Palestine St, Baghdad", "جامعة • شارع فلسطين، بغداد", "SCHOOL", SearchSuggestionSource.LANDMARK, latitude = 33.3665, longitude = 44.3982),
            SmartSearchSuggestion("lm_uotech", "University of Technology", "الجامعة التكنولوجية", "University • Al-Sinaa St, Baghdad", "جامعة • شارع الصناعة، بغداد", "SCHOOL", SearchSuggestionSource.LANDMARK, latitude = 33.3125, longitude = 44.4472),
            SmartSearchSuggestion("lm_amirat_st", "Al-Amirat Street", "شارع الأميرات", "Main Street • Al-Mansour, Baghdad", "شارع رئيسي • المنصور، بغداد", "STREET", SearchSuggestionSource.LANDMARK, latitude = 33.3168, longitude = 44.3405),
            SmartSearchSuggestion("lm_14ramadan_st", "14 Ramadan Street", "شارع ١٤ رمضان", "Main Street • Al-Mansour, Baghdad", "شارع رئيسي • المنصور، بغداد", "STREET", SearchSuggestionSource.LANDMARK, latitude = 33.3228, longitude = 44.3489),
            SmartSearchSuggestion("lm_rubeie_st", "Al-Rubeie Street", "شارع الربيعي", "Commercial Street • Zayouna, Baghdad", "شارع تجاري • زيونة، بغداد", "STREET", SearchSuggestionSource.LANDMARK, latitude = 33.3375, longitude = 44.4521),
            SmartSearchSuggestion("lm_abunuwas_st", "Abu Nuwas Corniche Street", "شارع أبو نؤاس", "Tigris Corniche • Baghdad", "كورنيش دجلة • بغداد", "STREET", SearchSuggestionSource.LANDMARK, latitude = 33.3065, longitude = 44.3950),
            SmartSearchSuggestion("lm_mutanabbi_st", "Al-Mutanabbi Street", "شارع المتنبي", "Historic Cultural Street • Baghdad", "شارع ثقافي وتراثي • بغداد", "STREET", SearchSuggestionSource.LANDMARK, latitude = 33.3406, longitude = 44.3849),
            SmartSearchSuggestion("lm_samad_rest", "Samad Restaurant", "مطعم صمد - المنصور", "Restaurant • Al-Amirat St, Baghdad", "مطعم • شارع الأميرات، المنصور", "RESTAURANT", SearchSuggestionSource.LANDMARK, latitude = 33.3172, longitude = 44.3478),
            SmartSearchSuggestion("lm_zarzour_rest", "Zarzour Karrada Restaurant", "مطعم زرزور الكرادة", "Restaurant • Al-Karrada, Baghdad", "مطعم • الكرادة، بغداد", "RESTAURANT", SearchSuggestionSource.LANDMARK, latitude = 33.3065, longitude = 44.4125),
            SmartSearchSuggestion("lm_bgd_mall", "Baghdad Mall", "بغداد مول", "Shopping & Dining • Al-Harthiya, Baghdad", "مركز تسوق ومطاعم • الحارثية، بغداد", "RESTAURANT", SearchSuggestionSource.LANDMARK, latitude = 33.3125, longitude = 44.3549),
            SmartSearchSuggestion("lm_mansour_mall", "Al-Mansour Mall", "المنصور مول", "Shopping & Dining • Al-Mansour, Baghdad", "مركز تسوق • المنصور، بغداد", "RESTAURANT", SearchSuggestionSource.LANDMARK, latitude = 33.3198, longitude = 44.3438),
            SmartSearchSuggestion("lm_umm_tubul", "Umm Al-Tubul Grand Mosque", "جامع أم الطبول", "Mosque • Al-Yarmouk, Baghdad", "جامع • اليرموك، بغداد", "MOSQUE", SearchSuggestionSource.LANDMARK, latitude = 33.3075, longitude = 44.3315),
            SmartSearchSuggestion("lm_hanifa_mosque", "Abu Hanifa Al-Numan Mosque", "جامع أبو حنيفة النعمان", "Mosque • Al-Adhamiya, Baghdad", "جامع • الأعظمية، بغداد", "MOSQUE", SearchSuggestionSource.LANDMARK, latitude = 33.3719, longitude = 44.3578),
            SmartSearchSuggestion("lm_tahrir_sq", "Tahrir Square", "ساحة التحرير", "Landmark • Central Baghdad", "معلم • وسط بغداد", "STREET", SearchSuggestionSource.LANDMARK, latitude = 33.3292, longitude = 44.4079)
        )

        private val popularDefaultSuggestions = listOf(
            iraqDistrictsAndCities[0], // المنصور
            iraqDistrictsAndCities[1], // الجادرية
            iraqDistrictsAndCities[2], // الكرادة
            iraqDistrictsAndCities[19], // أربيل
            iraqDistrictsAndCities[22], // البصرة
            curatedIraqLandmarks[0],   // مستشفى اليرموك
            curatedIraqLandmarks[8],   // شارع الأميرات
            curatedIraqLandmarks[13]   // مطعم صمد
        )
    }
}
