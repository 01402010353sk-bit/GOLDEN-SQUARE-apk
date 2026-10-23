package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberRole
import com.example.data.model.Team
import com.example.data.model.UserRole
import com.example.ui.components.GoldSquareLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.GoldenSquareUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    team: Team,
    state: GoldenSquareUiState,
    onBack: () -> Unit,
    onAddMember: (teamId: String, ign: String, ffUid: String, role: MemberRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.US)
    val isCaptainOrAdmin = state.currentUser.role == UserRole.ADMIN ||
        (state.currentUser.teamId == team.id && state.currentUser.role == UserRole.TEAM_CAPTAIN)

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newIgn by remember { mutableStateOf("") }
    var newUid by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(MemberRole.SNIPER) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TEAM PROFILE", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GoldPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                actions = {
                    if (isCaptainOrAdmin) {
                        IconButton(
                            onClick = { showAddMemberDialog = true },
                            modifier = Modifier.testTag("add_member_button")
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Member", tint = GoldPrimary)
                        }
                    }
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
                .testTag("team_detail_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            GoldSquareLogo(size = 48.dp)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(team.name, fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextPrimary)
                                    Surface(color = GoldContainer, shape = RoundedCornerShape(4.dp)) {
                                        Text(team.tag, color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Text("Unique Team ID: ${team.id}", fontSize = 11.sp, color = GoldLight)
                                Text("Registered: ${sdf.format(Date(team.registrationDate))}", fontSize = 10.sp, color = TextMuted)
                            }
                        }

                        Divider(color = DarkBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("GOLD POINTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                Text("${team.goldPoints} GP", fontSize = 18.sp, fontWeight = FontWeight.Black, color = GoldPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("VICTORIES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                Text("${team.winsCount} Wins", fontSize = 18.sp, fontWeight = FontWeight.Black, color = StatusSuccess)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MATCHES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                Text("${team.matchesPlayed}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // Roster Members Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEAM ROSTER (${team.members.size} PLAYERS)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    if (isCaptainOrAdmin) {
                        TextButton(onClick = { showAddMemberDialog = true }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Player", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Members List
            items(team.members) { member ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (member.role == MemberRole.CAPTAIN) GoldBorder else DarkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (member.role == MemberRole.CAPTAIN) GoldContainer else DarkSurfaceElevated)
                                    .border(1.dp, if (member.role == MemberRole.CAPTAIN) GoldPrimary else DarkBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (member.role == MemberRole.CAPTAIN) Icons.Default.Star else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (member.role == MemberRole.CAPTAIN) GoldPrimary else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column {
                                Text(member.ign, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text("Free Fire UID: ${member.ffUid}", fontSize = 11.sp, color = TextMuted)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (member.role == MemberRole.CAPTAIN) GoldContainer else DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, if (member.role == MemberRole.CAPTAIN) GoldBorder else DarkBorder)
                        ) {
                            Text(
                                text = member.role.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (member.role == MemberRole.CAPTAIN) GoldLight else TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add Player to Roster", fontWeight = FontWeight.Bold, color = GoldPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newIgn,
                        onValueChange = { newIgn = it },
                        label = { Text("In-Game Name (IGN) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUid,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) newUid = it },
                        label = { Text("Free Fire UID *") },
                        placeholder = { Text("e.g. 583920194") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Squad Role", fontSize = 12.sp, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(MemberRole.SNIPER, MemberRole.ASSAULTER, MemberRole.RUSHER).forEach { r ->
                            FilterChip(
                                selected = selectedRole == r,
                                onClick = { selectedRole = r },
                                label = { Text(r.name, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newIgn.isNotBlank() && newUid.isNotBlank()) {
                            showAddMemberDialog = false
                            onAddMember(team.id, newIgn, newUid, selectedRole)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Add Player", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
