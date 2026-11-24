@file:OptIn(ExperimentalStdlibApi::class, ExperimentalLayoutApi::class)

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AggregatedCrowdStatus
import com.example.model.CrowdLevel
import com.example.model.LiveShopStatus
import com.example.model.ReportVerificationState
import com.example.model.ReportedStatusType
import com.example.model.ResolvedShopStatus
import com.example.ui.theme.CaramelGold
import com.example.ui.theme.ClosedRed
import com.example.ui.theme.CocoaBorder
import com.example.ui.theme.DarkEspressoInk
import com.example.ui.theme.EspressoBlack
import com.example.ui.theme.FamilyHoney
import com.example.ui.theme.MutedCinnamon
import com.example.ui.theme.OpenGreen
import com.example.ui.theme.RoastedBeanElevated
import com.example.ui.theme.ToastedLatte
import com.example.ui.theme.WarmCream
import com.example.util.BangladeshTimeUtil

@Composable
fun LiveShopStatusCard(
    status: ResolvedShopStatus,
    isVerifiedBusiness: Boolean,
    canManageShop: Boolean,
    onReportStatusClick: () -> Unit,
    onOfficialUpdateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = CaramelGold, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.live_status_section_title), style = MaterialTheme.typography.titleMedium, color = WarmCream)
                }
                if (isVerifiedBusiness) {
                    VerifiedShopBadge()
                }
            }

            // 1. Scheduled Opening Status (preserved past-midnight math)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.status_scheduled_label), style = MaterialTheme.typography.bodySmall, color = ToastedLatte)
                LiveStatusChipBadge(
                    label = status.scheduledLabel,
                    isPositive = status.scheduledOpen,
                    isWarning = status.scheduledLabel == "Closing Soon"
                )
            }

            HorizontalDivider(color = CocoaBorder)

            // 2. Verified Live Status (admin or official staff override only)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.status_verified_label), style = MaterialTheme.typography.bodySmall, color = ToastedLatte)
                if (status.verifiedStatus != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        LiveStatusChipBadge(
                            label = status.verifiedStatus.label,
                            isPositive = status.verifiedStatus.isPositiveOpen(),
                            isWarning = status.verifiedStatus == LiveShopStatus.CLOSING_SOON
                        )
                        Text(
                            text = "Updated ${BangladeshTimeUtil.formatRelativeTime(status.verifiedUpdatedAtMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedCinnamon
                        )
                    }
                } else {
                    Text("Not officially overridden", style = MaterialTheme.typography.labelSmall, color = MutedCinnamon)
                }
            }

            HorizontalDivider(color = CocoaBorder)

            // 3. Last Reported Status (explicitly marked unverified or stale when applicable)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.status_reported_label), style = MaterialTheme.typography.bodySmall, color = ToastedLatte)
                    val rep = status.lastReportedStatus
                    if (rep == null) {
                        Text("No customer reports yet", style = MaterialTheme.typography.labelSmall, color = MutedCinnamon)
                    } else {
                        Text(
                            text = "${rep.statusEnum().label} · ${BangladeshTimeUtil.formatRelativeTime(rep.createdAt?.toDate()?.time)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (status.isLastReportStale) MutedCinnamon else FamilyHoney,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                status.lastReportedStatus?.let { rep ->
                    if (rep.verificationEnum() == ReportVerificationState.UNVERIFIED) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = FamilyHoney, modifier = Modifier.size(14.dp))
                            Text(
                                text = stringResource(R.string.status_unverified_notice),
                                style = MaterialTheme.typography.labelSmall,
                                color = FamilyHoney
                            )
                        }
                    }
                    if (status.isLastReportStale) {
                        Text(
                            text = stringResource(R.string.status_stale_notice),
                            style = MaterialTheme.typography.labelSmall,
                            color = ClosedRed
                        )
                    }
                    if (rep.note.isNotBlank()) {
                        Text(
                            text = "\"${rep.note}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = ToastedLatte
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReportStatusClick,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("report_live_status_button")
                ) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, tint = CaramelGold, modifier = Modifier.size(18.dp))
                    Text(" Report Status", color = CaramelGold, fontWeight = FontWeight.SemiBold)
                }

                if (canManageShop) {
                    Button(
                        onClick = onOfficialUpdateClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CaramelGold, contentColor = DarkEspressoInk),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("official_status_update_button")
                    ) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text(" Official Update", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveCrowdStatusCard(
    crowd: AggregatedCrowdStatus,
    canFlag: Boolean,
    onReportCrowdClick: () -> Unit,
    onFlagIncorrectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, tint = CaramelGold, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.crowd_section_title), style = MaterialTheme.typography.titleMedium, color = WarmCream)
                }
                if (crowd.level != null && crowd.isSufficientlyRecent) {
                    CrowdLevelPill(level = crowd.level)
                }
            }

            if (crowd.level != null && crowd.isSufficientlyRecent) {
                Text(
                    text = "Based on ${crowd.activeReportCount} active report(s) · Updated ${BangladeshTimeUtil.formatRelativeTime(crowd.latestUpdatedMillis)} (auto-expires after 2h).",
                    style = MaterialTheme.typography.bodySmall,
                    color = ToastedLatte
                )
            } else {
                Text(
                    text = stringResource(R.string.crowd_no_recent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedCinnamon
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReportCrowdClick,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CaramelGold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("report_crowd_button")
                ) {
                    Text(stringResource(R.string.crowd_report_button), color = CaramelGold, fontWeight = FontWeight.SemiBold)
                }
                if (canFlag) {
                    OutlinedButton(
                        onClick = onFlagIncorrectClick,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CocoaBorder),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("flag_crowd_button")
                    ) {
                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = ToastedLatte, modifier = Modifier.size(16.dp))
                        Text(" ${stringResource(R.string.crowd_flag_incorrect)}", color = ToastedLatte)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusReportDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ReportedStatusType, String) -> Unit
) {
    var selected by remember { mutableStateOf(ReportedStatusType.OPEN_NOW) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RoastedBeanElevated,
        title = { Text("Report Live Status or Hours", color = WarmCream) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Customer reports appear as unverified until confirmed by cafe staff or admin.", style = MaterialTheme.typography.labelSmall, color = FamilyHoney)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReportedStatusType.entries.forEach { st ->
                        FilterChip(
                            selected = selected == st,
                            onClick = { selected = st },
                            label = { Text(st.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 280) note = it },
                    label = { Text("Optional note (e.g. closing early today)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = CaramelGold, contentColor = DarkEspressoInk),
                onClick = { onSubmit(selected, note) }
            ) { Text("Submit Report", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedCinnamon) } }
    )
}

@Composable
fun OfficialStatusDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (LiveShopStatus) -> Unit
) {
    var selected by remember { mutableStateOf(LiveShopStatus.OPEN_NOW) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RoastedBeanElevated,
        title = { Text("Official Live Status Override", color = WarmCream) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LiveShopStatus.entries.forEach { st ->
                    FilterChip(
                        selected = selected == st,
                        onClick = { selected = st },
                        label = { Text(st.label) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = OpenGreen, contentColor = DarkEspressoInk),
                onClick = { onSubmit(selected) }
            ) { Text("Publish Official Status", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedCinnamon) } }
    )
}

@Composable
fun CrowdReportDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CrowdLevel, String) -> Unit
) {
    var selected by remember { mutableStateOf(CrowdLevel.MODERATE) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RoastedBeanElevated,
        title = { Text("Report Current Crowd Level", color = WarmCream) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CrowdLevel.entries.forEach { lvl ->
                        FilterChip(
                            selected = selected == lvl,
                            onClick = { selected = lvl },
                            label = { Text(lvl.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 200) note = it },
                    label = { Text("Optional seating note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = CaramelGold, contentColor = DarkEspressoInk),
                onClick = { onSubmit(selected, note) }
            ) { Text("Submit Crowd", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedCinnamon) } }
    )
}

@Composable
fun BusinessVerificationDialog(
    shopName: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ownerName: String, phone: String, note: String) -> Unit
) {
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RoastedBeanElevated,
        title = { Text("Request Verified Shop Badge", color = WarmCream) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Claiming: $shopName. Badges are granted only after administrator review.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ToastedLatte
                )
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { if (it.length <= 80) ownerName = it },
                    label = { Text("Owner / Manager Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 24) phone = it },
                    label = { Text("Official Business Phone *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 500) note = it },
                    label = { Text("Verification Proof / Trade License Note *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && ownerName.trim().length >= 2 && phone.trim().length >= 6 && note.trim().length >= 5,
                colors = ButtonDefaults.buttonColors(containerColor = CaramelGold, contentColor = DarkEspressoInk),
                onClick = { onSubmit(ownerName, phone, note) }
            ) { Text("Submit Request", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedCinnamon) } }
    )
}
