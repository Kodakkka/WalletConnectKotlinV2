package com.walletconnect.sign.engine.use_case.calls

import com.walletconnect.android.internal.common.JsonRpcResponse
import com.walletconnect.android.internal.common.exception.CannotFindSequenceForTopic
import com.walletconnect.android.internal.common.exception.Invalid
import com.walletconnect.android.internal.common.exception.RequestExpiredException
import com.walletconnect.android.internal.common.model.Expiry
import com.walletconnect.android.internal.common.model.IrnParams
import com.walletconnect.android.internal.common.model.Tags
import com.walletconnect.android.internal.common.model.WCRequest
import com.walletconnect.android.internal.common.model.type.ClientParams
import com.walletconnect.android.internal.common.model.type.EngineEvent
import com.walletconnect.android.internal.common.model.type.JsonRpcInteractorInterface
import com.walletconnect.android.internal.common.scope
import com.walletconnect.android.internal.common.storage.verify.VerifyContextStorageRepository
import com.walletconnect.android.internal.utils.CoreValidator.isExpired
import com.walletconnect.android.internal.utils.fiveMinutesInSeconds
import com.walletconnect.foundation.common.model.Topic
import com.walletconnect.foundation.common.model.Ttl
import com.walletconnect.foundation.util.Logger
import com.walletconnect.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import com.walletconnect.sign.engine.sessionRequestEventsQueue
import com.walletconnect.sign.json_rpc.domain.GetPendingJsonRpcHistoryEntryByIdUseCase
import com.walletconnect.sign.json_rpc.model.JsonRpcMethod
import com.walletconnect.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class RespondSessionRequestUseCase(
    private val jsonRpcInteractor: JsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val getPendingJsonRpcHistoryEntryByIdUseCase: GetPendingJsonRpcHistoryEntryByIdUseCase,
    private val logger: Logger,
    private val verifyContextStorageRepository: VerifyContextStorageRepository,
    private val enableRequestsQueue: Boolean
) : RespondSessionRequestUseCaseInterface {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    override val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    override suspend fun respondSessionRequest(
        topic: String,
        jsonRpcResponse: JsonRpcResponse,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) = supervisorScope {
        val topicWrapper = Topic(topic)

        if (!sessionStorageRepository.isSessionValid(topicWrapper)) {
            logger.error("Request response -  invalid session: $topic, id: ${jsonRpcResponse.id}")
            return@supervisorScope onFailure(CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic"))
        }

        if (getPendingJsonRpcHistoryEntryByIdUseCase(jsonRpcResponse.id) == null) {
            sendExpiryError(topic, jsonRpcResponse)
            logger.error("Request doesn't exist: $topic, id: ${jsonRpcResponse.id}")
            throw RequestExpiredException("This request has expired, id: ${jsonRpcResponse.id}")
        }

        val expiry = getPendingJsonRpcHistoryEntryByIdUseCase(jsonRpcResponse.id)?.params?.request?.expiryTimestamp
        expiry?.let {
            checkExpiry(Expiry(it), topic, jsonRpcResponse)
        }

        val irnParams = IrnParams(Tags.SESSION_REQUEST_RESPONSE, Ttl(fiveMinutesInSeconds))
        logger.log("Sending session request response on topic: $topic, id: ${jsonRpcResponse.id}")
        jsonRpcInteractor.publishJsonRpcResponse(topic = Topic(topic), params = irnParams, response = jsonRpcResponse,
            onSuccess = {
                onSuccess()
                logger.log("Session request response sent successfully on topic: $topic, id: ${jsonRpcResponse.id}")
                scope.launch {
                    supervisorScope {
                        removePendingSessionRequestAndEmit(jsonRpcResponse.id)
                    }
                }
            },
            onFailure = { error ->
                logger.error("Sending session response error: $error, id: ${jsonRpcResponse.id}")
                onFailure(error)
            }
        )
    }

    private fun checkExpiry(expiry: Expiry, topic: String, jsonRpcResponse: JsonRpcResponse) {
        if (expiry.isExpired()) {
            sendExpiryError(topic, jsonRpcResponse)
            logger.error("Request Expired: $topic, id: ${jsonRpcResponse.id}")
            throw RequestExpiredException("This request has expired, id: ${jsonRpcResponse.id}")
        }
    }

    private fun sendExpiryError(topic: String, jsonRpcResponse: JsonRpcResponse) {
        val irnParams = IrnParams(Tags.SESSION_REQUEST_RESPONSE, Ttl(fiveMinutesInSeconds))
        val request = WCRequest(Topic(topic), jsonRpcResponse.id, JsonRpcMethod.WC_SESSION_REQUEST, object : ClientParams {})
        jsonRpcInteractor.respondWithError(request, Invalid.RequestExpired, irnParams, onSuccess = {
            if (enableRequestsQueue) {
                scope.launch {
                    supervisorScope {
                        removePendingSessionRequestAndEmit(jsonRpcResponse.id)
                    }
                }
            }
        })
    }

    private suspend fun removePendingSessionRequestAndEmit(id: Long) {
        verifyContextStorageRepository.delete(id)
        sessionRequestEventsQueue.find { pendingRequestEvent -> pendingRequestEvent.request.request.id == id }?.let { event ->
            sessionRequestEventsQueue.remove(event)
        }
        if (sessionRequestEventsQueue.isNotEmpty()) {
            sessionRequestEventsQueue.find { event -> if (event.request.expiry != null) !event.request.expiry.isExpired() else true }?.let { event ->
                _events.emit(event)
            }
        }
    }
}

internal interface RespondSessionRequestUseCaseInterface {
    val events: SharedFlow<EngineEvent>
    suspend fun respondSessionRequest(
        topic: String,
        jsonRpcResponse: JsonRpcResponse,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    )
}