package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.notifications.DiyarNotificationHelper
import com.example.ui.DiyarApp

class MainActivity : ComponentActivity() {
    private var pendingPropertyId by mutableStateOf<String?>(null)

    private fun extractPropertyId(intent: Intent?): String? {
        val fromExtra = intent?.getStringExtra(DiyarNotificationHelper.EXTRA_PROPERTY_ID)
        if (!fromExtra.isNullOrBlank()) return fromExtra
        val uri = intent?.data ?: return null
        return when {
            uri.scheme == "diyar" && uri.host == "property" ->
                uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
            uri.host == "iraqdallal.iq" && uri.pathSegments.firstOrNull() == "property" ->
                uri.pathSegments.getOrNull(1)?.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingPropertyId = extractPropertyId(intent)
        setContent {
            DiyarApp(
                deepLinkPropertyId = pendingPropertyId,
                onDeepLinkConsumed = { pendingPropertyId = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val propId = extractPropertyId(intent)
        if (!propId.isNullOrBlank()) {
            pendingPropertyId = propId
        }
    }
}
