@file:OptIn(ExperimentalStdlibApi::class, ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CoffeeShop
import com.example.model.DigitalMenuItemDoc
import com.example.model.MenuCategory
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PlatformTopBar
import com.example.ui.theme.CaramelContainer
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.ClosedRed
import com.example.ui.theme.ClosedRedContainer
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.OpenGreenContainer
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import kotlinx.coroutines.launch

@Composable
fun DigitalMenuScreen(
    shop: CoffeeShop?,
    menuItems: List<DigitalMenuItemDoc>,
    isOfflineCached: Boolean,
    canManage: Boolean,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSaveItem: suspend (
        existingId: String?,
        category: MenuCategory,
        name: String,
        description: String,
        imageUrl: String,
        priceBdt: Long,
        available: Boolean,
        special: Boolean
    ) -> Result<Unit>,
    onDeleteItem: suspend (String) -> Result<Unit>
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf<MenuCategory?>(null) }
    var editingItem by remember { mutableStateOf<DigitalMenuItemDoc?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val filteredItems = remember(menuItems, selectedCategory) {
        menuItems
            .filter { selectedCategory == null || it.categoryEnum() == selectedCategory }
            .sortedWith(compareByDescending<DigitalMenuItemDoc> { it.special }.thenBy { it.name })
    }

    Scaffold(
        containerColor = EspressoBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
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
                    .widthIn(max = 720.dp)
            ) {
                PlatformTopBar(
                    title = stringResource(R.string.menu_screen_title),
                    subtitle = shop?.name ?: "NightCoffee BD",
                    onBack = onBack,
                    trailingAction = {
                        if (canManage && shop != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = CaramelGold,
                                modifier = Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .testTag("menu_add_item_button")
                            ) {
                                TextButton(
                                    onClick = {
                                        editingItem = null
                                        showEditor = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = DarkEspressoInk,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Add",
                                        color = DarkEspressoInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )

                if (isOfflineCached) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = OpenGreenContainer.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, OpenGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OfflinePin,
                                contentDescription = null,
                                tint = OpenGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = stringResource(R.string.menu_offline_cached_badge),
                                style = MaterialTheme.typography.labelMedium,
                                color = OpenGreen
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = "All",
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        modifier = Modifier.testTag("menu_cat_all")
                    )
                    MenuCategory.entries.forEach { cat ->
                        LiquidGlassChip(
                            label = cat.displayName,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            modifier = Modifier.testTag("menu_cat_${cat.name}")
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("digital_menu_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredItems.isEmpty()) {
                        item(key = "menu_empty") {
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.RestaurantMenu,
                                        contentDescription = null,
                                        tint = CaramelGold,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.menu_empty_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = WarmCream
                                    )
                                    Text(
                                        text = stringResource(R.string.menu_empty_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ToastedLatte
                                    )
                                }
                            }
                        }

                        if (shop != null && shop.popularItems.isNotEmpty()) {
                            item(key = "directory_popular_items") {
                                LiquidGlassSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Listed Directory Highlights",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = WarmCream
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            shop.popularItems.forEach { itemName ->
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = CaramelContainer.copy(alpha = 0.65f),
                                                    border = BorderStroke(1.dp, CocoaBorder)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.LocalCafe,
                                                            contentDescription = null,
                                                            tint = CaramelGold,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Text(
                                                            text = itemName,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = WarmCream
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredItems, key = { it.id }) { item ->
                            MenuItemCard(
                                item = item,
                                canManage = canManage,
                                onEdit = {
                                    editingItem = item
                                    showEditor = true
                                },
                                onDelete = {
                                    scope.launch {
                                        val res = onDeleteItem(item.id)
                                        snackbarHostState.showSnackbar(
                                            res.fold(
                                                onSuccess = { "Menu item removed" },
                                                onFailure = { it.message ?: "Failed to remove item" }
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                }
            }
        }
    }

    if (showEditor) {
        MenuEditorDialog(
            initial = editingItem,
            isBusy = isBusy,
            onDismiss = { showEditor = false },
            onSave = { cat, name, desc, img, price, avail, spec ->
                scope.launch {
                    val res = onSaveItem(editingItem?.id, cat, name, desc, img, price, avail, spec)
                    if (res.isSuccess) {
                        showEditor = false
                        snackbarHostState.showSnackbar("Digital menu updated")
                    } else {
                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Save failed")
                    }
                }
            }
        )
    }
}

@Composable
private fun MenuItemCard(
    item: DigitalMenuItemDoc,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.categoryEnum().displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = CaramelGold
                    )
                    if (item.special) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CaramelGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = stringResource(R.string.menu_special_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = CaramelGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!item.available) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ClosedRedContainer
                        ) {
                            Text(
                                text = stringResource(R.string.menu_unavailable_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = ClosedRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmCream,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ToastedLatte
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "৳${item.priceBdt}",
                    style = MaterialTheme.typography.titleMedium,
                    color = CaramelGold,
                    fontWeight = FontWeight.Bold
                )
                if (canManage) {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit menu item", tint = ToastedLatte)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete menu item", tint = ClosedRed)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MenuEditorDialog(
    initial: DigitalMenuItemDoc?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (MenuCategory, String, String, String, Long, Boolean, Boolean) -> Unit
) {
    var category by remember { mutableStateOf(initial?.categoryEnum() ?: MenuCategory.HOT_COFFEE) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl.orEmpty()) }
    var priceText by remember { mutableStateOf(initial?.priceBdt?.takeIf { it > 0 }?.toString().orEmpty()) }
    var available by remember { mutableStateOf(initial?.available ?: true) }
    var special by remember { mutableStateOf(initial?.special ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RoastedBeanElevated,
        title = {
            Text(
                text = if (initial == null) "Add Menu Item" else "Edit Menu Item",
                color = WarmCream
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MenuCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.displayName) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DeepRoastSurface,
                        unfocusedContainerColor = DeepRoastSurface,
                        focusedTextColor = WarmCream,
                        unfocusedTextColor = WarmCream
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { input -> priceText = input.filter { it.isDigit() }.take(5) },
                    label = { Text("Price (BDT ৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DeepRoastSurface,
                        unfocusedContainerColor = DeepRoastSurface,
                        focusedTextColor = WarmCream,
                        unfocusedTextColor = WarmCream
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 300) description = it },
                    label = { Text("Description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DeepRoastSurface,
                        unfocusedContainerColor = DeepRoastSurface,
                        focusedTextColor = WarmCream,
                        unfocusedTextColor = WarmCream
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { if (it.length <= 500) imageUrl = it },
                    label = { Text("Product Image URL (optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DeepRoastSurface,
                        unfocusedContainerColor = DeepRoastSurface,
                        focusedTextColor = WarmCream,
                        unfocusedTextColor = WarmCream
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available", color = WarmCream)
                    Switch(checked = available, onCheckedChange = { available = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chef Special", color = WarmCream)
                    Switch(checked = special, onCheckedChange = { special = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && name.isNotBlank() && (priceText.toLongOrNull() ?: 0L) > 0L,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CaramelGold,
                    contentColor = DarkEspressoInk
                ),
                onClick = {
                    val price = priceText.toLongOrNull() ?: 0L
                    onSave(category, name, description, imageUrl, price, available, special)
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedCinnamon)
            }
        }
    )
}
