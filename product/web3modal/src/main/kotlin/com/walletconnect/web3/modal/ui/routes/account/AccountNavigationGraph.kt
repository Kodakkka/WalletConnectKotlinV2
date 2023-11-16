package com.walletconnect.web3.modal.ui.routes.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.walletconnect.web3.modal.ui.components.internal.snackbar.LocalSnackBarHandler
import com.walletconnect.web3.modal.ui.navigation.Route
import com.walletconnect.web3.modal.ui.navigation.account.chainSwitchRoute
import com.walletconnect.web3.modal.ui.routes.account.account.AccountRoute
import com.walletconnect.web3.modal.ui.routes.account.change_network.ChangeNetworkRoute
import com.walletconnect.web3.modal.ui.routes.account.what_is_network.WhatIsNetworkRoute
import com.walletconnect.web3.modal.ui.utils.AnimatedNavGraph

@Composable
internal fun AccountNavGraph(
    navController: NavHostController,
    closeModal: () -> Unit,
    shouldOpenChangeNetwork: Boolean
) {
    val snackBar = LocalSnackBarHandler.current
    val accountState = rememberAccountState(
        coroutineScope = rememberCoroutineScope(),
        navController = navController,
        closeModal = closeModal,
        showError = { message -> snackBar.showErrorSnack(message ?: "Something went wrong") }
    )
    val startDestination = if (shouldOpenChangeNetwork) Route.CHANGE_NETWORK.path else Route.ACCOUNT.path
    AnimatedNavGraph(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Route.ACCOUNT.path) {
            AccountRoute(
                accountState = accountState,
                navController = navController
            )
        }
        composable(route = Route.CHANGE_NETWORK.path) {
            ChangeNetworkRoute(accountState = accountState)
        }
        composable(route = Route.WHAT_IS_WALLET.path) {
            WhatIsNetworkRoute()
        }
        chainSwitchRoute(accountState = accountState)
    }
}