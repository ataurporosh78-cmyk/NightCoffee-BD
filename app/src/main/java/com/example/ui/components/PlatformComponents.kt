@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CrowdLevel
import com.example.model.LiveShopStatus
import com.example.ui.theme.CaramelContainer
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.ClosedRed
import com.example.ui.theme.ClosedRedContainer
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.FamilyBadgeContainer
import com.example.ui.theme.FamilyHoney
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.OpenGreenContainer
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream

@Composable
fun PlatformTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MochaCardSurface.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, CocoaBorder),
            modifier = Modifier.size(48.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("platform_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.details_back_desc),
                    tint = WarmCream
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = WarmCream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ToastedLatte,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailingAction != null) {
            trailingAction()
        }
    }
}

@Composable
fun VerifiedShopBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = CaramelContainer.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.65f)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = null,
                tint = CaramelGold,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = stringResource(R.string.badge_verified_shop),
                style = MaterialTheme.typography.labelSmall,
                color = CaramelGold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LiveStatusChipBadge(
    label: String,
    isPositive: Boolean,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bg = when {
        isWarning -> FamilyBadgeContainer.copy(alpha = 0.9f)
        isPositive -> OpenGreenContainer.copy(alpha = 0.9f)
        else -> ClosedRedContainer.copy(alpha = 0.9f)
    }
    val fg = when {
        isWarning -> FamilyHoney
        isPositive -> OpenGreen
        else -> ClosedRed
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.48f))
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
                    .background(fg)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CrowdLevelPill(
    level: CrowdLevel,
    modifier: Modifier = Modifier
) {
    val tint = when (level) {
        CrowdLevel.QUIET -> OpenGreen
        CrowdLevel.MODERATE -> CaramelGold
        CrowdLevel.BUSY -> FamilyHoney
        CrowdLevel.VERY_BUSY -> ClosedRed
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = EspressoBlack.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Groups,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = level.label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun OfflineSyncBanner(
    isOnline: Boolean,
    pendingCount: Int,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isOnline && pendingCount == 0) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (!isOnline) ClosedRedContainer.copy(alpha = 0.85f) else FamilyBadgeContainer.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            if (!isOnline) ClosedRed.copy(alpha = 0.4f) else FamilyHoney.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (!isOnline) Icons.Rounded.CloudOff else Icons.Rounded.Sync,
                contentDescription = null,
                tint = if (!isOnline) ClosedRed else FamilyHoney,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (!isOnline) "Offline Mode" else "Syncing queued updates",
                    style = MaterialTheme.typography.labelLarge,
                    color = WarmCream,
                    fontWeight = FontWeight.SemiBold
                )
                val desc = when {
                    !isOnline && pendingCount > 0 ->
                        "$pendingCount update(s) queued — will sync automatically when online."
                    !isOnline ->
                        "Showing bundled Dhaka directory & cached menus/offers."
                    else ->
                        "$pendingCount offline update(s) waiting to sync."
                }
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = ToastedLatte
                )
            }
            if (isOnline && pendingCount > 0) {
                TextButton(
                    onClick = onSyncNow,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(text = "Sync", color = CaramelGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuickPlatformTile(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MochaCardSurface.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, CocoaBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CaramelGold,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = WarmCream,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (!badge.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(2.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CaramelGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = CaramelGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

fun LiveShopStatus.isPositiveOpen(): Boolean =
    this == LiveShopStatus.OPEN_NOW || this == LiveShopStatus.CLOSING_SOON
