@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.NotificationItemDoc
import com.example.model.UserPreferenceDoc
import com.example.notifications.NightCoffeeMessagingService
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PlatformTopBar
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.util.BangladeshTimeUtil
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItemDoc>,
    preferences: UserPreferenceDoc,
    isSignedIn: Boolean,
    onBack: () -> Unit,
    onSavePreferences: suspend (UserPreferenceDoc) -> Result<Unit>,
    onNotificationClick: (NotificationItemDoc) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(NightCoffeeMessagingService.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Push notifications enabled" else "Permission denied — alerts stay in your inbox"
            )
        }
    }

    Scaffold(
        containerColor = EspressoBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(EspressoRich.copy(alpha = 0.25f), EspressoBlack, EspressoBlack)
                    )
                )
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                PlatformTopBar(
                    title = stringResource(R.string.notifications_title),
                    subtitle = stringResource(R.string.notifications_subtitle),
                    onBack = onBack
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notifications_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        item(key = "notif_permission_card") {
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                accentGlow = true
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = CaramelGold)
                                        Text(
                                            text = stringResource(R.string.notifications_permission_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = WarmCream
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.notifications_permission_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ToastedLatte
                                    )
                                    Button(
                                        onClick = {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CaramelGold,
                                            contentColor = DarkEspressoInk
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("enable_notifications_button")
                                    ) {
                                        Text("Allow Push Notifications", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item(key = "notif_preferences_card") {
                        LiquidGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Alert Topics (Opt-In)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WarmCream
                                )
                                Text(
                                    text = "Only categories you switch on will trigger topic alerts.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedCinnamon
                                )
                                PrefToggleRow("New Coffee Shop Openings", preferences.optInNewOpenings) { checked ->
                                    scope.launch { onSavePreferences(preferences.copy(optInNewOpenings = checked)) }
                                }
                                HorizontalDivider(color = CocoaBorder)
                                PrefToggleRow("Exclusive Offers & Coupons", preferences.optInOffers) { checked ->
                                    scope.launch { onSavePreferences(preferences.copy(optInOffers = checked)) }
                                }
                                HorizontalDivider(color = CocoaBorder)
                                PrefToggleRow("Saved Coffee Shop Updates", preferences.optInSavedShops) { checked ->
                                    scope.launch { onSavePreferences(preferences.copy(optInSavedShops = checked)) }
                                }
                                HorizontalDivider(color = CocoaBorder)
                                PrefToggleRow("Reservation Updates", preferences.optInReservations) { checked ->
                                    scope.launch { onSavePreferences(preferences.copy(optInReservations = checked)) }
                                }
                                HorizontalDivider(color = CocoaBorder)
                                PrefToggleRow("Coffee Events", preferences.optInEvents) { checked ->
                                    scope.launch { onSavePreferences(preferences.copy(optInEvents = checked)) }
                                }
                                HorizontalDivider(color = CocoaBorder)
                                PrefToggleRow("Platform Announcements", preferences.optInAnnouncements) { checked ->
                                    scope.launch { onSavePreferences(preferences.copy(optInAnnouncements = checked)) }
                                }
                            }
                        }
                    }

                    item(key = "notif_inbox_header") {
                        Text(
                            text = "Notification History",
                            style = MaterialTheme.typography.titleMedium,
                            color = WarmCream
                        )
                    }

                    if (!isSignedIn || notifications.isEmpty()) {
                        item(key = "notif_empty") {
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Rounded.NotificationsNone, contentDescription = null, tint = CaramelGold)
                                    Text(
                                        text = if (!isSignedIn) {
                                            "Sign in with Google to receive personalized reservation & saved cafe alerts."
                                        } else {
                                            stringResource(R.string.notifications_empty)
                                        },
                                        color = ToastedLatte
                                    )
                                }
                            }
                        }
                    } else {
                        items(notifications, key = { it.id }) { item ->
                            LiquidGlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable { onNotificationClick(item) },
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (item.read) MutedCinnamon else OpenGreen)
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = item.categoryEnum().label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CaramelGold
                                        )
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = WarmCream,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = item.body,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ToastedLatte
                                        )
                                        Text(
                                            text = BangladeshTimeUtil.formatRelativeTime(item.createdAt?.toDate()?.time),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedCinnamon
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                }
            }
        }
    }
}

@Composable
private fun PrefToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = WarmCream, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
