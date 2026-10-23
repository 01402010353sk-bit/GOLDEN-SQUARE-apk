package com.example.data.repository

import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*

interface GoldenSquareRepository {
    val currentUser: StateFlow<User>
    val teams: StateFlow<List<Team>>
    val slots: StateFlow<List<CustomSlot>>
    val transactions: StateFlow<List<GoldPointsTransaction>>
    val auditLogs: StateFlow<List<AdminAuditLog>>

    // Account / Persona Switching
    suspend fun switchUserRole(role: UserRole)
    suspend fun switchActiveTeam(teamId: String)

    // Team Management
    suspend fun registerTeam(name: String, tag: String, captainIgn: String, captainUid: String): Result<Team>
    suspend fun getTeamById(teamId: String): Team?
    suspend fun addTeamMember(teamId: String, ign: String, ffUid: String, role: MemberRole): Result<Unit>

    // Gold Points & Transactions
    suspend fun checkMonthlyRewardEligibility(teamId: String): MonthlyRewardEligibility
    suspend fun claimMonthlyReward(teamId: String, idempotencyKey: String): Result<GoldPointsTransaction>
    suspend fun getTeamTransactions(teamId: String): List<GoldPointsTransaction>

    // Custom Slots
    suspend fun publishCustomSlot(
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
        roomPassword: String,
        idempotencyKey: String
    ): Result<CustomSlot>

    suspend fun requestJoinSlot(slotId: String, requestedSlotNumber: Int?): Result<JoinRequest>
    suspend fun respondToJoinRequest(slotId: String, requestId: String, accept: Boolean): Result<Unit>
    suspend fun closeSlot(slotId: String): Result<Unit>
    suspend fun cancelSlotByHost(slotId: String, reason: String): Result<Unit>

    // Admin Operations
    suspend fun adminRefundSlotFee(slotId: String, reason: String): Result<GoldPointsTransaction>
    suspend fun adminModerateSlot(slotId: String, newStatus: SlotStatus, reason: String): Result<Unit>
    suspend fun adminToggleTeamSuspension(teamId: String, reason: String): Result<Unit>
    suspend fun adminProcessMonthlyRewardsForEligibleTeams(): Result<Int>
}

class GoldenSquareRepositoryImpl : GoldenSquareRepository {
    private val mutex = Mutex()
    private val processedIdempotencyKeys = Collections.synchronizedSet(mutableSetOf<String>())

    // Demo Users
    private val captainUser = User(
        id = "user_cap_1",
        username = "ApexShadow",
        ign = "APEX・SHADOW",
        ffUid = "682910481",
        email = "captain@goldensquare.esports",
        role = UserRole.TEAM_CAPTAIN,
        teamId = "team_alpha"
    )

    private val memberUser = User(
        id = "user_mem_1",
        username = "PhantomSniper",
        ign = "APEX・PHANTOM",
        ffUid = "719302194",
        email = "sniper@goldensquare.esports",
        role = UserRole.ORDINARY_MEMBER,
        teamId = "team_alpha"
    )

    private val adminUser = User(
        id = "user_adm_1",
        username = "GoldenSquareAdmin",
        ign = "GS・SUPERVISOR",
        ffUid = "990000001",
        email = "security@goldensquare.io",
        role = UserRole.ADMIN,
        teamId = null
    )

    private val _currentUser = MutableStateFlow<User>(captainUser)
    override val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    override val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _slots = MutableStateFlow<List<CustomSlot>>(emptyList())
    override val slots: StateFlow<List<CustomSlot>> = _slots.asStateFlow()

    private val _transactions = MutableStateFlow<List<GoldPointsTransaction>>(emptyList())
    override val transactions: StateFlow<List<GoldPointsTransaction>> = _transactions.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AdminAuditLog>>(emptyList())
    override val auditLogs: StateFlow<List<AdminAuditLog>> = _auditLogs.asStateFlow()

    init {
        seedInitialEsportsData()
    }

    private fun getCurrentMonthKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }

    private fun seedInitialEsportsData() {
        val initialTeams = listOf(
            Team(
                id = "team_alpha",
                name = "Apex Predators",
                tag = "APEX",
                captainUserId = captainUser.id,
                captainName = captainUser.ign,
                members = listOf(
                    TeamMember(captainUser.id, captainUser.username, captainUser.ign, captainUser.ffUid, MemberRole.CAPTAIN),
                    TeamMember(memberUser.id, memberUser.username, memberUser.ign, memberUser.ffUid, MemberRole.SNIPER),
                    TeamMember("user_mem_2", "VortexStorm", "APEX・VORTEX", "719302195", MemberRole.ASSAULTER),
                    TeamMember("user_mem_3", "CyberGhost", "APEX・CYBER", "719302196", MemberRole.RUSHER)
                ),
                goldPoints = 1450L,
                registrationDate = System.currentTimeMillis() - (60L * 86400000L),
                monthlyRewardLastClaimedMonth = "2026-09", // eligible for current month
                winsCount = 18,
                matchesPlayed = 24
            ),
            Team(
                id = "team_beta",
                name = "Viper Syndicate",
                tag = "VIPR",
                captainUserId = "cap_viper",
                captainName = "VIPR・KOBRA",
                members = listOf(
                    TeamMember("cap_viper", "KobraKing", "VIPR・KOBRA", "584920192", MemberRole.CAPTAIN),
                    TeamMember("mem_v2", "Venomous", "VIPR・VENOM", "584920193", MemberRole.SNIPER),
                    TeamMember("mem_v3", "ToxicRush", "VIPR・TOXIC", "584920194", MemberRole.RUSHER),
                    TeamMember("mem_v4", "FangStrike", "VIPR・FANG", "584920195", MemberRole.SUPPORT)
                ),
                goldPoints = 1200L,
                registrationDate = System.currentTimeMillis() - (45L * 86400000L),
                monthlyRewardLastClaimedMonth = "2026-09",
                winsCount = 15,
                matchesPlayed = 21
            ),
            Team(
                id = "team_gamma",
                name = "Omega Dynasty",
                tag = "OMGA",
                captainUserId = "cap_omega",
                captainName = "OMGA・ZEUS",
                members = listOf(
                    TeamMember("cap_omega", "ZeusGod", "OMGA・ZEUS", "382910482", MemberRole.CAPTAIN),
                    TeamMember("mem_o2", "ApolloHit", "OMGA・APOLLO", "382910483", MemberRole.SNIPER),
                    TeamMember("mem_o3", "AresRage", "OMGA・ARES", "382910484", MemberRole.RUSHER),
                    TeamMember("mem_o4", "HermesRun", "OMGA・HERMES", "382910485", MemberRole.SUPPORT)
                ),
                goldPoints = 950L,
                registrationDate = System.currentTimeMillis() - (30L * 86400000L),
                monthlyRewardLastClaimedMonth = getCurrentMonthKey(),
                winsCount = 11,
                matchesPlayed = 19
            ),
            Team(
                id = "team_delta",
                name = "Solar Flare Esports",
                tag = "SOLR",
                captainUserId = "cap_sol",
                captainName = "SOLR・NOVA",
                members = listOf(
                    TeamMember("cap_sol", "NovaStrike", "SOLR・NOVA", "918273645", MemberRole.CAPTAIN),
                    TeamMember("mem_s2", "Solaris", "SOLR・SUN", "918273646", MemberRole.ASSAULTER)
                ),
                goldPoints = 650L,
                registrationDate = System.currentTimeMillis() - (15L * 86400000L),
                monthlyRewardLastClaimedMonth = getCurrentMonthKey(),
                winsCount = 8,
                matchesPlayed = 14
            ),
            Team(
                id = "team_epsilon",
                name = "Titan Vanguard",
                tag = "TITN",
                captainUserId = "cap_titan",
                captainName = "TITN・ATLAS",
                members = listOf(
                    TeamMember("cap_titan", "AtlasIron", "TITN・ATLAS", "473829104", MemberRole.CAPTAIN)
                ),
                goldPoints = 400L,
                registrationDate = System.currentTimeMillis() - (5L * 86400000L),
                monthlyRewardLastClaimedMonth = getCurrentMonthKey(),
                winsCount = 4,
                matchesPlayed = 9
            )
        )
        _teams.value = initialTeams

        val initialTransactions = listOf(
            GoldPointsTransaction(
                id = "tx_seed_1",
                teamId = "team_alpha",
                teamName = "Apex Predators",
                amount = 1000L,
                balanceAfter = 1500L,
                type = TransactionType.MONTHLY_REWARD,
                reason = "September 2026 Monthly Team Promotional Reward (+1,000 GP)",
                createdByUserId = "system",
                timestamp = System.currentTimeMillis() - (25L * 86400000L)
            ),
            GoldPointsTransaction(
                id = "tx_seed_2",
                teamId = "team_alpha",
                teamName = "Apex Predators",
                amount = -50L,
                balanceAfter = 1450L,
                type = TransactionType.SLOT_CREATION_FEE,
                reason = "Custom Room Slot Hosting Fee: Bermuda Pro Scrim T1",
                relatedSlotId = "slot_alpha_1",
                createdByUserId = captainUser.id,
                timestamp = System.currentTimeMillis() - (2L * 86400000L)
            ),
            GoldPointsTransaction(
                id = "tx_seed_3",
                teamId = "team_beta",
                teamName = "Viper Syndicate",
                amount = 1000L,
                balanceAfter = 1250L,
                type = TransactionType.MONTHLY_REWARD,
                reason = "September 2026 Monthly Team Promotional Reward (+1,000 GP)",
                createdByUserId = "system",
                timestamp = System.currentTimeMillis() - (22L * 86400000L)
            ),
            GoldPointsTransaction(
                id = "tx_seed_4",
                teamId = "team_beta",
                teamName = "Viper Syndicate",
                amount = -50L,
                balanceAfter = 1200L,
                type = TransactionType.SLOT_CREATION_FEE,
                reason = "Custom Room Slot Hosting Fee: Purgatory Clash Squad Cup",
                relatedSlotId = "slot_beta_1",
                createdByUserId = "cap_viper",
                timestamp = System.currentTimeMillis() - (1L * 86400000L)
            )
        )
        _transactions.value = initialTransactions

        val initialSlots = listOf(
            CustomSlot(
                id = "slot_alpha_1",
                title = "GS Bermuda Tier-1 Elite Scrim #42",
                hostTeamId = "team_alpha",
                hostTeamName = "Apex Predators",
                hostCaptainName = "APEX・SHADOW",
                hostCaptainUserId = captainUser.id,
                gameMode = GameMode.BATTLE_ROYALE_SQUAD,
                mapName = FreeFireMap.BERMUDA,
                matchDateTime = "Today at 20:00 UTC",
                matchTimestamp = System.currentTimeMillis() + (4L * 3600000L),
                maxTeams = 12,
                filledTeamsCount = 8,
                entryRequirements = "Tier 1 Verified Squads only. Level 60+ and Discord presence required.",
                rules = "Strict FFWS competitive rules. No character skill bans except active radar abuse. 3 matches: Bermuda, Purgatory, Alpine.",
                contactInstructions = "Join Discord server: discord.gg/goldensquare-scrims. Check #scrim-t1 channel 15 mins prior.",
                status = SlotStatus.PUBLISHED,
                creationFeePaid = 50L,
                roomId = "8920194",
                roomPassword = "APEX_GOLD_2026",
                joinedTeams = listOf(
                    JoinedTeam("team_alpha", "Apex Predators", "APEX・SHADOW", 1),
                    JoinedTeam("team_beta", "Viper Syndicate", "VIPR・KOBRA", 2),
                    JoinedTeam("team_gamma", "Omega Dynasty", "OMGA・ZEUS", 3),
                    JoinedTeam("team_delta", "Solar Flare Esports", "SOLR・NOVA", 4)
                ),
                joinRequests = listOf(
                    JoinRequest(
                        requestId = "req_1",
                        slotId = "slot_alpha_1",
                        requestingTeamId = "team_epsilon",
                        requestingTeamName = "Titan Vanguard",
                        captainName = "TITN・ATLAS",
                        captainUid = "473829104"
                    )
                )
            ),
            CustomSlot(
                id = "slot_beta_1",
                title = "Purgatory 4v4 Clash Squad Championship",
                hostTeamId = "team_beta",
                hostTeamName = "Viper Syndicate",
                hostCaptainName = "VIPR・KOBRA",
                hostCaptainUserId = "cap_viper",
                gameMode = GameMode.CLASH_SQUAD_4V4,
                mapName = FreeFireMap.PURGATORY,
                matchDateTime = "Tomorrow at 18:30 UTC",
                matchTimestamp = System.currentTimeMillis() + (26L * 3600000L),
                maxTeams = 8,
                filledTeamsCount = 6,
                entryRequirements = "Standard Clash Squad 4v4. Minimum Heroic rank in current season.",
                rules = "Best of 7 rounds. Limited ammo: NO. Grenades: Max 1 per player per round.",
                contactInstructions = "WhatsApp Captain hotline: +1 555-0199 or in-game invite.",
                status = SlotStatus.PUBLISHED,
                creationFeePaid = 50L,
                roomId = "5849102",
                roomPassword = "VIPER_CS_99",
                joinedTeams = listOf(
                    JoinedTeam("team_beta", "Viper Syndicate", "VIPR・KOBRA", 1),
                    JoinedTeam("team_alpha", "Apex Predators", "APEX・SHADOW", 2)
                )
            ),
            CustomSlot(
                id = "slot_gamma_1",
                title = "Nexterra High Gravity Night Scrim",
                hostTeamId = "team_gamma",
                hostTeamName = "Omega Dynasty",
                hostCaptainName = "OMGA・ZEUS",
                hostCaptainUserId = "cap_omega",
                gameMode = GameMode.BATTLE_ROYALE_SQUAD,
                mapName = FreeFireMap.NEXTERRA,
                matchDateTime = "Oct 25 at 21:00 UTC",
                matchTimestamp = System.currentTimeMillis() + (48L * 3600000L),
                maxTeams = 12,
                filledTeamsCount = 12,
                entryRequirements = "Open to all registered GS teams with verified UID.",
                rules = "Nexterra anti-gravity zones enabled. Official placement point system.",
                contactInstructions = "Room ID posted here 10 min prior to start.",
                status = SlotStatus.FULL,
                creationFeePaid = 50L,
                roomId = "7739182",
                roomPassword = "OMEGA_NEXT_7",
                joinedTeams = (1..12).map {
                    JoinedTeam("team_$it", "Contender #$it", "Captain_$it", it)
                }
            )
        )
        _slots.value = initialSlots

        val initialAudit = listOf(
            AdminAuditLog(
                id = "audit_1",
                adminUserId = adminUser.id,
                adminName = adminUser.ign,
                action = "SYSTEM_INITIALIZED",
                targetType = "PLATFORM",
                targetId = "GOLDEN_SQUARE_V1",
                details = "Initialized secure Gold Points ledger with atomic constraints and 50 GP custom slot hosting rules.",
                reason = "Platform deployment"
            )
        )
        _auditLogs.value = initialAudit
    }

    override suspend fun switchUserRole(role: UserRole) {
        mutex.withLock {
            when (role) {
                UserRole.TEAM_CAPTAIN -> _currentUser.value = captainUser
                UserRole.ORDINARY_MEMBER -> _currentUser.value = memberUser
                UserRole.ADMIN -> _currentUser.value = adminUser
            }
        }
    }

    override suspend fun switchActiveTeam(teamId: String) {
        mutex.withLock {
            val user = _currentUser.value
            _currentUser.value = user.copy(teamId = teamId)
        }
    }

    override suspend fun registerTeam(
        name: String,
        tag: String,
        captainIgn: String,
        captainUid: String
    ): Result<Team> = mutex.withLock {
        if (name.isBlank() || tag.isBlank() || captainIgn.isBlank() || captainUid.isBlank()) {
            return Result.failure(IllegalArgumentException("All registration fields are required."))
        }
        val cleanTag = tag.trim().uppercase()
        val cleanName = name.trim()

        if (_teams.value.any { it.tag.equals(cleanTag, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("Team tag '$cleanTag' is already registered."))
        }
        if (_teams.value.any { it.name.equals(cleanName, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("Team name '$cleanName' is already taken."))
        }

        val user = _currentUser.value
        val newTeamId = "team_" + UUID.randomUUID().toString().take(8)
        val captainMember = TeamMember(
            userId = user.id,
            username = user.username,
            ign = captainIgn.trim(),
            ffUid = captainUid.trim(),
            role = MemberRole.CAPTAIN
        )

        val newTeam = Team(
            id = newTeamId,
            name = cleanName,
            tag = cleanTag,
            captainUserId = user.id,
            captainName = captainIgn.trim(),
            members = listOf(captainMember),
            goldPoints = 1000L, // Welcome promotional reward
            registrationDate = System.currentTimeMillis(),
            monthlyRewardLastClaimedMonth = getCurrentMonthKey(),
            winsCount = 0,
            matchesPlayed = 0
        )

        val welcomeTx = GoldPointsTransaction(
            id = "tx_" + UUID.randomUUID().toString().take(8),
            teamId = newTeamId,
            teamName = cleanName,
            amount = 1000L,
            balanceAfter = 1000L,
            type = TransactionType.MONTHLY_REWARD,
            reason = "Welcome Team Registration & Monthly Promotional Reward (+1,000 GP)",
            createdByUserId = user.id
        )

        _teams.value = _teams.value + newTeam
        _transactions.value = listOf(welcomeTx) + _transactions.value
        _currentUser.value = user.copy(teamId = newTeamId, role = UserRole.TEAM_CAPTAIN, ign = captainIgn.trim(), ffUid = captainUid.trim())

        Result.success(newTeam)
    }

    override suspend fun getTeamById(teamId: String): Team? {
        return _teams.value.find { it.id == teamId }
    }

    override suspend fun addTeamMember(
        teamId: String,
        ign: String,
        ffUid: String,
        role: MemberRole
    ): Result<Unit> = mutex.withLock {
        val team = _teams.value.find { it.id == teamId }
            ?: return Result.failure(NoSuchElementException("Team not found"))

        if (team.members.any { it.ffUid == ffUid.trim() }) {
            return Result.failure(IllegalStateException("A player with Free Fire UID $ffUid is already on the roster."))
        }

        val newMember = TeamMember(
            userId = "user_" + UUID.randomUUID().toString().take(6),
            username = ign.trim(),
            ign = ign.trim(),
            ffUid = ffUid.trim(),
            role = role
        )

        val updatedTeam = team.copy(members = team.members + newMember)
        _teams.value = _teams.value.map { if (it.id == teamId) updatedTeam else it }
        Result.success(Unit)
    }

    override suspend fun checkMonthlyRewardEligibility(teamId: String): MonthlyRewardEligibility {
        val team = _teams.value.find { it.id == teamId }
        val currentMonth = getCurrentMonthKey()
        if (team == null) {
            return MonthlyRewardEligibility(
                teamId = teamId,
                isEligible = false,
                currentMonthKey = currentMonth,
                lastClaimedMonth = "None",
                nextEligibleDateStr = "Register team first"
            )
        }

        val isEligible = team.monthlyRewardLastClaimedMonth != currentMonth && !team.isSuspended

        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val nextEligible = if (isEligible) "Eligible now!" else sdf.format(cal.time)

        return MonthlyRewardEligibility(
            teamId = teamId,
            isEligible = isEligible,
            currentMonthKey = currentMonth,
            lastClaimedMonth = team.monthlyRewardLastClaimedMonth.ifBlank { "Never" },
            nextEligibleDateStr = nextEligible
        )
    }

    override suspend fun claimMonthlyReward(
        teamId: String,
        idempotencyKey: String
    ): Result<GoldPointsTransaction> = mutex.withLock {
        // Idempotency check to prevent duplicate clicks/retries
        if (processedIdempotencyKeys.contains(idempotencyKey)) {
            val existing = _transactions.value.find { it.idempotencyKey == idempotencyKey }
            if (existing != null) return Result.success(existing)
            return Result.failure(IllegalStateException("Duplicate reward claim request detected."))
        }

        val team = _teams.value.find { it.id == teamId }
            ?: return Result.failure(NoSuchElementException("Team not found"))

        if (team.isSuspended) {
            return Result.failure(IllegalStateException("Team is suspended. Monthly rewards cannot be claimed."))
        }

        val currentMonth = getCurrentMonthKey()
        if (team.monthlyRewardLastClaimedMonth == currentMonth) {
            return Result.failure(IllegalStateException("Team '${team.name}' has already claimed the 1,000 GP reward for $currentMonth."))
        }

        val newBalance = team.goldPoints + 1000L
        val tx = GoldPointsTransaction(
            id = "tx_rew_" + UUID.randomUUID().toString().take(8),
            teamId = team.id,
            teamName = team.name,
            amount = 1000L,
            balanceAfter = newBalance,
            type = TransactionType.MONTHLY_REWARD,
            reason = "$currentMonth Monthly Team Promotional Reward (+1,000 GP)",
            idempotencyKey = idempotencyKey,
            createdByUserId = _currentUser.value.id
        )

        processedIdempotencyKeys.add(idempotencyKey)
        val updatedTeam = team.copy(
            goldPoints = newBalance,
            monthlyRewardLastClaimedMonth = currentMonth
        )

        _teams.value = _teams.value.map { if (it.id == teamId) updatedTeam else it }
        _transactions.value = listOf(tx) + _transactions.value

        Result.success(tx)
    }

    override suspend fun getTeamTransactions(teamId: String): List<GoldPointsTransaction> {
        return _transactions.value.filter { it.teamId == teamId }
    }

    override suspend fun publishCustomSlot(
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
        roomPassword: String,
        idempotencyKey: String
    ): Result<CustomSlot> = mutex.withLock {
        // Idempotency check against double taps
        if (processedIdempotencyKeys.contains(idempotencyKey)) {
            val existing = _slots.value.find { it.id == idempotencyKey }
            if (existing != null) return Result.success(existing)
            return Result.failure(IllegalStateException("Slot submission already processed."))
        }

        val user = _currentUser.value
        val teamId = user.teamId
            ?: return Result.failure(IllegalStateException("You must be registered in a team to host a custom slot."))

        val team = _teams.value.find { it.id == teamId }
            ?: return Result.failure(NoSuchElementException("Hosting team not found."))

        if (team.isSuspended) {
            return Result.failure(IllegalStateException("Your team is suspended and cannot host custom room slots."))
        }

        // STRICT REQUIREMENT 8: Slot Creation Fee is EXACTLY 50 Gold Points
        val slotCreationFee = 50L
        if (team.goldPoints < slotCreationFee) {
            return Result.failure(IllegalStateException("Insufficient balance. Hosting requires exactly $slotCreationFee Gold Points. Your team has ${team.goldPoints} GP."))
        }

        if (title.isBlank() || roomId.isBlank() || roomPassword.isBlank()) {
            return Result.failure(IllegalArgumentException("Title, private Room ID, and Room Password are required."))
        }

        val newBalance = team.goldPoints - slotCreationFee
        val slotId = "slot_" + UUID.randomUUID().toString().take(8)

        // Deduct points atomically
        val feeTransaction = GoldPointsTransaction(
            id = "tx_fee_" + UUID.randomUUID().toString().take(8),
            teamId = team.id,
            teamName = team.name,
            amount = -slotCreationFee,
            balanceAfter = newBalance,
            type = TransactionType.SLOT_CREATION_FEE,
            reason = "Custom Room Slot Hosting Fee: $title (-50 GP)",
            relatedSlotId = slotId,
            idempotencyKey = idempotencyKey,
            createdByUserId = user.id
        )

        val newSlot = CustomSlot(
            id = slotId,
            title = title.trim(),
            hostTeamId = team.id,
            hostTeamName = team.name,
            hostCaptainName = team.captainName,
            hostCaptainUserId = user.id,
            gameMode = gameMode,
            mapName = mapName,
            matchDateTime = matchDateTime.trim(),
            matchTimestamp = matchTimestamp,
            maxTeams = maxTeams,
            filledTeamsCount = 1, // Host is registered in slot #1
            entryRequirements = entryRequirements.ifBlank { "Standard Free Fire Scrim rules apply." },
            rules = rules.ifBlank { "No hacking/emulators unless specified. 100% fair play." },
            contactInstructions = contactInstructions.ifBlank { "Join via in-game invite or room credentials." },
            status = SlotStatus.PUBLISHED,
            creationFeePaid = slotCreationFee,
            roomId = roomId.trim(),
            roomPassword = roomPassword.trim(),
            joinedTeams = listOf(
                JoinedTeam(
                    teamId = team.id,
                    teamName = team.name,
                    captainName = team.captainName,
                    slotNumber = 1
                )
            ),
            joinRequests = emptyList()
        )

        processedIdempotencyKeys.add(idempotencyKey)
        val updatedTeam = team.copy(goldPoints = newBalance)

        _teams.value = _teams.value.map { if (it.id == team.id) updatedTeam else it }
        _transactions.value = listOf(feeTransaction) + _transactions.value
        _slots.value = listOf(newSlot) + _slots.value

        Result.success(newSlot)
    }

    override suspend fun requestJoinSlot(
        slotId: String,
        requestedSlotNumber: Int?
    ): Result<JoinRequest> = mutex.withLock {
        val user = _currentUser.value
        val teamId = user.teamId
            ?: return Result.failure(IllegalStateException("You must have a team to join scrim slots."))
        val team = _teams.value.find { it.id == teamId }
            ?: return Result.failure(NoSuchElementException("Team not found"))

        val slot = _slots.value.find { it.id == slotId }
            ?: return Result.failure(NoSuchElementException("Slot not found"))

        if (slot.status != SlotStatus.PUBLISHED) {
            return Result.failure(IllegalStateException("Slot is not open for registration (Status: ${slot.status.label})."))
        }

        if (slot.joinedTeams.any { it.teamId == teamId }) {
            return Result.failure(IllegalStateException("Your team is already joined in this custom room."))
        }

        if (slot.joinRequests.any { it.requestingTeamId == teamId && it.status == JoinRequestStatus.PENDING }) {
            return Result.failure(IllegalStateException("Join request is already pending host approval."))
        }

        if (slot.filledTeamsCount >= slot.maxTeams) {
            return Result.failure(IllegalStateException("This custom room slot is full."))
        }

        val request = JoinRequest(
            requestId = "req_" + UUID.randomUUID().toString().take(8),
            slotId = slotId,
            requestingTeamId = team.id,
            requestingTeamName = team.name,
            captainName = team.captainName,
            captainUid = user.ffUid,
            requestedSlotNumber = requestedSlotNumber
        )

        val updatedSlot = slot.copy(joinRequests = slot.joinRequests + request)
        _slots.value = _slots.value.map { if (it.id == slotId) updatedSlot else it }

        Result.success(request)
    }

    override suspend fun respondToJoinRequest(
        slotId: String,
        requestId: String,
        accept: Boolean
    ): Result<Unit> = mutex.withLock {
        val slot = _slots.value.find { it.id == slotId }
            ?: return Result.failure(NoSuchElementException("Slot not found"))

        val req = slot.joinRequests.find { it.requestId == requestId }
            ?: return Result.failure(NoSuchElementException("Request not found"))

        if (accept) {
            if (slot.filledTeamsCount >= slot.maxTeams) {
                return Result.failure(IllegalStateException("Cannot approve: room is already at maximum capacity."))
            }
            val nextSlotNumber = (slot.joinedTeams.map { it.slotNumber }.maxOrNull() ?: 0) + 1
            val newJoinedTeam = JoinedTeam(
                teamId = req.requestingTeamId,
                teamName = req.requestingTeamName,
                captainName = req.captainName,
                slotNumber = req.requestedSlotNumber ?: nextSlotNumber
            )
            val updatedJoined = slot.joinedTeams + newJoinedTeam
            val newFilledCount = updatedJoined.size
            val newStatus = if (newFilledCount >= slot.maxTeams) SlotStatus.FULL else slot.status

            val updatedRequests = slot.joinRequests.map {
                if (it.requestId == requestId) it.copy(status = JoinRequestStatus.APPROVED) else it
            }

            val updatedSlot = slot.copy(
                joinedTeams = updatedJoined,
                filledTeamsCount = newFilledCount,
                status = newStatus,
                joinRequests = updatedRequests
            )
            _slots.value = _slots.value.map { if (it.id == slotId) updatedSlot else it }
        } else {
            val updatedRequests = slot.joinRequests.map {
                if (it.requestId == requestId) it.copy(status = JoinRequestStatus.REJECTED) else it
            }
            val updatedSlot = slot.copy(joinRequests = updatedRequests)
            _slots.value = _slots.value.map { if (it.id == slotId) updatedSlot else it }
        }

        Result.success(Unit)
    }

    override suspend fun closeSlot(slotId: String): Result<Unit> = mutex.withLock {
        val slot = _slots.value.find { it.id == slotId }
            ?: return Result.failure(NoSuchElementException("Slot not found"))

        val updated = slot.copy(status = SlotStatus.CLOSED)
        _slots.value = _slots.value.map { if (it.id == slotId) updated else it }
        Result.success(Unit)
    }

    override suspend fun cancelSlotByHost(slotId: String, reason: String): Result<Unit> = mutex.withLock {
        val slot = _slots.value.find { it.id == slotId }
            ?: return Result.failure(NoSuchElementException("Slot not found"))

        // Requirement: Do not automatically refund. Slot fee refund requires admin approval.
        val updated = slot.copy(status = SlotStatus.CANCELLED)
        _slots.value = _slots.value.map { if (it.id == slotId) updated else it }
        Result.success(Unit)
    }

    // REQUIREMENT 8 & 10: Admin Controlled Refund Workflow
    override suspend fun adminRefundSlotFee(
        slotId: String,
        reason: String
    ): Result<GoldPointsTransaction> = mutex.withLock {
        val slot = _slots.value.find { it.id == slotId }
            ?: return Result.failure(NoSuchElementException("Slot not found"))

        val team = _teams.value.find { it.id == slot.hostTeamId }
            ?: return Result.failure(NoSuchElementException("Host team not found"))

        if (reason.isBlank()) {
            return Result.failure(IllegalArgumentException("Admin refund reason is required for audit trail."))
        }

        // Check if already refunded
        val existingRefund = _transactions.value.find {
            it.relatedSlotId == slotId && it.type == TransactionType.ADMIN_REFUND
        }
        if (existingRefund != null) {
            return Result.failure(IllegalStateException("This slot fee of 50 GP has already been refunded."))
        }

        val refundAmount = slot.creationFeePaid
        val newBalance = team.goldPoints + refundAmount

        val refundTx = GoldPointsTransaction(
            id = "tx_ref_" + UUID.randomUUID().toString().take(8),
            teamId = team.id,
            teamName = team.name,
            amount = refundAmount,
            balanceAfter = newBalance,
            type = TransactionType.ADMIN_REFUND,
            reason = "Admin Refund for Slot '${slot.title}': $reason (+${refundAmount} GP)",
            relatedSlotId = slot.id,
            createdByUserId = _currentUser.value.id
        )

        val audit = AdminAuditLog(
            id = "audit_" + UUID.randomUUID().toString().take(8),
            adminUserId = _currentUser.value.id,
            adminName = _currentUser.value.ign.ifBlank { _currentUser.value.username },
            action = "REFUND_SLOT_FEE",
            targetType = "SLOT",
            targetId = slot.id,
            details = "Refunded $refundAmount GP to host team '${team.name}' (Balance: $newBalance GP)",
            reason = reason.trim()
        )

        val updatedTeam = team.copy(goldPoints = newBalance)
        _teams.value = _teams.value.map { if (it.id == team.id) updatedTeam else it }
        _transactions.value = listOf(refundTx) + _transactions.value
        _auditLogs.value = listOf(audit) + _auditLogs.value

        Result.success(refundTx)
    }

    override suspend fun adminModerateSlot(
        slotId: String,
        newStatus: SlotStatus,
        reason: String
    ): Result<Unit> = mutex.withLock {
        val slot = _slots.value.find { it.id == slotId }
            ?: return Result.failure(NoSuchElementException("Slot not found"))

        val updated = slot.copy(status = newStatus)
        val audit = AdminAuditLog(
            id = "audit_" + UUID.randomUUID().toString().take(8),
            adminUserId = _currentUser.value.id,
            adminName = _currentUser.value.username,
            action = "MODERATE_SLOT_STATUS",
            targetType = "SLOT",
            targetId = slot.id,
            details = "Changed slot status from ${slot.status} to $newStatus",
            reason = reason.ifBlank { "Standard moderation review" }
        )

        _slots.value = _slots.value.map { if (it.id == slotId) updated else it }
        _auditLogs.value = listOf(audit) + _auditLogs.value
        Result.success(Unit)
    }

    override suspend fun adminToggleTeamSuspension(teamId: String, reason: String): Result<Unit> = mutex.withLock {
        val team = _teams.value.find { it.id == teamId }
            ?: return Result.failure(NoSuchElementException("Team not found"))

        val updated = team.copy(isSuspended = !team.isSuspended)
        val action = if (updated.isSuspended) "SUSPEND_TEAM" else "UNSUSPEND_TEAM"

        val audit = AdminAuditLog(
            id = "audit_" + UUID.randomUUID().toString().take(8),
            adminUserId = _currentUser.value.id,
            adminName = _currentUser.value.username,
            action = action,
            targetType = "TEAM",
            targetId = team.id,
            details = if (updated.isSuspended) "Team suspended from hosting and rewards" else "Team restored to active standing",
            reason = reason.ifBlank { "Admin administrative action" }
        )

        _teams.value = _teams.value.map { if (it.id == teamId) updated else it }
        _auditLogs.value = listOf(audit) + _auditLogs.value
        Result.success(Unit)
    }

    override suspend fun adminProcessMonthlyRewardsForEligibleTeams(): Result<Int> = mutex.withLock {
        val currentMonth = getCurrentMonthKey()
        var processedCount = 0
        val newTxList = mutableListOf<GoldPointsTransaction>()
        val updatedTeams = _teams.value.map { team ->
            if (!team.isSuspended && team.monthlyRewardLastClaimedMonth != currentMonth) {
                processedCount++
                val newBal = team.goldPoints + 1000L
                newTxList.add(
                    GoldPointsTransaction(
                        id = "tx_batch_" + UUID.randomUUID().toString().take(8),
                        teamId = team.id,
                        teamName = team.name,
                        amount = 1000L,
                        balanceAfter = newBal,
                        type = TransactionType.MONTHLY_REWARD,
                        reason = "Admin Batch Processing: $currentMonth Monthly Promotional Reward (+1,000 GP)",
                        createdByUserId = _currentUser.value.id
                    )
                )
                team.copy(goldPoints = newBal, monthlyRewardLastClaimedMonth = currentMonth)
            } else {
                team
            }
        }

        if (processedCount > 0) {
            val audit = AdminAuditLog(
                id = "audit_" + UUID.randomUUID().toString().take(8),
                adminUserId = _currentUser.value.id,
                adminName = _currentUser.value.username,
                action = "BATCH_MONTHLY_REWARD",
                targetType = "TEAMS",
                targetId = "ALL_ELIGIBLE",
                details = "Processed 1,000 GP reward for $processedCount eligible teams ($currentMonth)",
                reason = "Scheduled monthly reward distribution"
            )
            _teams.value = updatedTeams
            _transactions.value = newTxList + _transactions.value
            _auditLogs.value = listOf(audit) + _auditLogs.value
        }

        Result.success(processedCount)
    }
}
