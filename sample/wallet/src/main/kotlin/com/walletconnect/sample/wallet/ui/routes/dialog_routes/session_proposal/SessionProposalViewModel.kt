package com.walletconnect.sample.wallet.ui.routes.dialog_routes.session_proposal

import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.walletconnect.sample.wallet.domain.WCDelegate
import com.walletconnect.sample.wallet.ui.common.peer.PeerUI
import com.walletconnect.sample.wallet.ui.common.peer.toPeerUI
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet

class SessionProposalViewModel : ViewModel() {
    val sessionProposal: SessionProposalUI? = generateSessionProposalUI()
    fun approve(proposalPublicKey: String, onSuccess: (String) -> Unit = {}, onError: (String) -> Unit = {}) {
        if (Web3Wallet.getSessionProposals().isNotEmpty()) {
            try {
                val sessionProposal: Wallet.Model.SessionProposal = requireNotNull(Web3Wallet.getSessionProposals().find { it.proposerPublicKey == proposalPublicKey })
                val sessionNamespaces = Web3Wallet.generateApprovedNamespaces(sessionProposal = sessionProposal, supportedNamespaces = walletMetaData.namespaces)
                val approveProposal = Wallet.Params.SessionApprove(proposerPublicKey = sessionProposal.proposerPublicKey, namespaces = sessionNamespaces)

                Web3Wallet.approveSession(approveProposal,
                    onError = { error ->
                        Firebase.crashlytics.recordException(error.throwable)
                        WCDelegate.sessionProposalEvent = null
                        onError(error.throwable.message ?: "Undefined error, please check your Internet connection")
                    },
                    onSuccess = {
                        WCDelegate.sessionProposalEvent = null
                        onSuccess(sessionProposal.redirect)
                    })
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                WCDelegate.sessionProposalEvent = null
                onError(e.message ?: "Undefined error, please check your Internet connection")
            }
        } else {
            onError("Proposal expired")
        }
    }

    fun reject(proposalPublicKey: String, onSuccess: (String) -> Unit = {}, onError: (String) -> Unit = {}) {
        if (Web3Wallet.getSessionProposals().isNotEmpty()) {
            try {
                val sessionProposal: Wallet.Model.SessionProposal = requireNotNull(Web3Wallet.getSessionProposals().find { it.proposerPublicKey == proposalPublicKey })
                val rejectionReason = "Reject Session"
                val reject = Wallet.Params.SessionReject(
                    proposerPublicKey = sessionProposal.proposerPublicKey,
                    reason = rejectionReason
                )

                Web3Wallet.rejectSession(reject,
                    onSuccess = {
                        WCDelegate.sessionProposalEvent = null
                        onSuccess(sessionProposal.redirect)
                    },
                    onError = { error ->
                        Firebase.crashlytics.recordException(error.throwable)
                        WCDelegate.sessionProposalEvent = null
                        onError(error.throwable.message ?: "Undefined error, please check your Internet connection")
                    })
            } catch (e: Exception) {
                Firebase.crashlytics.recordException(e)
                WCDelegate.sessionProposalEvent = null
                onError(e.message ?: "Undefined error, please check your Internet connection")
            }
        } else {
            onError("Proposal expired")
        }
    }

    private fun generateSessionProposalUI(): SessionProposalUI? {
        return if (WCDelegate.sessionProposalEvent != null) {
            val (proposal, context) = WCDelegate.sessionProposalEvent!!
            SessionProposalUI(
                peerUI = PeerUI(
                    peerIcon = proposal.icons.firstOrNull().toString(),
                    peerName = proposal.name,
                    peerDescription = proposal.description,
                    peerUri = proposal.url,
                ),
                namespaces = proposal.requiredNamespaces,
                optionalNamespaces = proposal.optionalNamespaces,
                peerContext = context.toPeerUI(),
                redirect = proposal.redirect,
                pubKey = proposal.proposerPublicKey
            )
        } else null
    }
}