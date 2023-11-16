package com.walletconnect.web3.modal.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walletconnect.android.internal.common.wcKoinApp
import com.walletconnect.foundation.util.Logger
import com.walletconnect.web3.modal.client.Modal
import com.walletconnect.web3.modal.client.Web3Modal
import com.walletconnect.web3.modal.domain.usecase.GetSelectedChainUseCase
import com.walletconnect.web3.modal.domain.usecase.GetSessionTopicUseCase
import com.walletconnect.web3.modal.domain.usecase.SaveChainSelectionUseCase
import com.walletconnect.web3.modal.domain.usecase.SaveSessionTopicUseCase
import com.walletconnect.web3.modal.utils.getSelectedChain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class Web3ModalViewModel: ViewModel() {

    private val logger: Logger = wcKoinApp.koin.get()

    private val saveSessionTopicUseCase: SaveSessionTopicUseCase = wcKoinApp.koin.get()
    private val saveChainSelectionUseCase: SaveChainSelectionUseCase = wcKoinApp.koin.get()
    private val getSessionTopicUseCase: GetSessionTopicUseCase = wcKoinApp.koin.get()
    private val getSelectedChainUseCase: GetSelectedChainUseCase = wcKoinApp.koin.get()

    private val _modalState: MutableStateFlow<Web3ModalState> = MutableStateFlow(Web3ModalState.Loading)

    val modalState: StateFlow<Web3ModalState>
        get() = _modalState.asStateFlow()

    init {
        require(Web3Modal.chains.isNotEmpty()) { "Be sure to set the Chains using Web3Modal.setChains" }
        initModalState()
    }

    internal fun initModalState() {
        viewModelScope.launch {
            getActiveSession()?.let { activeSession ->
                createAccountModalState()
            } ?: createConnectModalState()
        }
    }

    private suspend fun getActiveSession(): Modal.Model.Session? =
        getSessionTopicUseCase()?.let {
            Web3Modal.getActiveSessionByTopic(it)
        }

    private fun createAccountModalState() {
        _modalState.value = Web3ModalState.AccountState
    }

    private fun createConnectModalState() {
        _modalState.value = Web3ModalState.Connect
    }
    internal fun saveSession(event: Modal.Model.ApprovedSession) = viewModelScope.launch {
        saveSessionTopicUseCase(event.topic)
        saveChainSelectionUseCase(Web3Modal.chains.getSelectedChain(getSelectedChainUseCase()).id)
    }
}
