@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Roofing
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DhakaArea
import com.example.model.ExploreCategory
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.LiquidGlassShopCard
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.viewmodel.CoffeeHomeUiState

import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Map
import com.example.model.AggregatedCrowdStatus
import com.example.model.LiveShopStatus
import com.example.model.ResolvedShopStatus
import com.example.model.ShopOverlayDoc
import com.example.ui.components.ShimmerShopSkeletonList
import com.example.ui.components.StaggeredEntranceItem
import com.example.ui.components.isPositiveOpen

@Composable
fun ExploreScreen(
    uiState: CoffeeHomeUiState,
    searchInputText: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectCategory: (ExploreCategory) -> Unit,
    onToggleFamilyFriendly: (Boolean) -> Unit,
    onSelectArea: (DhakaArea) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onResetFilters: () -> Unit,
    onShopClick: (String) -> Unit,
    onNearMeClick: () -> Unit,
    modifier: Modifier = Modifier,
    comparedIds: Set<String> = emptySet(),
    overlays: Map<String, ShopOverlayDoc> = emptyMap(),
    resolveStatus: (com.example.viewmodel.CoffeeShopListItem) -> ResolvedShopStatus? = { null },
    resolveCrowd: (String) -> AggregatedCrowdStatus? = { null },
    onToggleCompare: (String) -> Unit = {},
    onOpenMap: () -> Unit = {},
    onOpenCompare: () -> Unit = {}
) {
    val lazyListState = rememberLazyListState()
    var areaDropdownExpanded by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.explore_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = WarmCream
                        )
                        Text(
                            text = stringResource(R.string.explore_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = ToastedLatte
                        )
                    }

                    Box {
                        val areaSelected = uiState.selectedArea != DhakaArea.ALL
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (areaSelected) CaramelGold.copy(alpha = 0.2f) else MochaCardSurface.copy(alpha = 0.9f),
                            border = BorderStroke(
                                1.dp,
                                if (areaSelected) CaramelGold.copy(alpha = 0.6f) else CocoaBorder
                            ),
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { areaDropdownExpanded = true }
                                .testTag("area_dropdown_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = CaramelGold,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = uiState.selectedArea.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WarmCream
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = ToastedLatte,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = areaDropdownExpanded,
                            onDismissRequest = { areaDropdownExpanded = false },
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
                                        areaDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("area_option_${area.name}")
                                )
                            }
                        }
                    }
                }

                // Translucent Search Field
                OutlinedTextField(
                    value = searchInputText,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedCinnamon,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = CaramelGold
                        )
                    },
                    trailingIcon = {
                        if (searchInputText.isNotEmpty()) {
                            IconButton(
                                onClick = onClearSearch,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("search_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.search_clear_desc),
                                    tint = ToastedLatte
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MochaCardSurface.copy(alpha = 0.88f),
                        unfocusedContainerColor = MochaCardSurface.copy(alpha = 0.82f),
                        focusedBorderColor = CaramelGold.copy(alpha = 0.7f),
                        unfocusedBorderColor = CocoaBorder,
                        focusedTextColor = WarmCream,
                        unfocusedTextColor = WarmCream,
                        cursorColor = CaramelGold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input")
                )

                // 7 Horizontal Glass Category Chips + Family Friendly option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassChip(
                        label = stringResource(R.string.filter_all_shops),
                        selected = uiState.selectedCategory == ExploreCategory.ALL,
                        leadingIcon = Icons.Rounded.Coffee,
                        onClick = { onSelectCategory(ExploreCategory.ALL) },
                        modifier = Modifier.testTag("filter_all_shops")
                    )
                    LiquidGlassChip(
                        label = stringResource(R.string.filter_open_now),
                        selected = uiState.selectedCategory == ExploreCategory.OPEN_NOW,
                        leadingIcon = Icons.Rounded.Schedule,
                        onClick = { onSelectCategory(ExploreCategory.OPEN_NOW) },
                        modifier = Modifier.testTag("filter_open_now")
                    )
                    LiquidGlassChip(
                        label = stringResource(R.string.filter_after_midnight),
                        selected = uiState.selectedCategory == ExploreCategory.AFTER_MIDNIGHT,
                        leadingIcon = Icons.Rounded.NightsStay,
                        onClick = { onSelectCategory(ExploreCategory.AFTER_MIDNIGHT) },
                        modifier = Modifier.testTag("filter_after_midnight")
                    )
                    LiquidGlassChip(
                        label = stringResource(R.string.filter_near_me),
                        selected = uiState.selectedCategory == ExploreCategory.NEAR_ME,
                        leadingIcon = Icons.Rounded.NearMe,
                        onClick = onNearMeClick,
                        modifier = Modifier.testTag("filter_near_me")
                    )
                    LiquidGlassChip(
                        label = stringResource(R.string.filter_rooftop),
                        selected = uiState.selectedCategory == ExploreCategory.ROOFTOP,
                        leadingIcon = Icons.Rounded.Roofing,
                        onClick = { onSelectCategory(ExploreCategory.ROOFTOP) },
                        modifier = Modifier.testTag("filter_rooftop")
                    )
                    LiquidGlassChip(
                        label = stringResource(R.string.filter_wifi),
                        selected = uiState.selectedCategory == ExploreCategory.WIFI,
                        leadingIcon = Icons.Rounded.Wifi,
                        onClick = { onSelectCategory(ExploreCategory.WIFI) },
                        modifier = Modifier.testTag("filter_wifi")
                    )
                    LiquidGlassChip(
                        label = stringResource(R.string.filter_quiet),
                        selected = uiState.selectedCategory == ExploreCategory.QUIET,
                        leadingIcon = Icons.Rounded.SelfImprovement,
                        onClick = { onSelectCategory(ExploreCategory.QUIET) },
                        modifier = Modifier.testTag("filter_quiet")
                    )
                    LiquidGlassChip(
                        label = stringResource(R.string.filter_family_friendly),
                        selected = uiState.familyFriendlyOnly,
                        leadingIcon = Icons.Rounded.FamilyRestroom,
                        onClick = { onToggleFamilyFriendly(!uiState.familyFriendlyOnly) },
                        modifier = Modifier.testTag("filter_family_friendly")
                    )
                }

                // Live animated result counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = uiState.shops.size,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "exploreCountAnim"
                    ) { count ->
                        val countText = if (count == 1) {
                            stringResource(R.string.shop_found_single)
                        } else {
                            stringResource(R.string.shops_found_count, count)
                        }
                        Text(
                            text = countText,
                            style = MaterialTheme.typography.labelLarge,
                            color = CaramelGold
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MochaCardSurface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, CocoaBorder),
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onOpenMap)
                                .testTag("explore_map_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Map, contentDescription = null, tint = CaramelGold, modifier = Modifier.size(15.dp))
                                Text("Map", style = MaterialTheme.typography.labelMedium, color = WarmCream)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (comparedIds.isNotEmpty()) CaramelGold.copy(alpha = 0.18f) else MochaCardSurface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, if (comparedIds.isNotEmpty()) CaramelGold.copy(alpha = 0.5f) else CocoaBorder),
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onOpenCompare)
                                .testTag("explore_compare_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.CompareArrows, contentDescription = null, tint = CaramelGold, modifier = Modifier.size(15.dp))
                                Text("Compare (${comparedIds.size}/3)", style = MaterialTheme.typography.labelMedium, color = WarmCream)
                            }
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    ShimmerShopSkeletonList(
                        itemCount = 4,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 116.dp)
                    )
                }

                uiState.shops.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LiquidGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SearchOff,
                                    contentDescription = null,
                                    tint = CaramelGold,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = stringResource(R.string.empty_state_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WarmCream,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.empty_state_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ToastedLatte,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onResetFilters,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CaramelGold,
                                        contentColor = DarkEspressoInk
                                    ),
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .height(48.dp)
                                        .testTag("reset_filters_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.reset_filters),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("coffee_shops_list"),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 116.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.shops,
                            key = { _, item -> item.shop.id },
                            contentType = { _, _ -> "liquid_shop_card" }
                        ) { index, item ->
                            val status = resolveStatus(item)
                            val crowd = resolveCrowd(item.shop.id)
                            StaggeredEntranceItem(index = index) {
                                LiquidGlassShopCard(
                                    item = item,
                                    onToggleFavorite = { onToggleFavorite(item.shop.id) },
                                    onClick = { onShopClick(item.shop.id) },
                                    isVerified = overlays[item.shop.id]?.verified == true,
                                    liveStatusLabel = status?.verifiedStatus?.label ?: status?.scheduledLabel,
                                    isLiveStatusPositive = status?.effectiveOpenNow ?: item.isOpenNow,
                                    isLiveStatusWarning = status?.verifiedStatus == LiveShopStatus.CLOSING_SOON ||
                                        status?.scheduledLabel == "Closing Soon",
                                    crowdLevel = crowd?.takeIf { it.isSufficientlyRecent }?.level,
                                    isCompared = item.shop.id in comparedIds,
                                    onToggleCompare = { onToggleCompare(item.shop.id) }
                                )
                            }
                        }
                        item(key = "explore_bottom_spacer") {
                            Spacer(modifier = Modifier.navigationBarsPadding())
                        }
                    }
                }
            }
        }
    }
}
