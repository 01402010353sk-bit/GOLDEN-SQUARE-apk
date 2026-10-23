package com.example.data.model

enum class UserRole {
    ORDINARY_MEMBER,
    TEAM_CAPTAIN,
    ADMIN
}

enum class MemberRole {
    CAPTAIN,
    SNIPER,
    ASSAULTER,
    RUSHER,
    SUPPORT,
    SUBSTITUTE
}

data class User(
    val id: String,
    val username: String,
    val ign: String, // In-game name
    val ffUid: String, // Free Fire UID
    val email: String,
    val role: UserRole,
    val teamId: String?,
    val avatarUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class TeamMember(
    val userId: String,
    val username: String,
    val ign: String,
    val ffUid: String,
    val role: MemberRole,
    val joinedAt: Long = System.currentTimeMillis()
)

data class Team(
    val id: String,
    val name: String,
    val tag: String,
    val captainUserId: String,
    val captainName: String,
    val logoType: String = "GOLD_SQUARE",
    val members: List<TeamMember>,
    val goldPoints: Long = 0L,
    val registrationDate: Long = System.currentTimeMillis(),
    val isSuspended: Boolean = false,
    val monthlyRewardLastClaimedMonth: String = "", // Format: "YYYY-MM"
    val winsCount: Int = 0,
    val matchesPlayed: Int = 0
)

enum class TransactionType {
    MONTHLY_REWARD,
    SLOT_CREATION_FEE,
    ADMIN_ADJUSTMENT,
    ADMIN_REFUND,
    SCRIM_PRIZE
}

data class GoldPointsTransaction(
    val id: String,
    val teamId: String,
    val teamName: String,
    val amount: Long, // Positive for credit, negative for debit
    val balanceAfter: Long,
    val type: TransactionType,
    val reason: String,
    val relatedSlotId: String? = null,
    val idempotencyKey: String? = null,
    val createdByUserId: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class GameMode(val displayName: String) {
    BATTLE_ROYALE_SQUAD("BR Squad (12 Squads)"),
    CLASH_SQUAD_4V4("Clash Squad (4v4)"),
    LONE_WOLF("Lone Wolf (2v2)"),
    BATTLE_ROYALE_SOLO("BR Solo (48 Players)")
}

enum class FreeFireMap(val displayName: String) {
    BERMUDA("Bermuda"),
    PURGATORY("Purgatory"),
    KALAHARI("Kalahari"),
    ALPINE("Alpine"),
    NEXTERRA("Nexterra")
}

enum class SlotStatus(val label: String) {
    DRAFT("Draft"),
    PENDING_REVIEW("Pending Review"),
    PUBLISHED("Published"),
    FULL("Full"),
    CLOSED("Closed"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed")
}

enum class JoinRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class JoinRequest(
    val requestId: String,
    val slotId: String,
    val requestingTeamId: String,
    val requestingTeamName: String,
    val captainName: String,
    val captainUid: String,
    val requestedSlotNumber: Int? = null,
    val status: JoinRequestStatus = JoinRequestStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis()
)

data class JoinedTeam(
    val teamId: String,
    val teamName: String,
    val captainName: String,
    val slotNumber: Int,
    val joinedAt: Long = System.currentTimeMillis()
)

data class CustomSlot(
    val id: String,
    val title: String,
    val hostTeamId: String,
    val hostTeamName: String,
    val hostCaptainName: String,
    val hostCaptainUserId: String,
    val gameMode: GameMode,
    val mapName: FreeFireMap,
    val matchDateTime: String,
    val matchTimestamp: Long,
    val maxTeams: Int, // e.g. 12 squads
    val filledTeamsCount: Int,
    val entryRequirements: String,
    val rules: String,
    val contactInstructions: String,
    val status: SlotStatus,
    val creationFeePaid: Long = 50L,
    val roomId: String, // Private, only revealed to host or approved participants
    val roomPassword: String, // Private
    val joinedTeams: List<JoinedTeam> = emptyList(),
    val joinRequests: List<JoinRequest> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class AdminAuditLog(
    val id: String,
    val adminUserId: String,
    val adminName: String,
    val action: String,
    val targetType: String, // TEAM, SLOT, TRANSACTION
    val targetId: String,
    val details: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class MonthlyRewardEligibility(
    val teamId: String,
    val isEligible: Boolean,
    val currentMonthKey: String,
    val lastClaimedMonth: String,
    val nextEligibleDateStr: String,
    val rewardAmount: Long = 1000L
)
