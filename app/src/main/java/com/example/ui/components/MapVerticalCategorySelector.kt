package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.LocalStrings
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DiyarActionBlue
import com.example.ui.theme.MortgageCyan
import com.example.ui.theme.RentViolet

enum class MapRailCategory {
    MORTGAGE,
    SALE,
    RENT,
    COMMERCIAL
}

@Composable
fun VerticalMapCategorySelector(
    activeCategory: MapRailCategory?,
    mortgageCount: Int,
    saleCount: Int,
    rentCount: Int,
    commercialCount: Int,
    onSelectCategory: (MapRailCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .widthIn(min = 52.dp, max = 60.dp)
            .testTag("vertical_map_category_selector")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            VerticalCategoryItem(
                label = strings.mortgageShort,
                icon = Icons.Default.AccountBalance,
                count = mortgageCount,
                selected = activeCategory == MapRailCategory.MORTGAGE,
                accentColor = MortgageCyan,
                testTag = "map_cat_mortgage",
                onClick = { onSelectCategory(MapRailCategory.MORTGAGE) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            VerticalCategoryItem(
                label = strings.saleShort,
                icon = Icons.Default.Home,
                count = saleCount,
                selected = activeCategory == MapRailCategory.SALE,
                accentColor = DiyarActionBlue,
                testTag = "map_cat_sale",
                onClick = { onSelectCategory(MapRailCategory.SALE) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            VerticalCategoryItem(
                label = strings.rentShort,
                icon = Icons.Default.Key,
                count = rentCount,
                selected = activeCategory == MapRailCategory.RENT,
                accentColor = RentViolet,
                testTag = "map_cat_rent",
                onClick = { onSelectCategory(MapRailCategory.RENT) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            VerticalCategoryItem(
                label = strings.commercialShort,
                icon = Icons.Default.Storefront,
                count = commercialCount,
                selected = activeCategory == MapRailCategory.COMMERCIAL,
                accentColor = AmberGold,
                testTag = "map_cat_commercial",
                onClick = { onSelectCategory(MapRailCategory.COMMERCIAL) }
            )
        }
    }
}

@Composable
private fun VerticalCategoryItem(
    label: String,
    icon: ImageVector,
    count: Int,
    selected: Boolean,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(),
        label = "railContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(),
        label = "railContent"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) Color.White else accentColor,
        animationSpec = spring(),
        label = "railIcon"
    )

    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(13.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 5.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) Color.White.copy(alpha = 0.18f)
                        else accentColor.copy(alpha = 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) Color.White else accentColor)
                        .padding(horizontal = 3.dp, vertical = 0.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
