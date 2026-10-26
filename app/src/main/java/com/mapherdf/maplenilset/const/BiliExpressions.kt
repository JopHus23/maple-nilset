package com.mapherdf.maplenilset.const

object BiliExpressions {
    const val USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    const val REFERER = "https://www.bilibili.com/"
    const val ACCEPT_JSON = "application/json, text/plain, */*"
    const val IMAGE_HOST = "hdslb.com"
    const val MEDIA_RELATIVE_PATH = "Download/Nilset/Cover"
    const val VIDEO_RELATIVE_PATH = "Download/Nilset/Video"
    const val CACHE_DIRECTORY = "bili_nil"
    const val GENERIC_UPSTREAM_CODE = -1
}

object BiliSettings {
    const val STORE_NAME = "bili_nil_settings"
    const val CONCURRENT_TASKS_KEY = "concurrent_tasks"
    const val MIN_CONCURRENT_TASKS = 1
    const val MAX_CONCURRENT_TASKS = 4
    const val DEFAULT_CONCURRENT_TASKS = 1
}
