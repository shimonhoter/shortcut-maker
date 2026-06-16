package com.shimon.shortcutmaker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.shimon.shortcutmaker.data.AppRepository
import com.shimon.shortcutmaker.data.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restores all scheduled alarms after a device reboot,
 * because AlarmManager does not survive reboots.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in listOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED
            )) return

        Log.d("BootReceiver", "Restoring alarms after boot/update...")

        val repo = AppRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            val tasks = repo.getAllTasks()
            tasks.filter { it.isEnabled }.forEach { task ->
                // Only reschedule repeating tasks — one-shot tasks that already passed are stale
                if (task.repeatMode != RepeatMode.NONE ||
                    task.triggerAtMillis > System.currentTimeMillis()
                ) {
                    SchedulerReceiver.scheduleTask(context, task)
                    Log.d("BootReceiver", "Re-scheduled: ${task.label}")
                }
            }
            Log.d("BootReceiver", "Done restoring ${tasks.size} tasks")
        }
    }
}
