@file:OptIn(ExperimentalStdlibApi::class)

package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DhakaArea
import com.example.ui.components.DistanceBadge
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.OpenStatusBadge
import com.example.ui.components.PlatformTopBar
import com.example.ui.components.StarRatingRow
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.ClosedRed
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.viewmodel.CoffeeShopListItem
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun NearbyMapScreen(
    shops: List<CoffeeShopListItem>,
    selectedArea: DhakaArea,
    hasLocationPermission: Boolean,
    onSelectArea: (DhakaArea) -> Unit,
    onRequestLocation: () -> Unit,
    onShopClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val noMapsMsg = stringResource(R.string.error_no_maps_app)

    var selectedShop by remember(shops) { mutableStateOf(shops.firstOrNull()) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Dhaka bounding box covering Uttara (23.89) down to Keraniganj (23.68), Mohammadpur (90.34) to Banasree/Aftabnagar (90.46)
    val minLat = 23.685
    val maxLat = 23.890
    val minLng = 90.345
    val maxLng = 90.465

    Scaffold(
        containerColor = EspressoBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EspressoBlack)
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                PlatformTopBar(
                    title = stringResource(R.string.map_title),
                    subtitle = stringResource(R.string.map_subtitle),
                    onBack = onBack,
                    trailingAction = {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (hasLocationPermission) CaramelGold.copy(alpha = 0.2f) else DeepRoastSurface,
                            border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onRequestLocation)
                                .testTag("map_locate_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = CaramelGold, modifier = Modifier.size(17.dp))
                                Text(
                                    text = if (hasLocationPermission) "Near Me" else "Enable GPS",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CaramelGold
                                )
                            }
                        }
                    }
                )

                if (!hasLocationPermission) {
                    Text(
                        text = stringResource(R.string.map_manual_area_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = ToastedLatte,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }

                // Quick Dhaka Area Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val featuredAreas = listOf(
                        DhakaArea.ALL,
                        DhakaArea.BANANI,
                        DhakaArea.GULSHAN,
                        DhakaArea.DHANMONDI,
                        DhakaArea.UTTARA,
                        DhakaArea.BASHUNDHARA,
                        DhakaArea.MIRPUR,
                        DhakaArea.KHILGAON,
                        DhakaArea.OLD_DHAKA
                    )
                    featuredAreas.forEach { area ->
                        LiquidGlassChip(
                            label = area.displayName,
                            selected = selectedArea == area,
                            onClick = {
                                onSelectArea(area)
                                panOffset = Offset.Zero
                            },
                            modifier = Modifier.testTag("map_area_${area.name}")
                        )
                    }
                }

                // Interactive Dhaka Discovery Canvas Map
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(EspressoRich.copy(alpha = 0.42f), DeepRoastSurface, EspressoBlack)
                            )
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                zoom = (zoom * gestureZoom).coerceIn(1f, 2.8f)
                                panOffset = Offset(
                                    x = (panOffset.x + pan.x).coerceIn(-350f * zoom, 350f * zoom),
                                    y = (panOffset.y + pan.y).coerceIn(-350f * zoom, 350f * zoom)
                                )
                            }
                        }
                        .testTag("dhaka_discovery_map")
                ) {
                    val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
                    val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
                    val pinSizePx = with(LocalDensity.current) { 34.dp.toPx() }

                    // Subtle coordinate grid + Buriganga / Hatirjheel watermark lines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 72.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = CocoaBorder.copy(alpha = 0.25f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = CocoaBorder.copy(alpha = 0.25f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            y += step
                        }
                    }

                    // Plot up to 60 shops smoothly for clean visual clarity
                    val visibleShops = remember(shops) { shops.take(60) }
                    visibleShops.forEach { item ->
                        val shop = item.shop
                        val normX = ((shop.longitude - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.05f, 0.95f)
                        val normY = (1.0 - (shop.latitude - minLat) / (maxLat - minLat)).toFloat().coerceIn(0.05f, 0.95f)

                        val centerX = widthPx / 2f
                        val centerY = heightPx / 2f
                        val px = ((normX * widthPx - centerX) * zoom + centerX + panOffset.x - pinSizePx / 2f).roundToInt()
                        val py = ((normY * heightPx - centerY) * zoom + centerY + panOffset.y - pinSizePx / 2f).roundToInt()

                        val isSelected = selectedShop?.shop?.id == shop.id
                        val pinColor = when {
                            isSelected -> CaramelGold
                            item.isOpenNow -> OpenGreen
                            else -> ClosedRed
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) CaramelGold else EspressoBlack.copy(alpha = 0.88f),
                            border = BorderStroke(if (isSelected) 2.dp else 1.5.dp, pinColor),
                            shadowElevation = if (isSelected) 12.dp else 4.dp,
                            modifier = Modifier
                                .offset { IntOffset(px, py) }
                                .size(if (isSelected) 40.dp else 32.dp)
                                .clip(CircleShape)
                                .clickable { selectedShop = item }
                                .testTag("map_pin_${shop.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalCafe,
                                    contentDescription = shop.name,
                                    tint = if (isSelected) DarkEspressoInk else pinColor,
                                    modifier = Modifier.size(if (isSelected) 20.dp else 15.dp)
                                )
                            }
                        }
                    }

                    // Map Legend overlay
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = EspressoBlack.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, CocoaBorder),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot(color = OpenGreen, label = "Open")
                            LegendDot(color = ClosedRed, label = "Closed")
                            Text("${visibleShops.size} pinned", style = MaterialTheme.typography.labelSmall, color = ToastedLatte)
                        }
                    }
                }

                // Selected Shop Bottom Card
                selectedShop?.let { item ->
                    val shop = item.shop
                    LiquidGlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .navigationBarsPadding(),
                        shape = RoundedCornerShape(26.dp),
                        accentGlow = true
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = shop.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = WarmCream,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${shop.areaLabel} · ${shop.address}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ToastedLatte,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OpenStatusBadge(isOpenNow = item.isOpenNow)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StarRatingRow(rating = item.effectiveRating, reviewCount = item.reviewCount)
                                item.distanceKm?.let { DistanceBadge(distanceKm = it) }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onShopClick(shop.id) },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("map_view_shop_button")
                                ) {
                                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("View Details", color = CaramelGold)
                                }

                                Button(
                                    onClick = {
                                        val encodedQuery = Uri.encode("${shop.name}, ${shop.address}, Dhaka")
                                        val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encodedQuery"))
                                        try {
                                            context.startActivity(geoIntent)
                                        } catch (_: ActivityNotFoundException) {
                                            val webIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
                                            )
                                            try {
                                                context.startActivity(webIntent)
                                            } catch (_: ActivityNotFoundException) {
                                                scope.launch { snackbarHostState.showSnackbar(noMapsMsg) }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CaramelGold,
                                        contentColor = DarkEspressoInk
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("map_directions_button")
                                ) {
                                    Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(stringResource(R.string.map_open_directions), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = WarmCream)
    }
}
