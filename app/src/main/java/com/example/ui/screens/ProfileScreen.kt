package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.TransactionType
import com.example.data.model.UserRole
import com.example.ui.components.GoldBalanceCard
import com.example.ui.components.GoldSquareLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.GoldenSquareUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    state: GoldenSquareUiState,
    onClaimReward: () -> Unit,
    onHostSlot: () -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    onOpenAdminDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = state.currentUser
    val team = state.currentTeam
    val eligible = state.monthlyRewardEligibility?.isEligible ?: false
    val nextEligibleStr = state.monthlyRewardEligibility?.nextEligibleDateStr ?: "N/A"

    var txFilter by remember { mutableStateOf<TransactionType?>(null) }

    val filteredTransactions = remember(state.transactions, txFilter, user.teamId) {
        val list = if (user.teamId != null) {
            state.transactions.filter { it.teamId == user.teamId }
        } else {
            state.transactions
        }
        if (txFilter != null) list.filter { it.type == txFilter } else list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .testTag("profile_wallet_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User & Team Identity Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GoldContainer)
                                    .border(1.5.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = user.ign.ifBlank { user.username },
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "UID: ${user.ffUid} • ${user.email}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (user.role) {
                                UserRole.ADMIN -> Color(0xFF3B1518)
                                UserRole.TEAM_CAPTAIN -> GoldContainer
                                UserRole.ORDINARY_MEMBER -> DarkSurfaceVariant
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (user.role) {
                                    UserRole.ADMIN -> StatusError
                                    UserRole.TEAM_CAPTAIN -> GoldPrimary
                                    UserRole.ORDINARY_MEMBER -> DarkBorder
                                }
                            )
                        ) {
                            Text(
                                text = user.role.name.replace("_", " "),
                                color = when (user.role) {
                                    UserRole.ADMIN -> StatusError
                                    UserRole.TEAM_CAPTAIN -> GoldLight
                                    UserRole.ORDINARY_MEMBER -> TextSecondary
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Role Switcher for Testing CUJs
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SWITCH DEMO PERSONA / ROLE:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UserRole.values().forEach { role ->
                                val isSelected = user.role == role
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSwitchRole(role) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) GoldContainer else DarkSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GoldPrimary else DarkBorder)
                                ) {
                                    Text(
                                        text = when (role) {
                                            UserRole.TEAM_CAPTAIN -> "Captain"
                                            UserRole.ORDINARY_MEMBER -> "Member"
                                            UserRole.ADMIN -> "Admin"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) GoldPrimary else TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Gold Balance Card
        item {
            GoldBalanceCard(
                teamName = team?.name,
                goldPoints = team?.goldPoints ?: 0L,
                isEligibleForReward = eligible,
                nextEligibleDateStr = nextEligibleStr,
                onClaimReward = onClaimReward,
                onHostSlot = onHostSlot
            )
        }

        // Monthly Reward Audit Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MONTHLY REWARD SAFEGUARD",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GoldLight
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (eligible) Color(0xFF0F2B1D) else DarkSurfaceVariant
                        ) {
                            Text(
                                text = if (eligible) "ELIGIBLE (+1,000 GP)" else "ALREADY CLAIMED",
                                color = if (eligible) StatusSuccess else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Last Claimed: ${team?.monthlyRewardLastClaimedMonth?.ifBlank { "Never" }} • Next Eligible: $nextEligibleStr",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Rule: Exactly 1 claim per calendar month per registered squad. Duplicate clicks are blocked on the server layer.",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // Transaction History Header & Filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSACTION LEDGER (${filteredTransactions.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = txFilter == null,
                        onClick = { txFilter = null },
                        label = { Text("All", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldContainer, selectedLabelColor = GoldLight)
                    )
                }
                item {
                    FilterChip(
                        selected = txFilter == TransactionType.MONTHLY_REWARD,
                        onClick = { txFilter = TransactionType.MONTHLY_REWARD },
                        label = { Text("Rewards", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldContainer, selectedLabelColor = GoldLight)
                    )
                }
                item {
                    FilterChip(
                        selected = txFilter == TransactionType.SLOT_CREATION_FEE,
                        onClick = { txFilter = TransactionType.SLOT_CREATION_FEE },
                        label = { Text("Slot Fees (-50)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldContainer, selectedLabelColor = GoldLight)
                    )
                }
                item {
                    FilterChip(
                        selected = txFilter == TransactionType.ADMIN_REFUND,
                        onClick = { txFilter = TransactionType.ADMIN_REFUND },
                        label = { Text("Refunds", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldContainer, selectedLabelColor = GoldLight)
                    )
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No transactions found in this category.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredTransactions) { tx ->
                TransactionDetailCard(tx = tx)
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun TransactionDetailCard(tx: com.example.data.model.GoldPointsTransaction) {
    val isCredit = tx.amount > 0
    val sdf = SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.US)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isCredit) Color(0xFF0F291E) else Color(0xFF2C1315)
                ) {
                    Text(
                        text = tx.type.name.replace("_", " "),
                        color = if (isCredit) StatusSuccess else StatusError,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = (if (isCredit) "+" else "") + "${tx.amount} GP",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = if (isCredit) StatusSuccess else StatusError
                )
            }

            Text(
                text = tx.reason,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Balance After: ${tx.balanceAfter} GP",
                    fontSize = 11.sp,
                    color = GoldLight,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = sdf.format(Date(tx.timestamp)),
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}
