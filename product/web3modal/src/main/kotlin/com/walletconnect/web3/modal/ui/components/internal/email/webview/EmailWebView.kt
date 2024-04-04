package com.walletconnect.web3.modal.ui.components.internal.email.webview

import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import com.walletconnect.android.internal.common.model.AppMetaData
import com.walletconnect.android.internal.common.model.ProjectId
import com.walletconnect.foundation.util.Logger
import com.walletconnect.web3.modal.domain.magic.model.IsConnected
import com.walletconnect.web3.modal.ui.utils.injectSendMessageScript
import com.walletconnect.web3.modal.ui.utils.sendMethod

internal class EmailMagicWebView(
    private val appData: AppMetaData,
    private val projectId: ProjectId,
    private val logger: Logger
) : WebViewClientCompat() {

    private var loadedUrl: String? = null
    private var isFinishedCounter = 0

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION)) {
            logger.error(error.description.toString())
        }
        super.onReceivedError(view, request, error)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        isFinishedCounter++ //todo: why this callback is called twice?

        if (url != loadedUrl && view != null && isFinishedCounter == 2) {
            loadedUrl = url
            view.apply {
                injectSendMessageScript()
                sendMethod(IsConnected)
//                sendMethod(SyncDappData(appData, "kotlin-1.22.1", projectId.value))
//                sendMethod(SyncTheme("dark"))
//            }
            }
        }
    }
}


