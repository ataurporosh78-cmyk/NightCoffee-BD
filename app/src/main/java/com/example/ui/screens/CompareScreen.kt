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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ResolvedShopStatus
import com.example.model.ShopFacility
import com.example.model.ShopOverlayDoc
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PlatformTopBar
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.viewmodel.CoffeeShopListItem
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CompareScreen(
    allItems: List<CoffeeShopListItem>,
    compareShopIds: List<String>,
    overlays: Map<String, ShopOverlayDoc>,
    resolveStatus: (CoffeeShopListItem) -> ResolvedShopStatus,
    onToggleCompare: suspend (String) -> Boolean,
    onClearAll: suspend () -> Unit,
    onOpenShop: (String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }

    val selectedItems = remember(allItems, compareShopIds) {
        val byId = allItems.associateBy { it.shop.id }
        compareShopIds.mapNotNull { byId[it] }
    }

    Scaffold(containerColor = EspressoBlack) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(EspressoRich.copy(alpha = 0.25f), EspressoBlack, EspressoBlack)
                    )
                )
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
            ) {
                PlatformTopBar(
                    title = stringResource(R.string.compare_title),
                    subtitle = "${selectedItems.size}/3 selected",
                    onBack = onBack,
                    trailingAction = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (selectedItems.isNotEmpty()) {
                                TextButton(
                                    onClick = { scope.launch { onClearAll() } },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Clear", color = MutedCinnamon)
                                }
                            }
                            if (selectedItems.size < 3) {
                                Button(
                                    onClick = { showPicker = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CaramelGold,
                                        contentColor = DarkEspressoInk
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("compare_add_shop_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Add", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                )

                if (selectedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LiquidGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.CompareArrows, contentDescription = null, tint = CaramelGold, modifier = Modifier.size(40.dp))
                                Text(
                                    text = stringResource(R.string.compare_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ToastedLatte
                                )
                                Button(
                                    onClick = { showPicker = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CaramelGold, contentColor = DarkEspressoInk),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(stringResource(R.string.compare_add_shop), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    val colWidth = 176.dp
                    val hScroll = rememberScrollState()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("compare_table"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(hScroll)
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Header Shop Names + Remove buttons
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Spacer(modifier = Modifier.width(108.dp))
                                        selectedItems.forEach { item ->
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = EspressoRich.copy(alpha = 0.45f),
                                                border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.4f)),
                                                modifier = Modifier
                                                    .width(colWidth)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable { onOpenShop(item.shop.id) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = item.shop.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = WarmCream,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(
                                                        onClick = { scope.launch { onToggleCompare(item.shop.id) } },
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.Close, contentDescription = "Remove ${item.shop.name}", tint = ToastedLatte)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    CompareRow("Area", selectedItems.map { it.shop.areaLabel.ifBlank { "Not listed" } }, colWidth)
                                    CompareRow(
                                        "Distance",
                                        selectedItems.map { item ->
                                            item.distanceKm?.let { String.format(Locale.US, "%.1f km", it) } ?: "Not listed"
                                        },
                                        colWidth
                                    )
                                    CompareRow(
                                        "Rating",
                                        selectedItems.map { item ->
                                            if (item.effectiveRating > 0.0) String.format(Locale.US, "%.1f / 5.0", item.effectiveRating) else "Not listed"
                                        },
                                        colWidth
                                    )
                                    CompareRow("Price Range", selectedItems.map { it.shop.priceRange.ifBlank { "Not listed" } }, colWidth)
                                    CompareRow("Hours", selectedItems.map { it.shop.hoursDisplay.ifBlank { "Not listed" } }, colWidth)
                                    CompareRow(
                                        "Status",
                                        selectedItems.map { item ->
                                            val st = resolveStatus(item)
                                            st.verifiedStatus?.label ?: st.scheduledLabel
                                        },
                                        colWidth
                                    )
                                    CompareRow(
                                        "Verified",
                                        selectedItems.map { if (overlays[it.shop.id]?.verified == true) "Verified Business" else "Not verified" },
                                        colWidth
                                    )
                                    CompareRow(
                                        "Wi-Fi",
                                        selectedItems.map { item ->
                                            val ov = overlays[item.shop.id]
                                            val hasWifi = ov?.wifiAvailable ?: (ShopFacility.WIFI in item.shop.resolvedFacilities)
                                            if (hasWifi) "Available" else "Not listed"
                                        },
                                        colWidth
                                    )
                                    CompareRow(
                                        "AC",
                                        selectedItems.map { item ->
                                            val ov = overlays[item.shop.id]
                                            if (ov != null) if (ov.acAvailable) "Available" else "Not listed" else "Not listed"
                                        },
                                        colWidth
                                    )
                                    CompareRow(
                                        "Rooftop",
                                        selectedItems.map { item ->
                                            val ov = overlays[item.shop.id]
                                            val hasRoof = ov?.rooftopAvailable ?: (ShopFacility.ROOFTOP in item.shop.resolvedFacilities)
                                            if (hasRoof) "Yes" else "Not listed"
                                        },
                                        colWidth
                                    )
                                    CompareRow(
                                        "Facilities",
                                        selectedItems.map { item ->
                                            val labels = item.shop.resolvedFacilities.joinToString(", ") { it.label }
                                            labels.ifBlank { "Not listed" }
                                        },
                                        colWidth
                                    )
                                    CompareRow(
                                        "Menu Highlights",
                                        selectedItems.map { item ->
                                            item.shop.popularItems.take(3).joinToString(", ").ifBlank { "Not listed" }
                                        },
                                        colWidth
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                    }
                }
            }
        }
    }

    if (showPicker) {
        val candidates = remember(allItems, compareShopIds) {
            allItems.filter { it.shop.id !in compareShopIds }.take(40)
        }
        AlertDialog(
            onDismissRequest = { showPicker = false },
            containerColor = RoastedBeanElevated,
            title = { Text("Add Cafe to Compare", color = WarmCream) },
            text = {
                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(candidates.size) { idx ->
                        val item = candidates[idx]
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = EspressoBlack.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, CocoaBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    scope.launch {
                                        onToggleCompare(item.shop.id)
                                        showPicker = false
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(item.shop.name, color = WarmCream, fontWeight = FontWeight.SemiBold)
                                Text("${item.shop.areaLabel} · ${item.shop.hoursDisplay}", style = MaterialTheme.typography.labelSmall, color = ToastedLatte)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("Close", color = OpenGreen) }
            }
        )
    }
}

@Composable
private fun CompareRow(
    attribute: String,
    values: List<String>,
    colWidth: androidx.compose.ui.unit.Dp
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = CocoaBorder)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = attribute,
                style = MaterialTheme.typography.labelLarge,
                color = CaramelGold,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(108.dp)
            )
            values.forEach { value ->
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (value == "Not listed") MutedCinnamon else WarmCream,
                    modifier = Modifier.width(colWidth)
                )
            }
        }
    }
}
