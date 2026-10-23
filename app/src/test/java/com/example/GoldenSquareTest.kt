package com.example

import com.example.data.model.*
import com.example.data.repository.GoldenSquareRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class GoldenSquareTest {

    private lateinit var repository: GoldenSquareRepositoryImpl

    @Before
    fun setup() {
        repository = GoldenSquareRepositoryImpl()
    }

    @Test
    fun testRankingsOrderingHighestToLowest() = runBlocking {
        val teams = repository.teams.value
        assertTrue("Teams list should not be empty", teams.isNotEmpty())

        val sorted = teams.sortedWith(
            compareByDescending<Team> { it.goldPoints }
                .thenByDescending { it.winsCount }
                .thenBy { it.registrationDate }
        )

        for (i in 0 until sorted.size - 1) {
            assertTrue(
                "Rankings must be sorted descending by Gold Points or tie breaker",
                sorted[i].goldPoints >= sorted[i + 1].goldPoints
            )
        }
    }

    @Test
    fun testMonthlyRewardEligibilityAndClaim() = runBlocking {
        // Team Alpha starts with monthlyRewardLastClaimedMonth = "2026-09", so it should be eligible for the current month
        val eligibility = repository.checkMonthlyRewardEligibility("team_alpha")
        assertTrue("Team Alpha should be eligible for monthly reward", eligibility.isEligible)

        val initialTeam = repository.getTeamById("team_alpha")!!
        val initialBalance = initialTeam.goldPoints

        val idempotencyKey = "test_claim_" + UUID.randomUUID().toString()
        val claimResult = repository.claimMonthlyReward("team_alpha", idempotencyKey)

        assertTrue("Claim should succeed", claimResult.isSuccess)
        val tx = claimResult.getOrNull()!!
        assertEquals(1000L, tx.amount)
        assertEquals(initialBalance + 1000L, tx.balanceAfter)

        // After claiming, team should not be eligible again for this month
        val secondEligibility = repository.checkMonthlyRewardEligibility("team_alpha")
        assertFalse("Team Alpha should no longer be eligible in the same month", secondEligibility.isEligible)

        // Second claim attempt should fail
        val duplicateClaimResult = repository.claimMonthlyReward("team_alpha", "different_key_" + UUID.randomUUID())
        assertTrue("Duplicate claim attempt in same month must fail", duplicateClaimResult.isFailure)
    }

    @Test
    fun testSlotCreationFeeDeductionExactly50Points() = runBlocking {
        val teamAlpha = repository.getTeamById("team_alpha")!!
        val startingBalance = teamAlpha.goldPoints
        assertTrue("Team must have at least 50 GP to host", startingBalance >= 50L)

        val idempotencyKey = "slot_pub_" + UUID.randomUUID().toString()
        val result = repository.publishCustomSlot(
            title = "Test Championship Scrim",
            gameMode = GameMode.BATTLE_ROYALE_SQUAD,
            mapName = FreeFireMap.BERMUDA,
            matchDateTime = "Tonight 20:00 UTC",
            matchTimestamp = System.currentTimeMillis() + 3600000L,
            maxTeams = 12,
            entryRequirements = "Open",
            rules = "Standard",
            contactInstructions = "Discord",
            roomId = "1122334",
            roomPassword = "SECRET_PASS_2026",
            idempotencyKey = idempotencyKey
        )

        assertTrue("Publishing slot should succeed", result.isSuccess)
        val createdSlot = result.getOrNull()!!
        assertEquals("Slot creation fee must be exactly 50 GP", 50L, createdSlot.creationFeePaid)

        val updatedTeam = repository.getTeamById("team_alpha")!!
        assertEquals("50 GP must be deducted from balance", startingBalance - 50L, updatedTeam.goldPoints)

        // Check transaction record
        val teamTx = repository.getTeamTransactions("team_alpha")
        val feeTx = teamTx.find { it.relatedSlotId == createdSlot.id }
        assertNotNull("Debit transaction must be recorded", feeTx)
        assertEquals(-50L, feeTx!!.amount)
        assertEquals(TransactionType.SLOT_CREATION_FEE, feeTx.type)
    }

    @Test
    fun testInsufficientBalanceBlocksSlotPublication() = runBlocking {
        // Register a poor team or reduce points to below 50
        // Switch to a new team
        val registerResult = repository.registerTeam("Broke Squad", "BROK", "BrokeCap", "111222333")
        assertTrue(registerResult.isSuccess)
        val newTeam = registerResult.getOrNull()!!

        // Deduct points until < 50 GP by publishing multiple slots
        var balance = newTeam.goldPoints
        while (balance >= 50L) {
            repository.publishCustomSlot(
                title = "Drain Slot",
                gameMode = GameMode.CLASH_SQUAD_4V4,
                mapName = FreeFireMap.BERMUDA,
                matchDateTime = "Now",
                matchTimestamp = System.currentTimeMillis(),
                maxTeams = 8,
                entryRequirements = "None",
                rules = "None",
                contactInstructions = "None",
                roomId = "123",
                roomPassword = "pass",
                idempotencyKey = UUID.randomUUID().toString()
            )
            balance = repository.getTeamById(newTeam.id)!!.goldPoints
        }

        assertTrue("Balance should now be less than 50 GP", balance < 50L)

        // Attempting to publish slot with insufficient balance
        val failureResult = repository.publishCustomSlot(
            title = "Unfunded Slot",
            gameMode = GameMode.BATTLE_ROYALE_SQUAD,
            mapName = FreeFireMap.PURGATORY,
            matchDateTime = "Now",
            matchTimestamp = System.currentTimeMillis(),
            maxTeams = 12,
            entryRequirements = "None",
            rules = "None",
            contactInstructions = "None",
            roomId = "999",
            roomPassword = "pass",
            idempotencyKey = UUID.randomUUID().toString()
        )

        assertTrue("Slot publication must fail with insufficient balance", failureResult.isFailure)
        assertTrue(
            "Error message must specify insufficient balance",
            failureResult.exceptionOrNull()?.message?.contains("Insufficient balance") == true
        )
    }

    @Test
    fun testDuplicateTransactionIdempotencyProtection() = runBlocking {
        val sameKey = "fixed_idempotency_token_12345"

        // First attempt with key
        val firstResult = repository.claimMonthlyReward("team_alpha", sameKey)
        assertTrue("First attempt should succeed", firstResult.isSuccess)

        // Immediate retry with the same key (e.g. rapid double tap or network retry)
        val retryResult = repository.claimMonthlyReward("team_alpha", sameKey)
        assertTrue("Idempotent retry should return existing transaction without doubling balance", retryResult.isSuccess)
        assertEquals(
            "Transaction IDs should match",
            firstResult.getOrNull()?.id,
            retryResult.getOrNull()?.id
        )
    }

    @Test
    fun testAdminRefundWorkflowWithAuditTrail() = runBlocking {
        // Publish a slot
        val pubResult = repository.publishCustomSlot(
            title = "Refundable Tournament",
            gameMode = GameMode.BATTLE_ROYALE_SQUAD,
            mapName = FreeFireMap.NEXTERRA,
            matchDateTime = "Tonight",
            matchTimestamp = System.currentTimeMillis(),
            maxTeams = 12,
            entryRequirements = "None",
            rules = "None",
            contactInstructions = "None",
            roomId = "555",
            roomPassword = "pass",
            idempotencyKey = UUID.randomUUID().toString()
        )
        assertTrue(pubResult.isSuccess)
        val slot = pubResult.getOrNull()!!

        val hostTeamBefore = repository.getTeamById(slot.hostTeamId)!!
        val balanceBeforeRefund = hostTeamBefore.goldPoints

        // Switch role to Admin and execute refund
        repository.switchUserRole(UserRole.ADMIN)
        val refundResult = repository.adminRefundSlotFee(slot.id, "Host emergency outage")
        assertTrue("Admin refund should succeed", refundResult.isSuccess)

        val hostTeamAfter = repository.getTeamById(slot.hostTeamId)!!
        assertEquals("Host team balance must increase by 50 GP", balanceBeforeRefund + 50L, hostTeamAfter.goldPoints)

        // Verify audit log created
        val auditLogs = repository.auditLogs.value
        val refundAudit = auditLogs.find { it.targetId == slot.id && it.action == "REFUND_SLOT_FEE" }
        assertNotNull("Admin refund must be recorded in the audit trail", refundAudit)
        assertEquals("Host emergency outage", refundAudit!!.reason)
    }
}
