package com.walletconnect.android.test.activity

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.walletconnect.android.BuildConfig
import com.walletconnect.android.internal.common.scope
import com.walletconnect.foundation.network.model.Relay
import com.walletconnect.android.test.utils.TestClient
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.fail
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds


class WCInstrumentedActivityScenario : BeforeAllCallback, AfterAllCallback {
    private var scenario: ActivityScenario<InstrumentedTestActivity>? = null
    private var scenarioLaunched: Boolean = false
    private val latch = CountDownLatch(1)
    private val testScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private fun initLogging() {
        if (Timber.treeCount == 0) {
            Timber.plant(
                object : Timber.DebugTree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        super.log(priority, "WalletConnectV2", message, t)
                    }
                }
            )
        }
    }

    init {
        initLogging()
        Timber.d("init")
    }

    override fun beforeAll(context: ExtensionContext?) {
        runBlocking(testScope.coroutineContext) {
            Timber.d("beforeAll")
            val isDappRelayReady = MutableStateFlow(false)
            val isWalletRelayReady = MutableStateFlow(false)

            val timeoutDuration = BuildConfig.TEST_TIMEOUT_SECONDS.seconds

            val isEverythingReady: StateFlow<Boolean> = combine(isDappRelayReady, isWalletRelayReady, TestClient.Primary.isInitialized, TestClient.Secondary.isInitialized)
            { dappRelay, walletRelay, dappSign, walletSign -> (dappRelay && walletRelay && dappSign && walletSign) }.stateIn(scope, SharingStarted.Eagerly, false)

            val dappRelayJob = TestClient.Secondary.Relay.eventsFlow.onEach { event ->
                when (event) {
                    is Relay.Model.Event.OnConnectionOpened<*> -> isDappRelayReady.compareAndSet(expect = false, update = true)
                    else -> {}
                }
            }.launchIn(scope)

            val walletRelayJob = TestClient.Primary.Relay.eventsFlow.onEach { event ->
                when (event) {
                    is Relay.Model.Event.OnConnectionOpened<*> -> isWalletRelayReady.compareAndSet(expect = false, update = true)
                    else -> {}
                }
            }.launchIn(scope)

            runCatching {
                withTimeout(timeoutDuration) {
                    while (!(isDappRelayReady.value && isWalletRelayReady.value && TestClient.Primary.isInitialized.value && TestClient.Secondary.isInitialized.value)) {
                        delay(100)
                    }
                }
            }.fold(
                onSuccess = { Timber.d("Connection established with: ${TestClient.RELAY_URL}") },
                onFailure = { fail("Unable to establish connection within $timeoutDuration") }
            )

            dappRelayJob.cancel()
            walletRelayJob.cancel()
        }
    }

    override fun afterAll(context: ExtensionContext?) {
        Timber.d("afterAll")
        scenario?.close()
    }

    fun launch(timeoutSeconds: Long = 1, testCodeBlock: suspend (scope: CoroutineScope) -> Unit) {
        require(!scenarioLaunched) { "Scenario has already been launched!" }

        scenario = ActivityScenario.launch(InstrumentedTestActivity::class.java)
        scenarioLaunched = true

        scenario?.moveToState(Lifecycle.State.RESUMED)
        assert(scenario?.state?.isAtLeast(Lifecycle.State.RESUMED) == true)

        testScope.launch { testCodeBlock(testScope) }

        try {
            assertTrue(latch.await(timeoutSeconds, TimeUnit.SECONDS))
        } catch (exception: Exception) {
            fail(exception)
        }
    }

    fun closeAsSuccess() {
        latch.countDown()
    }
}