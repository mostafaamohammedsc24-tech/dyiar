package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.LocalStrings
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DiyarActionBlue
import com.example.ui.theme.LightSurface
import com.example.ui.theme.RentViolet
import com.example.ui.theme.VerifiedEmerald

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DesignPrototypeSheet(
    activeCategory: MapRailCategory?,
    mortgageCount: Int = 0,
    saleCount: Int,
    rentCount: Int,
    commercialCount: Int,
    onSelectCategory: (MapRailCategory) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("design_prototype_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Sheet Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DesignServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = strings.designPrototypeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.designPrototypeSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 1. Live Interactive Vertical Category Rail + Map Marker States
            Text(
                text = strings.verticalRailSpecTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Interactive Rail Preview synced with live Map Screen
                    VerticalMapCategorySelector(
                        activeCategory = activeCategory,
                        mortgageCount = mortgageCount,
                        saleCount = saleCount,
                        rentCount = rentCount,
                        commercialCount = commercialCount,
                        onSelectCategory = onSelectCategory
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.markerAnatomyTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.showingAllMarkers,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Live Marker Tokens Preview (Mortgage / Sale / Rent / Commercial)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PricePinBadge(
                                priceText = if (strings.isArabic) "180 م" else "180M",
                                listingType = "MORTGAGE",
                                propertyType = "APARTMENT",
                                selected = activeCategory == MapRailCategory.MORTGAGE,
                                verified = true
                            )
                            PricePinBadge(
                                priceText = if (strings.isArabic) "950 م" else "950M",
                                listingType = "SALE",
                                propertyType = "VILLA",
                                selected = activeCategory == MapRailCategory.SALE,
                                verified = true
                            )
                            PricePinBadge(
                                priceText = if (strings.isArabic) "24 م" else "24M",
                                listingType = "RENT",
                                propertyType = "APARTMENT",
                                selected = activeCategory == MapRailCategory.RENT,
                                verified = true
                            )
                            PricePinBadge(
                                priceText = if (strings.isArabic) "1.25 مليار" else "1.25B",
                                listingType = "SALE",
                                propertyType = "COMMERCIAL",
                                selected = activeCategory == MapRailCategory.COMMERCIAL,
                                verified = true
                            )
                        }
                    }
                }
            }

            // 2. Color System & Surface Tokens
            Text(
                text = strings.colorTokensTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DesignTokenSwatch(
                    name = if (strings.isArabic) "برتقالي الشبكة" else "Action Orange",
                    hex = "#EA580C",
                    color = DiyarActionBlue
                )
                DesignTokenSwatch(
                    name = if (strings.isArabic) "داكن رئيسي" else "Obsidian BG",
                    hex = "#0B0E14",
                    color = DarkBackground
                )
                DesignTokenSwatch(
                    name = if (strings.isArabic) "بطاقات داكنة" else "Dark Surface",
                    hex = "#131722",
                    color = DarkSurface
                )
                DesignTokenSwatch(
                    name = if (strings.isArabic) "نهاري ناصع" else "Light Surface",
                    hex = "#FFFFFF",
                    color = LightSurface
                )
                DesignTokenSwatch(
                    name = if (strings.isArabic) "إيجار" else "Rent Violet",
                    hex = "#8B5CF6",
                    color = RentViolet
                )
                DesignTokenSwatch(
                    name = if (strings.isArabic) "تجاري" else "Commercial",
                    hex = "#F59E0B",
                    color = AmberGold
                )
                DesignTokenSwatch(
                    name = if (strings.isArabic) "توثيق موثوق" else "Verified",
                    hex = "#10B981",
                    color = VerifiedEmerald
                )
            }

            // 3. Geometry, Touch Target & Spacing Grid Specs
            Text(
                text = strings.spacingGridTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpecRow(
                        label = if (strings.isArabic) "حجم اللمس الأدنى للأيقونات" else "Minimum Touch Target",
                        value = "48.dp × 48.dp"
                    )
                    SpecRow(
                        label = if (strings.isArabic) "انحناء حاوية التصنيف العمودي" else "Vertical Rail Corner Radius",
                        value = "22.dp Capsule"
                    )
                    SpecRow(
                        label = if (strings.isArabic) "شبكة التباعد الهندسية" else "Base Spacing Grid",
                        value = "8.dp / 16.dp / 20.dp"
                    )
                    SpecRow(
                        label = if (strings.isArabic) "حركة التبديل" else "Motion Spec",
                        value = "M3 Spring (< 300ms)"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DesignTokenSwatch(
    name: String,
    hex: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = hex,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
