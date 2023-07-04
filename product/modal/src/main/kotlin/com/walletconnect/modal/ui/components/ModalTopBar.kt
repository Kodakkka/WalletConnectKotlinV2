package com.walletconnect.modal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletconnect.modal.R
import com.walletconnect.modal.ui.preview.ComponentPreview
import com.walletconnect.modal.ui.theme.ModalTheme
import com.walletconnect.modalcore.ui.components.common.VerticalSpacer

@Composable
internal fun ModalTopBar(
    title: String,
    endIcon: (@Composable () -> Unit)? = null,
    onBackPressed: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .height(60.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = ModalTheme.colors.textColor,
                fontSize = 20.sp
            ),
            modifier = Modifier.align(Alignment.Center)
        )
        onBackPressed?.let { onBackClick ->
            Icon(
                tint = Color(0xFF3496ff),
                imageVector = ImageVector.vectorResource(id = R.drawable.chevron_left),
                contentDescription = "BackArrow",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable { onBackClick() }
            )
        }
        endIcon?.let {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                endIcon()
            }
        }
    }
}

@Preview
@Composable
private fun PreviewWeb3TopBar() {
    ComponentPreview {
        ModalTopBar(
            title = "Connect your wallet",
            endIcon = {
                ImageWithMainTint(icon = R.drawable.ic_scan)
            },
            onBackPressed = null
        )
        VerticalSpacer(height = 6.dp)
        ModalTopBar(
            title = "Scan the code",
            endIcon = {
                ImageWithMainTint(icon = R.drawable.ic_copy)
            },
            onBackPressed = {})
        ModalTopBar(
            title = "What is wallet?",
            onBackPressed = {}
        )
    }
}
