package com.mapherdf.maplenilset.bili.api

class BiliApiException(
    val code: Int,
    override val message: String,
) : Exception(message)