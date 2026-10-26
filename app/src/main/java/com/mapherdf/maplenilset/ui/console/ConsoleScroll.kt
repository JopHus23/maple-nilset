package com.mapherdf.maplenilset.ui.console

internal fun isConsoleScrolledToBottom(
    scrollValue: Int,
    maxValue: Int,
    tolerancePx: Int = 1,
): Boolean = maxValue <= 0 || scrollValue >= maxValue - tolerancePx
