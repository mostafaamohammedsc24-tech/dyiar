package com.example.data.remote

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.SearchSuggestionSource
import com.example.data.model.SmartSearchSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

class GooglePlacesAutocompleteService(
    private val appContext: Context
) {

    fun hasValidApiKey(): Boolean {
        val key = BuildConfig.MAPS_API_KEY
        return key.isNotBlank() &&
            key != "YOUR_GOOGLE_MAPS_API_KEY" &&
            key != "MY_MAPS_API_KEY"
    }

    suspend fun fetchLivePlaceSuggestions(
        query: String,
        isArabic: Boolean
    ): List<SmartSearchSuggestion> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val results = mutableListOf<SmartSearchSuggestion>()

        if (hasValidApiKey()) {
            val placesNew = fetchPlacesApiNew(trimmed, isArabic)
            if (placesNew.isNotEmpty()) {
                results.addAll(placesNew)
            } else {
                results.addAll(fetchPlacesApiLegacy(trimmed, isArabic))
            }
        }

        // Also enrich with multi-result Android Google Geocoder scoped to Iraq
        if (results.size < 4) {
            val geocoded = geocodeMultipleInIraq(trimmed, isArabic)
            for (geo in geocoded) {
                if (results.none {
                        it.primaryTextEn.equals(geo.primaryTextEn, ignoreCase = true) ||
                            it.primaryTextAr.equals(geo.primaryTextAr, ignoreCase = true)
                    }
                ) {
                    results.add(geo)
                }
            }
        }

        results.take(6)
    }

    suspend fun resolvePlaceCoordinates(
        placeId: String?,
        fallbackQuery: String,
        isArabic: Boolean
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (!placeId.isNullOrBlank() && hasValidApiKey()) {
            fetchPlaceDetailsNew(placeId) ?: fetchPlaceDetailsLegacy(placeId)?.let { return@withContext it }
        }
        val geo = geocodeMultipleInIraq(fallbackQuery, isArabic).firstOrNull()
        if (geo?.latitude != null && geo.longitude != null) {
            geo.latitude to geo.longitude
        } else {
            null
        }
    }

    private fun fetchPlacesApiNew(
        query: String,
        isArabic: Boolean
    ): List<SmartSearchSuggestion> {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("https://places.googleapis.com/v1/places:autocomplete")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4500
                readTimeout = 4500
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
            }

            val payload = JSONObject().apply {
                put("input", query)
                put("languageCode", if (isArabic) "ar" else "en")
                put("includedRegionCodes", JSONArray().apply { put("iq") })
                put("locationBias", JSONObject().apply {
                    put("circle", JSONObject().apply {
                        put("center", JSONObject().apply {
                            put("latitude", 33.3152)
                            put("longitude", 44.3661)
                        })
                        put("radius", 50000.0)
                    })
                })
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            if (conn.responseCode !in 200..299) {
                return emptyList()
            }

            val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use {
                it.readText()
            }
            val root = JSONObject(body)
            val arr = root.optJSONArray("suggestions") ?: return emptyList()
            val out = mutableListOf<SmartSearchSuggestion>()

            for (i in 0 until arr.length()) {
                val pred = arr.optJSONObject(i)?.optJSONObject("placePrediction") ?: continue
                val placeId = pred.optString("placeId", "")
                val fullText = pred.optJSONObject("text")?.optString("text", "") ?: ""
                val structured = pred.optJSONObject("structuredFormat")
                val mainText = structured?.optJSONObject("mainText")?.optString("text", "")
                    ?.takeIf { it.isNotBlank() } ?: fullText
                val secondaryText = structured?.optJSONObject("secondaryText")?.optString("text", "")
                    ?.takeIf { it.isNotBlank() } ?: if (isArabic) "العراق • خرائط Google" else "Iraq • Google Maps"

                if (mainText.isBlank()) continue
                val types = pred.optJSONArray("types")
                val category = mapGooglePlaceTypesToCategory(types, mainText)

                out.add(
                    SmartSearchSuggestion(
                        id = "gplace_${placeId.ifBlank { mainText.hashCode().toString() }}",
                        primaryTextEn = mainText,
                        primaryTextAr = mainText,
                        secondaryTextEn = secondaryText,
                        secondaryTextAr = secondaryText,
                        category = category,
                        source = SearchSuggestionSource.GOOGLE_PLACES,
                        placeId = placeId.takeIf { it.isNotBlank() },
                        filterQueryText = mainText
                    )
                )
            }
            out
        } catch (e: Exception) {
            Log.w("GooglePlacesService", "Places Autocomplete (New) error", e)
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

    private fun fetchPlacesApiLegacy(
        query: String,
        isArabic: Boolean
    ): List<SmartSearchSuggestion> {
        var conn: HttpURLConnection? = null
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val lang = if (isArabic) "ar" else "en"
            val url = URL(
                "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
                    "?input=$encoded&components=country:iq&language=$lang&key=${BuildConfig.MAPS_API_KEY}"
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
            }
            if (conn.responseCode !in 200..299) return emptyList()

            val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use {
                it.readText()
            }
            val root = JSONObject(body)
            if (root.optString("status") != "OK") return emptyList()

            val preds = root.optJSONArray("predictions") ?: return emptyList()
            val out = mutableListOf<SmartSearchSuggestion>()
            for (i in 0 until preds.length()) {
                val item = preds.optJSONObject(i) ?: continue
                val placeId = item.optString("place_id", "")
                val sf = item.optJSONObject("structured_formatting")
                val mainText = sf?.optString("main_text", "")
                    ?.takeIf { it.isNotBlank() }
                    ?: item.optString("description", "")
                val secondaryText = sf?.optString("secondary_text", "")
                    ?.takeIf { it.isNotBlank() }
                    ?: if (isArabic) "العراق • خرائط Google" else "Iraq • Google Maps"

                if (mainText.isBlank()) continue
                val category = mapGooglePlaceTypesToCategory(item.optJSONArray("types"), mainText)
                out.add(
                    SmartSearchSuggestion(
                        id = "gplace_leg_${placeId.ifBlank { mainText.hashCode().toString() }}",
                        primaryTextEn = mainText,
                        primaryTextAr = mainText,
                        secondaryTextEn = secondaryText,
                        secondaryTextAr = secondaryText,
                        category = category,
                        source = SearchSuggestionSource.GOOGLE_PLACES,
                        placeId = placeId.takeIf { it.isNotBlank() },
                        filterQueryText = mainText
                    )
                )
            }
            out
        } catch (e: Exception) {
            Log.w("GooglePlacesService", "Places Autocomplete (Legacy) error", e)
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

    private fun fetchPlaceDetailsNew(placeId: String): Pair<Double, Double>? {
        var conn: HttpURLConnection? = null
        return try {
            val encodedId = URLEncoder.encode(placeId, "UTF-8")
            val url = URL("https://places.googleapis.com/v1/places/$encodedId")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                setRequestProperty("X-Goog-FieldMask", "location")
            }
            if (conn.responseCode !in 200..299) return null
            val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use {
                it.readText()
            }
            val loc = JSONObject(body).optJSONObject("location") ?: return null
            val lat = loc.optDouble("latitude", Double.NaN)
            val lng = loc.optDouble("longitude", Double.NaN)
            if (!lat.isNaN() && !lng.isNaN()) lat to lng else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun fetchPlaceDetailsLegacy(placeId: String): Pair<Double, Double>? {
        var conn: HttpURLConnection? = null
        return try {
            val encodedId = URLEncoder.encode(placeId, "UTF-8")
            val url = URL(
                "https://maps.googleapis.com/maps/api/place/details/json" +
                    "?place_id=$encodedId&fields=geometry&key=${BuildConfig.MAPS_API_KEY}"
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
            }
            if (conn.responseCode !in 200..299) return null
            val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use {
                it.readText()
            }
            val loc = JSONObject(body)
                .optJSONObject("result")
                ?.optJSONObject("geometry")
                ?.optJSONObject("location") ?: return null
            val lat = loc.optDouble("lat", Double.NaN)
            val lng = loc.optDouble("lng", Double.NaN)
            if (!lat.isNaN() && !lng.isNaN()) lat to lng else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun geocodeMultipleInIraq(
        query: String,
        isArabic: Boolean
    ): List<SmartSearchSuggestion> {
        return try {
            if (!Geocoder.isPresent()) return emptyList()
            val geocoder = Geocoder(appContext, Locale(if (isArabic) "ar" else "en"))
            val hasIraqHint = listOf(
                "iraq", "baghdad", "erbil", "basra", "najaf", "karbala", "mosul",
                "العراق", "بغداد", "أربيل", "اربيل", "البصرة", "النجف", "كربلاء", "الموصل"
            ).any { query.contains(it, ignoreCase = true) }

            val scopedQuery = if (hasIraqHint) query else "$query, Iraq"
            val addresses: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(scopedQuery, 5) { list ->
                        if (cont.isActive) cont.resume(list)
                    }
                }
            } else {
                geocoder.getFromLocationName(scopedQuery, 5).orEmpty()
            }

            addresses.mapIndexedNotNull { index, addr ->
                val title = addr.featureName
                    ?.takeIf { it.isNotBlank() }
                    ?: addr.subLocality
                    ?: addr.locality
                    ?: return@mapIndexedNotNull null
                val subtitle = listOfNotNull(
                    addr.subLocality?.takeIf { it != title },
                    addr.locality?.takeIf { it != title },
                    addr.adminArea,
                    addr.countryName
                ).distinct().joinToString(" • ").ifBlank {
                    if (isArabic) "العراق" else "Iraq"
                }

                SmartSearchSuggestion(
                    id = "geo_${index}_${title.hashCode()}",
                    primaryTextEn = title,
                    primaryTextAr = title,
                    secondaryTextEn = subtitle,
                    secondaryTextAr = subtitle,
                    category = inferCategoryFromText("$title $subtitle"),
                    source = SearchSuggestionSource.GEOCODER,
                    latitude = addr.latitude,
                    longitude = addr.longitude,
                    filterQueryText = title
                )
            }
        } catch (e: Exception) {
            Log.w("GooglePlacesService", "Geocoder suggestions failed for $query", e)
            emptyList()
        }
    }

    private fun mapGooglePlaceTypesToCategory(types: JSONArray?, name: String): String {
        val typeList = buildList {
            if (types != null) {
                for (i in 0 until types.length()) {
                    add(types.optString(i, "").lowercase())
                }
            }
        }
        return when {
            typeList.any { it.contains("hospital") || it.contains("doctor") || it.contains("health") || it.contains("pharmacy") } -> "HOSPITAL"
            typeList.any { it.contains("restaurant") || it.contains("cafe") || it.contains("food") || it.contains("bakery") } -> "RESTAURANT"
            typeList.any { it.contains("school") || it.contains("university") } -> "SCHOOL"
            typeList.any { it.contains("mosque") || it.contains("place_of_worship") } -> "MOSQUE"
            typeList.any { it.contains("route") || it.contains("street") || it.contains("intersection") } -> "STREET"
            typeList.any { it.contains("sublocality") || it.contains("neighborhood") || it.contains("locality") || it.contains("administrative_area") } -> "DISTRICT"
            else -> inferCategoryFromText(name)
        }
    }

    fun inferCategoryFromText(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("hospital") || lower.contains("clinic") || lower.contains("medical") ||
                lower.contains("مستشفى") || lower.contains("مستوصف") || lower.contains("مدينة الطب") || lower.contains("عيادة") -> "HOSPITAL"
            lower.contains("school") || lower.contains("university") || lower.contains("college") ||
                lower.contains("مدرس") || lower.contains("جامعة") || lower.contains("كلية") -> "SCHOOL"
            lower.contains("restaurant") || lower.contains("cafe") || lower.contains("grill") || lower.contains("mall") ||
                lower.contains("مطعم") || lower.contains("مقهى") || lower.contains("كافيه") || lower.contains("مول") -> "RESTAURANT"
            lower.contains("mosque") || lower.contains("shrine") ||
                lower.contains("جامع") || lower.contains("مسجد") || lower.contains("حضرة") || lower.contains("عتبة") -> "MOSQUE"
            lower.contains("street") || lower.contains("road") || lower.contains("ave") || lower.contains("square") ||
                lower.contains("شارع") || lower.contains("طريق") || lower.contains("ساحة") || lower.contains("تقاطع") -> "STREET"
            else -> "DISTRICT"
        }
    }
}
