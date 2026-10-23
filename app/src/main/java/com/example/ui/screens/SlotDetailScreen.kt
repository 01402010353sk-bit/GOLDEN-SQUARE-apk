package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.GoldSquareLogo
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.GoldenSquareUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotDetailScreen(
    slot: CustomSlot,
    state: GoldenSquareUiState,
    onBack: () -> Unit,
    onRequestJoin: (slotId: String) -> Unit,
    onRespondToJoinRequest: (slotId: String, requestId: String, accept: Boolean) -> Unit,
    onCloseSlot: (slotId: String) -> Unit,
    onCancelSlot: (slotId: String, reason: String) -> Unit,
    onAdminRefund: (slotId: String, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val currentUserId = state.currentUser.id
    val userTeamId = state.currentUser.teamId
    val isAdmin = state.currentUser.role == UserRole.ADMIN
    val isHostCaptain = slot.hostCaptainUserId == currentUserId || (userTeamId != null && userTeamId == slot.hostTeamId)
    val isJoined = userTeamId != null && slot.joinedTeams.any { it.teamId == userTeamId }
    val isApprovedOrHostOrAdmin = isHostCaptain || isJoined || isAdmin

    val pendingUserRequest = slot.joinRequests.find {
        it.requestingTeamId == userTeamId && it.status == JoinRequestStatus.PENDING
    }

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    var showRefundDialog by remember { mutableStateOf(false) }
    var refundReason by remember { mutableStateOf("Host emergency schedule conflict") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SLOT DETAILS", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GoldPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                actions = {
                    StatusBadge(status = slot.status)
                    Spacer(modifier = Modifier.width(12.dp))
                }
            )
        },
        containerColor = DarkBg,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("slot_detail_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Slot Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = slot.title,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(color = DarkSurfaceVariant, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = slot.mapName.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(color = DarkSurfaceVariant, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = slot.gameMode.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(color = GoldContainer, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = slot.matchDateTime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Divider(color = DarkBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("HOST SQUAD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Text(slot.hostTeamName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Captain: ${slot.hostCaptainName}", fontSize = 11.sp, color = TextSecondary)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("CAPACITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Text(
                                    "${slot.filledTeamsCount} / ${slot.maxTeams} Teams",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (slot.filledTeamsCount >= slot.maxTeams) StatusWarning else GoldPrimary
                                )
                                Text("50 GP Creation Fee Paid", fontSize = 10.sp, color = GoldLight)
                            }
                        }
                    }
                }
            }

            // Private Room Credentials Box (CRITICAL SECURITY & PRIVACY)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isApprovedOrHostOrAdmin) Color(0xFF192518) else Color(0xFF241C16)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isApprovedOrHostOrAdmin) StatusSuccess else GoldBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isApprovedOrHostOrAdmin) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isApprovedOrHostOrAdmin) StatusSuccess else GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "CUSTOM ROOM CREDENTIALS",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (isApprovedOrHostOrAdmin) StatusSuccess else GoldPrimary
                            )
                        }

                        if (isApprovedOrHostOrAdmin) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("ROOM ID: ${slot.roomId}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Text("PASSWORD: ${slot.roomPassword}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldLight)
                                }
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString("ID: ${slot.roomId} / PASS: ${slot.roomPassword}"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Keep credentials private. Free Fire room must be created by the host in-game.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = "Room ID and Password are protected. They will be revealed here automatically after your squad's join request is approved by the host.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Rules & Entry Specs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ENTRY REQUIREMENTS & RULES", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldLight)
                        Text(slot.entryRequirements, fontSize = 12.sp, color = TextPrimary)
                        Divider(color = DarkBorder)
                        Text("RULES:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = TextSecondary)
                        Text(slot.rules, fontSize = 12.sp, color = TextMuted)
                        Divider(color = DarkBorder)
                        Text("JOINING & CONTACT:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = TextSecondary)
                        Text(slot.contactInstructions, fontSize = 12.sp, color = TextMuted)
                    }
                }
            }

            // Joined Squads List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REGISTERED SQUADS (${slot.joinedTeams.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            items(slot.joinedTeams) { team ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(GoldContainer)
                                    .border(1.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${team.slotNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                            }
                            Column {
                                Text(team.teamName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("Captain: ${team.captainName}", fontSize = 10.sp, color = TextMuted)
                            }
                        }

                        if (team.teamId == slot.hostTeamId) {
                            Surface(color = GoldContainer, shape = RoundedCornerShape(4.dp)) {
                                Text("HOST", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GoldLight, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            // Pending Join Requests (Host or Admin only)
            if ((isHostCaptain || isAdmin) && slot.joinRequests.any { it.status == JoinRequestStatus.PENDING }) {
                item {
                    Text(
                        text = "PENDING SQUAD JOIN REQUESTS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GoldAccent
                    )
                }

                items(slot.joinRequests.filter { it.status == JoinRequestStatus.PENDING }) { req ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF261D12),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(req.requestingTeamName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("Captain: ${req.captainName} (UID: ${req.captainUid})", fontSize = 10.sp, color = GoldLight)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onRespondToJoinRequest(slot.id, req.requestId, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onRespondToJoinRequest(slot.id, req.requestId, false) },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Reject", fontSize = 11.sp, color = StatusError)
                                }
                            }
                        }
                    }
                }
            }

            // Join Room Button (for non-hosts, non-joined)
            if (!isHostCaptain && !isJoined && slot.status == SlotStatus.PUBLISHED) {
                item {
                    if (pendingUserRequest != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.HourglassTop, contentDescription = null, tint = GoldLight)
                                Text("Your team's join request is pending host approval.", fontSize = 12.sp, color = GoldLight)
                            }
                        }
                    } else if (slot.filledTeamsCount >= slot.maxTeams) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Room is Full. No further slots available.", fontSize = 12.sp, color = StatusWarning, modifier = Modifier.padding(12.dp))
                        }
                    } else {
                        Button(
                            onClick = { onRequestJoin(slot.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_join_slot_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Request to Join Custom Room", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Host Controls
            if (isHostCaptain && slot.status == SlotStatus.PUBLISHED) {
                item {
                    Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Text("HOST CONTROLS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextMuted)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onCloseSlot(slot.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close Registration", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { showCancelDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1518)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel Slot", fontSize = 11.sp, color = StatusError)
                        }
                    }
                }
            }

            // Admin Controls
            if (isAdmin) {
                item {
                    Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Text("ADMIN MODERATION & REFUND", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldAccent)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showRefundDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_refund_button")
                        ) {
                            Icon(imageVector = Icons.Default.CurrencyExchange, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refund 50 GP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onCancelSlot(slot.id, "Admin cancellation") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Force Cancel", fontSize = 11.sp, color = StatusError)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Cancel Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Custom Slot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to cancel this custom room slot? Note that slot creation fees are not automatically refunded. A refund request requires admin review.")
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Cancellation Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelSlot(slot.id, cancelReason.ifBlank { "Host cancelled" })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Back")
                }
            }
        )
    }

    // Admin Refund Dialog
    if (showRefundDialog) {
        AlertDialog(
            onDismissRequest = { showRefundDialog = false },
            title = { Text("Process Admin 50 GP Refund", fontWeight = FontWeight.Bold, color = GoldPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will refund exactly 50 Gold Points to host team '${slot.hostTeamName}'. A full audit log will be created.")
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
                        showRefundDialog = false
                        onAdminRefund(slot.id, refundReason.ifBlank { "Administrative refund" })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Refund 50 GP", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefundDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
