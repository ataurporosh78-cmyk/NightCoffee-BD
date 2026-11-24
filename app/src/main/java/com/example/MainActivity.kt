package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.FirebaseSafeAccess
import com.example.notifications.NightCoffeeMessagingService
import com.example.ui.NightCoffeeNavigation
import com.example.ui.theme.NightCoffeeTheme

data class NotificationDeepLink(
    val route: String,
    val targetId: String
)

class MainActivity : ComponentActivity() {
    private var pendingDeepLink by mutableStateOf<NotificationDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
            )
        }
        FirebaseSafeAccess.ensureInitialized(applicationContext)
        NightCoffeeMessagingService.ensureNotificationChannel(applicationContext)
        extractDeepLink(intent)
        setContent {
            NightCoffeeTheme {
                NightCoffeeNavigation(
                    pendingDeepLink = pendingDeepLink,
                    onConsumeDeepLink = { pendingDeepLink = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDeepLink(intent)
    }

    private fun extractDeepLink(intent: Intent?) {
        val route = intent?.getStringExtra(NightCoffeeMessagingService.EXTRA_TARGET_ROUTE)
            ?.trim()
            .orEmpty()
        if (route.isNotEmpty()) {
            val targetId = intent?.getStringExtra(NightCoffeeMessagingService.EXTRA_TARGET_ID)
                ?.trim()
                .orEmpty()
            pendingDeepLink = NotificationDeepLink(route = route, targetId = targetId)
        }
    }
}
