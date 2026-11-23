package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AlertRule
import com.example.data.model.PropertyListing
import com.example.data.model.UiState
import com.example.i18n.LocalStrings
import com.example.notifications.DiyarNotificationHelper
import com.example.ui.components.PropertyReportCard
import com.example.ui.components.formatCurrencyPrice
import com.example.ui.components.parseLocalizedDouble
import com.example.ui.viewmodel.DiyarViewModel

@Composable
fun FavoritesScreen(
    viewModel: DiyarViewModel,
    onOpenDetail: (String) -> Unit
) {
    val strings = LocalStrings.current
    val activeState by viewModel.activePropertiesState.collectAsStateWithLifecycle()
    val myState by viewModel.myPropertiesState.collectAsStateWithLifecycle()
    val favoritesState by viewModel.favoritesState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = strings.favoritesTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }

        when (val favState = favoritesState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(favState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                val activeList = (activeState as? UiState.Success<List<PropertyListing>>)?.data ?: emptyList()
                val myList = (myState as? UiState.Success<List<PropertyListing>>)?.data ?: emptyList()
                val allKnown = (activeList + myList).associateBy { it.id }
                val savedProperties = favState.data.mapNotNull { fav -> allKnown[fav.propertyId] }

                if (savedProperties.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(strings.noFavoritesTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(strings.noFavoritesBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(savedProperties, key = { it.id }) { prop ->
                            PropertyReportCard(
                                property = prop,
                                isFavorite = true,
                                onToggleFavorite = { viewModel.toggleFavorite(prop) },
                                onClick = { onOpenDetail(prop.id) },
                                modifier = Modifier.widthIn(max = 600.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlertsScreen(
    viewModel: DiyarViewModel,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    BackHandler { onBack() }

    val strings = LocalStrings.current
    val context = LocalContext.current
    val alertsState by viewModel.alertsState.collectAsStateWithLifecycle()
    val rulesState by viewModel.alertRulesState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    var showCreateRuleSheet by remember { mutableStateOf(false) }

    var hasPushPerm by remember { mutableStateOf(DiyarNotificationHelper.hasNotificationPermission(context)) }
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPushPerm = granted
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateRuleSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_alert_rule_fab")
            ) {
                Icon(Icons.Default.AddAlert, contentDescription = strings.createWatchRule)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("alerts_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.prevStep,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = strings.alertsTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPushPerm) {
                            Button(
                                onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(strings.enablePushAlerts, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    PrimaryTabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text(strings.inboxTab) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text(strings.watchRulesTab) }
                        )
                    }
                }
            }

            if (activeTab == 0) {
                when (val state = alertsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(strings.noAlertsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text(strings.noAlertsBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.data, key = { it.id }) { alert ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (alert.read) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (alert.read) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier
                                            .widthIn(max = 680.dp)
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.markAlertRead(alert.id)
                                                onOpenDetail(alert.propertyId)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (alert.read) MaterialTheme.colorScheme.surfaceVariant
                                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.NotificationsActive,
                                                    contentDescription = null,
                                                    tint = if (alert.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (strings.isArabic) alert.titleAr else alert.titleEn,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (strings.isArabic) alert.messageAr else alert.messageEn,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = formatCurrencyPrice(alert.price, "IQD", false, strings.isArabic),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                when (val state = rulesState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.data, key = { it.id }) { rule ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (strings.isArabic) rule.nameAr else rule.nameEn,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${strings.localizeListingType(rule.listingType)} • ${strings.localizePropertyType(rule.propertyType)} • <= ${formatCurrencyPrice(rule.maxPrice, "IQD", false, strings.isArabic)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = rule.active,
                                            onCheckedChange = { viewModel.toggleAlertRule(rule.id, it) }
                                        )
                                        IconButton(onClick = { viewModel.deleteAlertRule(rule.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = strings.deleteListing, tint = MaterialTheme.colorScheme.error)
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

    if (showCreateRuleSheet) {
        var ruleName by remember { mutableStateOf("") }
        var listingType by remember { mutableStateOf("ALL") }
        var propertyType by remember { mutableStateOf("VILLA") }
        var maxPriceInput by remember { mutableStateOf("650000000") }
        var cityInput by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { showCreateRuleSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.createWatchRule,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text(strings.ruleNameHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("alert_rule_name_input")
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
                    ).forEach { (code, lbl) ->
                        FilterChip(
                            selected = listingType == code,
                            onClick = { listingType = code },
                            label = { Text(lbl) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            border = null
                        )
                    }
                }
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
                    ).forEach { (code, lbl) ->
                        FilterChip(
                            selected = propertyType == code,
                            onClick = { propertyType = code },
                            label = { Text(lbl) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            border = null
                        )
                    }
                }
                OutlinedTextField(
                    value = maxPriceInput,
                    onValueChange = { maxPriceInput = it },
                    label = { Text(strings.maxPriceLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = { Text(strings.cityFilterLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val name = ruleName.trim().ifBlank {
                            if (strings.isArabic) "تنبيه شبكة دلال العراق" else "Iraq Dallal Smart Alert"
                        }
                        viewModel.createAlertRule(
                            AlertRule(
                                nameEn = name,
                                nameAr = name,
                                listingType = listingType,
                                propertyType = propertyType,
                                maxPrice = parseLocalizedDouble(maxPriceInput)?.coerceAtLeast(0.0) ?: 650_000_000.0,
                                minAreaSqm = 0.0,
                                cityFilter = cityInput.trim()
                            )
                        ) {
                            showCreateRuleSheet = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_alert_rule_button")
                ) {
                    Text(strings.saveRule)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
