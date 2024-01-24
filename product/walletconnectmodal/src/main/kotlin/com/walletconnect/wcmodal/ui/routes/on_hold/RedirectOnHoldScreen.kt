package com.walletconnect.wcmodal.ui.routes.on_hold

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.walletconnect.android.internal.common.modal.data.model.Wallet
import com.walletconnect.modal.ui.components.common.HorizontalSpacer
import com.walletconnect.modal.ui.components.common.VerticalSpacer
import com.walletconnect.modal.ui.components.common.WeightSpacer
import com.walletconnect.modal.utils.goToNativeWallet
import com.walletconnect.modal.utils.openWebAppLink
import com.walletconnect.modal.utils.openPlayStore
import com.walletconnect.wcmodal.R
import com.walletconnect.wcmodal.client.Modal
import com.walletconnect.wcmodal.domain.WalletConnectModalDelegate
import com.walletconnect.wcmodal.ui.WalletConnectModalViewModel
import com.walletconnect.wcmodal.ui.components.ModalTopBar
import com.walletconnect.wcmodal.ui.components.RoundedMainButton
import com.walletconnect.wcmodal.ui.components.WalletImage
import com.walletconnect.wcmodal.ui.preview.ModalPreview
import com.walletconnect.wcmodal.ui.theme.ModalTheme

@Composable
internal fun RedirectOnHoldScreen(
    navController: NavController,
    wallet: Wallet,
    viewModel: WalletConnectModalViewModel
) {
    val uriHandler = LocalUriHandler.current
    val redirectState = remember { mutableStateOf<RedirectState>(RedirectState.Loading) }

    LaunchedEffect(Unit) {
        WalletConnectModalDelegate
            .wcEventModels
            .collect {
                when (it) {
                    is Modal.Model.RejectedSession -> redirectState.value = RedirectState.Reject
                    else -> redirectState.value = RedirectState.Loading
                }
            }
    }

    RedirectOnHoldScreen(
        wallet = wallet,
        state = redirectState.value,
        onBackPressed = navController::popBackStack,
        onRetry = {
            viewModel.connect { uri ->
                redirectState.value = RedirectState.Loading
                uriHandler.goToNativeWallet(uri, wallet.mobileLink)
            }
        },
        onOpenWebLink = {
            viewModel.connect { uri ->
                uriHandler.openWebAppLink(uri, wallet.webAppLink)
            }
        },
        onOpenPlayStore = { uriHandler.openPlayStore(wallet.playStore) }
    )

    LaunchedEffect(Unit) {
        viewModel.connect { uri ->
            uriHandler.goToNativeWallet(uri, wallet.mobileLink)
        }
    }
}

@Composable
private fun RedirectOnHoldScreen(
    wallet: Wallet,
    state: RedirectState,
    onBackPressed: () -> Unit,
    onRetry: () -> Unit,
    onOpenWebLink: () -> Unit,
    onOpenPlayStore: () -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ModalTopBar(
                title = wallet.name,
                onBackPressed = onBackPressed
            )
            VerticalSpacer(height = 20.dp)
            RedirectStateContent(state = state, wallet = wallet)
            VerticalSpacer(height = 20.dp)
        }
        BottomSection(wallet, onRetry, onOpenWebLink, onOpenPlayStore)
    }
}

@Composable
private fun RedirectStateContent(state: RedirectState, wallet: Wallet) {
    when (state) {
        RedirectState.Loading -> {
            WalletImageWithLoader(wallet.imageUrl)
            VerticalSpacer(height = 20.dp)
            Text(text = "Continue in ${wallet.name}...", color = ModalTheme.colors.textColor)
        }

        RedirectState.Reject -> {
            WalletImage(
                url = wallet.imageUrl,
                modifier = Modifier
                    .border(1.dp, ModalTheme.colors.errorColor, shape = RoundedCornerShape(22.dp))
                    .size(90.dp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            VerticalSpacer(height = 20.dp)
            Text(text = "Connection declined", color = ModalTheme.colors.errorColor)
        }
    }
}

@Composable
private fun WalletImageWithLoader(imageUrl: String) {
    val mainColor = ModalTheme.colors.main
    val infiniteTransition = rememberInfiniteTransition()

    val value by infiniteTransition.animateFloat(
        initialValue = 100f, targetValue = 0f, animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pathWithProgress by remember { mutableStateOf(Path()) }
    val pathMeasure by remember { mutableStateOf(PathMeasure()) }
    val path = remember { Path() }


    WalletImage(
        url = imageUrl,
        modifier = Modifier
            .size(90.dp)
            .drawBehind {
                if (path.isEmpty) {
                    path.addRoundRect(
                        RoundRect(
                            Rect(offset = Offset.Zero, size),
                            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                        )
                    )
                }
                pathWithProgress.reset()
                pathWithProgress.rewind()
                pathMeasure.setPath(path, forceClosed = true)
                pathMeasure.getSegment(
                    startDistance = pathMeasure.length * value / 100,
                    stopDistance = pathMeasure.length * value / 100 + minOf(250f, value * 10),
                    pathWithProgress,
                    startWithMoveTo = true
                )
                drawPath(
                    path = pathWithProgress,
                    style = Stroke(
                        3.dp.toPx()
                    ),
                    color = mainColor
                )
            }
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
    )
}

@Composable
private fun BottomSection(
    wallet: Wallet,
    onRetry: () -> Unit,
    onOpenUniversalLink: () -> Unit,
    onOpenPlayStore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ModalTheme.colors.secondaryBackgroundColor)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RoundedMainButton(
            text = "Retry",
            onClick = { onRetry() },
            endIcon = {
                Image(imageVector = ImageVector.vectorResource(id = R.drawable.ic_retry), contentDescription = null)
            }
        )
        VerticalSpacer(height = 10.dp)
        if (wallet.webAppLink != null) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text(text = "Still doesn't work?", color = ModalTheme.colors.secondaryTextColor)
                HorizontalSpacer(width = 2.dp)
                Text(text = "Try this alternate link", color = ModalTheme.colors.main, modifier = Modifier.clickable { onOpenUniversalLink() })
            }
        }
        Divider(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth(), color = ModalTheme.colors.dividerColor
        )
        PlayStoreRow(wallet, onOpenPlayStore)
    }
}

@Composable
private fun PlayStoreRow(wallet: Wallet, onOpenPlayStore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onOpenPlayStore() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WalletImage(
            url = wallet.imageUrl,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        HorizontalSpacer(width = 6.dp)
        Text(text = "Get ${wallet.name}", color = ModalTheme.colors.textColor)
        WeightSpacer()
        Text(text = "Play Store", color = ModalTheme.colors.secondaryTextColor)
        Image(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, colorFilter = ColorFilter.tint(ModalTheme.colors.secondaryTextColor))
    }
}

@Preview
@Composable
private fun OnHoldScreenPreview(
    @PreviewParameter(RedirectStateProvider::class) state: RedirectState
) {
    ModalPreview {
        val wallet = Wallet("Id", "Kotlin Wallet", "url", "", "", "", null, "", false)
        RedirectOnHoldScreen(wallet = wallet, state = state, onBackPressed = { }, onRetry = { }, onOpenWebLink = { }, onOpenPlayStore = {})
    }
}

private class RedirectStateProvider : PreviewParameterProvider<RedirectState> {
    override val values: Sequence<RedirectState>
        get() = sequenceOf(RedirectState.Loading, RedirectState.Reject)
}
