@file:OptIn(ExperimentalStdlibApi::class, ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.SupportTicketDoc
import com.example.model.SupportTicketStatus
import com.example.model.SupportTicketType
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PlatformTopBar
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.DeepRoastSurface
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.FamilyHoney
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.OpenGreenContainer
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.util.BangladeshTimeUtil
import kotlinx.coroutines.launch

@Composable
fun HelpSupportScreen(
    preselectedShopId: String?,
    preselectedShopName: String?,
    myTickets: List<SupportTicketDoc>,
    isSignedIn: Boolean,
    isBusy: Boolean,
    onBack: () -> Unit,
    onRequireSignIn: () -> Unit,
    onSubmitTicket: suspend (
        type: SupportTicketType,
        targetId: String,
        targetName: String,
        subject: String,
        description: String,
        contactInfo: String
    ) -> Result<Boolean>
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val faqs = remember {
        listOf(
            "How are live shop statuses verified?" to "Scheduled status follows opening/closing hours (including past-midnight hours in Dhaka). Customer reports appear clearly as unverified until confirmed by cafe staff or an admin.",
            "How does Live Crowd Level stay accurate?" to "Crowd reports automatically expire after 2 hours so stale crowd counts never show as live. Repeated submissions from the same device are rate-limited.",
            "Do table reservations charge online payment?" to "No. NightCoffee BD sends reservation requests directly to cafe staff with zero online payments. Bookings remain Pending until confirmed by staff.",
            "How do cafe owners earn a Verified badge?" to "Cafe representatives submit a verification request from a cafe's Details screen. Only platform administrators can approve and grant the Verified badge."
        )
    }

    var expandedFaqIndex by remember { mutableIntStateOf(-1) }
    var ticketType by remember(preselectedShopId) {
        mutableStateOf(
            if (!preselectedShopId.isNullOrBlank()) SupportTicketType.INCORRECT_SHOP_INFO else SupportTicketType.GENERAL_SUPPORT
        )
    }
    var subject by remember(preselectedShopName) {
        mutableStateOf(preselectedShopName?.let { "Issue with $it" }.orEmpty())
    }
    var description by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }

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
                    title = stringResource(R.string.support_title),
                    subtitle = stringResource(R.string.support_subtitle),
                    onBack = onBack
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("support_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "faq_card") {
                        LiquidGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Frequently Asked Questions", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                                faqs.forEachIndexed { idx, (q, a) ->
                                    val expanded = expandedFaqIndex == idx
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = DeepRoastSurface,
                                        border = BorderStroke(1.dp, CocoaBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { expandedFaqIndex = if (expanded) -1 else idx }
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = q,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = CaramelGold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = ToastedLatte
                                                )
                                            }
                                            AnimatedVisibility(visible = expanded) {
                                                Text(
                                                    text = a,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ToastedLatte,
                                                    modifier = Modifier.padding(top = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "ticket_form") {
                        LiquidGlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            accentGlow = true
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = CaramelGold)
                                    Text("Submit a Report or Support Ticket", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SupportTicketType.entries.forEach { t ->
                                        FilterChip(
                                            selected = ticketType == t,
                                            onClick = { ticketType = t },
                                            label = { Text(t.label) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = subject,
                                    onValueChange = { if (it.length <= 120) subject = it },
                                    label = { Text("Subject *") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DeepRoastSurface,
                                        unfocusedContainerColor = DeepRoastSurface,
                                        focusedTextColor = WarmCream,
                                        unfocusedTextColor = WarmCream
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("support_subject_input")
                                )

                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { if (it.length <= 1500) description = it },
                                    label = { Text("Details (shop name, wrong prices/hours, or issue) *") },
                                    minLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DeepRoastSurface,
                                        unfocusedContainerColor = DeepRoastSurface,
                                        focusedTextColor = WarmCream,
                                        unfocusedTextColor = WarmCream
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("support_description_input")
                                )

                                OutlinedTextField(
                                    value = contactInfo,
                                    onValueChange = { if (it.length <= 120) contactInfo = it },
                                    label = { Text("Optional Contact Email or Phone") },
                                    singleLine = true,
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
                                            scope.launch {
                                                val res = onSubmitTicket(
                                                    ticketType,
                                                    preselectedShopId.orEmpty(),
                                                    preselectedShopName.orEmpty(),
                                                    subject,
                                                    description,
                                                    contactInfo
                                                )
                                                res.fold(
                                                    onSuccess = { queuedOffline ->
                                                        subject = ""
                                                        description = ""
                                                        snackbarHostState.showSnackbar(
                                                            if (queuedOffline) "Offline: ticket queued & will sync automatically"
                                                            else "Support ticket submitted"
                                                        )
                                                    },
                                                    onFailure = {
                                                        snackbarHostState.showSnackbar(it.message ?: "Failed to submit ticket")
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CaramelGold,
                                        contentColor = DarkEspressoInk
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("support_submit_button")
                                ) {
                                    Text(
                                        text = if (isSignedIn) stringResource(R.string.support_submit_ticket) else "Sign in to Submit Ticket",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item(key = "my_tickets_header") {
                        Text("My Support Tickets (${myTickets.size})", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                    }

                    if (!isSignedIn || myTickets.isEmpty()) {
                        item(key = "my_tickets_empty") {
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text(
                                    text = if (!isSignedIn) "Sign in with Google to view your private support ticket history." else "No support tickets submitted yet.",
                                    color = ToastedLatte,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(myTickets, key = { it.id }) { ticket ->
                            val status = ticket.statusEnum()
                            val badgeColor = when (status) {
                                SupportTicketStatus.RESOLVED, SupportTicketStatus.CLOSED -> OpenGreen
                                SupportTicketStatus.IN_PROGRESS -> CaramelGold
                                SupportTicketStatus.OPEN -> FamilyHoney
                            }
                            LiquidGlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(ticket.typeEnum().label, style = MaterialTheme.typography.labelSmall, color = CaramelGold)
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = OpenGreenContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = status.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Text(ticket.subject, style = MaterialTheme.typography.titleSmall, color = WarmCream, fontWeight = FontWeight.Bold)
                                    Text(ticket.description, style = MaterialTheme.typography.bodySmall, color = ToastedLatte)
                                    if (ticket.adminResponse.isNotBlank()) {
                                        Text("Response: ${ticket.adminResponse}", style = MaterialTheme.typography.bodySmall, color = OpenGreen)
                                    }
                                    Text(
                                        text = BangladeshTimeUtil.formatRelativeTime(ticket.createdAt?.toDate()?.time),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedCinnamon
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.navigationBarsPadding()) }
                }
            }
        }
    }
}
