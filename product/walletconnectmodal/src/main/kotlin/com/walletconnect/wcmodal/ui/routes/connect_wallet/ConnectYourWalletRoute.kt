package com.walletconnect.wcmodal.ui.routes.connect_wallet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.walletconnect.android.internal.common.modal.data.model.Wallet
import com.walletconnect.modal.ui.components.common.ClickableImage
import com.walletconnect.modal.ui.model.UiState
import com.walletconnect.wcmodal.R
import com.walletconnect.wcmodal.ui.components.ModalTopBar
import com.walletconnect.wcmodal.ui.components.WalletImage
import com.walletconnect.wcmodal.ui.components.WalletListItem
import com.walletconnect.wcmodal.ui.components.WalletsLazyGridView
import com.walletconnect.wcmodal.ui.components.walletsGridItems
import com.walletconnect.wcmodal.ui.navigation.Route
import com.walletconnect.wcmodal.ui.preview.ModalPreview
import com.walletconnect.wcmodal.ui.theme.ModalTheme
import com.walletconnect.modal.utils.isLandscape
import com.walletconnect.wcmodal.ui.ErrorModalState
import com.walletconnect.wcmodal.ui.LoadingModalState
import com.walletconnect.wcmodal.ui.WalletConnectModalViewModel

@Composable
internal fun ConnectYourWalletRoute(
    navController: NavController,
    viewModel: WalletConnectModalViewModel,
) {

    val uiState by viewModel.uiState.collectAsState()
    AnimatedContent(
        targetState = uiState,
        label = "UiStateBuilder $uiState",
        transitionSpec = { fadeIn() + slideInHorizontally { it / 2 } togetherWith fadeOut() }
    ) { state ->
        when (state) {
            is UiState.Error -> ErrorModalState { viewModel.fetchInitialWallets() }
            is UiState.Loading -> LoadingModalState()
            is UiState.Success -> ConnectYourWalletContent(
                wallets = state.data,
                onWalletItemClick = { navController.navigate(Route.OnHold.path + "/${it.id}") },
                onViewAllClick = { navController.navigate(Route.AllWallets.path) },
                onScanIconClick = { navController.navigate(Route.ScanQRCode.path) }
            )
        }
    }
}

@Composable
private fun ConnectYourWalletContent(
    wallets: List<Wallet>,
    onWalletItemClick: (Wallet) -> Unit,
    onViewAllClick: () -> Unit,
    onScanIconClick: () -> Unit,
) {
    Column {
        ModalTopBar(title = "Connect your wallet", endIcon = {
            ClickableImage(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_scan),
                tint = ModalTheme.colors.main,
                contentDescription = "Scan Icon",
                onClick = onScanIconClick
            )
        })
        WalletsGrid(
            wallets = wallets,
            onWalletItemClick = onWalletItemClick,
            onViewAllClick = onViewAllClick,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WalletsGrid(
    wallets: List<Wallet>,
    onWalletItemClick: (Wallet) -> Unit,
    onViewAllClick: () -> Unit
) {
    val isLandscape = isLandscape
    if (wallets.isNotEmpty()) {
        WalletsLazyGridView(
            modifier = Modifier.fillMaxWidth(),
        ) { walletsInColumn ->
            if (wallets.size <= walletsInColumn) {
                walletsGridItems(wallets, onWalletItemClick)
            } else {
                walletsGridItemsWithViewAll(
                    if (isLandscape) walletsInColumn else walletsInColumn * 2,
                    wallets,
                    onWalletItemClick,
                    onViewAllClick
                )
            }
        }
    } else {
        NoWalletsFoundItem()
    }
}

@Composable
private fun NoWalletsFoundItem() {
    Text(
        text = "No wallets found",
        style = TextStyle(color = ModalTheme.colors.secondaryTextColor, fontSize = 16.sp),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 50.dp)
    )
}

private fun LazyGridScope.walletsGridItemsWithViewAll(
    maxGridElementsSize: Int,
    wallets: List<Wallet>,
    onWalletItemClick: (Wallet) -> Unit,
    onViewAllClick: () -> Unit
) {
    val walletsSize = maxGridElementsSize - 1
    itemsIndexed(
        wallets.take(walletsSize),
        key = { _, wallet -> wallet.id }
    ) { _, wallet ->
        WalletListItem(
            wallet = wallet,
            onWalletItemClick = onWalletItemClick
        )
    }
    item {
        ViewAllItem(wallets.subList(walletsSize, wallets.size), onViewAllClick)
    }
}

@Composable
private fun ViewAllItem(
    wallets: List<Wallet>,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onViewAllClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .size(80.dp)
                .padding(10.dp)
                .background(ModalTheme.colors.secondaryBackgroundColor, shape = RoundedCornerShape(14.dp))
                .border(1.dp, ModalTheme.colors.dividerColor, shape = RoundedCornerShape(14.dp))
                .padding(1.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            wallets.chunked(2).forEach {
                Row {
                    it.forEach { item ->
                        WalletImage(
                            url = item.imageUrl, Modifier
                                .size(30.dp)
                                .padding(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

        }
        Text(text = "View All", style = TextStyle(color = ModalTheme.colors.onBackgroundColor, fontSize = 12.sp))
    }
}

@Preview
@Composable
private fun ConnectYourWalletPreview() {
    ModalPreview {
        ConnectYourWalletContent(listOf(), {}, {}, {})
    }
}
