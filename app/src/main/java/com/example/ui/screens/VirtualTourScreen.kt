package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UiState
import com.example.i18n.LocalStrings
import com.example.ui.components.Panorama360Viewer
import com.example.ui.viewmodel.DiyarViewModel

@Composable
fun VirtualTourScreen(
    propertyId: String,
    viewModel: DiyarViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val strings = LocalStrings.current
    val activeState by viewModel.activePropertiesState.collectAsStateWithLifecycle()
    val myState by viewModel.myPropertiesState.collectAsStateWithLifecycle()
    val property = remember(activeState, myState, propertyId) {
        (activeState as? UiState.Success)?.data?.firstOrNull { it.id == propertyId }
            ?: (myState as? UiState.Success)?.data?.firstOrNull { it.id == propertyId }
    }
    var activeSceneIdx by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060E1A))
            .safeDrawingPadding()
    ) {
        if (property != null && property.tourScenes.isNotEmpty()) {
            val safeIdx = activeSceneIdx.coerceIn(0, property.tourScenes.lastIndex)

            // Top Navigation & Room Spec Context
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xCC0D1B2A),
                    shape = CircleShape
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("tour_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.prevStep,
                            tint = Color.White
                        )
                    }
                }

                Surface(
                    color = Color(0xCC0D1B2A),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).padding(start = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            text = if (strings.isArabic) property.titleAr else property.titleEn,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        val roomHint = property.rooms.getOrNull(safeIdx)
                        if (roomHint != null) {
                            Text(
                                text = "${roomHint.widthM}×${roomHint.lengthM}${strings.meterUnit} • ${if (strings.isArabic) roomHint.paintNameAr else roomHint.paintNameEn}",
                                color = Color(0xFF93C5FD),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Panorama360Viewer(
                scenes = property.tourScenes,
                activeSceneIndex = safeIdx,
                onSelectScene = { activeSceneIdx = it },
                onExpandFullscreen = null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(strings.loadingText, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}
