@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch
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
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.OpenGreenContainer
import com.example.ui.theme.StarAmber
import com.example.ui.theme.WarmCream
import com.example.viewmodel.CoffeeShopListItem
import java.util.Locale

@Composable
fun OpenStatusBadge(
    isOpenNow: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isOpenNow) OpenGreenContainer.copy(alpha = 0.88f) else ClosedRedContainer.copy(alpha = 0.88f)
    val contentColor = if (isOpenNow) OpenGreen else ClosedRed
    val label = stringResource(if (isOpenNow) R.string.status_open_now else R.string.status_closed)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.48f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PulsingStatusDot(
                color = contentColor,
                isPulsing = isOpenNow,
                dotSize = 7.dp
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
        color = EspressoBlack.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.42f)),
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
        color = FamilyBadgeContainer.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, FamilyHoney.copy(alpha = 0.35f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FamilyRestroom,
                contentDescription = null,
                tint = FamilyHoney,
                modifier = Modifier.size(13.dp)
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
        color = CaramelContainer.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.45f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Text(
            text = stringResource(R.string.badge_community_added),
            style = MaterialTheme.typography.labelSmall,
            color = CaramelGold,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
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
    val density = LocalDensity.current
    val sparkRadiusPx = remember(density) { with(density) { 26.dp.toPx() } }
    val sparkDotRadiusPx = remember(density) { with(density) { 3.dp.toPx() } }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 100)
        } else {
            spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)
        },
        label = "favoritePressScale"
    )

    var prevFavorite by remember { mutableStateOf(isFavorite) }
    val popScale = remember { Animatable(1f) }
    val sparkProgress = remember { Animatable(0f) }

    LaunchedEffect(isFavorite) {
        if (isFavorite != prevFavorite) {
            prevFavorite = isFavorite
            if (isFavorite) {
                launch {
                    popScale.snapTo(0.55f)
                    popScale.animateTo(
                        targetValue = 1.14f,
                        animationSpec = spring(
                            dampingRatio = 0.38f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    popScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 160)
                    )
                }
                launch {
                    sparkProgress.snapTo(0.01f)
                    sparkProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 480, easing = EaseOutMotion)
                    )
                    sparkProgress.snapTo(0f)
                }
            } else {
                launch {
                    popScale.snapTo(0.78f)
                    popScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 250, easing = EaseOut)
                    )
                }
            }
        }
    }

    val heartColor = if (isFavorite) Color(0xFFFF4D6D) else WarmCream
    val bgColor by animateColorAsState(
        targetValue = if (isFavorite) FavoriteHeartContainer.copy(alpha = 0.90f) else EspressoBlack.copy(alpha = 0.70f),
        animationSpec = tween(200),
        label = "favoriteBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFFF4D6D).copy(alpha = 0.65f) else CocoaBorder,
        animationSpec = tween(200),
        label = "favoriteBorder"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = bgColor,
            border = BorderStroke(width = 1.dp, color = borderColor),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.size(48.dp)
        ) {
            IconButton(
                onClick = onToggle,
                interactionSource = interactionSource,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = description,
                    tint = heartColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = popScale.value
                            scaleY = popScale.value
                        }
                )
            }
        }

        if (sparkProgress.value > 0f) {
            Canvas(modifier = Modifier.size(48.dp)) {
                val p = sparkProgress.value
                val cx = size.width / 2f
                val cy = size.height / 2f
                val particleRadius = sparkDotRadiusPx * (1f - p).coerceIn(0f, 1f)
                val particleAlpha = (1f - p).coerceIn(0f, 1f)
                val distance = sparkRadiusPx * p

                for (k in 0 until 8) {
                    val angleRad = Math.toRadians(k * 45.0)
                    val dx = (cos(angleRad) * distance).toFloat()
                    val dy = (sin(angleRad) * distance).toFloat()
                    val color = if (k % 2 == 1) Color(0xFFD9B26F) else Color(0xFFFF4D6D)
                    drawCircle(
                        color = color.copy(alpha = particleAlpha),
                        radius = particleRadius,
                        center = Offset(cx + dx, cy + dy)
                    )
                }
            }
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

@Composable
fun CompactCoffeeShopCard(
    item: CoffeeShopListItem,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassShopCard(
        item = item,
        onToggleFavorite = onToggleFavorite,
        onClick = onClick,
        modifier = modifier
    )
}
