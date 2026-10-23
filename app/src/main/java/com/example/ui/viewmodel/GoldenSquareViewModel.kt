package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiEsportsService
import com.example.data.model.*
import com.example.data.repository.GoldenSquareRepository
import com.example.data.repository.GoldenSquareRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class GoldenSquareUiState(
    val currentUser: User,
    val teams: List<Team>,
    val slots: List<CustomSlot>,
    val transactions: List<GoldPointsTransaction>,
    val auditLogs: List<AdminAuditLog>,
    val currentTeam: Team?,
    val monthlyRewardEligibility: MonthlyRewardEligibility?,
    val currentScreen: String = "HOME", // HOME, RANKINGS, SLOTS, TEAMS, PROFILE
    val activeSlotFilter: String = "ALL", // ALL, UPCOMING, AVAILABLE, FULL, COMPLETED, MY_SLOTS
    val slotsSearchQuery: String = "",
    val rankingsSearchQuery: String = "",
    val teamsSearchQuery: String = "",
    val selectedSlotForDetail: CustomSlot? = null,
    val selectedTeamForDetail: Team? = null,
    val showAddSlotDialog: Boolean = false,
    val showCreateTeamDialog: Boolean = false,
    val showAdminDashboard: Boolean = false,
    val showAiStrategist: Boolean = false,
    val isProcessing: Boolean = false,
    val toastMessage: String? = null,
    val aiAnalysisResult: String? = null,
    val isAiThinking: Boolean = false
)

class GoldenSquareViewModel(
    private val repository: GoldenSquareRepository = GoldenSquareRepositoryImpl(),
    private val aiService: AiEsportsService = AiEsportsService()
) : ViewModel() {

    private val _currentScreen = MutableStateFlow("HOME")
    private val _activeSlotFilter = MutableStateFlow("ALL")
    private val _slotsSearchQuery = MutableStateFlow("")
    private val _rankingsSearchQuery = MutableStateFlow("")
    private val _teamsSearchQuery = MutableStateFlow("")
    private val _selectedSlot = MutableStateFlow<CustomSlot?>(null)
    private val _selectedTeam = MutableStateFlow<Team?>(null)
    private val _showAddSlot = MutableStateFlow(false)
    private val _showCreateTeam = MutableStateFlow(false)
    private val _showAdmin = MutableStateFlow(false)
    private val _showAi = MutableStateFlow(false)
    private val _isProcessing = MutableStateFlow(false)
    private val _toast = MutableStateFlow<String?>(null)
    private val _aiResult = MutableStateFlow<String?>(null)
    private val _isAiThinking = MutableStateFlow(false)
    private val _eligibility = MutableStateFlow<MonthlyRewardEligibility?>(null)

    val uiState: StateFlow<GoldenSquareUiState> = combine(
        repository.currentUser,
        repository.teams,
        repository.slots,
        repository.transactions,
        repository.auditLogs,
        _currentScreen,
        _activeSlotFilter,
        _slotsSearchQuery,
        _rankingsSearchQuery,
        _teamsSearchQuery,
        _selectedSlot,
        _selectedTeam,
        _showAddSlot,
        _showCreateTeam,
        _showAdmin,
        _showAi,
        _isProcessing,
        _toast,
        _aiResult,
        _isAiThinking,
        _eligibility
    ) { params ->
        val user = params[0] as User
        val teams = params[1] as List<Team>
        val slots = params[2] as List<CustomSlot>
        val txs = params[3] as List<GoldPointsTransaction>
        val audits = params[4] as List<AdminAuditLog>
        val screen = params[5] as String
        val slotFilter = params[6] as String
        val slotSearch = params[7] as String
        val rankSearch = params[8] as String
        val teamSearch = params[9] as String
        val selSlot = params[10] as CustomSlot?
        val selTeam = params[11] as Team?
        val showAdd = params[12] as Boolean
        val showCreate = params[13] as Boolean
        val showAdm = params[14] as Boolean
        val showAiModal = params[15] as Boolean
        val proc = params[16] as Boolean
        val toastMsg = params[17] as String?
        val aiRes = params[18] as String?
        val thinking = params[19] as Boolean
        val elig = params[20] as MonthlyRewardEligibility?

        val activeTeam = teams.find { it.id == user.teamId }

        GoldenSquareUiState(
            currentUser = user,
            teams = teams,
            slots = slots,
            transactions = txs,
            auditLogs = audits,
            currentTeam = activeTeam,
            monthlyRewardEligibility = elig,
            currentScreen = screen,
            activeSlotFilter = slotFilter,
            slotsSearchQuery = slotSearch,
            rankingsSearchQuery = rankSearch,
            teamsSearchQuery = teamSearch,
            selectedSlotForDetail = selSlot,
            selectedTeamForDetail = selTeam,
            showAddSlotDialog = showAdd,
            showCreateTeamDialog = showCreate,
            showAdminDashboard = showAdm,
            showAiStrategist = showAiModal,
            isProcessing = proc,
            toastMessage = toastMsg,
            aiAnalysisResult = aiRes,
            isAiThinking = thinking
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GoldenSquareUiState(
            currentUser = repository.currentUser.value,
            teams = repository.teams.value,
            slots = repository.slots.value,
            transactions = repository.transactions.value,
            auditLogs = repository.auditLogs.value,
            currentTeam = null,
            monthlyRewardEligibility = null
        )
    )

    init {
        refreshEligibility()
    }

    private fun refreshEligibility() {
        viewModelScope.launch {
            val user = repository.currentUser.value
            val teamId = user.teamId
            if (teamId != null) {
                _eligibility.value = repository.checkMonthlyRewardEligibility(teamId)
            } else {
                _eligibility.value = null
            }
        }
    }

    fun setScreen(screen: String) {
        _currentScreen.value = screen
        _selectedSlot.value = null
        _selectedTeam.value = null
    }

    fun setSlotFilter(filter: String) {
        _activeSlotFilter.value = filter
    }

    fun setSlotsSearchQuery(query: String) {
        _slotsSearchQuery.value = query
    }

    fun setRankingsSearchQuery(query: String) {
        _rankingsSearchQuery.value = query
    }

    fun setTeamsSearchQuery(query: String) {
        _teamsSearchQuery.value = query
    }

    fun selectSlotForDetail(slot: CustomSlot?) {
        _selectedSlot.value = slot
    }

    fun selectTeamForDetail(team: Team?) {
        _selectedTeam.value = team
    }

    fun setShowAddSlotDialog(show: Boolean) {
        _showAddSlot.value = show
    }

    fun setShowCreateTeamDialog(show: Boolean) {
        _showCreateTeam.value = show
    }

    fun setShowAdminDashboard(show: Boolean) {
        _showAdmin.value = show
    }

    fun setShowAiStrategist(show: Boolean) {
        _showAi.value = show
    }

    fun clearToast() {
        _toast.value = null
    }

    fun switchRole(role: UserRole) {
        viewModelScope.launch {
            repository.switchUserRole(role)
            refreshEligibility()
            _toast.value = "Switched to ${role.name.replace("_", " ")} mode"
        }
    }

    fun claimMonthlyReward() {
        viewModelScope.launch {
            val user = repository.currentUser.value
            val teamId = user.teamId
            if (teamId == null) {
                _toast.value = "You must be registered in a team to claim monthly rewards."
                return@launch
            }

            _isProcessing.value = true
            val idempotencyKey = "claim_${teamId}_" + UUID.randomUUID().toString().take(6)
            val result = repository.claimMonthlyReward(teamId, idempotencyKey)
            _isProcessing.value = false

            result.fold(
                onSuccess = {
                    _toast.value = "Claimed +1,000 Gold Points monthly reward successfully!"
                    refreshEligibility()
                },
                onFailure = { error ->
                    _toast.value = "Claim Failed: ${error.message}"
                }
            )
        }
    }

    fun publishSlot(
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
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            val idempotencyKey = "slot_pub_" + UUID.randomUUID().toString()
            val result = repository.publishCustomSlot(
                title = title,
                gameMode = gameMode,
                mapName = mapName,
                matchDateTime = matchDateTime,
                matchTimestamp = matchTimestamp,
                maxTeams = maxTeams,
                entryRequirements = entryRequirements,
                rules = rules,
                contactInstructions = contactInstructions,
                roomId = roomId,
                roomPassword = roomPassword,
                idempotencyKey = idempotencyKey
            )
            _isProcessing.value = false

            result.fold(
                onSuccess = { slot ->
                    _showAddSlot.value = false
                    _toast.value = "Slot Published! -50 GP deducted atomically."
                    _selectedSlot.value = slot
                },
                onFailure = { error ->
                    _toast.value = "Publication Failed: ${error.message}"
                }
            )
        }
    }

    fun requestJoinSlot(slotId: String, requestedSlotNumber: Int? = null) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.requestJoinSlot(slotId, requestedSlotNumber)
            _isProcessing.value = false

            result.fold(
                onSuccess = {
                    _toast.value = "Join request sent to room host!"
                    // Update selected slot view if currently opened
                    val updated = repository.slots.value.find { it.id == slotId }
                    _selectedSlot.value = updated
                },
                onFailure = { error ->
                    _toast.value = "Failed to Join: ${error.message}"
                }
            )
        }
    }

    fun respondToJoinRequest(slotId: String, requestId: String, accept: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.respondToJoinRequest(slotId, requestId, accept)
            _isProcessing.value = false

            result.fold(
                onSuccess = {
                    _toast.value = if (accept) "Approved join request!" else "Declined request."
                    val updated = repository.slots.value.find { it.id == slotId }
                    _selectedSlot.value = updated
                },
                onFailure = { error ->
                    _toast.value = "Action failed: ${error.message}"
                }
            )
        }
    }

    fun closeSlot(slotId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.closeSlot(slotId)
            _isProcessing.value = false
            result.fold(
                onSuccess = {
                    _toast.value = "Slot closed for new registrations."
                    val updated = repository.slots.value.find { it.id == slotId }
                    _selectedSlot.value = updated
                },
                onFailure = { error ->
                    _toast.value = "Failed: ${error.message}"
                }
            )
        }
    }

    fun cancelSlotByHost(slotId: String, reason: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.cancelSlotByHost(slotId, reason)
            _isProcessing.value = false
            result.fold(
                onSuccess = {
                    _toast.value = "Slot marked cancelled. Note: Slot fee refunds require Admin review."
                    val updated = repository.slots.value.find { it.id == slotId }
                    _selectedSlot.value = updated
                },
                onFailure = { error ->
                    _toast.value = "Cancel failed: ${error.message}"
                }
            )
        }
    }

    fun adminRefundSlot(slotId: String, reason: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.adminRefundSlotFee(slotId, reason)
            _isProcessing.value = false
            result.fold(
                onSuccess = {
                    _toast.value = "Admin refund processed: 50 GP credited to host team."
                    val updated = repository.slots.value.find { it.id == slotId }
                    _selectedSlot.value = updated
                },
                onFailure = { error ->
                    _toast.value = "Refund failed: ${error.message}"
                }
            )
        }
    }

    fun adminModerateSlot(slotId: String, newStatus: SlotStatus, reason: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.adminModerateSlot(slotId, newStatus, reason)
            _isProcessing.value = false
            result.fold(
                onSuccess = {
                    _toast.value = "Slot status updated to ${newStatus.label}"
                    val updated = repository.slots.value.find { it.id == slotId }
                    _selectedSlot.value = updated
                },
                onFailure = { error ->
                    _toast.value = "Moderation failed: ${error.message}"
                }
            )
        }
    }

    fun adminToggleTeamSuspension(teamId: String, reason: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.adminToggleTeamSuspension(teamId, reason)
            _isProcessing.value = false
            result.fold(
                onSuccess = {
                    _toast.value = "Team status updated."
                },
                onFailure = { error ->
                    _toast.value = "Operation failed: ${error.message}"
                }
            )
        }
    }

    fun adminBatchProcessMonthlyRewards() {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.adminProcessMonthlyRewardsForEligibleTeams()
            _isProcessing.value = false
            result.fold(
                onSuccess = { count ->
                    _toast.value = "Batch completed: Issued rewards to $count eligible teams."
                    refreshEligibility()
                },
                onFailure = { error ->
                    _toast.value = "Batch processing error: ${error.message}"
                }
            )
        }
    }

    fun registerTeam(name: String, tag: String, captainIgn: String, captainUid: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.registerTeam(name, tag, captainIgn, captainUid)
            _isProcessing.value = false
            result.fold(
                onSuccess = { team ->
                    _showCreateTeam.value = false
                    _toast.value = "Team '${team.name}' registered! +1,000 GP Welcome Reward granted."
                    refreshEligibility()
                },
                onFailure = { error ->
                    _toast.value = "Registration error: ${error.message}"
                }
            )
        }
    }

    fun addTeamMember(teamId: String, ign: String, ffUid: String, role: MemberRole) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.addTeamMember(teamId, ign, ffUid, role)
            _isProcessing.value = false
            result.fold(
                onSuccess = {
                    _toast.value = "Player '$ign' added to team roster."
                    val updated = repository.teams.value.find { it.id == teamId }
                    _selectedTeam.value = updated
                },
                onFailure = { error ->
                    _toast.value = "Add member error: ${error.message}"
                }
            )
        }
    }

    fun consultAiStrategist(prompt: String, context: String = "") {
        viewModelScope.launch {
            _isAiThinking.value = true
            _aiResult.value = null
            val result = aiService.consultStrategist(prompt, context)
            _isAiThinking.value = false
            result.fold(
                onSuccess = { text ->
                    _aiResult.value = text
                },
                onFailure = { err ->
                    _aiResult.value = "Strategist offline or encountered an error: ${err.message}"
                }
            )
        }
    }
}
