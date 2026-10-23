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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberRole
import com.example.data.model.Team
import com.example.ui.components.GoldSquareLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.GoldenSquareUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeamsScreen(
    state: GoldenSquareUiState,
    onTeamClick: (Team) -> Unit,
    onCreateTeamClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredTeams = remember(state.teams, searchQuery) {
        if (searchQuery.isBlank()) {
            state.teams
        } else {
            state.teams.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.tag.contains(searchQuery, ignoreCase = true) ||
                it.captainName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTeamClick,
                containerColor = GoldPrimary,
                contentColor = androidx.compose.ui.graphics.Color.Black,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("Register Team (+1,000 GP)", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("create_team_fab")
            )
        },
        containerColor = DarkBg,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("teams_directory_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TEAMS DIRECTORY",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = GoldPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Registered Free Fire Esports Rosters",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        color = GoldContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder)
                    ) {
                        Text(
                            text = "${state.teams.size} SQUADS",
                            color = GoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search teams by squad name, tag, captain...", color = TextMuted, fontSize = 13.sp) },
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
                        .testTag("teams_search_input"),
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

            // User's Active Squad Card
            state.currentTeam?.let { myTeam ->
                item {
                    Text("YOUR REGISTERED SQUAD", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldLight)
                    TeamDirectoryCard(team = myTeam, isUserTeam = true, onClick = { onTeamClick(myTeam) })
                }
                item {
                    Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Text("ALL COMPETITIVE ROSTERS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextSecondary)
                }
            }

            items(filteredTeams) { team ->
                TeamDirectoryCard(
                    team = team,
                    isUserTeam = team.id == state.currentUser.teamId,
                    onClick = { onTeamClick(team) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}

@Composable
fun TeamDirectoryCard(team: Team, isUserTeam: Boolean, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("MMM yyyy", Locale.US)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("team_card_${team.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isUserTeam) Color(0xFF1C1910) else DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isUserTeam) GoldPrimary else DarkBorder)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GoldSquareLogo(size = 32.dp)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = team.name,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Surface(
                                color = GoldContainer,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, GoldBorder)
                            ) {
                                Text(
                                    text = team.tag,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Team ID: ${team.id} • Registered ${sdf.format(Date(team.registrationDate))}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${team.goldPoints} GP",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = GoldPrimary
                    )
                    Text(
                        text = "Wallet",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Divider(color = DarkBorder, thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Captain: ${team.captainName}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Text(
                        text = "${team.members.size} Roster Members",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
