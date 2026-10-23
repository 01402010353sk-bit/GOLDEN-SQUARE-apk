package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserRole
import com.example.ui.components.GoldenSquareTopBar
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.GoldenSquareViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GoldenSquareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GoldenSquareApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun GoldenSquareApp(viewModel: GoldenSquareViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRoleSwitcherDialog by remember { mutableStateOf(false) }

    // Display toast messages
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // Handle back button behavior
    BackHandler(enabled = state.selectedSlotForDetail != null || state.selectedTeamForDetail != null || state.showAdminDashboard || state.currentScreen != "HOME") {
        when {
            state.selectedSlotForDetail != null -> viewModel.selectSlotForDetail(null)
            state.selectedTeamForDetail != null -> viewModel.selectTeamForDetail(null)
            state.showAdminDashboard -> viewModel.setShowAdminDashboard(false)
            state.currentScreen != "HOME" -> viewModel.setScreen("HOME")
        }
    }

    Scaffold(
        topBar = {
            if (state.selectedSlotForDetail == null && state.selectedTeamForDetail == null && !state.showAdminDashboard) {
                GoldenSquareTopBar(
                    userRole = state.currentUser.role,
                    goldPoints = state.currentTeam?.goldPoints ?: 0L,
                    onRoleClick = { showRoleSwitcherDialog = true },
                    onAdminClick = { viewModel.setShowAdminDashboard(true) },
                    onAiClick = { viewModel.setShowAiStrategist(true) }
                )
            }
        },
        bottomBar = {
            if (state.selectedSlotForDetail == null && state.selectedTeamForDetail == null && !state.showAdminDashboard) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = GoldPrimary,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val navItems = listOf(
                        Triple("HOME", "Home", Icons.Default.Home),
                        Triple("RANKINGS", "Rankings", Icons.Default.Leaderboard),
                        Triple("SLOTS", "Custom Slots", Icons.Default.SportsEsports),
                        Triple("TEAMS", "Teams", Icons.Default.Groups),
                        Triple("PROFILE", "Wallet", Icons.Default.AccountBalanceWallet)
                    )

                    navItems.forEach { (route, label, icon) ->
                        val isSelected = state.currentScreen == route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.setScreen(route) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) GoldPrimary else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) GoldLight else TextSecondary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = GoldContainer,
                                selectedIconColor = GoldPrimary,
                                unselectedIconColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_tab_${route.lowercase()}")
                        )
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = DarkSurfaceElevated,
                    contentColor = TextPrimary,
                    actionColor = GoldPrimary,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .border(1.dp, GoldBorder, RoundedCornerShape(10.dp))
                )
            }
        },
        containerColor = DarkBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.showAdminDashboard -> {
                    AdminDashboardScreen(
                        state = state,
                        onBack = { viewModel.setShowAdminDashboard(false) },
                        onRefundSlot = { slotId, reason -> viewModel.adminRefundSlot(slotId, reason) },
                        onModerateSlot = { slotId, status, reason -> viewModel.adminModerateSlot(slotId, status, reason) },
                        onToggleSuspension = { teamId, reason -> viewModel.adminToggleTeamSuspension(teamId, reason) },
                        onBatchProcessRewards = { viewModel.adminBatchProcessMonthlyRewards() }
                    )
                }
                state.selectedSlotForDetail != null -> {
                    SlotDetailScreen(
                        slot = state.selectedSlotForDetail!!,
                        state = state,
                        onBack = { viewModel.selectSlotForDetail(null) },
                        onRequestJoin = { slotId -> viewModel.requestJoinSlot(slotId) },
                        onRespondToJoinRequest = { slotId, reqId, accept -> viewModel.respondToJoinRequest(slotId, reqId, accept) },
                        onCloseSlot = { slotId -> viewModel.closeSlot(slotId) },
                        onCancelSlot = { slotId, reason -> viewModel.cancelSlotByHost(slotId, reason) },
                        onAdminRefund = { slotId, reason -> viewModel.adminRefundSlot(slotId, reason) }
                    )
                }
                state.selectedTeamForDetail != null -> {
                    TeamDetailScreen(
                        team = state.selectedTeamForDetail!!,
                        state = state,
                        onBack = { viewModel.selectTeamForDetail(null) },
                        onAddMember = { teamId, ign, uid, role -> viewModel.addTeamMember(teamId, ign, uid, role) }
                    )
                }
                state.currentScreen == "HOME" -> {
                    HomeScreen(
                        state = state,
                        onClaimReward = { viewModel.claimMonthlyReward() },
                        onHostSlotClick = { viewModel.setShowAddSlotDialog(true) },
                        onSlotClick = { slot -> viewModel.selectSlotForDetail(slot) },
                        onAiClick = { viewModel.setShowAiStrategist(true) },
                        onRankingsClick = { viewModel.setScreen("RANKINGS") },
                        onTeamsClick = { viewModel.setScreen("TEAMS") }
                    )
                }
                state.currentScreen == "RANKINGS" -> {
                    RankingsScreen(
                        state = state,
                        onTeamClick = { team -> viewModel.selectTeamForDetail(team) }
                    )
                }
                state.currentScreen == "SLOTS" -> {
                    SlotsScreen(
                        state = state,
                        onSlotClick = { slot -> viewModel.selectSlotForDetail(slot) },
                        onAddSlotClick = { viewModel.setShowAddSlotDialog(true) }
                    )
                }
                state.currentScreen == "TEAMS" -> {
                    TeamsScreen(
                        state = state,
                        onTeamClick = { team -> viewModel.selectTeamForDetail(team) },
                        onCreateTeamClick = { viewModel.setShowCreateTeamDialog(true) }
                    )
                }
                state.currentScreen == "PROFILE" -> {
                    ProfileScreen(
                        state = state,
                        onClaimReward = { viewModel.claimMonthlyReward() },
                        onHostSlot = { viewModel.setShowAddSlotDialog(true) },
                        onSwitchRole = { role -> viewModel.switchRole(role) },
                        onOpenAdminDashboard = { viewModel.setShowAdminDashboard(true) }
                    )
                }
            }

            // Dialogs
            if (state.showAddSlotDialog) {
                AddSlotDialog(
                    state = state,
                    onDismiss = { viewModel.setShowAddSlotDialog(false) },
                    onConfirmPublish = { title, mode, map, dt, ts, maxT, reqs, rules, contact, rId, rPass ->
                        viewModel.publishSlot(title, mode, map, dt, ts, maxT, reqs, rules, contact, rId, rPass)
                    }
                )
            }

            if (state.showCreateTeamDialog) {
                CreateTeamDialog(
                    state = state,
                    onDismiss = { viewModel.setShowCreateTeamDialog(false) },
                    onConfirmRegister = { name, tag, ign, uid ->
                        viewModel.registerTeam(name, tag, ign, uid)
                    }
                )
            }

            if (state.showAiStrategist) {
                AiStrategistDialog(
                    state = state,
                    onDismiss = { viewModel.setShowAiStrategist(false) },
                    onConsult = { prompt -> viewModel.consultAiStrategist(prompt) }
                )
            }
        }
    }

    // Role Switcher Modal
    if (showRoleSwitcherDialog) {
        AlertDialog(
            onDismissRequest = { showRoleSwitcherDialog = false },
            title = { Text("Select Persona / Role", fontWeight = FontWeight.Bold, color = GoldPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Switch between roles to test the complete user journeys:", fontSize = 12.sp, color = TextSecondary)
                    RoleSelectOption(
                        title = "Team Captain",
                        desc = "Apex Predators Captain. Can host slots (-50 GP), claim monthly rewards, and approve squad join requests.",
                        selected = state.currentUser.role == UserRole.TEAM_CAPTAIN,
                        onClick = {
                            viewModel.switchRole(UserRole.TEAM_CAPTAIN)
                            showRoleSwitcherDialog = false
                        }
                    )
                    RoleSelectOption(
                        title = "Ordinary Team Member",
                        desc = "Squad sniper. Can view team roster, wallet balances, and request to join public custom rooms.",
                        selected = state.currentUser.role == UserRole.ORDINARY_MEMBER,
                        onClick = {
                            viewModel.switchRole(UserRole.ORDINARY_MEMBER)
                            showRoleSwitcherDialog = false
                        }
                    )
                    RoleSelectOption(
                        title = "Authorized Administrator",
                        desc = "Platform supervisor. Accesses Admin Dashboard, moderates listings, processes 50 GP fee refunds and batch rewards.",
                        selected = state.currentUser.role == UserRole.ADMIN,
                        onClick = {
                            viewModel.switchRole(UserRole.ADMIN)
                            showRoleSwitcherDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleSwitcherDialog = false }) {
                    Text("Close", color = GoldPrimary)
                }
            }
        )
    }
}

@Composable
fun RoleSelectOption(
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (selected) GoldContainer else DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) GoldPrimary else DarkBorder),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (selected) GoldLight else TextPrimary)
                if (selected) {
                    Surface(color = GoldPrimary, shape = RoundedCornerShape(3.dp)) {
                        Text("ACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }
    }
}
