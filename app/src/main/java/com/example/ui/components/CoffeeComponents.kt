@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.StarHalf
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CaramelContainer
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.ClosedRed
import com.example.ui.theme.ClosedRedContainer
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.FamilyBadgeContainer
import com.example.ui.theme.FamilyHoney
import com.example.ui.theme.FavoriteHeart
import com.example.ui.theme.FavoriteHeartContainer
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.OpenGreenContainer
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.StarAmber
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.viewmodel.CoffeeShopListItem
import java.util.Locale

@Composable
fun OpenStatusBadge(
    isOpenNow: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isOpenNow) OpenGreenContainer else ClosedRedContainer
    val contentColor = if (isOpenNow) OpenGreen else ClosedRed
    val label = stringResource(if (isOpenNow) R.string.status_open_now else R.string.status_closed)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.5f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(contentColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AreaBadge(
    areaName: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = RoastedBeanElevated,
        border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.4f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = CaramelGold,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = areaName,
                style = MaterialTheme.typography.labelSmall,
                color = WarmCream,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun FamilyFriendlyBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = FamilyBadgeContainer,
        border = BorderStroke(1.dp, FamilyHoney.copy(alpha = 0.4f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FamilyRestroom,
                contentDescription = null,
                tint = FamilyHoney,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.badge_family_friendly),
                style = MaterialTheme.typography.labelSmall,
                color = FamilyHoney,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CommunityBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = CaramelContainer,
        border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.45f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Text(
            text = stringResource(R.string.badge_community_added),
            style = MaterialTheme.typography.labelSmall,
            color = CaramelGold,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun FavoriteHeartButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = stringResource(
        if (isFavorite) R.string.favorite_remove_desc else R.string.favorite_add_desc
    )
    Surface(
        shape = CircleShape,
        color = if (isFavorite) FavoriteHeartContainer else EspressoBlack,
        border = BorderStroke(
            width = 1.dp,
            color = if (isFavorite) FavoriteHeart.copy(alpha = 0.6f) else CocoaBorder
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = modifier.size(48.dp)
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = description,
                tint = if (isFavorite) FavoriteHeart else WarmCream,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun StarRatingRow(
    rating: Double,
    reviewCount: Int = 0,
    showOutOfFive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ratingDescription = stringResource(R.string.rating_out_of_five, rating)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = ratingDescription
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 1..5) {
            val icon = when {
                rating >= index - 0.15 -> Icons.Rounded.Star
                rating >= index - 0.65 -> Icons.AutoMirrored.Rounded.StarHalf
                else -> Icons.Rounded.StarOutline
            }
            val tint = if (rating >= index - 0.65) StarAmber else MutedCinnamon.copy(alpha = 0.5f)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = when {
                rating <= 0.0 -> "New"
                showOutOfFive -> String.format(Locale.US, "%.1f / 5.0", rating)
                else -> String.format(Locale.US, "%.1f", rating)
            },
            style = MaterialTheme.typography.labelLarge,
            color = WarmCream,
            fontWeight = FontWeight.SemiBold
        )
        if (reviewCount > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($reviewCount)",
                style = MaterialTheme.typography.labelMedium,
                color = MutedCinnamon
            )
        }
    }
}

@Composable
fun InteractiveStarSelector(
    selectedRating: Int,
    onSelectRating: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (star in 1..5) {
            val selected = star <= selectedRating
            IconButton(
                onClick = { onSelectRating(star) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("star_selector_$star")
            ) {
                Icon(
                    imageVector = if (selected) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = "$star stars",
                    tint = if (selected) StarAmber else MutedCinnamon,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactCoffeeShopCard(
    item: CoffeeShopListItem,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shop = item.shop
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("shop_card_${shop.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MochaCardSurface,
        border = BorderStroke(1.dp, CocoaBorder),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AreaBadge(areaName = shop.areaLabel)
                    OpenStatusBadge(isOpenNow = item.isOpenNow)
                }

                FavoriteHeartButton(
                    isFavorite = item.isFavorite,
                    onToggle = onToggleFavorite,
                    modifier = Modifier.testTag("favorite_button_${shop.id}")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = WarmCream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = shop.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = ToastedLatte,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

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

                if (shop.priceRange.isNotEmpty()) {
                    Text(
                        text = shop.priceRange,
                        style = MaterialTheme.typography.labelLarge,
                        color = CaramelGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (shop.popularItems.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    shop.popularItems.take(3).forEach { drink ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RoastedBeanElevated,
                            border = BorderStroke(1.dp, CocoaBorder.copy(alpha = 0.7f)),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = drink,
                                style = MaterialTheme.typography.labelSmall,
                                color = ToastedLatte,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = CocoaBorder.copy(alpha = 0.55f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StarRatingRow(
                    rating = item.effectiveRating,
                    reviewCount = item.reviewCount
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (shop.isCommunitySubmitted) {
                        CommunityBadge()
                    }
                    if (shop.isFamilyFriendly) {
                        FamilyFriendlyBadge()
                    }
                }
            }
        }
    }
}
