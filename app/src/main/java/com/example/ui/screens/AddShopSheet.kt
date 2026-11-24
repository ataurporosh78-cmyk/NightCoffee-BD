package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.NewCoffeeShopInput
import com.example.model.DhakaArea
import com.example.model.ShopTimeParser
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.ClosedRed
import com.example.ui.theme.ClosedRedContainer
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.MochaCardSurface
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShopSheet(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (NewCoffeeShopInput) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf(DhakaArea.DHANMONDI) }
    var areaMenuExpanded by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var openTime by remember { mutableStateOf("10:00 AM") }
    var closeTime by remember { mutableStateOf("02:00 AM") }
    var popularItemsText by remember { mutableStateOf("") }
    var priceRange by remember { mutableStateOf("৳250 - ৳550") }
    var phone by remember { mutableStateOf("") }
    var familyFriendly by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MochaCardSurface,
        unfocusedContainerColor = MochaCardSurface,
        focusedBorderColor = CaramelGold,
        unfocusedBorderColor = CocoaBorder,
        focusedTextColor = WarmCream,
        unfocusedTextColor = WarmCream,
        cursorColor = CaramelGold,
        focusedLabelColor = CaramelGold,
        unfocusedLabelColor = ToastedLatte
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepRoastSurface,
        contentColor = WarmCream,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.add_shop_title),
                style = MaterialTheme.typography.headlineSmall,
                color = WarmCream
            )

            validationError?.let { errorMsg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ClosedRedContainer,
                    border = BorderStroke(1.dp, ClosedRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = ClosedRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.add_shop_name_label)) },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_shop_name_input")
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MochaCardSurface,
                    border = BorderStroke(1.dp, CocoaBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { areaMenuExpanded = true }
                        .testTag("add_shop_area_dropdown")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(R.string.add_shop_area_label)}: ${selectedArea.displayName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = WarmCream
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = ToastedLatte
                        )
                    }
                }

                DropdownMenu(
                    expanded = areaMenuExpanded,
                    onDismissRequest = { areaMenuExpanded = false },
                    containerColor = RoastedBeanElevated
                ) {
                    DhakaArea.selectableAreas.forEach { area ->
                        val isSelected = area == selectedArea
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
                                selectedArea = area
                                areaMenuExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.add_shop_address_label)) },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_shop_address_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = openTime,
                    onValueChange = { openTime = it },
                    label = { Text("Open Time *") },
                    placeholder = { Text("10:00 AM / 16:00", color = MutedCinnamon) },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_shop_open_input")
                )

                OutlinedTextField(
                    value = closeTime,
                    onValueChange = { closeTime = it },
                    label = { Text("Close Time *") },
                    placeholder = { Text("02:00 AM / 02:00", color = MutedCinnamon) },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_shop_close_input")
                )
            }

            OutlinedTextField(
                value = popularItemsText,
                onValueChange = { popularItemsText = it },
                label = { Text(stringResource(R.string.add_shop_popular_items_label)) },
                placeholder = { Text("Cappuccino, Spanish Latte, Brownie", color = MutedCinnamon) },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_shop_items_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = priceRange,
                    onValueChange = { priceRange = it },
                    label = { Text(stringResource(R.string.add_shop_price_range_label)) },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_shop_price_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.add_shop_phone_label)) },
                    placeholder = { Text("+88017...", color = MutedCinnamon) },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_shop_phone_input")
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MochaCardSurface,
                border = BorderStroke(1.dp, CocoaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.add_shop_family_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = WarmCream
                    )
                    Switch(
                        checked = familyFriendly,
                        onCheckedChange = { familyFriendly = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EspressoBlack,
                            checkedTrackColor = CaramelGold,
                            uncheckedThumbColor = ToastedLatte,
                            uncheckedTrackColor = RoastedBeanElevated
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CocoaBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ToastedLatte),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.add_shop_cancel))
                }

                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val trimmedAddress = address.trim()
                        val trimmedOpen = openTime.trim()
                        val trimmedClose = closeTime.trim()
                        val trimmedPrice = priceRange.trim()
                        val trimmedPhone = phone.trim()

                        validationError = when {
                            trimmedName.length !in 2..80 -> "Shop name must be 2 to 80 characters."
                            trimmedAddress.length !in 3..200 -> "Full address must be 3 to 200 characters."
                            ShopTimeParser.parseMinuteOfDay(trimmedOpen) == null ->
                                "Enter a valid opening time (e.g. 10:00 AM or 16:00)."
                            ShopTimeParser.parseMinuteOfDay(trimmedClose) == null ->
                                "Enter a valid closing time (e.g. 02:00 AM or 02:00)."
                            trimmedPrice.length !in 1..40 -> "Price range is required (max 40 characters)."
                            trimmedPhone.length > 24 -> "Phone number must be 24 characters or fewer."
                            else -> null
                        }

                        if (validationError == null) {
                            val parsedItems = popularItemsText
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .take(20)
                            onSubmit(
                                NewCoffeeShopInput(
                                    name = trimmedName,
                                    area = selectedArea,
                                    address = trimmedAddress,
                                    openTime = trimmedOpen,
                                    closeTime = trimmedClose,
                                    popularItems = parsedItems,
                                    priceRange = trimmedPrice,
                                    phone = trimmedPhone,
                                    familyFriendly = familyFriendly
                                )
                            )
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CaramelGold,
                        contentColor = DarkEspressoInk
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(50.dp)
                        .testTag("add_shop_submit_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = DarkEspressoInk,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.add_shop_submit),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
