package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.CustomSlot
import com.example.data.model.SlotStatus
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.GoldenSquareUiState

@Composable
fun SlotsScreen(
    state: GoldenSquareUiState,
    onSlotClick: (CustomSlot) -> Unit,
    onAddSlotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf(state.slotsSearchQuery) }
    var selectedFilter by remember { mutableStateOf(state.activeSlotFilter) }

    val filteredSlots = remember(state.slots, selectedFilter, searchQuery, state.currentUser) {
        state.slots.filter { slot ->
            val matchesSearch = searchQuery.isBlank() ||
                slot.title.contains(searchQuery, ignoreCase = true) ||
                slot.hostTeamName.contains(searchQuery, ignoreCase = true) ||
                slot.mapName.displayName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "UPCOMING" -> slot.status == SlotStatus.PUBLISHED
                "AVAILABLE" -> slot.status == SlotStatus.PUBLISHED && slot.filledTeamsCount < slot.maxTeams
                "FULL" -> slot.status == SlotStatus.FULL
                "COMPLETED" -> slot.status == SlotStatus.COMPLETED || slot.status == SlotStatus.CLOSED
                "MY_SLOTS" -> slot.hostTeamId == state.currentUser.teamId || slot.joinedTeams.any { it.teamId == state.currentUser.teamId }
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSlotClick,
                containerColor = GoldPrimary,
                contentColor = androidx.compose.ui.graphics.Color.Black,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("Host Slot (-50 GP)", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_slot_fab")
            )
        },
        containerColor = DarkBg,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("slots_directory_screen"),
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
                            text = "CUSTOM ROOM SLOTS",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = GoldPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Free Fire Scrims & Tournament Rooms",
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
                            text = "FEE: 50 GP",
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
                    placeholder = { Text("Search slots by title, host, map...", color = TextMuted, fontSize = 13.sp) },
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
                        .testTag("slots_search_input"),
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

            // Filter Chips Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf(
                        "ALL" to "All Slots",
                        "UPCOMING" to "Upcoming",
                        "AVAILABLE" to "Available",
                        "FULL" to "Full",
                        "MY_SLOTS" to "My Team Slots",
                        "COMPLETED" to "Completed"
                    )
                    items(filters) { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldContainer,
                                selectedLabelColor = GoldLight
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == key,
                                borderColor = DarkBorder,
                                selectedBorderColor = GoldPrimary
                            )
                        )
                    }
                }
            }

            if (filteredSlots.isEmpty()) {
                item {
                    EmptySlotsView(selectedFilter = selectedFilter, onHostSlot = onAddSlotClick)
                }
            } else {
                items(filteredSlots) { slot ->
                    SlotItemCard(slot = slot, onClick = { onSlotClick(slot) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp)) // Space for FAB
            }
        }
    }
}

@Composable
fun SlotItemCard(
    slot: CustomSlot,
    onClick: () -> Unit
) {
    val isFull = slot.filledTeamsCount >= slot.maxTeams

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("slot_card_${slot.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (slot.status == SlotStatus.PUBLISHED) GoldBorder else DarkBorder)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = GoldLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = slot.matchDateTime,
                        fontSize = 11.sp,
                        color = GoldLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = slot.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )

            // Game Mode, Map, and Capacity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkBorder)
                ) {
                    Text(
                        text = slot.mapName.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkBorder)
                ) {
                    Text(
                        text = slot.gameMode.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    color = if (isFull) Color(0xFF261D12) else Color(0xFF14241A),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, if (isFull) GoldAccent else StatusSuccess)
                ) {
                    Text(
                        text = "${slot.filledTeamsCount}/${slot.maxTeams} Teams",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFull) GoldAccent else StatusSuccess,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Rules Preview
            Text(
                text = slot.entryRequirements,
                fontSize = 11.sp,
                color = TextMuted,
                maxLines = 1
            )

            Divider(color = DarkBorder, thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Host: ${slot.hostTeamName} (${slot.hostCaptainName})",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Text(
                    text = "View Details →",
                    fontSize = 12.sp,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptySlotsView(selectedFilter: String, onHostSlot: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EventBusy,
                contentDescription = null,
                tint = GoldLight,
                modifier = Modifier.size(42.dp)
            )
            Text(
                text = "No Custom Slots Found",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Text(
                text = "No custom room matches filter '$selectedFilter'. Be the first team to host!",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onHostSlot,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = androidx.compose.ui.graphics.Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Host a Custom Room (-50 GP)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
