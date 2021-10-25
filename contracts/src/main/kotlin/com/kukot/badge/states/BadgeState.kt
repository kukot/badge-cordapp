package com.kukot.badge.states

import com.kukot.badge.contracts.BadgeContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.LocalDate

@BelongsToContract(BadgeContract::class)
data class BadgeState(
    val badgeName: String,
    val badgeId: UniqueIdentifier,
    val issueDate: LocalDate,
    val issueAgency: Party,
    val receiver: Party,
    val description: String,
    val showOffCountBalance: Int,
    override val participants: List<AbstractParty> = listOf(receiver),
    override val linearId: UniqueIdentifier = badgeId
) : LinearState
