package com.walletconnect.web3.inbox.push.request

import android.net.Uri
import com.walletconnect.push.client.Push
import com.walletconnect.push.client.PushWalletInterface
import com.walletconnect.web3.inbox.client.Inbox
import com.walletconnect.web3.inbox.client.toPush
import com.walletconnect.web3.inbox.common.proxy.PushProxyInteractor
import com.walletconnect.web3.inbox.json_rpc.Web3InboxParams
import com.walletconnect.web3.inbox.json_rpc.Web3InboxRPC

internal class SubscribeRequestUseCase(
    private val pushWalletClient: PushWalletInterface,
    private val onSign: (message: String) -> Inbox.Model.Cacao.Signature,
    proxyInteractor: PushProxyInteractor,
) : PushRequestUseCase<Web3InboxParams.Request.Push.SubscribeParams>(proxyInteractor) {

    override fun invoke(rpc: Web3InboxRPC, params: Web3InboxParams.Request.Push.SubscribeParams) {
        pushWalletClient.subscribe(
            Push.Params.Subscribe(Uri.parse(params.metadata.url), params.account, onSign = { message -> onSign(message).toPush() }),
            onSuccess = { respondWithVoid(rpc) },
            onError = { error -> respondWithError(rpc, error) }
        )
    }
}