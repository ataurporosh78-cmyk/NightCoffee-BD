package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DhakaArea
import com.example.ui.theme.CaramelContainer
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.CremaGold
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenMint
import com.example.ui.theme.OpenMintContainer
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream

@Composable
fun HomeScreen(
    uiState: NightCoffeeUiState,
    onToggleOpenNow: (Boolean) -> Unit,
    onSelectArea: (DhakaArea) -> Unit,
    onResetFilters: () -> Unit,
    onShopClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EspressoBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(EspressoBlack)
        ) {
            NightCoffeeHeader(
                dhakaTimeDisplay = uiState.dhakaTimeDisplay
            )

            FilterControlsBar(
                openNowOnly = uiState.openNowOnly,
                selectedArea = uiState.selectedArea,
                visibleCount = uiState.filteredShops.size,
                onToggleOpenNow = onToggleOpenNow,
                onSelectArea = onSelectArea,
                onResetFilters = onResetFilters
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                val isWideLayout = maxWidth >= 600.dp

                if (uiState.filteredShops.isEmpty()) {
                    EmptyFilterState(
                        onResetFilters = onResetFilters,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    )
                } else if (isWideLayout) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = 28.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.filteredShops,
                            key = { it.shop.id }
                        ) { item ->
                            CoffeeShopCard(
                                item = item,
                                onClick = { onShopClick(item.shop.id) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 28.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.filteredShops,
                            key = { it.shop.id }
                        ) { item ->
                            CoffeeShopCard(
                                item = item,
                                onClick = { onShopClick(item.shop.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 600.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NightCoffeeHeader(
    dhakaTimeDisplay: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF23150E),
                            EspressoBlack
                        )
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CremaGold.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.25f),
                        radius = size.width * 0.55f
                    )
                )
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CaramelContainer,
                    border = BorderStroke(1.dp, CremaGold.copy(alpha = 0.4f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.NightsStay,
                            contentDescription = null,
                            tint = CremaGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = WarmCream
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = ToastedLatte
                    )
                }
            }

            if (dhakaTimeDisplay.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MochaCardSurface,
                    border = BorderStroke(1.dp, CocoaBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = CremaGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = dhakaTimeDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCream
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterControlsBar(
    openNowOnly: Boolean,
    selectedArea: DhakaArea,
    visibleCount: Int,
    onToggleOpenNow: (Boolean) -> Unit,
    onSelectArea: (DhakaArea) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var areaMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Open Now" Toggle Filter
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (openNowOnly) OpenMintContainer else MochaCardSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (openNowOnly) OpenMint.copy(alpha = 0.55f) else CocoaBorder
                ),
                modifier = Modifier
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Switch) { onToggleOpenNow(!openNowOnly) }
                    .testTag("open_now_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.filter_open_now),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (openNowOnly) OpenMint else WarmCream
                    )
                    Switch(
                        checked = openNowOnly,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EspressoBlack,
                            checkedTrackColor = OpenMint,
                            uncheckedThumbColor = ToastedLatte,
                            uncheckedTrackColor = RoastedBeanElevated,
                            uncheckedBorderColor = CocoaBorder
                        )
                    )
                }
            }

            // Area Dropdown Filter
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selectedArea != DhakaArea.ALL) CaramelContainer else MochaCardSurface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selectedArea != DhakaArea.ALL) CremaGold.copy(alpha = 0.55f) else CocoaBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { areaMenuExpanded = true }
                        .testTag("area_filter_dropdown")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = CremaGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${stringResource(R.string.filter_area_label)}: ${selectedArea.displayName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = WarmCream,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = ToastedLatte,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = areaMenuExpanded,
                    onDismissRequest = { areaMenuExpanded = false },
                    containerColor = RoastedBeanElevated,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    DhakaArea.entries.forEach { area ->
                        val isSelected = area == selectedArea
                        DropdownMenuItem(
                            modifier = Modifier.testTag("area_option_${area.name.lowercase()}"),
                            text = {
                                Text(
                                    text = area.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) CremaGold else WarmCream
                                )
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = CremaGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                onSelectArea(area)
                                areaMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (visibleCount == 1) {
                    stringResource(R.string.shop_found_single)
                } else {
                    stringResource(R.string.shops_found_count, visibleCount)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MutedCinnamon
            )

            AnimatedVisibility(
                visible = openNowOnly || selectedArea != DhakaArea.ALL,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TextButton(
                    onClick = onResetFilters,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = stringResource(R.string.reset_filters),
                        style = MaterialTheme.typography.labelMedium,
                        color = CremaGold
                    )
                }
            }
        }
    }
}

@Composable
fun CoffeeShopCard(
    item: CoffeeShopUiItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shop = item.shop
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag("shop_card_${shop.id}"),
        shape = RoundedCornerShape(22.dp),
        color = MochaCardSurface,
        border = BorderStroke(1.dp, CocoaBorder),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
            ) {
                Image(
                    painter = painterResource(id = shop.imageRes),
                    contentDescription = shop.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    EspressoBlack.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    MochaCardSurface.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AreaBadge(areaName = shop.area.displayName)
                    OpenStatusBadge(isOpenNow = item.isOpenNow)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = WarmCream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = CremaGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = shop.hoursDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ToastedLatte,
                        fontWeight = FontWeight.Medium
                    )
                }

                HorizontalDivider(color = CocoaBorder.copy(alpha = 0.55f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StarRatingRow(rating = shop.rating)

                    if (shop.isFamilyFriendly) {
                        FamilyFriendlyBadge()
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFilterState(
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = DeepRoastSurface,
            border = BorderStroke(1.dp, CocoaBorder),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.LocalCafe,
                    contentDescription = null,
                    tint = CremaGold,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.titleLarge,
            color = WarmCream,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.empty_state_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = ToastedLatte,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onResetFilters,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CremaGold,
                contentColor = DarkEspressoInk
            ),
            modifier = Modifier
                .height(48.dp)
                .testTag("reset_filters_button")
        ) {
            Text(
                text = stringResource(R.string.reset_filters),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
