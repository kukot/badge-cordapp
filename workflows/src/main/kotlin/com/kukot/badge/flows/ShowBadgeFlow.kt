package com.kukot.badge.flows

import co.paralleluniverse.fibers.Suspendable
import com.kukot.badge.contracts.BadgeContract
import com.kukot.badge.states.BadgeState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.UUID

@InitiatingFlow
class ShowBadgeFlowInitiator(
    val badgeId: UUID
) : FlowLogic<String>() {

    override val progressTracker = ProgressTracker()

    @Throws(FlowException::class)
    @Suspendable
    override fun call(): String {
        // step 1: get a reference to the notary service on our network and our key pair
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // step 2: query the badge from the vault
        val inputCriteria = QueryCriteria.LinearStateQueryCriteria().withUuid(listOf(badgeId))
        val inputStateAndRef = serviceHub.vaultService.queryBy(BadgeState::class.java, inputCriteria).states.first()
        val inputBadge = inputStateAndRef.state.data

        // step 3: compose state that carries the Hello World message
        val balanceNew = inputBadge.showOffCountBalance - 1
        val outputBadge = BadgeState(
            inputBadge.badgeName,
            UniqueIdentifier(null, badgeId),
            inputBadge.issueDate,
            inputBadge.issueAgency,
            inputBadge.receiver,
            inputBadge.description,
            balanceNew
        )

        // step 4: create new transaction builder object
        val txBuilder = TransactionBuilder(notary)

        // step 5: add the BadgeState as an output state, as well as the command to the transaction builder
        txBuilder.addInputState(inputStateAndRef)
        txBuilder.addOutputState(outputBadge)
        txBuilder.addCommand(
            BadgeContract.Commands.Show(),
            listOf(ourIdentity.owningKey)
        )

        // step 6: Verify and sign it with our KeyPair
        txBuilder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(txBuilder)

        val session = initiateFlow(inputBadge.issueAgency)

        // step 7: assuming no exceptions, we can now finalise the transaction
        val signedTx = subFlow(FinalityFlow(ptx, session))

        return "The ${inputBadge.badgeName} badge has $balanceNew times left to show!"
    }
}

@InitiatedBy(ShowBadgeFlowInitiator::class)
class ShowBadgeFlowResponder(
    val counterpartySession: FlowSession
) : FlowLogic<Unit>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call() {
        val subLogic = object : SignTransactionFlow(counterpartySession) {

            @Throws(FlowException::class)
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) {
                // do nothing here for the sake of simplicity
            }
        }
        val signedTransaction = subFlow(subLogic)
        subFlow(ReceiveFinalityFlow(counterpartySession, signedTransaction.id))
    }
}
