package com.kukot.badge.flows

import co.paralleluniverse.fibers.Suspendable
import com.kukot.badge.contracts.BadgeContract
import com.kukot.badge.states.BadgeState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.LocalDate

@InitiatingFlow
@StartableByRPC
class IssueBadgeFlowInitiator(
    var badgeName: String,
    var receiver: Party,
    var description: String,
    var showOffCountBalance: Int
) : FlowLogic<String>() {

    private lateinit var badgeId: UniqueIdentifier
    private lateinit var issueAgency: Party
    override val progressTracker: ProgressTracker = ProgressTracker()

    @Throws(FlowException::class)
    @Suspendable
    override fun call(): String {

        // issue flow will run on Issuer only, do a self assignment
        issueAgency = ourIdentity
        badgeId = UniqueIdentifier()

        // step 1: get a ref to notary service 
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // step 2: compose the state that carries the Hello World message
        val output = BadgeState(
            badgeName,
            badgeId,
            LocalDate.now(),
            issueAgency,
            receiver,
            description,
            showOffCountBalance
        )
        // step 3: create new TransactionBuilder object
        val txBuilder = TransactionBuilder(notary)

        // step 4: add the badge as an out put state
        txBuilder.addOutputState(output)
        txBuilder.addCommand(
            BadgeContract.Commands.Issue(),
            listOf(issueAgency.owningKey, receiver.owningKey)
        )

        // step 5: verify and sign it with our KeyPair
        txBuilder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(txBuilder)

        // step 6: collect other parties' signatures using SignTransactionFlow.
        val otherParties = output.participants.map { it as Party }.toMutableList()
        otherParties.remove(ourIdentity)
        val sessions = otherParties.map { initiateFlow(it) }.toList()
        val signedTx = subFlow(CollectSignaturesFlow(ptx, sessions))

        // step 7: assuming no exceptions, we can now finalise the transaction
        val finalisedTx = subFlow(FinalityFlow(signedTx, sessions))

        return "$badgeName Badge (ID: $badgeId) issued to $receiver txID: ${finalisedTx.id}"
    }
}

@InitiatedBy(IssueBadgeFlowInitiator::class)
class IssueBadgeFlowResponder(
    val counterpartySession: FlowSession
) : FlowLogic<Unit>() {

    @Throws(FlowException::class)
    @Suspendable
    override fun call() {
        val subLogic = object : SignTransactionFlow(counterpartySession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) {
                /*
                    * SignTransactionFlow will automatically verify the transaction and its signatures before signing it.
                    * However, just because a transaction is contractually valid doesn’t mean we necessarily want to sign.
                    * What if we don’t want to deal with the counterparty in question, or the value is too high,
                    * or we’re not happy with the transaction’s structure? checkTransaction
                    * allows us to define these additional checks. If any of these conditions are not met,
                    * we will not sign the transaction - even if the transaction and its signatures are contractually valid.
                    * ----------
                    * For this hello-world cordapp, we will not implement any aditional checks.
                    * */
            }
        }
        val signedTx = subFlow(subLogic)
        // store the transaction to DB
        subFlow(ReceiveFinalityFlow(counterpartySession, signedTx.id))
    }
}
