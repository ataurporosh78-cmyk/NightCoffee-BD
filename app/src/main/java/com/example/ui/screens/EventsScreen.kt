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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CoffeeEventDoc
import com.example.model.DhakaArea
import com.example.model.EventCategory
import com.example.model.EventRegistrationDoc
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PlatformTopBar
import com.example.ui.theme.CaramelContainer
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.OpenGreenContainer
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.util.BangladeshTimeUtil
import com.example.util.PlatformLogic
import kotlinx.coroutines.launch

@Composable
fun EventsScreen(
    events: List<CoffeeEventDoc>,
    myRegistrations: Map<String, EventRegistrationDoc>,
    isOfflineCached: Boolean,
    isSignedIn: Boolean,
    canCreateEvents: Boolean,
    isBusy: Boolean,
    onBack: () -> Unit,
    onRequireSignIn: () -> Unit,
    onToggleRegistration: suspend (eventId: String, status: String, note: String) -> Result<Unit>,
    onCreateEvent: suspend (
        title: String,
        category: EventCategory,
        description: String,
        venueName: String,
        area: DhakaArea,
        shopId: String?,
        dateDhaka: String,
        startTime: String,
        endTime: String,
        organizer: String,
        registrationInfo: String
    ) -> Result<Unit>
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf<EventCategory?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val sortedEvents = remember(events, selectedCategory) {
        PlatformLogic.filterAndSortEvents(events, selectedCategory)
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
                    title = stringResource(R.string.events_title),
                    subtitle = if (isOfflineCached) "Offline cached events" else stringResource(R.string.events_subtitle),
                    onBack = onBack,
                    trailingAction = {
                        if (canCreateEvents) {
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CaramelGold,
                                    contentColor = DarkEspressoInk
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("events_create_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Host", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassChip(
                        label = "All Events",
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                    EventCategory.entries.forEach { cat ->
                        LiquidGlassChip(
                            label = cat.label,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("events_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (sortedEvents.isEmpty()) {
                        item(key = "events_empty") {
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Event, contentDescription = null, tint = CaramelGold, modifier = Modifier.size(36.dp))
                                    Text(stringResource(R.string.events_empty), color = ToastedLatte)
                                }
                            }
                        }
                    } else {
                        items(sortedEvents, key = { it.id }) { event ->
                            val reg = myRegistrations[event.id]
                            val isSaved = reg?.status == "Saved"
                            val isRequested = reg?.status == "Requested"
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = CaramelContainer,
                                            border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = event.categoryEnum().label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CaramelGold,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = OpenGreenContainer
                                        ) {
                                            Text(
                                                text = event.statusEnum().label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = OpenGreen,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = WarmCream
                                    )
                                    Text(
                                        text = "${event.eventDateDhaka} · ${event.startTime} - ${event.endTime}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = CaramelGold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = ToastedLatte, modifier = Modifier.size(15.dp))
                                        Text("${event.venueName}, ${event.area}", style = MaterialTheme.typography.bodySmall, color = ToastedLatte)
                                    }
                                    Text(event.description, style = MaterialTheme.typography.bodySmall, color = ToastedLatte)
                                    Text("Organizer: ${event.organizer}", style = MaterialTheme.typography.labelSmall, color = MutedCinnamon)
                                    if (event.registrationInfo.isNotBlank()) {
                                        Text("Details: ${event.registrationInfo}", style = MaterialTheme.typography.labelSmall, color = WarmCream)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                if (!isSignedIn) onRequireSignIn()
                                                else scope.launch {
                                                    val next = if (isSaved) "Remove" else "Saved"
                                                    onToggleRegistration(event.id, next, "")
                                                }
                                            },
                                            enabled = !isBusy,
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, CocoaBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Icon(
                                                if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                                contentDescription = null,
                                                tint = CaramelGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(if (isSaved) " Saved" else " Save", color = WarmCream)
                                        }
                                        Button(
                                            onClick = {
                                                if (!isSignedIn) onRequireSignIn()
                                                else scope.launch {
                                                    val next = if (isRequested) "Remove" else "Requested"
                                                    val res = onToggleRegistration(event.id, next, "RSVP via NightCoffee BD")
                                                    if (res.isSuccess) {
                                                        snackbarHostState.showSnackbar(
                                                            if (next == "Requested") "Registration request sent" else "Registration withdrawn"
                                                        )
                                                    }
                                                }
                                            },
                                            enabled = !isBusy,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isRequested) OpenGreenContainer else CaramelGold,
                                                contentColor = if (isRequested) OpenGreen else DarkEspressoInk
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text(if (isRequested) "Requested" else "Request RSVP", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEventDialog(
            isBusy = isBusy,
            onDismiss = { showCreateDialog = false },
            onSubmit = { title, cat, desc, venue, area, date, start, end, org, regInfo ->
                scope.launch {
                    val res = onCreateEvent(title, cat, desc, venue, area, null, date, start, end, org, regInfo)
                    if (res.isSuccess) {
                        showCreateDialog = false
                        snackbarHostState.showSnackbar("Event published")
                    } else {
                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Failed to create event")
                    }
                }
            }
        )
    }
}

@Composable
private fun CreateEventDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, EventCategory, String, String, DhakaArea, String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EventCategory.LIVE_MUSIC) }
    var description by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var area by remember { mutableStateOf(DhakaArea.BANANI) }
    var dateDhaka by remember { mutableStateOf(BangladeshTimeUtil.todayDateDhaka()) }
    var startTime by remember { mutableStateOf("8:00 PM") }
    var endTime by remember { mutableStateOf("11:00 PM") }
    var organizer by remember { mutableStateOf("") }
    var regInfo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RoastedBeanElevated,
        title = { Text("Publish Coffee Event", color = WarmCream) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EventCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title *") }, singleLine = true)
                OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Venue Name *") }, singleLine = true)
                OutlinedTextField(value = dateDhaka, onValueChange = { dateDhaka = it }, label = { Text("Date YYYY-MM-DD *") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description *") })
                OutlinedTextField(value = organizer, onValueChange = { organizer = it }, label = { Text("Organizer") }, singleLine = true)
                OutlinedTextField(value = regInfo, onValueChange = { regInfo = it }, label = { Text("RSVP / Entry Notes") })
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && title.length >= 3 && venue.length >= 2 && description.length >= 5,
                colors = ButtonDefaults.buttonColors(containerColor = CaramelGold, contentColor = DarkEspressoInk),
                onClick = { onSubmit(title, category, description, venue, area, dateDhaka, startTime, endTime, organizer, regInfo) }
            ) {
                Text("Publish", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MutedCinnamon) }
        }
    )
}
