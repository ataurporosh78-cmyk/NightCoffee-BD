@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.OpenGreenContainer
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.viewmodel.CoffeeHomeUiState

import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SupportAgent
import com.example.model.UserRoleState

@Composable
fun ProfileScreen(
    uiState: CoffeeHomeUiState,
    isSignedIn: Boolean,
    displayName: String?,
    onAddShopClick: () -> Unit,
    onEnableLocationClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
    userRole: UserRoleState = UserRoleState(),
    unreadNotificationsCount: Int = 0,
    compareCount: Int = 0,
    onOpenReservations: () -> Unit = {},
    onOpenOffers: () -> Unit = {},
    onOpenEvents: () -> Unit = {},
    onOpenCompare: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenAdminConsole: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EspressoRich.copy(alpha = 0.24f),
                        EspressoBlack,
                        EspressoBlack
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 116.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "profile_header") {
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = WarmCream
                )
            }

            item(key = "account_card") {
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    accentGlow = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = CaramelGold.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.5f)),
                                modifier = Modifier.size(58.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = CaramelGold,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isSignedIn) {
                                        displayName?.takeIf { it.isNotBlank() } ?: "Dhaka Coffee Lover"
                                    } else {
                                        stringResource(R.string.profile_guest_name)
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = WarmCream
                                )
                                if (isSignedIn) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.VerifiedUser,
                                            contentDescription = null,
                                            tint = OpenGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.profile_signed_in_badge),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = OpenGreen
                                        )
                                    }
                                } else {
                                    Text(
                                        text = stringResource(R.string.profile_guest_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ToastedLatte
                                    )
                                }
                            }
                        }

                        if (isSignedIn) {
                            OutlinedButton(
                                onClick = onSignOutClick,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, CocoaBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ToastedLatte),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.auth_sign_out_desc),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Button(
                                onClick = onSignInClick,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CaramelGold,
                                    contentColor = DarkEspressoInk
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("profile_sign_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.auth_sign_in_google),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item(key = "stats_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileStatCard(
                        icon = Icons.Rounded.Coffee,
                        value = uiState.totalCatalogSize.toString(),
                        label = "Listed Cafes",
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        icon = Icons.Rounded.Favorite,
                        value = uiState.favoritesCount.toString(),
                        label = "Saved",
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        icon = Icons.Rounded.LocationOn,
                        value = uiState.selectedArea.displayName,
                        label = "Focus Area",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "add_shop_action") {
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onAddShopClick)
                        .testTag("add_shop_fab"),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CaramelGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.5f)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AddBusiness,
                                    contentDescription = null,
                                    tint = CaramelGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.profile_add_shop_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmCream
                            )
                            Text(
                                text = stringResource(R.string.profile_add_shop_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = ToastedLatte
                            )
                        }
                    }
                }
            }

            item(key = "location_card") {
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (uiState.hasLocationPermission) {
                                OpenGreenContainer.copy(alpha = 0.85f)
                            } else {
                                MochaCardSurface
                            },
                            border = BorderStroke(
                                1.dp,
                                if (uiState.hasLocationPermission) OpenGreen.copy(alpha = 0.45f) else CocoaBorder
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.NearMe,
                                    contentDescription = null,
                                    tint = if (uiState.hasLocationPermission) OpenGreen else CaramelGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.profile_location_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmCream
                            )
                            Text(
                                text = stringResource(
                                    if (uiState.hasLocationPermission) {
                                        R.string.profile_location_active
                                    } else {
                                        R.string.profile_location_inactive
                                    }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = ToastedLatte
                            )
                        }

                        if (!uiState.hasLocationPermission) {
                            OutlinedButton(
                                onClick = onEnableLocationClick,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CaramelGold),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("profile_enable_location_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.profile_location_enable),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            if (userRole.hasAnyManagementAccess) {
                item(key = "admin_console_action") {
                    ProfileActionRowCard(
                        icon = Icons.Rounded.AdminPanelSettings,
                        title = stringResource(R.string.admin_title),
                        subtitle = if (userRole.isAdmin) "Platform Admin Console" else "Manage assigned coffee shop(s)",
                        onClick = onOpenAdminConsole,
                        testTag = "profile_admin_console_button"
                    )
                }
            }

            item(key = "platform_services_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Platform Services",
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmCream
                    )
                    ProfileActionRowCard(
                        icon = Icons.Rounded.EventSeat,
                        title = stringResource(R.string.reservations_title),
                        subtitle = "Book a table in Dhaka (UTC+6) & track bookings",
                        onClick = onOpenReservations,
                        testTag = "profile_reservations_button"
                    )
                    ProfileActionRowCard(
                        icon = Icons.Rounded.LocalOffer,
                        title = stringResource(R.string.offers_title),
                        subtitle = "Browse promo codes & cafe discounts",
                        onClick = onOpenOffers,
                        testTag = "profile_offers_button"
                    )
                    ProfileActionRowCard(
                        icon = Icons.Rounded.Event,
                        title = stringResource(R.string.events_title),
                        subtitle = "Workshops, tastings, live acoustic nights",
                        onClick = onOpenEvents,
                        testTag = "profile_events_button"
                    )
                    ProfileActionRowCard(
                        icon = Icons.Rounded.CompareArrows,
                        title = stringResource(R.string.compare_title),
                        subtitle = if (compareCount > 0) "$compareCount/3 cafes selected" else "Compare up to 3 Dhaka cafes side-by-side",
                        onClick = onOpenCompare,
                        testTag = "profile_compare_button"
                    )
                    ProfileActionRowCard(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.notifications_title),
                        subtitle = if (unreadNotificationsCount > 0) "$unreadNotificationsCount unread alert(s)" else "Manage topic preferences & notification inbox",
                        onClick = onOpenNotifications,
                        testTag = "profile_notifications_button"
                    )
                    ProfileActionRowCard(
                        icon = Icons.Rounded.SupportAgent,
                        title = stringResource(R.string.support_title),
                        subtitle = "FAQs, data error reports & support tickets",
                        onClick = onOpenSupport,
                        testTag = "profile_support_button"
                    )
                }
            }

            item(key = "profile_bottom_spacer") {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun ProfileActionRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CaramelGold.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.45f)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = CaramelGold, modifier = Modifier.size(21.dp))
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = WarmCream, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = ToastedLatte)
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CaramelGold,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = WarmCream,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ToastedLatte
            )
        }
    }
}
