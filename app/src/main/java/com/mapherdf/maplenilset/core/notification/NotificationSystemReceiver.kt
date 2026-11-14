package com.mapherdf.maplenilset.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import com.mapherdf.maplenilset.di.ApplicationScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationSystemReceiver : BroadcastReceiver() {
    @Inject
    lateinit var coordinator: NotificationCoordinator

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                coordinator.refresh()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
