package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.PropertyListing
import com.example.i18n.LocalStrings
import kotlinx.coroutines.launch

/**
 * Resolves a unified gallery list for a property:
 * Supports both uploaded content/http URIs and `"preset:<key>"` bundled assets.
 */
fun resolvePropertyGalleryItems(property: PropertyListing): List<String> {
    if (property.galleryImages.isNotEmpty()) {
        return property.galleryImages
    }
    val derived = mutableListOf<String>()
    derived.add("preset:${property.heroPresetKey.ifBlank { "villa_exterior" }}")
    property.tourScenes.forEach { scene ->
        val token = if (scene.customPhotoUri.isNotBlank()) {
            scene.customPhotoUri
        } else {
            "preset:${scene.presetKey.ifBlank { "pano_living" }}"
        }
        if (!derived.contains(token)) derived.add(token)
    }
    return derived
}

@Composable
fun CarouselMediaImage(
    mediaToken: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    if (mediaToken.startsWith("preset:")) {
        val presetKey = mediaToken.removePrefix("preset:")
        Image(
            painter = painterResource(id = resolvePresetDrawable(presetKey)),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = Uri.parse(mediaToken),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

/**
 * Swipeable Property Image Carousel with page indicator badge, dot indicators,
 * optional 360° open trigger, and thumbnail strip for rapid jumping across many images.
 */
@Composable
fun PropertyImageCarousel(
    images: List<String>,
    contentDescription: String,
    showThumbnails: Boolean = true,
    onOpenIn360: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    carouselHeight: androidx.compose.ui.unit.Dp = 285.dp
) {
    val safeImages = images.ifEmpty { listOf("preset:villa_exterior") }
    val pagerState = rememberPagerState(pageCount = { safeImages.size })
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(carouselHeight)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("property_image_carousel_pager")
            ) { page ->
                CarouselMediaImage(
                    mediaToken = safeImages[page],
                    contentDescription = "$contentDescription (${page + 1})",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Soft edge vignette for legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.42f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.68f)
                            )
                        )
                    )
            )

            // Bottom-End Counter Pill + 360° View Current Photo button
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onOpenIn360 != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clickable { onOpenIn360(safeImages[pagerState.currentPage]) }
                            .testTag("carousel_open_360_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = strings.tour360Badge,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "360°",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${safeImages.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Page Dots Center Bottom
            if (safeImages.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(safeImages.size.coerceAtMost(10)) { idx ->
                        val active = idx == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .size(width = if (active) 16.dp else 6.dp, height = 6.dp)
                                .clip(CircleShape)
                                .background(if (active) Color.White else Color.White.copy(alpha = 0.45f))
                        )
                    }
                }
            }
        }

        // Scrollable Thumbnail Strip when multiple photos exist
        if (showThumbnails && safeImages.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .testTag("carousel_thumbnail_strip")
            ) {
                itemsIndexed(safeImages) { idx, token ->
                    val selected = idx == pagerState.currentPage
                    CarouselMediaImage(
                        mediaToken = token,
                        contentDescription = "$contentDescription ${idx + 1}",
                        modifier = Modifier
                            .size(width = 72.dp, height = 54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(idx) }
                            }
                    )
                }
            }
        }
    }
}

/**
 * Editable Multi-Image Carousel for the Publish screen allowing users to add many images
 * via Android Photo Picker, swipe through them, remove items, or use any photo as a 360° scene.
 */
@Composable
fun EditableMultiImageCarousel(
    images: List<String>,
    onAddManyClick: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onSetAs360Scene: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val pagerState = rememberPagerState(pageCount = { images.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.galleryCarouselTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${images.size} ${strings.photosCountLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            if (images.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("publish_images_carousel_pager")
                    ) { page ->
                        val token = images[page]
                        Box(modifier = Modifier.fillMaxSize()) {
                            CarouselMediaImage(
                                mediaToken = token,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Remove Photo Button
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { onRemoveImage(page) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = strings.deleteListing,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Promote current carousel image to 360° Spherical Scene
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .clickable { onSetAs360Scene(token) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Explore,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (strings.isArabic) "استخدام كصورة ٣٦٠°" else "Use as 360°",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.62f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "${page + 1} / ${images.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Thumbnails Strip
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(images) { idx, token ->
                        val selected = idx == pagerState.currentPage
                        CarouselMediaImage(
                            mediaToken = token,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { scope.launch { pagerState.animateScrollToPage(idx) } }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onAddManyClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("publish_add_many_photos_button")
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.addMultiplePhotos, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
