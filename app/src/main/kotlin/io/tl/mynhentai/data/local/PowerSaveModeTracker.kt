package io.tl.mynhentai.data.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether the system is currently in battery-saver (power save) mode.
 *
 * The initial value is read once from [PowerManager.isPowerSaveMode]; a dynamic
 * receiver for [PowerManager.ACTION_POWER_SAVE_MODE_CHANGED] keeps the state in
 * sync when the user toggles battery saver while the app is running.
 */
class PowerSaveModeTracker(private val appContext: Context) {

    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val _isPowerSaveMode = MutableStateFlow(powerManager.isPowerSaveMode)
    val isPowerSaveMode: StateFlow<Boolean> = _isPowerSaveMode.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _isPowerSaveMode.value = powerManager.isPowerSaveMode
        }
    }

    init {
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }
}