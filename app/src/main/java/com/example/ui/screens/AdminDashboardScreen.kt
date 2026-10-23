package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SlotStatus
import com.example.data.model.UserRole
import com.example.ui.components.GoldSquareLogo
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.GoldenSquareUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    state: GoldenSquareUiState,
    onBack: () -> Unit,
    onRefundSlot: (slotId: String, reason: String) -> Unit,
    onModerateSlot: (slotId: String, status: SlotStatus, reason: String) -> Unit,
    onToggleSuspension: (teamId: String, reason: String) -> Unit,
    onBatchProcessRewards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.US)
    var selectedTab by remember { mutableStateOf(0) } // 0: Overview & Slots, 1: Teams & Moderation, 2: Audit Logs

    val totalGoldCirculating = remember(state.teams) { state.teams.sumOf { it.goldPoints } }
    val publishedSlotsCount = remember(state.slots) { state.slots.count { it.status == SlotStatus.PUBLISHED } }
    val eligibleTeamsCount = remember(state.teams) {
        state.teams.count { !it.isSuspended && it.monthlyRewardLastClaimedMonth != "2026-10" }
    }

    var showRefundDialogForSlot by remember { mutableStateOf<String?>(null) }
    var refundReason by remember { mutableStateOf("Host technical outage / rescheduled match") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoldSquareLogo(size = 28.dp)
                        Column {
                            Text("ADMIN DASHBOARD", fontWeight = FontWeight.Black, fontSize = 16.sp, color = GoldPrimary)
                            Text("SECURITY & TOURNAMENT SUPERVISION", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBg,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("admin_dashboard_screen")
        ) {
            // Admin Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = GoldPrimary,
                indicator = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview & Slots", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Teams & Rewards", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Audit Trail", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedTab == 0) {
                    // KPI Metric Cards
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminKpiCard(
                                title = "REGISTERED SQUADS",
                                value = "${state.teams.size}",
                                icon = Icons.Default.Groups,
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiCard(
                                title = "TOTAL GP CIRCULATION",
                                value = "%,d".format(totalGoldCirculating),
                                icon = Icons.Default.MonetizationOn,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminKpiCard(
                                title = "ACTIVE SCRIM ROOMS",
                                value = "$publishedSlotsCount",
                                icon = Icons.Default.SportsEsports,
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiCard(
                                title = "PENDING REWARD TEAMS",
                                value = "$eligibleTeamsCount",
                                icon = Icons.Default.CardGiftcard,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))
                        Text("CUSTOM ROOM SLOTS MODERATION", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldLight)
                    }

                    items(state.slots) { slot ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusBadge(status = slot.status)
                                    Text(
                                        text = "${slot.filledTeamsCount}/${slot.maxTeams} Teams",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldLight
                                    )
                                }

                                Text(text = slot.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text(
                                    text = "Host: ${slot.hostTeamName} • Mode: ${slot.gameMode.displayName} • Map: ${slot.mapName.displayName}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Private Room ID: ${slot.roomId} • Pass: ${slot.roomPassword}",
                                    fontSize = 11.sp,
                                    color = GoldLight,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showRefundDialogForSlot = slot.id },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 6.dp)
                                    ) {
                                        Text("Refund 50 GP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (slot.status != SlotStatus.CANCELLED) {
                                        OutlinedButton(
                                            onClick = { onModerateSlot(slot.id, SlotStatus.CANCELLED, "Admin cancelled slot") },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Text("Cancel", fontSize = 11.sp, color = StatusError)
                                        }
                                    }

                                    if (slot.status != SlotStatus.CLOSED && slot.status != SlotStatus.CANCELLED) {
                                        OutlinedButton(
                                            onClick = { onModerateSlot(slot.id, SlotStatus.CLOSED, "Admin closed registrations") },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Text("Close", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                } else if (selectedTab == 1) {
                    // Batch Monthly Rewards Processing Action
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = GoldContainer),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("BATCH MONTHLY REWARD PROCESSING", fontWeight = FontWeight.Black, fontSize = 13.sp, color = GoldLight)
                                Text("Automatically identify all registered teams eligible for the current calendar month and issue 1,000 Gold Points atomically with idempotency protection.", fontSize = 11.sp, color = TextPrimary)
                                Button(
                                    onClick = onBatchProcessRewards,
                                    enabled = !state.isProcessing,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                    modifier = Modifier.testTag("admin_batch_process_rewards_button")
                                ) {
                                    Text("Process Rewards ($eligibleTeamsCount Eligible Squads)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text("TEAM SUSPENSIONS & BALANCES", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextSecondary)
                    }

                    items(state.teams) { team ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (team.isSuspended) StatusError else DarkBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(team.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                        if (team.isSuspended) {
                                            Surface(color = Color(0xFF3B1518), shape = RoundedCornerShape(4.dp)) {
                                                Text("SUSPENDED", color = StatusError, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Text("Captain: ${team.captainName} • Balance: ${team.goldPoints} GP", fontSize = 11.sp, color = GoldLight)
                                    Text("Monthly Reward Last Claimed: ${team.monthlyRewardLastClaimedMonth.ifBlank { "Never" }}", fontSize = 10.sp, color = TextMuted)
                                }

                                Button(
                                    onClick = { onToggleSuspension(team.id, if (team.isSuspended) "Reinstated" else "Administrative freeze") },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (team.isSuspended) StatusSuccess else Color(0xFF3B1518)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(if (team.isSuspended) "Unsuspend" else "Suspend", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                } else {
                    // Audit Trail
                    item {
                        Text("SYSTEM AUDIT TRAIL & LOGS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldLight)
                    }

                    items(state.auditLogs) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(color = DarkSurfaceVariant, shape = RoundedCornerShape(4.dp)) {
                                        Text(log.action, color = GoldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Text(sdf.format(Date(log.timestamp)), fontSize = 10.sp, color = TextMuted)
                                }
                                Text(log.details, fontSize = 12.sp, color = TextPrimary)
                                Text("Reason: ${log.reason} • Admin: ${log.adminName}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    if (showRefundDialogForSlot != null) {
        val slotId = showRefundDialogForSlot!!
        AlertDialog(
            onDismissRequest = { showRefundDialogForSlot = null },
            title = { Text("Controlled Admin Refund", fontWeight = FontWeight.Bold, color = GoldPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Refund 50 Gold Points to the host squad. Reason will be logged in the permanent audit trail.")
                    OutlinedTextField(
                        value = refundReason,
                        onValueChange = { refundReason = it },
                        label = { Text("Audit Reason *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRefundDialogForSlot = null
                        onRefundSlot(slotId, refundReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Execute 50 GP Refund", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefundDialogForSlot = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            }
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        }
    }
}
