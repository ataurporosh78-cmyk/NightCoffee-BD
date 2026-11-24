@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.screens

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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.NewCoffeeShopInput
import com.example.model.DhakaArea
import com.example.ui.components.CompactCoffeeShopCard
import com.example.ui.theme.CaramelContainer
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.FamilyBadgeContainer
import com.example.ui.theme.FamilyHoney
import com.example.ui.theme.FavoriteHeart
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.OpenGreenContainer
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.viewmodel.CoffeeHomeUiState
import com.example.viewmodel.HomeTab
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    uiState: CoffeeHomeUiState,
    searchInputText: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectTab: (HomeTab) -> Unit,
    onToggleOpenNow: (Boolean) -> Unit,
    onToggleFamilyFriendly: (Boolean) -> Unit,
    onSelectArea: (DhakaArea) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onResetFilters: () -> Unit,
    onShopClick: (String) -> Unit,
    onAddShop: suspend (NewCoffeeShopInput) -> Result<Unit>,
    onClearError: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSignedIn: Boolean = true,
    onRequireSignIn: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    var showAddShopSheet by rememberSaveable { mutableStateOf(false) }
    var areaDropdownExpanded by remember { mutableStateOf(false) }

    val shopPublishedMessage = stringResource(R.string.add_shop_success)

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onClearError()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EspressoBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = RoastedBeanElevated,
                    contentColor = WarmCream,
                    actionColor = CaramelGold
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (isSignedIn) {
                        showAddShopSheet = true
                    } else {
                        onRequireSignIn()
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.fab_add_shop),
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = CaramelGold,
                contentColor = DarkEspressoInk,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_shop_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EspressoBlack)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                HomeHeaderSection(
                    localTimeDisplay = uiState.localTimeDisplay,
                    isSignedIn = isSignedIn,
                    onSignOutClick = onSignOutClick
                )

                HomeControlsSection(
                    uiState = uiState,
                    searchInputText = searchInputText,
                    areaDropdownExpanded = areaDropdownExpanded,
                    onAreaDropdownExpandedChange = { areaDropdownExpanded = it },
                    onSearchQueryChange = onSearchQueryChange,
                    onClearSearch = onClearSearch,
                    onSelectTab = onSelectTab,
                    onToggleOpenNow = onToggleOpenNow,
                    onToggleFamilyFriendly = onToggleFamilyFriendly,
                    onSelectArea = onSelectArea
                )

                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = CaramelGold)
                                Text(
                                    text = stringResource(R.string.loading_catalog),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ToastedLatte
                                )
                            }
                        }
                    }

                    uiState.shops.isEmpty() -> {
                        EmptyCatalogState(
                            selectedTab = uiState.selectedTab,
                            hasActiveFilters = uiState.searchQuery.isNotBlank() ||
                                uiState.openNowOnly ||
                                uiState.familyFriendlyOnly ||
                                uiState.selectedArea != DhakaArea.ALL,
                            onResetFilters = onResetFilters,
                            onSwitchToAllCafes = { onSelectTab(HomeTab.ALL_CAFES) }
                        )
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
                                top = 8.dp,
                                bottom = 104.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.shops,
                                key = { it.shop.id },
                                contentType = { "coffee_shop" }
                            ) { item ->
                                CompactCoffeeShopCard(
                                    item = item,
                                    onToggleFavorite = { onToggleFavorite(item.shop.id) },
                                    onClick = { onShopClick(item.shop.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddShopSheet) {
        AddShopSheet(
            isSubmitting = uiState.isSubmittingAction,
            onDismiss = { showAddShopSheet = false },
            onSubmit = { input ->
                coroutineScope.launch {
                    val result = onAddShop(input)
                    if (result.isSuccess) {
                        showAddShopSheet = false
                        snackbarHostState.showSnackbar(shopPublishedMessage)
                    }
                }
            }
        )
    }
}

@Composable
private fun HomeHeaderSection(
    localTimeDisplay: String,
    isSignedIn: Boolean,
    onSignOutClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
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
                border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.45f)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.NightsStay,
                        contentDescription = null,
                        tint = CaramelGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = WarmCream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = ToastedLatte,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (localTimeDisplay.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MochaCardSurface,
                    border = BorderStroke(1.dp, CocoaBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = CaramelGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = localTimeDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCream,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            IconButton(
                onClick = onSignOutClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(if (isSignedIn) "sign_out_button" else "sign_in_button")
            ) {
                Icon(
                    imageVector = if (isSignedIn) {
                        Icons.AutoMirrored.Rounded.Logout
                    } else {
                        Icons.AutoMirrored.Rounded.Login
                    },
                    contentDescription = stringResource(
                        if (isSignedIn) R.string.auth_sign_out_desc else R.string.auth_sign_in_desc
                    ),
                    tint = if (isSignedIn) ToastedLatte else CaramelGold
                )
            }
        }
    }
}

@Composable
private fun HomeControlsSection(
    uiState: CoffeeHomeUiState,
    searchInputText: String,
    areaDropdownExpanded: Boolean,
    onAreaDropdownExpandedChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectTab: (HomeTab) -> Unit,
    onToggleOpenNow: (Boolean) -> Unit,
    onToggleFamilyFriendly: (Boolean) -> Unit,
    onSelectArea: (DhakaArea) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MochaCardSurface,
                unfocusedContainerColor = MochaCardSurface,
                focusedBorderColor = CaramelGold,
                unfocusedBorderColor = CocoaBorder,
                focusedTextColor = WarmCream,
                unfocusedTextColor = WarmCream,
                cursorColor = CaramelGold
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeTabButton(
                label = stringResource(R.string.tab_all_cafes),
                selected = uiState.selectedTab == HomeTab.ALL_CAFES,
                icon = Icons.Rounded.Coffee,
                onClick = { onSelectTab(HomeTab.ALL_CAFES) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_all_cafes")
            )

            val favoritesLabel = if (uiState.favoritesCount > 0) {
                stringResource(R.string.tab_favorites_count, uiState.favoritesCount)
            } else {
                stringResource(R.string.tab_favorites)
            }
            HomeTabButton(
                label = favoritesLabel,
                selected = uiState.selectedTab == HomeTab.FAVORITES,
                icon = Icons.Rounded.Favorite,
                onClick = { onSelectTab(HomeTab.FAVORITES) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_favorites")
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = uiState.openNowOnly,
                onClick = { onToggleOpenNow(!uiState.openNowOnly) },
                label = {
                    Text(
                        text = stringResource(R.string.filter_open_now),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (uiState.openNowOnly) OpenGreen else MutedCinnamon)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MochaCardSurface,
                    labelColor = ToastedLatte,
                    selectedContainerColor = OpenGreenContainer,
                    selectedLabelColor = OpenGreen
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = uiState.openNowOnly,
                    borderColor = CocoaBorder,
                    selectedBorderColor = OpenGreen.copy(alpha = 0.6f)
                ),
                modifier = Modifier.testTag("filter_open_now")
            )

            FilterChip(
                selected = uiState.familyFriendlyOnly,
                onClick = { onToggleFamilyFriendly(!uiState.familyFriendlyOnly) },
                label = {
                    Text(
                        text = stringResource(R.string.filter_family_friendly),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.FamilyRestroom,
                        contentDescription = null,
                        tint = if (uiState.familyFriendlyOnly) FamilyHoney else MutedCinnamon,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MochaCardSurface,
                    labelColor = ToastedLatte,
                    selectedContainerColor = FamilyBadgeContainer,
                    selectedLabelColor = FamilyHoney
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = uiState.familyFriendlyOnly,
                    borderColor = CocoaBorder,
                    selectedBorderColor = FamilyHoney.copy(alpha = 0.6f)
                ),
                modifier = Modifier.testTag("filter_family_friendly")
            )

            Box {
                val areaSelected = uiState.selectedArea != DhakaArea.ALL
                FilterChip(
                    selected = areaSelected,
                    onClick = { onAreaDropdownExpandedChange(true) },
                    label = {
                        Text(
                            text = if (areaSelected) {
                                uiState.selectedArea.displayName
                            } else {
                                "${stringResource(R.string.filter_area_label)}: ${uiState.selectedArea.displayName}"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = if (areaSelected) CaramelGold else MutedCinnamon,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (areaSelected) CaramelGold else ToastedLatte,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MochaCardSurface,
                        labelColor = ToastedLatte,
                        selectedContainerColor = CaramelContainer,
                        selectedLabelColor = CaramelGold
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = areaSelected,
                        borderColor = CocoaBorder,
                        selectedBorderColor = CaramelGold.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.testTag("area_dropdown_button")
                )

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
                            },
                            modifier = Modifier.testTag("area_option_${area.name}")
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val countText = if (uiState.shops.size == 1) {
                stringResource(R.string.shop_found_single)
            } else {
                stringResource(R.string.shops_found_count, uiState.shops.size)
            }
            Text(
                text = countText,
                style = MaterialTheme.typography.labelMedium,
                color = MutedCinnamon
            )
            if (uiState.totalCatalogSize > 0) {
                Text(
                    text = "${uiState.totalCatalogSize} total listed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedCinnamon
                )
            }
        }
    }
}

@Composable
private fun HomeTabButton(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) CaramelContainer else DeepRoastSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) CaramelGold.copy(alpha = 0.7f) else CocoaBorder
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) CaramelGold else MutedCinnamon,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) WarmCream else ToastedLatte,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyCatalogState(
    selectedTab: HomeTab,
    hasActiveFilters: Boolean,
    onResetFilters: () -> Unit,
    onSwitchToAllCafes: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MochaCardSurface,
            border = BorderStroke(1.dp, CocoaBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (selectedTab == HomeTab.FAVORITES && !hasActiveFilters) {
                        Icons.Rounded.Favorite
                    } else {
                        Icons.Rounded.SearchOff
                    },
                    contentDescription = null,
                    tint = if (selectedTab == HomeTab.FAVORITES && !hasActiveFilters) {
                        FavoriteHeart
                    } else {
                        CaramelGold
                    },
                    modifier = Modifier.size(40.dp)
                )

                Text(
                    text = stringResource(
                        if (selectedTab == HomeTab.FAVORITES && !hasActiveFilters) {
                            R.string.empty_favorites_title
                        } else {
                            R.string.empty_state_title
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmCream,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(
                        if (selectedTab == HomeTab.FAVORITES && !hasActiveFilters) {
                            R.string.empty_favorites_subtitle
                        } else {
                            R.string.empty_state_subtitle
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ToastedLatte,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = {
                        if (selectedTab == HomeTab.FAVORITES && !hasActiveFilters) {
                            onSwitchToAllCafes()
                        } else {
                            onResetFilters()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
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
                        text = stringResource(
                            if (selectedTab == HomeTab.FAVORITES && !hasActiveFilters) {
                                R.string.tab_all_cafes
                            } else {
                                R.string.reset_filters
                            }
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
