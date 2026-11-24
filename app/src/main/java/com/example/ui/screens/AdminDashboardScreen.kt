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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.CrowdReportDoc
import com.example.model.NotificationCategory
import com.example.model.PhotoReviewDoc
import com.example.model.ReportVerificationState
import com.example.model.ReservationDoc
import com.example.model.ReservationStatus
import com.example.model.ShopStatusReportDoc
import com.example.model.SupportTicketDoc
import com.example.model.SupportTicketStatus
import com.example.model.UserRoleState
import com.example.model.VerificationRequestDoc
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PlatformTopBar
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.ClosedRed
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.EspressoRich
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import kotlinx.coroutines.launch

private enum class AdminSection(val label: String) {
    VERIFICATIONS("Verifications"),
    REPORTS("Status & Crowd"),
    REVIEWS("Reviews"),
    TICKETS("Support Tickets"),
    RESERVATIONS("Bookings"),
    BROADCAST("Broadcast")
}

@Composable
fun AdminDashboardScreen(
    userRole: UserRoleState,
    verificationRequests: List<VerificationRequestDoc>,
    statusReports: List<ShopStatusReportDoc>,
    crowdReports: List<CrowdReportDoc>,
    photoReviews: List<PhotoReviewDoc>,
    supportTickets: List<SupportTicketDoc>,
    reservations: List<ReservationDoc>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onReviewVerification: suspend (VerificationRequestDoc, Boolean) -> Result<Unit>,
    onVerifyStatusReport: suspend (String, ReportVerificationState) -> Result<Unit>,
    onDismissCrowdReport: suspend (String) -> Result<Unit>,
    onModerateReview: suspend (String, String) -> Result<Unit>,
    onUpdateTicket: suspend (String, SupportTicketStatus, String) -> Result<Unit>,
    onUpdateReservation: suspend (String, ReservationStatus, String) -> Result<Unit>,
    onSendAnnouncement: suspend (NotificationCategory, String, String, String, String) -> Result<Unit>
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var activeSection by remember { mutableStateOf(AdminSection.VERIFICATIONS) }

    var announceCategory by remember { mutableStateOf(NotificationCategory.ANNOUNCEMENT) }
    var announceTitle by remember { mutableStateOf("") }
    var announceBody by remember { mutableStateOf("") }

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
                    .widthIn(max = 760.dp)
            ) {
                PlatformTopBar(
                    title = stringResource(R.string.admin_title),
                    subtitle = if (userRole.isAdmin) "Platform Admin" else "Shop Staff (${userRole.staffShopIds.size} shop)",
                    onBack = onBack
                )

                if (!userRole.hasAnyManagementAccess) {
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
                            Text(
                                text = "Access restricted to verified platform administrators and assigned coffee shop staff.",
                                color = ClosedRed,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                    return@Column
                }

                val allowedSections = remember(userRole) {
                    if (userRole.isAdmin) {
                        AdminSection.entries
                    } else {
                        listOf(AdminSection.REPORTS, AdminSection.RESERVATIONS)
                    }
                }
                if (activeSection !in allowedSections) {
                    activeSection = allowedSections.first()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allowedSections.forEach { sec ->
                        LiquidGlassChip(
                            label = sec.label,
                            selected = activeSection == sec,
                            onClick = { activeSection = sec },
                            modifier = Modifier.testTag("admin_tab_${sec.name}")
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("admin_console_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (activeSection) {
                        AdminSection.VERIFICATIONS -> {
                            val pending = verificationRequests.filter { it.status == "Pending" }
                            if (pending.isEmpty()) {
                                item { AdminEmptyCard("No pending verification requests.") }
                            } else {
                                items(pending, key = { it.id }) { req ->
                                    LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(req.shopName, style = MaterialTheme.typography.titleMedium, color = WarmCream)
                                            Text("Owner: ${req.businessOwnerName} · ${req.businessPhone}", color = CaramelGold, style = MaterialTheme.typography.bodySmall)
                                            Text(req.registrationNote, color = ToastedLatte, style = MaterialTheme.typography.bodySmall)
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        scope.launch {
                                                            val res = onReviewVerification(req, false)
                                                            snackbarHostState.showSnackbar(if (res.isSuccess) "Rejected request" else res.exceptionOrNull()?.message ?: "Failed")
                                                        }
                                                    },
                                                    enabled = !isBusy,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) { Text("Reject", color = ClosedRed) }
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            val res = onReviewVerification(req, true)
                                                            snackbarHostState.showSnackbar(if (res.isSuccess) "Verified & granted staff role" else res.exceptionOrNull()?.message ?: "Failed")
                                                        }
                                                    },
                                                    enabled = !isBusy,
                                                    colors = ButtonDefaults.buttonColors(containerColor = OpenGreen, contentColor = DarkEspressoInk),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) { Text("Approve", fontWeight = FontWeight.Bold) }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AdminSection.REPORTS -> {
                            val scopedStatus = statusReports.filter { userRole.canManageShop(it.shopId) }.take(25)
                            if (scopedStatus.isEmpty() && crowdReports.isEmpty()) {
                                item { AdminEmptyCard("No status or crowd reports to review.") }
                            } else {
                                items(scopedStatus, key = { "st_${it.id}" }) { rep ->
                                    LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("Shop ID: ${rep.shopId}", style = MaterialTheme.typography.labelSmall, color = CaramelGold)
                                            Text("${rep.statusEnum().label} (${rep.verificationEnum().label})", color = WarmCream, fontWeight = FontWeight.Bold)
                                            if (rep.note.isNotBlank()) Text(rep.note, color = ToastedLatte, style = MaterialTheme.typography.bodySmall)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { scope.launch { onVerifyStatusReport(rep.id, ReportVerificationState.DISMISSED) } },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) { Text("Dismiss", color = ClosedRed) }
                                                Button(
                                                    onClick = { scope.launch { onVerifyStatusReport(rep.id, ReportVerificationState.VERIFIED) } },
                                                    colors = ButtonDefaults.buttonColors(containerColor = OpenGreen, contentColor = DarkEspressoInk),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) { Text("Verify", fontWeight = FontWeight.Bold) }
                                            }
                                        }
                                    }
                                }
                                if (userRole.isAdmin) {
                                    items(crowdReports.take(15), key = { "cr_${it.id}" }) { cr ->
                                        LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Crowd: ${cr.levelEnum().label} · ${cr.shopId}", color = WarmCream, fontWeight = FontWeight.SemiBold)
                                                    if (cr.note.isNotBlank()) Text(cr.note, color = ToastedLatte, style = MaterialTheme.typography.bodySmall)
                                                }
                                                OutlinedButton(
                                                    onClick = { scope.launch { onDismissCrowdReport(cr.id) } },
                                                    modifier = Modifier.height(48.dp)
                                                ) { Text("Dismiss", color = ClosedRed) }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AdminSection.REVIEWS -> {
                            val flaggedOrRecent = photoReviews.sortedByDescending { it.reportCount }.take(25)
                            if (flaggedOrRecent.isEmpty()) {
                                item { AdminEmptyCard("No photo reviews pending moderation.") }
                            } else {
                                items(flaggedOrRecent, key = { it.id }) { rev ->
                                    LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("${rev.authorName} · Shop: ${rev.shopId} · Reports: ${rev.reportCount}", color = CaramelGold, style = MaterialTheme.typography.labelMedium)
                                            Text(rev.comment, color = WarmCream, style = MaterialTheme.typography.bodySmall)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { scope.launch { onModerateReview(rev.id, "hidden") } },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) { Text("Hide", color = ClosedRed) }
                                                Button(
                                                    onClick = { scope.launch { onModerateReview(rev.id, "public") } },
                                                    colors = ButtonDefaults.buttonColors(containerColor = OpenGreen, contentColor = DarkEspressoInk),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) { Text("Keep Public", fontWeight = FontWeight.Bold) }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AdminSection.TICKETS -> {
                            if (supportTickets.isEmpty()) {
                                item { AdminEmptyCard("No support tickets found.") }
                            } else {
                                items(supportTickets, key = { it.id }) { ticket ->
                                    LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("${ticket.typeEnum().label} · ${ticket.statusEnum().label}", color = CaramelGold, style = MaterialTheme.typography.labelSmall)
                                            Text(ticket.subject, color = WarmCream, fontWeight = FontWeight.Bold)
                                            Text(ticket.description, color = ToastedLatte, style = MaterialTheme.typography.bodySmall)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        scope.launch {
                                                            onUpdateTicket(ticket.id, SupportTicketStatus.IN_PROGRESS, "Reviewing your report.")
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) { Text("In Progress") }
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            onUpdateTicket(ticket.id, SupportTicketStatus.RESOLVED, "Updated and resolved by NightCoffee BD Admin.")
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = OpenGreen, contentColor = DarkEspressoInk),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) { Text("Resolve", fontWeight = FontWeight.Bold) }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AdminSection.RESERVATIONS -> {
                            val scoped = reservations.filter { userRole.canManageShop(it.shopId) }
                            if (scoped.isEmpty()) {
                                item { AdminEmptyCard("No table reservations to manage.") }
                            } else {
                                items(scoped, key = { it.id }) { booking ->
                                    LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("${booking.shopName} · ${booking.status}", color = CaramelGold, fontWeight = FontWeight.Bold)
                                            Text("${booking.dateDhaka} at ${booking.timeSlot} · ${booking.guestName} (${booking.guestCount})", color = WarmCream)
                                            if (booking.statusEnum() == ReservationStatus.PENDING) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(
                                                        onClick = { scope.launch { onUpdateReservation(booking.id, ReservationStatus.REJECTED, "Slot full") } },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(48.dp)
                                                    ) { Text("Reject", color = ClosedRed) }
                                                    Button(
                                                        onClick = { scope.launch { onUpdateReservation(booking.id, ReservationStatus.CONFIRMED, "Confirmed") } },
                                                        colors = ButtonDefaults.buttonColors(containerColor = OpenGreen, contentColor = DarkEspressoInk),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(48.dp)
                                                    ) { Text("Confirm", fontWeight = FontWeight.Bold) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AdminSection.BROADCAST -> {
                            item {
                                LiquidGlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Send Topic Broadcast", style = MaterialTheme.typography.titleMedium, color = WarmCream)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            NotificationCategory.entries.forEach { cat ->
                                                FilterChip(
                                                    selected = announceCategory == cat,
                                                    onClick = { announceCategory = cat },
                                                    label = { Text(cat.label) }
                                                )
                                            }
                                        }
                                        OutlinedTextField(
                                            value = announceTitle,
                                            onValueChange = { if (it.length <= 100) announceTitle = it },
                                            label = { Text("Notification Title *") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = announceBody,
                                            onValueChange = { if (it.length <= 500) announceBody = it },
                                            label = { Text("Notification Message *") },
                                            minLines = 2,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val res = onSendAnnouncement(announceCategory, announceTitle, announceBody, "notifications", "")
                                                    if (res.isSuccess) {
                                                        announceTitle = ""
                                                        announceBody = ""
                                                        snackbarHostState.showSnackbar("Broadcast sent")
                                                    } else {
                                                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Failed")
                                                    }
                                                }
                                            },
                                            enabled = !isBusy && announceTitle.length >= 2 && announceBody.length >= 2,
                                            colors = ButtonDefaults.buttonColors(containerColor = CaramelGold, contentColor = DarkEspressoInk),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                        ) {
                                            Text("Publish Alert", fontWeight = FontWeight.Bold)
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
}

@Composable
private fun AdminEmptyCard(message: String) {
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MutedCinnamon,
            modifier = Modifier.padding(18.dp)
        )
    }
}
