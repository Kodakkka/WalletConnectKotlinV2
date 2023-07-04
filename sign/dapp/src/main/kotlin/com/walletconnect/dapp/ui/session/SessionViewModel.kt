package com.walletconnect.dapp.ui.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletconnect.dapp.domain.DappDelegate
import com.walletconnect.dapp.domain.PushDappDelegate
import com.walletconnect.dapp.ui.SampleDappEvents
import com.walletconnect.push.common.Push
import com.walletconnect.push.dapp.client.PushDappClient
import com.walletconnect.sample_common.Chains
import com.walletconnect.sample_common.tag
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SessionViewModel : ViewModel() {
    private val _sessionUI: MutableStateFlow<List<SessionUI>> = MutableStateFlow(getSessions())
    val uiState: StateFlow<List<SessionUI>> = _sessionUI.asStateFlow()

    private val _navigationEvents: MutableSharedFlow<SampleDappEvents> = MutableSharedFlow()
    val navigationEvents: SharedFlow<SampleDappEvents> = _navigationEvents.asSharedFlow()

    init {
        DappDelegate.wcEventModels
            .filterNotNull()
            .onEach { walletEvent ->
                when (walletEvent) {
                    is Sign.Model.UpdatedSession -> {
                        _sessionUI.value = getSessions(walletEvent.topic)
                    }
                    is Sign.Model.DeletedSession -> {
                        _navigationEvents.emit(SampleDappEvents.Disconnect)
                    }
                    else -> Unit
                }
            }.launchIn(viewModelScope)

    }

    private fun getSessions(topic: String? = null): List<SessionUI> {
        return SignClient.getListOfActiveSessions().filter {
            if (topic != null) {
                it.topic == topic
            } else {
                it.topic == DappDelegate.selectedSessionTopic
            }
        }.flatMap { settledSession ->
            settledSession.namespaces.values.flatMap { it.accounts }
        }.map { caip10Account ->
            val (chainNamespace, chainReference, account) = caip10Account.split(":")
            val chain = Chains.values().first { chain ->
                chain.chainNamespace == chainNamespace && chain.chainReference == chainReference
            }

            SessionUI(chain.icon, chain.name, account, chain.chainNamespace, chain.chainReference)
        }
    }

    fun ping() {
        val pingParams = Sign.Params.Ping(topic = requireNotNull(DappDelegate.selectedSessionTopic))

        SignClient.ping(pingParams, object : Sign.Listeners.SessionPing {
            override fun onSuccess(pingSuccess: Sign.Model.Ping.Success) {
                viewModelScope.launch {
                    _navigationEvents.emit(SampleDappEvents.PingSuccess(pingSuccess.topic))
                }
            }

            override fun onError(pingError: Sign.Model.Ping.Error) {
                viewModelScope.launch {
                    _navigationEvents.emit(SampleDappEvents.PingError)
                }
            }
        })
    }

    fun disconnect() {
        if (DappDelegate.selectedSessionTopic != null) {
            val disconnectParams = Sign.Params.Disconnect(sessionTopic = requireNotNull(DappDelegate.selectedSessionTopic))

            SignClient.disconnect(disconnectParams) { error ->
                Log.e(tag(this), error.throwable.stackTraceToString())
            }
            DappDelegate.deselectAccountDetails()

            PushDappClient.getActiveSubscriptions().entries.firstOrNull()?.value?.topic?.let { pushTopic ->
                PushDappClient.deleteSubscription(Push.Dapp.Params.Delete(pushTopic)) { error ->
                    Log.e(tag(this), error.throwable.stackTraceToString())
                }
            }
        }

        viewModelScope.launch {
            _navigationEvents.emit(SampleDappEvents.Disconnect)
        }
    }

    fun pushRequest() {
        //TODO: commented out since it's not a part of PushDappClient any more
//        val pairingTopic = CoreClient.Pairing.getPairings().map { it.topic }.first { pairingTopic ->
//            SignClient.getListOfActiveSessions().any { session -> session.pairingTopic == pairingTopic }
//        }
//        val ethAccount = SignClient.getListOfActiveSessions().first { session ->
//            session.pairingTopic == pairingTopic
//        }.namespaces.entries.first().value.accounts.first()
//
//        PushDappClient.request(Push.Dapp.Params.Request(ethAccount, pairingTopic), { pushRequestId ->
//            Log.e(tag(this), "Request sent with id ${pushRequestId.id}")
//        }, {
//            Log.e(tag(this), it.throwable.stackTraceToString())
//        })
    }

    fun pushNotify() {
        val pushTopic = PushDappDelegate.activePushSubscription?.topic ?: PushDappClient.getActiveSubscriptions().keys.first()
        val pushMessage = Push.Model.Message(
            title = "Kotlin Dapp Title",
            body = "Kotlin Dapp Body",
            icon = "https://raw.githubusercontent.com/WalletConnect/walletconnect-assets/master/Icon/Gradient/Icon.png",
            url = "https://walletconnect.com",
            type = ""
        )
        val notifyParams = Push.Dapp.Params.Notify(pushTopic, pushMessage)

        PushDappClient.notify(notifyParams) { error ->
            Log.e(tag(this), error.throwable.stackTraceToString())
        }
    }
}