package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SlotStatus
import com.example.data.model.UserRole
import com.example.ui.theme.*

@Composable
fun GoldSquareLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F1215))
            .border(1.dp, GoldPrimary, RoundedCornerShape(6.dp))
            .padding(size * 0.12f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(GoldLight, GoldPrimary, GoldDark)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoldenSquareTopBar(
    appName: String = "GOLDEN SQUARE",
    userRole: UserRole,
    goldPoints: Long,
    onRoleClick: () -> Unit,
    onAdminClick: () -> Unit,
    onAiClick: () -> Unit
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GoldSquareLogo(size = 38.dp)
                Column {
                    Text(
                        text = appName,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp,
                        color = GoldPrimary
                    )
                    Text(
                        text = "ESPORTS TOURNAMENTS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Gold Balance Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GoldContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder),
                    modifier = Modifier.clickable { onRoleClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Gold Points",
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$goldPoints GP",
                            color = GoldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // AI Strategist Button
                IconButton(
                    onClick = onAiClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, CircleShape)
                        .testTag("ai_strategist_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Strategist",
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Admin Dashboard Button
                IconButton(
                    onClick = onAdminClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (userRole == UserRole.ADMIN) GoldContainer else DarkSurfaceVariant)
                        .border(1.dp, if (userRole == UserRole.ADMIN) GoldPrimary else DarkBorder, CircleShape)
                        .testTag("admin_dashboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Dashboard",
                        tint = if (userRole == UserRole.ADMIN) GoldPrimary else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GoldBalanceCard(
    teamName: String?,
    goldPoints: Long,
    isEligibleForReward: Boolean,
    nextEligibleDateStr: String,
    onClaimReward: () -> Unit,
    onHostSlot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gold_balance_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(GoldPrimary, GoldDark, DarkBorder)))
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoldSquareLogo(size = 28.dp)
                    Text(
                        text = teamName ?: "NO TEAM REGISTERED",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF262010),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder)
                ) {
                    Text(
                        text = "OFFICIAL WALLET",
                        color = GoldLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "GOLD POINTS BALANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "%,d".format(goldPoints),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary
                    )
                    Text(
                        text = "GP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            // Legal non-cash notice
            Text(
                text = "Gold Points are non-cash promotional platform points only. They cannot be bought, sold, traded, or redeemed for money.",
                fontSize = 11.sp,
                color = TextMuted,
                lineHeight = 15.sp
            )

            Divider(color = DarkBorder, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Claim Monthly Reward Button
                Button(
                    onClick = onClaimReward,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("claim_monthly_reward_button"),
                    enabled = isEligibleForReward,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black,
                        disabledContainerColor = DarkSurfaceElevated,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEligibleForReward) "+1,000 GP" else "Claimed",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Host Slot Button (-50 GP)
                OutlinedButton(
                    onClick = onHostSlot,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("host_slot_quick_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddBox,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Host Slot (-50)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            if (!isEligibleForReward) {
                Text(
                    text = "Next monthly reward: $nextEligibleDateStr (1 claim per calendar month)",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: SlotStatus) {
    val (bgColor, textColor, borderCol) = when (status) {
        SlotStatus.PUBLISHED -> Triple(Color(0xFF0F291E), StatusSuccess, Color(0xFF165B37))
        SlotStatus.FULL -> Triple(Color(0xFF2E220D), GoldAccent, Color(0xFF7A5515))
        SlotStatus.PENDING_REVIEW -> Triple(Color(0xFF262112), GoldLight, GoldBorder)
        SlotStatus.CLOSED -> Triple(Color(0xFF1E2229), TextSecondary, DarkBorder)
        SlotStatus.CANCELLED -> Triple(Color(0xFF2B1214), StatusError, Color(0xFF781B22))
        SlotStatus.COMPLETED -> Triple(Color(0xFF14243B), StatusInfo, Color(0xFF1D4E89))
        SlotStatus.DRAFT -> Triple(Color(0xFF1E2229), TextMuted, DarkBorder)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
    ) {
        Text(
            text = status.label.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
