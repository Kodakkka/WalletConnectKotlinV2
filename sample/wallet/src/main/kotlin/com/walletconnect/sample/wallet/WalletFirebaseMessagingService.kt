package com.walletconnect.sample.wallet

import android.annotation.SuppressLint
import com.google.firebase.messaging.RemoteMessage
import com.walletconnect.android.internal.common.di.AndroidCommonDITags
import com.walletconnect.android.internal.common.wcKoinApp
import com.walletconnect.foundation.util.Logger
import com.walletconnect.notify.client.Notify
import com.walletconnect.notify.client.NotifyMessageService
import com.walletconnect.sample.wallet.domain.NotificationHandler
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.named

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class WalletFirebaseMessagingService : NotifyMessageService() {
    private val logger: Logger by lazy { wcKoinApp.koin.get(named(AndroidCommonDITags.LOGGER)) }

    override fun newToken(token: String) {
        logger.log("Registering New Token Success:\t$token")
    }

    override fun registeringFailed(token: String, throwable: Throwable) {
        logger.log("Registering New Token Failed:\t$token")
    }

    override fun onMessage(message: Notify.Model.Message, originalMessage: RemoteMessage) {
        logger.log("Message:\t$message")

        runBlocking { NotificationHandler.addNotification(message) }
    }

    override fun onDefaultBehavior(message: RemoteMessage) {
        logger.log("onDefaultBehavior: ${message.to}")
    }

    override fun onError(throwable: Throwable, defaultMessage: RemoteMessage) {
        logger.error("onError Message To: ${defaultMessage.to}, throwable: $throwable")
    }
}