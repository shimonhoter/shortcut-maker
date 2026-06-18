package com.shimon.shortcutmaker.receiver

// [START: SchedulerReceiver]

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.shimon.shortcutmaker.data.AppRepository
import com.shimon.shortcutmaker.data.RepeatMode
import com.shimon.shortcutmaker.data.ScheduledTask
import com.shimon.shortcutmaker.data.TaskType
import com.shimon.shortcutmaker.service.LocationSharingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class SchedulerReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "SchedulerReceiver"
        const val ACTION_TASK = "com.shimon.shortcutmaker.ACTION_SCHEDULED_TASK"
        const val EXTRA_TASK_ID = "task_id"

        /**
         * Schedule or reschedule a task using AlarmManager.
         */
        /**
         * Schedule or reschedule a task using AlarmManager.
         * Returns the actual epoch millis the task was scheduled for, or -1 on failure.
         */
        fun scheduleTask(context: Context, task: ScheduledTask): Long {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = pendingIntentForTask(context, task)

            var triggerMillis = when (task.repeatMode) {
                RepeatMode.NONE ->
                    // Prefer explicit date+time picker value if set (spec 2.2 Date & Time Picker),
                    // fall back to legacy triggerAtMillis, then to a safe "today at chosen time" default
                    // instead of silently failing with 0.
                    when {
                        task.targetDateMillis > 0 -> Calendar.getInstance().apply {
                            timeInMillis = task.targetDateMillis
                            set(Calendar.HOUR_OF_DAY, task.hourOfDay)
                            set(Calendar.MINUTE, task.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        task.triggerAtMillis > 0 -> task.triggerAtMillis
                        else -> nextOccurrenceMillis(task.hourOfDay, task.minute, listOf())
                    }

                RepeatMode.DAILY -> nextOccurrenceMillis(task.hourOfDay, task.minute, listOf())

                RepeatMode.WEEKLY -> nextOccurrenceMillis(
                    task.hourOfDay, task.minute, task.daysOfWeek
                )
            }

            // הגנה קריטית: אם המשתמש בחר "היום" בשעה שכבר עברה (או עברה בין הבחירה לשמירה),
            // הזמן המחושב נמצא בעבר. ל-RepeatMode.NONE בלבד נדחוף ליום הבא במקום לכשול בשקט.
            if (task.repeatMode == RepeatMode.NONE && triggerMillis <= System.currentTimeMillis()) {
                Log.w(TAG, "Computed trigger time is in the past (${triggerMillis}); pushing to tomorrow")
                triggerMillis += 24L * 60 * 60 * 1000
            }

            if (triggerMillis <= 0) {
                Log.w(TAG, "Invalid trigger time for task ${task.id}")
                return -1L
            }

            // Android 12+ (API 31+) requires explicit user grant for exact alarms.
            // If not granted, setExactAndAllowWhileIdle throws SecurityException silently.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e(TAG, "Exact alarm permission NOT granted - task ${task.label} will NOT fire. " +
                            "User must enable 'Alarms & reminders' for this app in system settings.")
                    return -1L
                }
            }

            return try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pi
                )
                Log.d(TAG, "Scheduled task ${task.label} at $triggerMillis (now=${System.currentTimeMillis()})")
                triggerMillis
            } catch (e: SecurityException) {
                Log.e(TAG, "Need SCHEDULE_EXACT_ALARM permission: ${e.message}")
                -1L
            }
        }

        /**
         * Cancel a previously scheduled task.
         */
        fun cancelTask(context: Context, task: ScheduledTask) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntentForTask(context, task))
            Log.d(TAG, "Cancelled task ${task.id}")
        }

        private fun pendingIntentForTask(context: Context, task: ScheduledTask): PendingIntent {
            val intent = Intent(context, SchedulerReceiver::class.java).apply {
                action = ACTION_TASK
                putExtra(EXTRA_TASK_ID, task.id)
            }
            // Use task id hashcode as request code for uniqueness
            return PendingIntent.getBroadcast(
                context,
                task.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /**
         * Returns epoch millis of the next occurrence at [hour]:[minute].
         * If [daysOfWeek] is non-empty, picks the next matching day.
         */
        fun nextOccurrenceMillis(hour: Int, minute: Int, daysOfWeek: List<Int>): Long {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // If already past today's time, move to tomorrow
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            // For weekly: advance until we hit an allowed day
            if (daysOfWeek.isNotEmpty()) {
                var tries = 0
                while (cal.get(Calendar.DAY_OF_WEEK) !in daysOfWeek && tries < 7) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    tries++
                }
            }
            return cal.timeInMillis
        }
    }

    // ─── onReceive ────────────────────────────────────────────────────────────

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TASK) return

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        Log.d(TAG, "Alarm fired for task id=$taskId")

        // קריטי: onReceive הוא סינכרוני. בלי goAsync(), המערכת יכולה להרוג את
        // התהליך מיידית אחרי שהפונקציה חוזרת - לפני שהקורוטינה האסינכרונית
        // (שליחת ה-SMS עצמו!) מספיקה לרוץ. זו הייתה הסיבה שה-SMS לא נשלח
        // בפועל אפילו כשה-Alarm נורה בהצלחה.
        val pendingResult = goAsync()

        val repo = AppRepository(context)
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val tasks = repo.getAllTasks()
                val task  = tasks.find { it.id == taskId } ?: run {
                    Log.w(TAG, "Task $taskId not found"); return@launch
                }
                if (!task.isEnabled) return@launch

                val result = executeTask(context, task)

                // Update status per spec 2.1: Pending -> Sent / Failed
                val updated = if (result.isSuccess) {
                    task.copy(
                        status = com.shimon.shortcutmaker.data.TaskStatus.SENT,
                        sentAtMillis = System.currentTimeMillis(),
                        errorReason = "",
                    )
                } else {
                    task.copy(
                        status = com.shimon.shortcutmaker.data.TaskStatus.FAILED,
                        errorReason = result.exceptionOrNull()?.message ?: "שגיאה לא ידועה",
                        retryCount = task.retryCount + 1,
                    )
                }
                repo.saveTask(updated)

                // Reschedule if repeating — reset to PENDING for the next occurrence
                when (task.repeatMode) {
                    RepeatMode.DAILY, RepeatMode.WEEKLY -> {
                        repo.saveTask(updated.copy(status = com.shimon.shortcutmaker.data.TaskStatus.PENDING))
                        scheduleTask(context, task)
                    }
                    RepeatMode.NONE -> { /* one-shot, nothing to do */ }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing scheduled task: ${e.message}", e)
            } finally {
                // חובה לשחרר - בלי זה ה-wakelock נשאר תפוס וה-OS עלול להעניש את האפליקציה
                pendingResult.finish()
            }
        }
    }

    // ─── Execution ────────────────────────────────────────────────────────────

    private fun executeTask(context: Context, task: ScheduledTask): Result<Unit> {
        return when (task.type) {
            TaskType.SMS               -> sendSms(context, task)
            TaskType.WHATSAPP_MESSAGE  -> openWhatsApp(context, task)
            TaskType.WHATSAPP_LOCATION -> startLocationService(context, task)
        }
    }

    private fun sendSms(context: Context, task: ScheduledTask): Result<Unit> {
        return try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION") SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(task.messageBody)
            if (parts.size == 1)
                smsManager.sendTextMessage(task.phoneNumber.trim(), null, task.messageBody, null, null)
            else
                smsManager.sendMultipartTextMessage(task.phoneNumber.trim(), null, parts, null, null)
            Log.d(TAG, "SMS sent to ${task.phoneNumber}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "SMS failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun openWhatsApp(context: Context, task: ScheduledTask): Result<Unit> {
        return try {
            val phone = task.phoneNumber.trimStart('+').replace(" ", "").replace("-", "")
            val encodedMsg = java.net.URLEncoder.encode(task.messageBody, "UTF-8")
            val uri = android.net.Uri.parse("https://wa.me/$phone?text=$encodedMsg")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp not installed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun startLocationService(context: Context, task: ScheduledTask): Result<Unit> {
        return try {
            val serviceIntent = Intent(context, LocationSharingService::class.java).apply {
                putExtra(LocationSharingService.EXTRA_PHONE, task.phoneNumber)
                putExtra(LocationSharingService.EXTRA_CONTACT_NAME, task.contactName)
            }
            context.startForegroundService(serviceIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// [END: SchedulerReceiver]
