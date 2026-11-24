@file:OptIn(ExperimentalStdlibApi::class, ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CoffeeShop
import com.example.model.ReservationDoc
import com.example.model.ReservationStatus
import com.example.model.UserRoleState
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PlatformTopBar
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.ClosedRed
import com.example.ui.theme.ClosedRedContainer
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.FamilyBadgeContainer
import com.example.ui.theme.FamilyHoney
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.OpenGreenContainer
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.util.BangladeshTimeUtil
import kotlinx.coroutines.launch

@Composable
fun ReservationsScreen(
    preselectedShop: CoffeeShop?,
    allShops: List<CoffeeShop>,
    myReservations: List<ReservationDoc>,
    manageableReservations: List<ReservationDoc>,
    userRole: UserRoleState,
    defaultGuestName: String,
    isSignedIn: Boolean,
    isBusy: Boolean,
    onBack: () -> Unit,
    onRequireSignIn: () -> Unit,
    onCreateReservation: suspend (
        shopId: String,
        shopName: String,
        guestName: String,
        contactPhone: String,
        dateDhaka: String,
        timeSlot: String,
        guestCount: Int,
        specialRequest: String
    ) -> Result<Unit>,
    onCancelReservation: suspend (String) -> Result<Unit>,
    onStaffUpdateStatus: suspend (reservationId: String, status: ReservationStatus, note: String) -> Result<Unit>
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val upcomingDates = remember { BangladeshTimeUtil.upcomingDatesDhaka(7) }
    val timeSlots = remember {
        listOf("6:00 PM", "7:30 PM", "9:00 PM", "10:30 PM", "11:45 PM", "1:00 AM")
    }

    var selectedShop by remember(preselectedShop, allShops) {
        mutableStateOf(preselectedShop ?: allShops.firstOrNull())
    }
    var shopMenuExpanded by remember { mutableStateOf(false) }
    var guestName by remember(defaultGuestName) { mutableStateOf(defaultGuestName) }
    var contactPhone by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(upcomingDates.first()) }
    var selectedTimeSlot by remember { mutableStateOf(timeSlots[1]) }
    var guestCount by remember { mutableIntStateOf(2) }
    var specialRequest by remember { mutableStateOf("") }
    var showManageTab by remember { mutableStateOf(false) }

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
                    title = stringResource(R.string.reservations_title),
                    subtitle = stringResource(R.string.reservations_subtitle),
                    onBack = onBack
                )

                if (userRole.hasAnyManagementAccess) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidGlassChip(
                            label = "My Bookings (${myReservations.size})",
                            selected = !showManageTab,
                            onClick = { showManageTab = false }
                        )
                        LiquidGlassChip(
                            label = "Manage Requests (${manageableReservations.size})",
                            selected = showManageTab,
                            onClick = { showManageTab = true }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("reservations_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!showManageTab) {
                        item(key = "reservation_form") {
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                accentGlow = true
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Request a Table",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = WarmCream
                                    )
                                    Text(
                                        text = stringResource(R.string.reservations_no_payment_note),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FamilyHoney
                                    )

                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = DeepRoastSurface,
                                            border = BorderStroke(1.dp, CocoaBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { shopMenuExpanded = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = selectedShop?.let { "${it.name} (${it.areaLabel})" } ?: "Choose a cafe",
                                                    color = WarmCream,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text("Change", color = CaramelGold, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = shopMenuExpanded,
                                            onDismissRequest = { shopMenuExpanded = false },
                                            containerColor = RoastedBeanElevated
                                        ) {
                                            allShops.take(30).forEach { candidate ->
                                                DropdownMenuItem(
                                                    text = { Text("${candidate.name} · ${candidate.areaLabel}", color = WarmCream) },
                                                    onClick = {
                                                        selectedShop = candidate
                                                        shopMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Text("Date (Dhaka BST)", style = MaterialTheme.typography.labelMedium, color = ToastedLatte)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        upcomingDates.forEach { date ->
                                            LiquidGlassChip(
                                                label = date,
                                                selected = selectedDate == date,
                                                leadingIcon = Icons.Rounded.CalendarToday,
                                                onClick = { selectedDate = date }
                                            )
                                        }
                                    }

                                    Text("Time Slot", style = MaterialTheme.typography.labelMedium, color = ToastedLatte)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        timeSlots.forEach { slot ->
                                            LiquidGlassChip(
                                                label = slot,
                                                selected = selectedTimeSlot == slot,
                                                onClick = { selectedTimeSlot = slot }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Guests", style = MaterialTheme.typography.titleSmall, color = WarmCream)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { if (guestCount > 1) guestCount-- },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease guests", tint = CaramelGold)
                                            }
                                            Text(
                                                text = "$guestCount",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = WarmCream,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = { if (guestCount < 20) guestCount++ },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Rounded.Add, contentDescription = "Increase guests", tint = CaramelGold)
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = guestName,
                                        onValueChange = { if (it.length <= 80) guestName = it },
                                        label = { Text("Your Name *") },
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
                                        value = contactPhone,
                                        onValueChange = { if (it.length <= 24) contactPhone = it },
                                        label = { Text("Contact Phone (e.g. 017xxxxxxxx) *") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DeepRoastSurface,
                                            unfocusedContainerColor = DeepRoastSurface,
                                            focusedTextColor = WarmCream,
                                            unfocusedTextColor = WarmCream
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reservation_phone_input")
                                    )

                                    OutlinedTextField(
                                        value = specialRequest,
                                        onValueChange = { if (it.length <= 300) specialRequest = it },
                                        label = { Text("Seating or Special Request (optional)") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DeepRoastSurface,
                                            unfocusedContainerColor = DeepRoastSurface,
                                            focusedTextColor = WarmCream,
                                            unfocusedTextColor = WarmCream
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            if (!isSignedIn) {
                                                onRequireSignIn()
                                            } else {
                                                val target = selectedShop ?: return@Button
                                                scope.launch {
                                                    val res = onCreateReservation(
                                                        target.id,
                                                        target.name,
                                                        guestName,
                                                        contactPhone,
                                                        selectedDate,
                                                        selectedTimeSlot,
                                                        guestCount,
                                                        specialRequest
                                                    )
                                                    if (res.isSuccess) {
                                                        specialRequest = ""
                                                        snackbarHostState.showSnackbar("Reservation request sent (Pending staff confirmation)")
                                                    } else {
                                                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Request failed")
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isBusy && selectedShop != null,
                                        shape = RoundedCornerShape(18.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CaramelGold,
                                            contentColor = DarkEspressoInk
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("submit_reservation_button")
                                    ) {
                                        Text(
                                            text = if (isSignedIn) stringResource(R.string.reservations_book_button) else "Sign in to Request Table",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "my_bookings_header") {
                            Text(
                                text = "My Reservations",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmCream
                            )
                        }

                        if (myReservations.isEmpty()) {
                            item(key = "my_bookings_empty") {
                                LiquidGlassSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.EventSeat, contentDescription = null, tint = CaramelGold)
                                        Text(stringResource(R.string.reservations_empty), color = ToastedLatte)
                                    }
                                }
                            }
                        } else {
                            items(myReservations, key = { it.id }) { booking ->
                                ReservationCard(
                                    booking = booking,
                                    canManage = false,
                                    isBusy = isBusy,
                                    onCancel = {
                                        scope.launch {
                                            val res = onCancelReservation(booking.id)
                                            snackbarHostState.showSnackbar(
                                                res.fold(
                                                    onSuccess = { "Booking cancelled" },
                                                    onFailure = { it.message ?: "Unable to cancel" }
                                                )
                                            )
                                        }
                                    },
                                    onUpdateStatus = { _, _ -> }
                                )
                            }
                        }
                    } else {
                        if (manageableReservations.isEmpty()) {
                            item {
                                LiquidGlassSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp)
                                ) {
                                    Text(
                                        text = "No incoming bookings for your managed cafes yet.",
                                        color = ToastedLatte,
                                        modifier = Modifier.padding(18.dp)
                                    )
                                }
                            }
                        } else {
                            items(manageableReservations, key = { "mgr_${it.id}" }) { booking ->
                                ReservationCard(
                                    booking = booking,
                                    canManage = userRole.canManageShop(booking.shopId),
                                    isBusy = isBusy,
                                    onCancel = {},
                                    onUpdateStatus = { nextStatus, note ->
                                        scope.launch {
                                            val res = onStaffUpdateStatus(booking.id, nextStatus, note)
                                            snackbarHostState.showSnackbar(
                                                res.fold(
                                                    onSuccess = { "Updated booking to ${nextStatus.label}" },
                                                    onFailure = { it.message ?: "Update failed" }
                                                )
                                            )
                                        }
                                    }
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

@Composable
private fun ReservationCard(
    booking: ReservationDoc,
    canManage: Boolean,
    isBusy: Boolean,
    onCancel: () -> Unit,
    onUpdateStatus: (ReservationStatus, String) -> Unit
) {
    val status = booking.statusEnum()
    val statusColor = when (status) {
        ReservationStatus.CONFIRMED -> OpenGreen
        ReservationStatus.PENDING -> FamilyHoney
        ReservationStatus.COMPLETED -> CaramelGold
        ReservationStatus.REJECTED, ReservationStatus.CANCELLED -> ClosedRed
    }
    val statusBg = when (status) {
        ReservationStatus.CONFIRMED -> OpenGreenContainer
        ReservationStatus.PENDING -> FamilyBadgeContainer
        ReservationStatus.COMPLETED -> EspressoRich.copy(alpha = 0.55f)
        ReservationStatus.REJECTED, ReservationStatus.CANCELLED -> ClosedRedContainer
    }

    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.shopName,
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmCream,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "${booking.dateDhaka} at ${booking.timeSlot} (BST) · ${booking.guestCount} guest(s)",
                style = MaterialTheme.typography.bodyMedium,
                color = CaramelGold
            )
            Text(
                text = "Guest: ${booking.guestName} · ${booking.contactPhone}",
                style = MaterialTheme.typography.bodySmall,
                color = ToastedLatte
            )
            if (booking.specialRequest.isNotBlank()) {
                Text(
                    text = "Request: ${booking.specialRequest}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ToastedLatte
                )
            }
            if (booking.staffNote.isNotBlank()) {
                Text(
                    text = "Cafe Note: ${booking.staffNote}",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmCream
                )
            }

            if (!canManage && (status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED)) {
                TextButton(
                    onClick = onCancel,
                    enabled = !isBusy,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cancel Reservation", color = ClosedRed)
                }
            }

            if (canManage && status == ReservationStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onUpdateStatus(ReservationStatus.REJECTED, "Unable to accommodate slot") },
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Reject", color = ClosedRed)
                    }
                    Button(
                        onClick = { onUpdateStatus(ReservationStatus.CONFIRMED, "Table reserved for your party") },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = OpenGreen, contentColor = DarkEspressoInk),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Confirm", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (canManage && status == ReservationStatus.CONFIRMED) {
                Button(
                    onClick = { onUpdateStatus(ReservationStatus.COMPLETED, "Completed visit") },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = CaramelGold, contentColor = DarkEspressoInk),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Mark Completed", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
