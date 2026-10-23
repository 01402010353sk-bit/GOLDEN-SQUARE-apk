package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CustomSlot
import com.example.data.model.GoldPointsTransaction
import com.example.ui.components.GoldBalanceCard
import com.example.ui.components.GoldSquareLogo
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.GoldenSquareUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    state: GoldenSquareUiState,
    onClaimReward: () -> Unit,
    onHostSlotClick: () -> Unit,
    onSlotClick: (CustomSlot) -> Unit,
    onAiClick: () -> Unit,
    onRankingsClick: () -> Unit,
    onTeamsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTeam = state.currentTeam
    val eligible = state.monthlyRewardEligibility?.isEligible ?: false
    val nextEligibleStr = state.monthlyRewardEligibility?.nextEligibleDateStr ?: "N/A"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Esports Championship Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Try to load banner image drawable
                    Image(
                        painter = painterResource(id = R.drawable.banner_esports),
                        contentDescription = "Free Fire Battle Arena Championship Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xCC0D0F12), Color(0xF00D0F12))
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Surface(
                            color = Color(0xCC111315),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                        ) {
                            Text(
                                text = "FREE FIRE COMPETITIVE SCRIMS",
                                color = GoldPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "GOLDEN SQUARE ARENA",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "Host custom rooms for 50 GP • Monthly 1,000 GP Team Rewards",
                            fontSize = 11.sp,
                            color = GoldLight
                        )
                    }
                }
            }
        }

        // Demo Prototype Notice
        item {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Demo Prototype: Real-world transactions, rewards, and room creation are executed securely by tournament hosts. No cash value.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Gold Balance Card
        item {
            GoldBalanceCard(
                teamName = activeTeam?.name,
                goldPoints = activeTeam?.goldPoints ?: 0L,
                isEligibleForReward = eligible,
                nextEligibleDateStr = nextEligibleStr,
                onClaimReward = onClaimReward,
                onHostSlot = onHostSlotClick
            )
        }

        // Quick Navigation Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionTile(
                    title = "Rankings",
                    subtitle = "Leaderboard",
                    icon = Icons.Default.Leaderboard,
                    onClick = onRankingsClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    title = "AI Tactician",
                    subtitle = "Free Fire AI",
                    icon = Icons.Default.Psychology,
                    onClick = onAiClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    title = "Teams",
                    subtitle = "Directory",
                    icon = Icons.Default.Groups,
                    onClick = onTeamsClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Featured Live Custom Rooms Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(StatusSuccess)
                    )
                    Text(
                        text = "FEATURED SCRIM SLOTS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "${state.slots.size} Total",
                    fontSize = 12.sp,
                    color = GoldLight,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Featured Slot Cards
        items(state.slots.take(3)) { slot ->
            FeaturedSlotCard(slot = slot, onClick = { onSlotClick(slot) })
        }

        // Recent Gold Points Transactions Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT WALLET ACTIVITY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Ledger",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        val recentTx = state.transactions.take(4)
        if (recentTx.isEmpty()) {
            item {
                Text(
                    text = "No recent transactions found for this account.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        } else {
            items(recentTx) { tx ->
                TransactionRowItem(tx = tx)
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun FeaturedSlotCard(
    slot: CustomSlot,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("slot_item_${slot.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = slot.status)
                Text(
                    text = slot.matchDateTime,
                    fontSize = 11.sp,
                    color = GoldLight,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = slot.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = slot.mapName.displayName,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = slot.gameMode.displayName,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${slot.filledTeamsCount}/${slot.maxTeams} Teams",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (slot.filledTeamsCount >= slot.maxTeams) StatusWarning else GoldLight
                    )
                }
            }

            Divider(color = DarkBorder, thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hosted by: ${slot.hostTeamName}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "View Details →",
                    fontSize = 11.sp,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TransactionRowItem(tx: GoldPointsTransaction) {
    val isCredit = tx.amount > 0
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.US)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isCredit) Color(0xFF0E2E1D) else Color(0xFF33191D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isCredit) StatusSuccess else StatusError,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = tx.reason,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "${sdf.format(Date(tx.timestamp))} • ${tx.teamName}",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Text(
                text = (if (isCredit) "+" else "") + "${tx.amount} GP",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = if (isCredit) StatusSuccess else StatusError
            )
        }
    }
}
