@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DhakaArea
import com.example.model.ExploreCategory
import com.example.ui.components.FeaturedSpotlightCard
import com.example.ui.components.LiquidGlassShopCard
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.OpenNowCompactCard
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.viewmodel.CoffeeHomeUiState

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.SupportAgent
import com.example.model.AggregatedCrowdStatus
import com.example.model.ResolvedShopStatus
import com.example.model.ShopOverlayDoc
import com.example.ui.components.OfflineSyncBanner
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.QuickPlatformTile
import com.example.ui.components.ShimmerShopSkeletonList
import com.example.ui.components.StaggeredEntranceItem
import com.example.ui.components.isPositiveOpen

@Composable
fun HomeScreen(
    uiState: CoffeeHomeUiState,
    onSelectArea: (DhakaArea) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShopClick: (String) -> Unit,
    onNavigateToExplore: (ExploreCategory) -> Unit,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    pendingSyncCount: Int = 0,
    onSyncNow: () -> Unit = {},
    compareCount: Int = 0,
    comparedIds: Set<String> = emptySet(),
    overlays: Map<String, ShopOverlayDoc> = emptyMap(),
    resolveStatus: (com.example.viewmodel.CoffeeShopListItem) -> ResolvedShopStatus? = { null },
    resolveCrowd: (String) -> AggregatedCrowdStatus? = { null },
    onToggleCompare: (String) -> Unit = {},
    onOpenMap: () -> Unit = {},
    onOpenOffers: () -> Unit = {},
    onOpenEvents: () -> Unit = {},
    onOpenCompare: () -> Unit = {},
    onOpenReservations: () -> Unit = {},
    onOpenSupport: () -> Unit = {}
) {
    var areaDropdownExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EspressoRich.copy(alpha = 0.28f),
                        EspressoBlack,
                        EspressoBlack
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        if (uiState.isLoading) {
            ShimmerShopSkeletonList(
                modifier = Modifier.statusBarsPadding(),
                itemCount = 5,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 116.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .testTag("home_feed_list"),
                contentPadding = PaddingValues(bottom = 116.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "home_header") {
                    HomeHeroHeader(
                        uiState = uiState,
                        areaDropdownExpanded = areaDropdownExpanded,
                        onAreaDropdownExpandedChange = { areaDropdownExpanded = it },
                        onSelectArea = onSelectArea,
                        onSearchBarClick = { onNavigateToExplore(ExploreCategory.ALL) },
                        onRequestLocation = onRequestLocation
                    )
                }

                if (!isOnline || pendingSyncCount > 0) {
                    item(key = "home_offline_banner") {
                        OfflineSyncBanner(
                            isOnline = isOnline,
                            pendingCount = pendingSyncCount,
                            onSyncNow = onSyncNow,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item(key = "home_quick_hub") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickPlatformTile(
                            icon = Icons.Rounded.Map,
                            label = stringResource(R.string.map_title),
                            onClick = onOpenMap,
                            modifier = Modifier.testTag("home_tile_map")
                        )
                        QuickPlatformTile(
                            icon = Icons.Rounded.LocalOffer,
                            label = "Offers",
                            onClick = onOpenOffers,
                            modifier = Modifier.testTag("home_tile_offers")
                        )
                        QuickPlatformTile(
                            icon = Icons.Rounded.Event,
                            label = "Events",
                            onClick = onOpenEvents,
                            modifier = Modifier.testTag("home_tile_events")
                        )
                        QuickPlatformTile(
                            icon = Icons.Rounded.EventSeat,
                            label = "Reserve",
                            onClick = onOpenReservations,
                            modifier = Modifier.testTag("home_tile_reserve")
                        )
                        QuickPlatformTile(
                            icon = Icons.Rounded.CompareArrows,
                            label = "Compare",
                            badge = if (compareCount > 0) "$compareCount/3" else null,
                            onClick = onOpenCompare,
                            modifier = Modifier.testTag("home_tile_compare")
                        )
                        QuickPlatformTile(
                            icon = Icons.Rounded.SupportAgent,
                            label = "Support",
                            onClick = onOpenSupport,
                            modifier = Modifier.testTag("home_tile_support")
                        )
                    }
                }

                if (uiState.featuredShops.isNotEmpty()) {
                    item(key = "featured_section") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HomeSectionHeader(
                                title = stringResource(R.string.home_section_featured),
                                actionLabel = stringResource(R.string.home_see_all),
                                onActionClick = { onNavigateToExplore(ExploreCategory.ALL) }
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(
                                    items = uiState.featuredShops,
                                    key = { "feat_${it.shop.id}" }
                                ) { item ->
                                    FeaturedSpotlightCard(
                                        item = item,
                                        onToggleFavorite = { onToggleFavorite(item.shop.id) },
                                        onClick = { onShopClick(item.shop.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.openNowShops.isNotEmpty()) {
                    item(key = "open_now_section") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HomeSectionHeader(
                                title = stringResource(R.string.home_section_open_now),
                                actionLabel = stringResource(R.string.home_see_all),
                                accentDot = OpenGreen,
                                onActionClick = { onNavigateToExplore(ExploreCategory.OPEN_NOW) }
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.openNowShops,
                                    key = { "open_${it.shop.id}" }
                                ) { item ->
                                    OpenNowCompactCard(
                                        item = item,
                                        onClick = { onShopClick(item.shop.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "popular_header") {
                    HomeSectionHeader(
                        title = stringResource(R.string.home_section_popular),
                        actionLabel = stringResource(R.string.home_see_all),
                        onActionClick = { onNavigateToExplore(ExploreCategory.ALL) }
                    )
                }

                itemsIndexed(
                    items = uiState.popularShops,
                    key = { _, item -> "pop_${item.shop.id}" },
                    contentType = { _, _ -> "liquid_shop_card" }
                ) { index, item ->
                    val status = resolveStatus(item)
                    val crowd = resolveCrowd(item.shop.id)
                    StaggeredEntranceItem(index = index) {
                        LiquidGlassShopCard(
                            item = item,
                            onToggleFavorite = { onToggleFavorite(item.shop.id) },
                            onClick = { onShopClick(item.shop.id) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            isVerified = overlays[item.shop.id]?.verified == true,
                            liveStatusLabel = status?.verifiedStatus?.label ?: status?.scheduledLabel,
                            isLiveStatusPositive = status?.effectiveOpenNow ?: item.isOpenNow,
                            isLiveStatusWarning = status?.verifiedStatus == com.example.model.LiveShopStatus.CLOSING_SOON ||
                                status?.scheduledLabel == "Closing Soon",
                            crowdLevel = crowd?.takeIf { it.isSufficientlyRecent }?.level,
                            isCompared = item.shop.id in comparedIds,
                            onToggleCompare = { onToggleCompare(item.shop.id) }
                        )
                    }
                }

                item(key = "home_bottom_spacer") {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun HomeHeroHeader(
    uiState: CoffeeHomeUiState,
    areaDropdownExpanded: Boolean,
    onAreaDropdownExpandedChange: (Boolean) -> Unit,
    onSelectArea: (DhakaArea) -> Unit,
    onSearchBarClick: () -> Unit,
    onRequestLocation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Brand emblem + Time pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MochaCardSurface,
                    border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.45f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nightcoffee_liquid_fg),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = CaramelGold,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.localTimeDisplay.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MochaCardSurface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, CocoaBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = CaramelGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = uiState.localTimeDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCream,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Greeting + Subtitle
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.home_greeting_evening),
                style = MaterialTheme.typography.displaySmall,
                color = WarmCream
            )
            Text(
                text = stringResource(R.string.home_greeting_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = ToastedLatte
            )
        }

        // Translucent Search Bar Launcher
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onSearchBarClick)
                .testTag("home_search_trigger"),
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = CaramelGold,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.home_search_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedCinnamon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Dhaka Location Picker & Distance Trigger Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                val areaSelected = uiState.selectedArea != DhakaArea.ALL
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (areaSelected) CaramelGold.copy(alpha = 0.18f) else MochaCardSurface.copy(alpha = 0.85f),
                    border = BorderStroke(
                        1.dp,
                        if (areaSelected) CaramelGold.copy(alpha = 0.6f) else CocoaBorder
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onAreaDropdownExpandedChange(true) }
                        .testTag("home_area_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = CaramelGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (areaSelected) {
                                "${uiState.selectedArea.displayName}, Dhaka"
                            } else {
                                "Dhaka · All Areas"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = WarmCream
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = ToastedLatte,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = areaDropdownExpanded,
                    onDismissRequest = { onAreaDropdownExpandedChange(false) },
                    containerColor = RoastedBeanElevated
                ) {
                    DhakaArea.entries.forEach { area ->
                        val isSelected = area == uiState.selectedArea
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = area.displayName,
                                    color = if (isSelected) CaramelGold else WarmCream,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = CaramelGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                onSelectArea(area)
                                onAreaDropdownExpandedChange(false)
                            }
                        )
                    }
                }
            }

            if (!uiState.hasLocationPermission) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MochaCardSurface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, CocoaBorder),
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onRequestLocation)
                        .testTag("home_enable_distance_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NearMe,
                            contentDescription = null,
                            tint = CaramelGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.filter_near_me),
                            style = MaterialTheme.typography.labelLarge,
                            color = ToastedLatte
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    accentDot: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (accentDot != null) {
                PulsingStatusDot(
                    color = accentDot,
                    isPulsing = true,
                    dotSize = 9.dp
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = WarmCream
            )
        }

        TextButton(
            onClick = onActionClick,
            modifier = Modifier.height(48.dp)
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = CaramelGold
            )
        }
    }
}
