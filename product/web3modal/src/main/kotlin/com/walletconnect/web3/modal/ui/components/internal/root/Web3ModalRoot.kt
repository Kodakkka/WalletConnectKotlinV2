@file:OptIn(ExperimentalComposeUiApi::class)

package com.walletconnect.web3.modal.ui.components.internal.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.walletconnect.modal.ui.components.common.roundedClickable
import com.walletconnect.web3.modal.client.Modal
import com.walletconnect.web3.modal.domain.delegate.Web3ModalDelegate
import com.walletconnect.web3.modal.ui.components.internal.Web3ModalTopBar
import com.walletconnect.web3.modal.ui.components.internal.commons.BackArrowIcon
import com.walletconnect.web3.modal.ui.components.internal.commons.FullWidthDivider
import com.walletconnect.web3.modal.ui.components.internal.commons.FullWidthOrDivider
import com.walletconnect.web3.modal.ui.components.internal.commons.QuestionMarkIcon
import com.walletconnect.web3.modal.ui.components.internal.commons.VerticalSpacer
import com.walletconnect.web3.modal.ui.components.internal.email.input.EmailInput
import com.walletconnect.web3.modal.ui.components.internal.email.input.EmailInputState
import com.walletconnect.web3.modal.ui.components.internal.email.input.rememberEmailInputState
import com.walletconnect.web3.modal.ui.components.internal.snackbar.ModalSnackBarHost
import com.walletconnect.web3.modal.ui.components.internal.snackbar.SnackBarState
import com.walletconnect.web3.modal.ui.components.internal.snackbar.rememberSnackBarState
import com.walletconnect.web3.modal.ui.navigation.Route
import com.walletconnect.web3.modal.ui.previews.MultipleComponentsPreview
import com.walletconnect.web3.modal.ui.previews.UiModePreview
import com.walletconnect.web3.modal.ui.theme.ProvideWeb3ModalThemeComposition
import com.walletconnect.web3.modal.ui.theme.Web3ModalTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach

@Composable
internal fun Web3ModalRoot(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    closeModal: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val rootState = rememberWeb3ModalRootState(coroutineScope = scope, navController = navController)
    val snackBarState = rememberSnackBarState(coroutineScope = scope)
    val emailInputState = rememberEmailInputState { input ->
        //todo: validate input, trigger magic to send email
//        rootState.navigateToRegisterDevice()
        rootState.navigateToConfirmEmail()
    }
    val title by rootState.title.collectAsState(null)

    LaunchedEffect(Unit) {
        Web3ModalDelegate
            .wcEventModels
            .filterIsInstance<Modal.Model.Error>()
            .onEach { event -> snackBarState.showErrorSnack(event.throwable.localizedMessage ?: "Something went wrong") }
            .collect()
    }

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier
    ) {
        ProvideWeb3ModalThemeComposition {
            Web3ModalRoot(rootState, emailInputState, snackBarState, title, closeModal, content)
        }
    }
}

@Composable
internal fun Web3ModalRoot(
    rootState: Web3ModalRootState,
    emailInputState: EmailInputState,
    snackBarState: SnackBarState,
    title: String?,
    closeModal: () -> Unit,
    content: @Composable () -> Unit
) {

    ModalSnackBarHost(snackBarState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Web3ModalTheme.colors.background.color125)
        ) {
            title?.let { title ->
                Web3ModalTopBar(
                    title = title,
                    startIcon = { TopBarStartIcon(rootState) },
                    onCloseIconClick = closeModal
                )
                FullWidthDivider()
                if (rootState.currentDestinationRoute == Route.CONNECT_YOUR_WALLET.path) {
                    VerticalSpacer(6.dp)
                    EmailInput(emailInputState)
                    FullWidthOrDivider()
                }
            }
            content()
        }
    }
}

@Composable
private fun TopBarStartIcon(
    rootState: Web3ModalRootState
) {
    if (rootState.canPopUp) {
        val keyboardController = LocalSoftwareKeyboardController.current
        BackArrowIcon(onClick = {
            keyboardController?.hide()
            rootState.popUp()
        })
    } else {
        when (rootState.currentDestinationRoute) {
            Route.CONNECT_YOUR_WALLET.path -> QuestionMarkIcon(
                modifier = Modifier
                    .size(36.dp)
                    .roundedClickable(onClick = rootState::navigateToHelp)
                    .padding(10.dp)
            )
        }
    }
}

@Composable
@UiModePreview
private fun PreviewWeb3ModalRoot() {
    val content: @Composable () -> Unit = { Box(modifier = Modifier.size(200.dp)) }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val rootState = rememberWeb3ModalRootState(coroutineScope = scope, navController = navController)
    val snackBarState = rememberSnackBarState(coroutineScope = scope)
    val emailInputState = rememberEmailInputState { }

    MultipleComponentsPreview(
        { Web3ModalRoot(rootState, emailInputState, snackBarState, null, {}, { content() }) },
        { Web3ModalRoot(rootState, emailInputState, snackBarState, "Top Bar Title", {}, { content() }) }
    )
}

