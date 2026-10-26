package com.mapherdf.maplenilset.bili.auth

import android.webkit.CookieManager

class SystemBiliWebCookieStore : BiliWebCookieStore {

    override fun flush() {
        CookieManager.getInstance().flush()
    }

    override fun readCookie(url: String): String? {
        return CookieManager.getInstance().getCookie(url)
    }

    override fun expireCookie(url: String, name: String, domain: String?) {
        val domainAttribute = domain?.let { "; Domain=$it" }.orEmpty()
        CookieManager.getInstance().setCookie(
            url,
            "$name=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT$domainAttribute; Path=/",
        )
    }
}
