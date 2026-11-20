package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ServicePackage
import com.example.data.model.ServiceRequest
import com.example.data.model.UiState
import com.example.data.repository.DiyarRepository
import com.example.i18n.LocalStrings
import com.example.ui.components.formatCurrencyPrice
import com.example.ui.theme.AmberGold
import com.example.ui.viewmodel.DiyarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(viewModel: DiyarViewModel) {
    val strings = LocalStrings.current
    val requestsState by viewModel.serviceRequestsState.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("ALL") }
    var bookingPackage by remember { mutableStateOf<ServicePackage?>(null) }

    val packages = remember(selectedCategory) {
        DiyarRepository.defaultServicePackages.filter {
            selectedCategory == "ALL" || it.category == selectedCategory
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth()
                    .height(172.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_services_hero),
                    contentDescription = strings.servicesTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xD91A120B),
                                    Color(0x661A120B)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(18.dp)
                ) {
                    Text(
                        text = strings.servicesHeroTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = strings.servicesHeroSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to strings.all,
                    "STAGING" to strings.stagingCat,
                    "MAINTENANCE" to strings.maintenanceCat,
                    "REPAIR" to strings.repairCat,
                    "MOVING" to strings.movingCat
                ).forEach { (cat, label) ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(label) }
                    )
                }
            }
        }

        items(packages, key = { it.id }) { pkg ->
            val icon = when (pkg.category) {
                "STAGING" -> Icons.Default.DesignServices
                "MAINTENANCE" -> Icons.Default.CleaningServices
                "REPAIR" -> Icons.Default.Build
                else -> Icons.Default.LocalShipping
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.widthIn(max = 700.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isArabic) pkg.titleAr else pkg.titleEn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = AmberGold, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "${pkg.rating} • ${if (strings.isArabic) pkg.durationAr else pkg.durationEn}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (strings.isArabic) pkg.subtitleAr else pkg.subtitleEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatCurrencyPrice(pkg.priceSar, "IQD", false, strings.isArabic),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { bookingPackage = pkg },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("book_service_${pkg.id}")
                        ) {
                            Text(strings.bookService)
                        }
                    }
                }
            }
        }

        if (requestsState is UiState.Success && (requestsState as UiState.Success<List<ServiceRequest>>).data.isNotEmpty()) {
            val bookings = (requestsState as UiState.Success<List<ServiceRequest>>).data
            item {
                Text(
                    text = strings.myBookings,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(max = 700.dp).fillMaxWidth().padding(top = 8.dp)
                )
            }
            items(bookings, key = { it.id }) { req ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.widthIn(max = 700.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isArabic) req.packageTitleAr else req.packageTitleEn,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${req.address} • ${req.scheduledDate} • ${strings.localizeServiceStatus(req.status)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (req.status != "CANCELLED" && req.status != "COMPLETED") {
                            OutlinedButton(onClick = { viewModel.cancelServiceRequest(req.id) }) {
                                Text(strings.cancelBooking)
                            }
                        }
                    }
                }
            }
        }
    }

    val selectedPkg = bookingPackage
    if (selectedPkg != null) {
        var propertyTitle by remember { mutableStateOf(if (strings.isArabic) "فيلا المنصور" else "Al-Mansour Villa") }
        var address by remember { mutableStateOf("") }
        var scheduledDate by remember { mutableStateOf("2026-11-26") }
        var notes by remember { mutableStateOf("") }

        ModalBottomSheet(onDismissRequest = { bookingPackage = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (strings.isArabic) selectedPkg.titleAr else selectedPkg.titleEn,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = propertyTitle,
                    onValueChange = { propertyTitle = it },
                    label = { Text(if (strings.isArabic) "اسم العقار" else "Property Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(strings.addressLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = scheduledDate,
                    onValueChange = { scheduledDate = it },
                    label = { Text(strings.scheduleDateLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notesLabel) },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.bookService(
                            ServiceRequest(
                                serviceCategory = selectedPkg.category,
                                packageTitleEn = selectedPkg.titleEn,
                                packageTitleAr = selectedPkg.titleAr,
                                propertyTitle = propertyTitle.ifBlank { "Residence" },
                                address = address.ifBlank { if (strings.isArabic) "بغداد، العراق" else "Baghdad, Iraq" },
                                scheduledDate = scheduledDate.ifBlank { "2026-11-26" },
                                notes = notes.ifBlank { null },
                                estimatedPrice = selectedPkg.priceSar
                            )
                        ) {
                            bookingPackage = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("confirm_service_booking")
                ) {
                    Text(strings.confirmBooking)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
