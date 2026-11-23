package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class RoomSpec(
    val nameEn: String = "",
    val nameAr: String = "",
    val widthM: Double = 4.0,
    val lengthM: Double = 5.0,
    val ceilingM: Double = 3.2,
    val paintNameEn: String = "Warm Alabaster",
    val paintNameAr: String = "أبيض ألاباستر دافئ",
    val paintHex: String = "#F4F1EA",
    val flooringEn: String = "Porcelain Tile",
    val flooringAr: String = "بورسلان إيطالي",
    val floor: Int = 1,
    val xNorm: Double = -1.0,
    val yNorm: Double = -1.0,
    val wNorm: Double = 0.0,
    val hNorm: Double = 0.0
) {
    fun areaSqm(): Double = widthM * lengthM

    fun toMap(): Map<String, Any> = mapOf(
        "nameEn" to nameEn,
        "nameAr" to nameAr,
        "widthM" to widthM,
        "lengthM" to lengthM,
        "ceilingM" to ceilingM,
        "paintNameEn" to paintNameEn,
        "paintNameAr" to paintNameAr,
        "paintHex" to paintHex,
        "flooringEn" to flooringEn,
        "flooringAr" to flooringAr,
        "floor" to floor,
        "xNorm" to xNorm,
        "yNorm" to yNorm,
        "wNorm" to wNorm,
        "hNorm" to hNorm
    )
}

@Keep
@IgnoreExtraProperties
data class TourScene(
    val sceneId: String = "",
    val titleEn: String = "",
    val titleAr: String = "",
    val presetKey: String = "pano_living",
    val initialYaw: Double = 0.0,
    val customPhotoUri: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "sceneId" to sceneId,
        "titleEn" to titleEn,
        "titleAr" to titleAr,
        "presetKey" to presetKey,
        "initialYaw" to initialYaw,
        "customPhotoUri" to customPhotoUri
    )
}

@Keep
@IgnoreExtraProperties
data class NearbyLandmark(
    val category: String = "SERVICE", // MOSQUE, SCHOOL, HOSPITAL, RESTAURANT, STREET, SERVICE
    val nameEn: String = "",
    val nameAr: String = "",
    val distanceMeters: Long = 350L,
    val walkMinutes: Long = 4L,
    val latitude: Double = 33.3152,
    val longitude: Double = 44.3661
) {
    fun toMap(): Map<String, Any> = mapOf(
        "category" to category,
        "nameEn" to nameEn,
        "nameAr" to nameAr,
        "distanceMeters" to distanceMeters,
        "walkMinutes" to walkMinutes,
        "latitude" to latitude,
        "longitude" to longitude
    )
}

@Keep
@IgnoreExtraProperties
data class PropertyListing(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerDisplayName: String = "",
    val titleEn: String = "",
    val titleAr: String = "",
    val descriptionEn: String = "",
    val descriptionAr: String = "",
    val listingType: String = "SALE", // SALE, RENT, MORTGAGE
    val propertyType: String = "VILLA", // VILLA, APARTMENT, PENTHOUSE, TOWNHOUSE, COMMERCIAL, LAND
    val status: String = "ACTIVE", // ACTIVE, DRAFT, SOLD
    val verificationStatus: String = "VERIFIED", // VERIFIED, PENDING
    val verified: Boolean = true,
    val deedReference: String = "",
    val price: Double = 0.0,
    val currency: String = "IQD",
    val areaSqm: Double = 0.0,
    val bedrooms: Long = 0L,
    val bathrooms: Long = 0L,
    val yearBuilt: Long = 2024L,
    val amenities: List<String> = emptyList(), // POOL, GARDEN, ELEVATOR, SMART_HOME, PARKING, MAID_ROOM, GYM
    val cityEn: String = "",
    val cityAr: String = "",
    val neighborhoodEn: String = "",
    val neighborhoodAr: String = "",
    val latitude: Double = 33.3152,
    val longitude: Double = 44.3661,
    val hasVirtualTour: Boolean = false,
    val hasFloorPlan: Boolean = false,
    val heroPresetKey: String = "villa_exterior",
    val ceilingHeightM: Double = 3.2,
    val floorPlanWidthM: Double = 15.0,
    val floorPlanLengthM: Double = 20.0,
    val rooms: List<RoomSpec> = emptyList(),
    val tourScenes: List<TourScene> = emptyList(),
    val nearbyLandmarks: List<NearbyLandmark> = emptyList(),
    val galleryImages: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteProperty(
    @DocumentId val id: String = "",
    val propertyId: String = "",
    val titleEn: String = "",
    val titleAr: String = "",
    val price: Double = 0.0,
    val status: String = "ACTIVE",
    val cityEn: String = "",
    val cityAr: String = "",
    val listingType: String = "SALE",
    val propertyType: String = "VILLA",
    val heroPresetKey: String = "villa_exterior",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AlertRule(
    @DocumentId val id: String = "",
    val nameEn: String = "",
    val nameAr: String = "",
    val listingType: String = "ALL", // ALL, SALE, RENT, MORTGAGE
    val propertyType: String = "ALL", // ALL, VILLA, APARTMENT, PENTHOUSE, TOWNHOUSE, COMMERCIAL, LAND
    val maxPrice: Double = 0.0,
    val minAreaSqm: Double = 0.0,
    val cityFilter: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PropertyAlert(
    @DocumentId val id: String = "",
    val propertyId: String = "",
    val ruleId: String = "",
    val titleEn: String = "",
    val titleAr: String = "",
    val messageEn: String = "",
    val messageAr: String = "",
    val price: Double = 0.0,
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ServiceRequest(
    @DocumentId val id: String = "",
    val serviceCategory: String = "STAGING", // STAGING, MAINTENANCE, REPAIR, MOVING
    val packageTitleEn: String = "",
    val packageTitleAr: String = "",
    val propertyTitle: String = "",
    val address: String = "",
    val scheduledDate: String = "",
    val notes: String? = null,
    val estimatedPrice: Double = 0.0,
    val status: String = "REQUESTED", // REQUESTED, CONFIRMED, COMPLETED, CANCELLED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class ListingSort {
    NEWEST,
    PRICE_ASC,
    PRICE_DESC,
    AREA_DESC
}

enum class DiyarMapType {
    NORMAL,
    SATELLITE,
    HYBRID,
    TERRAIN
}

data class MapPointOfInterest(
    val id: String,
    val category: String, // HOSPITAL, SCHOOL, RESTAURANT, MOSQUE, STREET, SERVICE
    val nameEn: String,
    val nameAr: String,
    val subtitleEn: String,
    val subtitleAr: String,
    val latitude: Double,
    val longitude: Double
)

enum class SearchSuggestionSource {
    GOOGLE_PLACES,
    GEOCODER,
    IRAQ_DISTRICT,
    LANDMARK,
    PROPERTY,
    CATEGORY_FILTER
}

data class SmartSearchSuggestion(
    val id: String,
    val primaryTextEn: String,
    val primaryTextAr: String,
    val secondaryTextEn: String,
    val secondaryTextAr: String,
    val category: String, // CITY, DISTRICT, HOSPITAL, SCHOOL, RESTAURANT, MOSQUE, STREET, PROPERTY, SERVICE, CATEGORY
    val source: SearchSuggestionSource,
    val placeId: String? = null,
    val propertyId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val filterQueryText: String = ""
)

data class PropertyFilter(
    val query: String = "",
    val listingType: String = "ALL", // ALL, SALE, RENT, MORTGAGE
    val propertyType: String = "ALL", // ALL, VILLA, APARTMENT, PENTHOUSE, TOWNHOUSE, COMMERCIAL, LAND
    val minPrice: Double = 0.0,
    val maxPrice: Double = 2_000_000_000.0,
    val minAreaSqm: Double = 0.0,
    val minBedrooms: Int = 0,
    val selectedAmenities: Set<String> = emptySet(),
    val minYearBuilt: Int = 0,
    val radiusKm: Double = 100.0,
    val centerLat: Double = 33.3152,
    val centerLng: Double = 44.3661,
    val geoRadiusEnabled: Boolean = false,
    val verifiedOnly: Boolean = false,
    val virtualTourOnly: Boolean = false,
    val floorPlanOnly: Boolean = false,
    val sort: ListingSort = ListingSort.NEWEST
)

data class ServicePackage(
    val id: String,
    val category: String, // STAGING, MAINTENANCE, REPAIR, MOVING
    val titleEn: String,
    val titleAr: String,
    val subtitleEn: String,
    val subtitleAr: String,
    val durationEn: String,
    val durationAr: String,
    val priceSar: Double,
    val rating: Double
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
