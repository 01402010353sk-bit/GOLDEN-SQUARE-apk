package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Team
import com.example.ui.components.GoldSquareLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.GoldenSquareUiState

@Composable
fun RankingsScreen(
    state: GoldenSquareUiState,
    onTeamClick: (Team) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortBy by remember { mutableStateOf("POINTS") } // POINTS, WINS, MATCHES
    var searchQuery by remember { mutableStateOf("") }

    val sortedTeams = remember(state.teams, sortBy, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            state.teams
        } else {
            state.teams.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.tag.contains(searchQuery, ignoreCase = true) ||
                it.captainName.contains(searchQuery, ignoreCase = true)
            }
        }

        when (sortBy) {
            "WINS" -> filtered.sortedWith(compareByDescending<Team> { it.winsCount }.thenByDescending { it.goldPoints })
            "MATCHES" -> filtered.sortedWith(compareByDescending<Team> { it.matchesPlayed }.thenByDescending { it.goldPoints })
            else -> filtered.sortedWith(compareByDescending<Team> { it.goldPoints }.thenByDescending { it.winsCount }.thenBy { it.registrationDate })
        }
    }

    val userTeamId = state.currentUser.teamId
    val userTeamRank = sortedTeams.indexOfFirst { it.id == userTeamId }.let { if (it >= 0) it + 1 else null }
    val userTeam = state.teams.find { it.id == userTeamId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .testTag("rankings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "GOLD POINTS LEADERBOARD",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = GoldPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Official Free Fire Esports Season Standings",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Demo notice
        item {
            Surface(
                color = Color(0xFF1B1F26),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Text(
                    text = "Sample Prototype Season: Rankings dynamically recalculate when teams earn monthly rewards or publish custom room slots.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // Search Bar & Sort Chips
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search teams by name, tag, or captain...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = GoldPrimary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rankings_search_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                singleLine = true
            )
        }

        // Sort Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortBy == "POINTS",
                    onClick = { sortBy = "POINTS" },
                    label = { Text("Gold Points", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldContainer,
                        selectedLabelColor = GoldLight
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = sortBy == "POINTS",
                        borderColor = DarkBorder,
                        selectedBorderColor = GoldPrimary
                    )
                )
                FilterChip(
                    selected = sortBy == "WINS",
                    onClick = { sortBy = "WINS" },
                    label = { Text("Wins", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldContainer,
                        selectedLabelColor = GoldLight
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = sortBy == "WINS",
                        borderColor = DarkBorder,
                        selectedBorderColor = GoldPrimary
                    )
                )
                FilterChip(
                    selected = sortBy == "MATCHES",
                    onClick = { sortBy = "MATCHES" },
                    label = { Text("Matches", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldContainer,
                        selectedLabelColor = GoldLight
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = sortBy == "MATCHES",
                        borderColor = DarkBorder,
                        selectedBorderColor = GoldPrimary
                    )
                )
            }
        }

        // Top 3 Podium Cards
        if (sortedTeams.size >= 3 && searchQuery.isBlank()) {
            item {
                PodiumSection(topThree = sortedTeams.take(3), onTeamClick = onTeamClick)
            }
        }

        // User's own team rank highlight
        if (userTeam != null && userTeamRank != null) {
            item {
                UserTeamRankBanner(
                    rank = userTeamRank,
                    team = userTeam,
                    onClick = { onTeamClick(userTeam) }
                )
            }
        }

        // Full Rankings List
        item {
            Text(
                text = "FULL STANDINGS",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
        }

        itemsIndexed(sortedTeams) { index, team ->
            val rank = index + 1
            val isUserTeam = team.id == userTeamId
            RankItemRow(rank = rank, team = team, isUserTeam = isUserTeam, onClick = { onTeamClick(team) })
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun PodiumSection(topThree: List<Team>, onTeamClick: (Team) -> Unit) {
    val first = topThree[0]
    val second = topThree[1]
    val third = topThree[2]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TOP PODIUM CONTENDERS",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = GoldLight,
                    letterSpacing = 1.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Rank 2 (Silver)
                PodiumPillar(team = second, rank = 2, crownColor = Color(0xFFC0C0C0), height = 110.dp, onClick = { onTeamClick(second) })
                // Rank 1 (Gold)
                PodiumPillar(team = first, rank = 1, crownColor = GoldPrimary, height = 140.dp, onClick = { onTeamClick(first) })
                // Rank 3 (Bronze)
                PodiumPillar(team = third, rank = 3, crownColor = Color(0xFFCD7F32), height = 95.dp, onClick = { onTeamClick(third) })
            }
        }
    }
}

@Composable
fun PodiumPillar(
    team: Team,
    rank: Int,
    crownColor: Color,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(crownColor.copy(alpha = 0.2f))
                .border(1.5.dp, crownColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = crownColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = team.tag,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = TextPrimary
        )
        Text(
            text = "${team.goldPoints} GP",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = crownColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(crownColor.copy(alpha = 0.35f), DarkSurfaceVariant)
                    )
                )
                .border(1.dp, crownColor.copy(alpha = 0.5f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            contentAlignment = Alignment.Center
        ) {
            GoldSquareLogo(size = 24.dp)
        }
    }
}

@Composable
fun UserTeamRankBanner(rank: Int, team: Team, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = GoldContainer,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
                Column {
                    Text(
                        text = "YOUR TEAM STANDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${team.name} [${team.tag}]",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${team.goldPoints} GP",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = GoldPrimary
                )
                Text(
                    text = "${team.winsCount} Wins",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun RankItemRow(rank: Int, team: Team, isUserTeam: Boolean, onClick: () -> Unit) {
    val rankBadgeColor = when (rank) {
        1 -> GoldPrimary
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> DarkBorder
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("rank_row_$rank"),
        shape = RoundedCornerShape(10.dp),
        color = if (isUserTeam) Color(0xFF242014) else DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isUserTeam) GoldBorder else DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (rank <= 3) rankBadgeColor.copy(alpha = 0.2f) else DarkSurfaceVariant)
                        .border(1.dp, rankBadgeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (rank <= 3) rankBadgeColor else TextSecondary
                    )
                }

                GoldSquareLogo(size = 26.dp)

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = team.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Surface(
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = team.tag,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Captain: ${team.captainName} • ${team.winsCount}W / ${team.matchesPlayed}M",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Text(
                text = "${team.goldPoints} GP",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = if (rank <= 3) GoldPrimary else TextPrimary
            )
        }
    }
}
