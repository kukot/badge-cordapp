package com.kukot.badge.contracts

import com.kukot.badge.states.BadgeState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.LedgerTransaction

const val ID = "com.kukot.badge.contracts.BadgeContract"

class BadgeContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        // a single command is used for now, for the sake of simplicity
        val command = tx.commands.requireSingleCommand<Commands>()
        val output = tx.outputsOfType<BadgeState>().single()
        when (command.value) {
            is Commands.Issue -> requireThat {
                "Issuer agency must be issuer node" using (output.issueAgency.name == CordaX500Name("IssuerNode", "London", "GB"))
            }
            is Commands.Show -> requireThat {
                "Show off balance must be more than 0" using (output.showOffCountBalance > 0)
            }
        }
    }

    interface Commands : CommandData {
        // issue badge & show badge command
        class Issue : TypeOnlyCommandData(), Commands
        class Show : TypeOnlyCommandData(), Commands
    }
}
