package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ClosedTerracotta
import com.example.ui.theme.ClosedTerracottaContainer
import com.example.ui.theme.CremaGold
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.FamilyBadgeContainer
import com.example.ui.theme.FamilyHoney
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenMint
import com.example.ui.theme.OpenMintContainer
import com.example.ui.theme.StarAmber
import com.example.ui.theme.WarmCream
import java.util.Locale

@Composable
fun OpenStatusBadge(
    isOpenNow: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isOpenNow) OpenMintContainer else ClosedTerracottaContainer
    val contentColor = if (isOpenNow) OpenMint else ClosedTerracotta
    val label = stringResource(if (isOpenNow) R.string.status_open_now else R.string.status_closed)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
        color = EspressoBlack.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, CremaGold.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = CremaGold,
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
        border = BorderStroke(1.dp, FamilyHoney.copy(alpha = 0.38f))
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
fun StarRatingRow(
    rating: Double,
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
            val tint = if (rating >= index - 0.65) StarAmber else MutedCinnamon.copy(alpha = 0.55f)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (showOutOfFive) {
                String.format(Locale.US, "%.1f / 5.0", rating)
            } else {
                String.format(Locale.US, "%.1f", rating)
            },
            style = MaterialTheme.typography.labelLarge,
            color = WarmCream,
            fontWeight = FontWeight.SemiBold
        )
    }
}
