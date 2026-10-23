package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.GoldSquareLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.GoldenSquareUiState

@Composable
fun CreateTeamDialog(
    state: GoldenSquareUiState,
    onDismiss: () -> Unit,
    onConfirmRegister: (name: String, tag: String, captainIgn: String, captainUid: String) -> Unit
) {
    var teamName by remember { mutableStateOf("") }
    var teamTag by remember { mutableStateOf("") }
    var captainIgn by remember { mutableStateOf(state.currentUser.ign.ifBlank { "CAP・PRO" }) }
    var captainUid by remember { mutableStateOf(state.currentUser.ffUid.ifBlank { "789102934" }) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("create_team_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
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
                        GoldSquareLogo(size = 30.dp)
                        Column {
                            Text(
                                text = "REGISTER SQUAD",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = GoldPrimary
                            )
                            Text(
                                text = "Golden Square Esports Roster",
                                fontSize = 11.sp,
                                color = GoldLight
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Reward Incentive Banner
                Surface(
                    color = GoldContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bonus: Newly registered squads receive a 1,000 Gold Points welcome promotional allocation!",
                        fontSize = 11.sp,
                        color = GoldLight,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name *") },
                    placeholder = { Text("e.g. Shadow Wolves") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("team_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = teamTag,
                    onValueChange = { teamTag = it.take(5).uppercase() },
                    label = { Text("Team Tag (2-5 letters) *") },
                    placeholder = { Text("e.g. WOLF") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("team_tag_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = captainIgn,
                    onValueChange = { captainIgn = it },
                    label = { Text("Captain In-Game Name (IGN) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("captain_ign_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = captainUid,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) captainUid = it },
                    label = { Text("Captain Free Fire UID *") },
                    placeholder = { Text("e.g. 582910482") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("captain_uid_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        onConfirmRegister(teamName, teamTag, captainIgn, captainUid)
                    },
                    enabled = teamName.isNotBlank() && teamTag.isNotBlank() && captainIgn.isNotBlank() && captainUid.isNotBlank() && !state.isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_create_team_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (state.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Register Team & Claim +1,000 GP", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
