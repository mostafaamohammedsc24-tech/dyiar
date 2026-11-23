package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.PropertyListing
import com.example.data.repository.DiyarRepository
import com.example.i18n.DiyarStrings

const val PROPERTY_SHARE_BASE_URL = "https://iraqdallal.iq/property/"

fun propertyExternalUrl(propertyId: String): String = "$PROPERTY_SHARE_BASE_URL$propertyId"

/**
 * Normalizes Arabic-Indic (٠-٩) and Eastern Arabic (۰-۹) digits plus localized separators
 * so numeric fields work smoothly with both Arabic and English keyboards.
 */
fun normalizeDigits(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
        when (ch) {
            in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
            in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
            '٫' -> sb.append('.')
            '٬', ',', ' ', '_' -> Unit
            else -> sb.append(ch)
        }
    }
    return sb.toString().trim()
}

fun parseLocalizedDouble(input: String): Double? = normalizeDigits(input).toDoubleOrNull()

fun parseLocalizedLong(input: String): Long? = normalizeDigits(input).toLongOrNull()

fun openPropertyWhatsApp(
    context: Context,
    property: PropertyListing,
    strings: DiyarStrings
) {
    val link = propertyExternalUrl(property.id)
    val title = if (strings.isArabic) property.titleAr else property.titleEn
    val listingLabel = strings.localizeListingType(property.listingType)
    val propTypeLabel = strings.localizePropertyType(property.propertyType)
    val location = if (strings.isArabic) "${property.neighborhoodAr}، ${property.cityAr}"
    else "${property.neighborhoodEn}, ${property.cityEn}"
    val priceText = formatCurrencyPrice(
        property.price,
        property.currency,
        property.listingType == "RENT",
        strings.isArabic
    )

    val message = if (strings.isArabic) {
        buildString {
            append("السلام عليكم شبكة دلال العراق، أرغب بالتواصل والاستفسار حول هذا العقار:\n")
            append("🏠 ").append(title).append("\n")
            append("🏷️ ").append(listingLabel).append(" • ").append(propTypeLabel).append("\n")
            append("📍 ").append(location).append("\n")
            append("💰 ").append(priceText).append("\n")
            append("🔗 رابط المنشور: ").append(link)
        }
    } else {
        buildString {
            append("Hello Iraq Dallal Network, I would like to inquire about this property:\n")
            append("🏠 ").append(title).append("\n")
            append("🏷️ ").append(listingLabel).append(" • ").append(propTypeLabel).append("\n")
            append("📍 ").append(location).append("\n")
            append("💰 ").append(priceText).append("\n")
            append("🔗 Post Link: ").append(link)
        }
    }

    val waUrl = "https://wa.me/${DiyarRepository.WHATSAPP_SERVICE_NUMBER}?text=${Uri.encode(message)}"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            if (strings.isArabic) "يرجى التواصل عبر واتساب على الرقم 07740080310"
            else "Please contact via WhatsApp on 07740080310",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun sharePropertyExternally(
    context: Context,
    property: PropertyListing,
    strings: DiyarStrings
) {
    val link = propertyExternalUrl(property.id)
    val title = if (strings.isArabic) property.titleAr else property.titleEn
    val desc = (if (strings.isArabic) property.descriptionAr else property.descriptionEn).trim()
    val listingLabel = strings.localizeListingType(property.listingType)
    val propTypeLabel = strings.localizePropertyType(property.propertyType)
    val location = if (strings.isArabic) "${property.neighborhoodAr}، ${property.cityAr}"
    else "${property.neighborhoodEn}, ${property.cityEn}"
    val priceText = formatCurrencyPrice(
        property.price,
        property.currency,
        property.listingType == "RENT",
        strings.isArabic
    )
    val amenitiesText = if (property.amenities.isNotEmpty()) {
        property.amenities.joinToString(" • ") { strings.localizeAmenity(it) }
    } else ""

    val shareBody = if (strings.isArabic) {
        buildString {
            append("🏡 ${strings.appName} — عقار موثق\n")
            append("📌 $title\n")
            append("🏷️ العرض: $listingLabel | النوع: $propTypeLabel\n")
            append("📍 الموقع: $location\n")
            append("💰 السعر: $priceText\n")
            append("📐 المساحة: ${property.areaSqm.toInt()} م² | 🛏️ الغرف: ${property.bedrooms} | 🚿 الحمامات: ${property.bathrooms} | 📅 البناء: ${property.yearBuilt}\n")
            if (property.rooms.isNotEmpty()) {
                val floorsCount = property.rooms.maxOf { it.floor.coerceAtLeast(1) }
                append("🏗️ الطوابق: $floorsCount | الغرف المصممة: ${property.rooms.size}\n")
            }
            if (amenitiesText.isNotEmpty()) {
                append("✨ المرافق: $amenitiesText\n")
            }
            if (desc.isNotEmpty()) {
                append("📝 الوصف: ${desc.take(220)}\n")
            }
            append("📞 للتواصل عبر واتساب: 07740080310\n")
            append("🔗 رابط العقار المباشر:\n$link")
        }
    } else {
        buildString {
            append("🏡 ${strings.appName} — Verified Listing\n")
            append("📌 $title\n")
            append("🏷️ Category: $listingLabel | Type: $propTypeLabel\n")
            append("📍 Location: $location\n")
            append("💰 Price: $priceText\n")
            append("📐 Area: ${property.areaSqm.toInt()} m² | 🛏️ Beds: ${property.bedrooms} | 🚿 Baths: ${property.bathrooms} | 📅 Built: ${property.yearBuilt}\n")
            if (property.rooms.isNotEmpty()) {
                val floorsCount = property.rooms.maxOf { it.floor.coerceAtLeast(1) }
                append("🏗️ Floors: $floorsCount | Designed Rooms: ${property.rooms.size}\n")
            }
            if (amenitiesText.isNotEmpty()) {
                append("✨ Amenities: $amenitiesText\n")
            }
            if (desc.isNotEmpty()) {
                append("📝 Overview: ${desc.take(220)}\n")
            }
            append("📞 WhatsApp Contact: 07740080310\n")
            append("🔗 Direct Property Link:\n$link")
        }
    }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, shareBody)
    }
    val chooser = Intent.createChooser(
        sendIntent,
        if (strings.isArabic) "مشاركة العقار عبر" else "Share Property"
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            if (strings.isArabic) "لا يوجد تطبيق متاح للمشاركة" else "No app available to share",
            Toast.LENGTH_SHORT
        ).show()
    }
}
