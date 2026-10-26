package com.mapherdf.maplenilset.ui.bili

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.mapherdf.maplenilset.R

private const val BILI_LOGIN_URL = "https://passport.bilibili.com/login"

@Composable
fun BiliWebLoginScreen(
    onNavigateBack: () -> Unit,
    viewModel: BiliVideoViewModel = hiltViewModel(),
) {
    val loginState by viewModel.biliLoginState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var isClosingWithImport by remember { mutableStateOf(false) }
    var isCheckingLogin by remember { mutableStateOf(false) }

    fun closeWithImport() {
        if (isClosingWithImport) {
            return
        }
        isClosingWithImport = true
        coroutineScope.launch {
            viewModel.importBiliWebViewCookies()
            onNavigateBack()
        }
    }

    fun checkLoginAndExit() {
        if (isCheckingLogin || isClosingWithImport) {
            return
        }
        isCheckingLogin = true
        coroutineScope.launch {
            val success = viewModel.importBiliWebViewCookies()
            if (success) {
                isClosingWithImport = true
                onNavigateBack()
            } else {
                isCheckingLogin = false
            }
        }
    }

    BackHandler {
        closeWithImport()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BiliLoginWebView(
            modifier = Modifier.fillMaxSize(),
            onLoginCandidate = ::checkLoginAndExit,
        )
        IconButton(
            onClick = ::closeWithImport,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
                .size(32.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        if (loginState.isValidating) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 12.dp)
                    .size(20.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BiliLoginWebView(
    modifier: Modifier = Modifier,
    onLoginCandidate: () -> Unit,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.destroy()
        }
    }

    AndroidView(
        factory = { context ->
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.allowFileAccess = false
            webView.settings.allowContentAccess = false
            webView.settings.userAgentString = webView.settings.userAgentString.replace("; wv", "")
            webView.webViewClient = object : WebViewClient() {
                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                    if (!url.isNullOrBlank() && !isBiliLoginPage(url)) {
                        onLoginCandidate()
                    }
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    if (!url.isNullOrBlank() && !isBiliLoginPage(url)) {
                        onLoginCandidate()
                    }
                }
            }
            webView.webChromeClient = WebChromeClient()
            val cookieManager = android.webkit.CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
            webView.loadUrl(BILI_LOGIN_URL)
            webView.also { webViewRef = it }
        },
        modifier = modifier,
    )
}

private fun isBiliLoginPage(url: String): Boolean {
    val uri = Uri.parse(url)
    return uri.host == "passport.bilibili.com" && uri.path?.startsWith("/login") == true
}
