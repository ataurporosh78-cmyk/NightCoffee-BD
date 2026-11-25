@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Roofing
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ShopFacility
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.GlassBottomNavFill
import com.example.ui.theme.GlassHighlightBorder
import com.example.ui.theme.GlassSurfaceFill
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.viewmodel.CoffeeShopListItem
import java.util.Locale

enum class MainNavTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    EXPLORE(R.string.nav_explore, Icons.Rounded.Explore, Icons.Outlined.Explore),
    FAVORITES(R.string.nav_favorites, Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder),
    PROFILE(R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person)
}

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderColor: Color = CocoaBorder,
    accentGlow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundBrush = remember(accentGlow) {
        if (accentGlow) {
            Brush.verticalGradient(
                colors = listOf(
                    EspressoRich.copy(alpha = 0.55f),
                    MochaCardSurface.copy(alpha = 0.90f),
                    EspressoBlack.copy(alpha = 0.95f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xD6221B17),
                    GlassSurfaceFill
                )
            )
        }
    }

    Surface(
        modifier = modifier.shadow(
            elevation = 14.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.65f),
            spotColor = Color.Black.copy(alpha = 0.65f)
        ),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.background(backgroundBrush),
            content = content
        )
    }
}

@Composable
fun LiquidGlassFloatingBottomBar(
    selectedTab: MainNavTab,
    favoritesCount: Int,
    onSelectTab: (MainNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .height(68.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = barShape,
                    ambientColor = Color.Black.copy(alpha = 0.8f),
                    spotColor = Color.Black.copy(alpha = 0.8f)
                )
                .clip(barShape)
        ) {
            // Dedicated backdrop layer with Android 12+ blur and translucent espresso fallback
            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(20.dp)
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(blurModifier)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                EspressoRich.copy(alpha = 0.50f),
                                GlassBottomNavFill,
                                EspressoBlack.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // Crisp top specular edge & 12% white border rendered unblurred
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GlassHighlightBorder,
                                CocoaBorder
                            )
                        ),
                        shape = barShape
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainNavTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val pillColor by animateColorAsState(
                        targetValue = if (isSelected) CaramelGold.copy(alpha = 0.18f) else Color.Transparent,
                        animationSpec = tween(220),
                        label = "navPillColor"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) CaramelGold else ToastedLatte,
                        animationSpec = tween(220),
                        label = "navContentColor"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.04f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "navScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .padding(horizontal = 3.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(26.dp))
                            .background(pillColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = CaramelGold.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelectTab(tab) }
                            .testTag("nav_tab_${tab.name.lowercase(Locale.US)}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelRes),
                                    tint = contentColor,
                                    modifier = Modifier.size(21.dp)
                                )
                                if (tab == MainNavTab.FAVORITES && favoritesCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(CaramelGold)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidGlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) CaramelGold.copy(alpha = 0.22f) else MochaCardSurface.copy(alpha = 0.85f),
        animationSpec = tween(200),
        label = "chipContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) CaramelGold.copy(alpha = 0.65f) else CocoaBorder,
        animationSpec = tween(200),
        label = "chipBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) CaramelGold else ToastedLatte,
        animationSpec = tween(200),
        label = "chipText"
    )

    val chipInteraction = remember { MutableInteractionSource() }
    val chipPressed by chipInteraction.collectIsPressedAsState()
    val chipScale by animateFloatAsState(
        targetValue = if (chipPressed) 0.93f else 1f,
        animationSpec = if (chipPressed) {
            tween(durationMillis = 100)
        } else {
            spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "chipPressScale"
    )

    Surface(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
            }
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = chipInteraction,
                indication = androidx.compose.material3.ripple(color = CaramelGold),
                onClick = onClick
            ),
        shape = RoundedCornerShape(50),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DistanceBadge(
    distanceKm: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = EspressoBlack.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, CocoaBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.NearMe,
                contentDescription = null,
                tint = CaramelGold,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = stringResource(R.string.distance_km_short, distanceKm),
                style = MaterialTheme.typography.labelSmall,
                color = WarmCream,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun FacilityPillBadge(
    facility: ShopFacility,
    modifier: Modifier = Modifier
) {
    val icon = when (facility) {
        ShopFacility.ROOFTOP -> Icons.Rounded.Roofing
        ShopFacility.WIFI -> Icons.Rounded.Wifi
        ShopFacility.QUIET -> Icons.Rounded.SelfImprovement
        ShopFacility.LATE_NIGHT -> Icons.Rounded.NightsStay
        ShopFacility.FAMILY_FRIENDLY -> Icons.Rounded.LocationOn
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = EspressoRich.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, CocoaBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CaramelGold,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = facility.label,
                style = MaterialTheme.typography.labelSmall,
                color = ToastedLatte,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiquidGlassShopCard(
    item: CoffeeShopListItem,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVerified: Boolean = false,
    liveStatusLabel: String? = null,
    isLiveStatusPositive: Boolean = item.isOpenNow,
    isLiveStatusWarning: Boolean = false,
    crowdLevel: com.example.model.CrowdLevel? = null,
    isCompared: Boolean = false,
    onToggleCompare: (() -> Unit)? = null
) {
    val shop = item.shop
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 120)
        } else {
            spring(
                dampingRatio = 0.48f,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "cardPressScale"
    )
    val cardBorder by animateColorAsState(
        targetValue = if (isPressed) CaramelGold.copy(alpha = 0.45f) else CocoaBorder,
        animationSpec = tween(150),
        label = "cardBorderColor"
    )
    val imageRes = remember(shop.id, shop.imageRes) { ShopImageResolver.resolveDrawable(shop) }

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = CaramelGold),
                onClick = onClick
            )
            .testTag("shop_card_${shop.id}"),
        shape = RoundedCornerShape(26.dp),
        borderColor = cardBorder
    ) {
        // Framed editorial photography header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 10.dp)
                .height(158.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            Image(
                painter = painterResource(id = imageRes),
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
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                EspressoBlack.copy(alpha = 0.82f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (liveStatusLabel != null) {
                        LiveStatusChipBadge(
                            label = liveStatusLabel,
                            isPositive = isLiveStatusPositive,
                            isWarning = isLiveStatusWarning
                        )
                    } else {
                        OpenStatusBadge(isOpenNow = item.isOpenNow)
                    }
                    item.distanceKm?.let { km ->
                        DistanceBadge(distanceKm = km)
                    }
                }

                FavoriteHeartButton(
                    isFavorite = item.isFavorite,
                    onToggle = onToggleFavorite,
                    modifier = Modifier.testTag("favorite_button_${shop.id}")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AreaBadge(areaName = shop.areaLabel)
                    if (isVerified) {
                        VerifiedShopBadge()
                    }
                }

                if (shop.priceRange.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = EspressoBlack.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, CocoaBorder)
                    ) {
                        Text(
                            text = shop.priceRange,
                            style = MaterialTheme.typography.labelSmall,
                            color = CaramelGold,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = WarmCream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (onToggleCompare != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isCompared) CaramelGold.copy(alpha = 0.2f) else EspressoBlack.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, if (isCompared) CaramelGold.copy(alpha = 0.65f) else CocoaBorder),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onToggleCompare)
                            .testTag("compare_toggle_${shop.id}")
                    ) {
                        Text(
                            text = if (isCompared) "Compared" else "+ Compare",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCompared) CaramelGold else ToastedLatte,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Text(
                text = shop.address,
                style = MaterialTheme.typography.bodySmall,
                color = ToastedLatte,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = CaramelGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = shop.hoursDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmCream,
                        fontWeight = FontWeight.Medium
                    )
                }

                StarRatingRow(
                    rating = item.effectiveRating,
                    reviewCount = item.reviewCount
                )
            }

            val badges = remember(shop.id, shop.facilities, shop.isFamilyFriendly) {
                shop.resolvedFacilities.filter { it != ShopFacility.FAMILY_FRIENDLY }.take(3)
            }
            if (badges.isNotEmpty() || shop.isCommunitySubmitted || shop.isFamilyFriendly || crowdLevel != null) {
                HorizontalDivider(color = CocoaBorder)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (crowdLevel != null) {
                        CrowdLevelPill(level = crowdLevel)
                    }
                    badges.forEach { facility ->
                        FacilityPillBadge(facility = facility)
                    }
                    if (shop.isFamilyFriendly) {
                        FamilyFriendlyBadge()
                    }
                    if (shop.isCommunitySubmitted) {
                        CommunityBadge()
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedSpotlightCard(
    item: CoffeeShopListItem,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 286.dp
) {
    val shop = item.shop
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 120)
        } else {
            spring(
                dampingRatio = 0.48f,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "featuredPressScale"
    )
    val imageRes = remember(shop.id, shop.imageRes) { ShopImageResolver.resolveDrawable(shop) }

    Surface(
        modifier = modifier
            .width(cardWidth)
            .height(228.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.7f),
                spotColor = Color.Black.copy(alpha = 0.7f)
            )
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = CaramelGold),
                onClick = onClick
            )
            .testTag("featured_card_${shop.id}"),
        shape = RoundedCornerShape(28.dp),
        color = MochaCardSurface,
        border = BorderStroke(1.dp, CocoaBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = imageRes),
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
                                EspressoBlack.copy(alpha = 0.42f),
                                Color.Transparent,
                                EspressoBlack.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OpenStatusBadge(isOpenNow = item.isOpenNow)
                    item.distanceKm?.let { km ->
                        DistanceBadge(distanceKm = km)
                    }
                }

                FavoriteHeartButton(
                    isFavorite = item.isFavorite,
                    onToggle = onToggleFavorite
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = shop.areaLabel.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall,
                    color = CaramelGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = WarmCream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = shop.hoursDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = ToastedLatte
                    )
                    StarRatingRow(rating = item.effectiveRating)
                }
            }
        }
    }
}

@Composable
fun OpenNowCompactCard(
    item: CoffeeShopListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shop = item.shop
    val imageRes = remember(shop.id, shop.imageRes) { ShopImageResolver.resolveDrawable(shop) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 120)
        } else {
            spring(
                dampingRatio = 0.48f,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "compactPressScale"
    )

    LiquidGlassSurface(
        modifier = modifier
            .width(220.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = CaramelGold),
                onClick = onClick
            )
            .testTag("open_now_card_${shop.id}"),
        shape = RoundedCornerShape(22.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = shop.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, EspressoBlack.copy(alpha = 0.75f))
                        )
                    )
            )
            item.distanceKm?.let { km ->
                DistanceBadge(
                    distanceKm = km,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
            AreaBadge(
                areaName = shop.areaLabel,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = shop.name,
                style = MaterialTheme.typography.titleSmall,
                color = WarmCream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = shop.hoursDisplay,
                style = MaterialTheme.typography.labelSmall,
                color = CaramelGold
            )
            StarRatingRow(rating = item.effectiveRating)
        }
    }
}

@Composable
fun StartupSplashOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "splashAlpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splashScale"
    )

    LaunchedEffect(Unit) {
        visible = true
        kotlinx.coroutines.delay(900L)
        visible = false
        kotlinx.coroutines.delay(360L)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EspressoBlack)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        EspressoRich.copy(alpha = 0.55f),
                        EspressoBlack,
                        EspressoBlack
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .alpha(alphaAnim)
                .graphicsLayer {
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MochaCardSurface,
                border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.45f)),
                shadowElevation = 20.dp,
                modifier = Modifier.size(104.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nightcoffee_liquid_fg),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = WarmCream
                )
                Text(
                    text = stringResource(R.string.home_greeting_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CaramelGold
                )
            }
        }
    }
}
