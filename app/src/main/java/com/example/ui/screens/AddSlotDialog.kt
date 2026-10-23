package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FreeFireMap
import com.example.data.model.GameMode
import com.example.ui.components.GoldSquareLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.GoldenSquareUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSlotDialog(
    state: GoldenSquareUiState,
    onDismiss: () -> Unit,
    onConfirmPublish: (
        title: String,
        gameMode: GameMode,
        mapName: FreeFireMap,
        matchDateTime: String,
        matchTimestamp: Long,
        maxTeams: Int,
        entryRequirements: String,
        rules: String,
        contactInstructions: String,
        roomId: String,
        roomPassword: String
    ) -> Unit
) {
    val activeTeam = state.currentTeam
    val teamBalance = activeTeam?.goldPoints ?: 0L
    val slotCreationFee = 50L
    val hasSufficientBalance = teamBalance >= slotCreationFee && activeTeam != null

    var title by remember { mutableStateOf("") }
    var selectedGameMode by remember { mutableStateOf(GameMode.BATTLE_ROYALE_SQUAD) }
    var selectedMap by remember { mutableStateOf(FreeFireMap.BERMUDA) }
    var matchDateTime by remember { mutableStateOf("Today at 21:00 UTC") }
    var maxTeamsStr by remember { mutableStateOf("12") }
    var entryRequirements by remember { mutableStateOf("Tier 1 Verified Squads only. Level 55+.") }
    var rules by remember { mutableStateOf("Official FFWS competitive point distribution. No gun attributes.") }
    var contactInstructions by remember { mutableStateOf("Captain contact on Discord: discord.gg/goldensquare") }
    var roomId by remember { mutableStateOf("") }
    var roomPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var isPreviewStep by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .testTag("add_slot_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoldSquareLogo(size = 30.dp)
                        Column {
                            Text(
                                text = if (isPreviewStep) "CONFIRM SLOT HOSTING" else "HOST CUSTOM ROOM",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = GoldPrimary
                            )
                            Text(
                                text = "Fee: Exactly 50 Gold Points",
                                fontSize = 11.sp,
                                color = GoldLight
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))

                // Balance status banner
                Surface(
                    color = if (hasSufficientBalance) GoldContainer else Color(0xFF2C1315),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (hasSufficientBalance) GoldBorder else StatusError),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Hosting Team: ${activeTeam?.name ?: "None"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (hasSufficientBalance)
                                    "Balance: $teamBalance GP • 50 GP will be deducted upon confirmation."
                                else
                                    "Insufficient balance ($teamBalance GP). 50 GP required to host.",
                                fontSize = 11.sp,
                                color = if (hasSufficientBalance) GoldLight else StatusError
                            )
                        }
                        Icon(
                            imageVector = if (hasSufficientBalance) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (hasSufficientBalance) GoldPrimary else StatusError
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isPreviewStep) {
                    // Form Step
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Slot / Tournament Title *") },
                            placeholder = { Text("e.g. Bermuda Friday Night Tier-1 Scrim") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            ),
                            singleLine = true
                        )

                        // Mode selector
                        Text("Game Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GameMode.values().take(2).forEach { mode ->
                                FilterChip(
                                    selected = selectedGameMode == mode,
                                    onClick = { selectedGameMode = mode },
                                    label = { Text(mode.displayName, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldContainer,
                                        selectedLabelColor = GoldLight
                                    )
                                )
                            }
                        }

                        // Map selector
                        Text("Map", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FreeFireMap.values().take(3).forEach { map ->
                                FilterChip(
                                    selected = selectedMap == map,
                                    onClick = { selectedMap = map },
                                    label = { Text(map.displayName, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldContainer,
                                        selectedLabelColor = GoldLight
                                    )
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = matchDateTime,
                                onValueChange = { matchDateTime = it },
                                label = { Text("Match Date & Time") },
                                modifier = Modifier.weight(1.4f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant
                                )
                            )

                            OutlinedTextField(
                                value = maxTeamsStr,
                                onValueChange = { if (it.all { ch -> ch.isDigit() }) maxTeamsStr = it },
                                label = { Text("Max Teams") },
                                modifier = Modifier.weight(0.8f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant
                                )
                            )
                        }

                        OutlinedTextField(
                            value = entryRequirements,
                            onValueChange = { entryRequirements = it },
                            label = { Text("Entry Requirements") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = rules,
                            onValueChange = { rules = it },
                            label = { Text("Tournament Rules & Format") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = contactInstructions,
                            onValueChange = { contactInstructions = it },
                            label = { Text("Contact / WhatsApp / Discord") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            )
                        )

                        // Privacy Notice
                        Surface(
                            color = Color(0xFF1E222A),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Private Room Credentials: Only shown to you and approved participants.",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = roomId,
                                onValueChange = { roomId = it },
                                label = { Text("Room ID *") },
                                placeholder = { Text("e.g. 7829104") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant
                                )
                            )

                            OutlinedTextField(
                                value = roomPassword,
                                onValueChange = { roomPassword = it },
                                label = { Text("Password *") },
                                placeholder = { Text("e.g. GS2026") },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { isPreviewStep = true },
                        enabled = hasSufficientBalance && title.isNotBlank() && roomId.isNotBlank() && roomPassword.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preview_slot_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Preview Slot & Fee (-50 GP) →", fontWeight = FontWeight.Bold)
                    }

                } else {
                    // Pre-submission Preview Step
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = TextPrimary)
                                Text(text = "Map: ${selectedMap.displayName} • Mode: ${selectedGameMode.displayName}", fontSize = 12.sp, color = GoldLight)
                                Text(text = "Match Schedule: $matchDateTime", fontSize = 12.sp, color = TextSecondary)
                                Text(text = "Capacity: ${maxTeamsStr} Teams (Squads)", fontSize = 12.sp, color = TextSecondary)
                                Text(text = "Entry: $entryRequirements", fontSize = 12.sp, color = TextSecondary)
                                Text(text = "Rules: $rules", fontSize = 12.sp, color = TextMuted)
                            }
                        }

                        // Atomic Transaction Ledger Preview
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D2218)),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("TRANSACTION LEDGER PREVIEW", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GoldPrimary)
                                Text("Team: ${activeTeam?.name} (${activeTeam?.id})", fontSize = 12.sp, color = TextPrimary)
                                Text("Current Balance: $teamBalance GP", fontSize = 12.sp, color = TextSecondary)
                                Text("Hosting Deduction: -50 GP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusError)
                                Text("Balance After Publishing: ${teamBalance - 50} GP", fontSize = 13.sp, fontWeight = FontWeight.Black, color = GoldLight)
                                Text("Safeguard: 50 GP will be deducted atomically in one operation.", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { isPreviewStep = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("← Back to Edit", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                val teamsCount = maxTeamsStr.toIntOrNull() ?: 12
                                onConfirmPublish(
                                    title,
                                    selectedGameMode,
                                    selectedMap,
                                    matchDateTime,
                                    System.currentTimeMillis() + 7200000L,
                                    teamsCount,
                                    entryRequirements,
                                    rules,
                                    contactInstructions,
                                    roomId,
                                    roomPassword
                                )
                            },
                            enabled = !state.isProcessing,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("confirm_publish_slot_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (state.isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Confirm & Pay 50 GP", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
