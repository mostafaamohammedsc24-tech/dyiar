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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingPropertyId = intent?.getStringExtra(DiyarNotificationHelper.EXTRA_PROPERTY_ID)
        setContent {
            DiyarApp(
                deepLinkPropertyId = pendingPropertyId,
                onDeepLinkConsumed = { pendingPropertyId = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val propId = intent.getStringExtra(DiyarNotificationHelper.EXTRA_PROPERTY_ID)
        if (!propId.isNullOrBlank()) {
            pendingPropertyId = propId
        }
    }
}
