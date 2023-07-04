package com.walletconnect.modal.ui.routes.scan_code

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.walletconnect.modal.R
import com.walletconnect.modal.ui.components.ModalTopBar
import com.walletconnect.modal.ui.preview.ModalPreview
import com.walletconnect.modal.ui.theme.ModalTheme
import com.walletconnect.modalcore.ui.components.qr.WalletConnectQRCode

@Composable
internal fun ScanQRCodeRoute(
    navController: NavController,
    uri: String
) {
    ScanQRCodeContent(
        uri = uri,
        onBackArrowClick = navController::popBackStack
    )
}

@Composable
private fun ScanQRCodeContent(
    uri: String,
    onBackArrowClick: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Column {
        ModalTopBar(
            title = "Scan the code",
            onBackPressed = onBackArrowClick,
            endIcon = {
                Image(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_copy),
                    colorFilter = ColorFilter.tint(ModalTheme.colors.main),
                    contentDescription = "Scan Icon",
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                        clipboardManager.setText(AnnotatedString(uri))
                    }
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        WalletConnectQRCode(
            qrData = uri,
            primaryColor = ModalTheme.colors.onBackgroundColor,
            logoColor = ModalTheme.colors.main
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
private fun ScanQRCodePreview() {
    ModalPreview {
        ScanQRCodeContent("47442c19ea7c6a7a836fa3e53af1ddd375438daaeea9acdbf595e989a731b73249a10a7cc0e343ca627e536609",{})
    }
}