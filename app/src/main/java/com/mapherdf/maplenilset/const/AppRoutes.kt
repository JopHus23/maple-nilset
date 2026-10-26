package com.mapherdf.maplenilset.const

import android.net.Uri

object AppRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val REGISTERED_USERNAME_KEY = "registered_username"
    const val CONSOLE = "console"
    const val CONSOLE_SETTINGS = "console/settings"
    const val MAIN = "main"
    const val THEME_SETTINGS = "theme_settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val CUSTOM_SETTINGS = "custom_settings"
    const val ABOUT = "about"
    const val BACKGROUND_CROP = "background_crop/{sourceUri}"

    fun backgroundCrop(sourceUri: String): String {
        return "background_crop/${Uri.encode(sourceUri)}"
    }

    object Tab {
        const val HOME = "home"
        const val SETTINGS = "settings"
    }
}
