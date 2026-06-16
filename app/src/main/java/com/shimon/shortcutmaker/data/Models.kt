package com.shimon.shortcutmaker.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ─── Shortcut Types ──────────────────────────────────────────────────────────

enum class ShortcutType {
    DIAL,           // Direct phone call
    NAVIGATE_MAPS,  // Google Maps to fixed destination
    NAVIGATE_WAZE,  // Waze to fixed destination
    OPEN_URL,       // Open any URL in browser
    OPEN_APP,       // Launch another app by package name
    SEND_SMS,       // Send SMS immediately on tap
}

@Parcelize
data class ShortcutConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String = "",
    val type: ShortcutType = ShortcutType.DIAL,

    // DIAL
    val phoneNumber: String = "",

    // NAVIGATE
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val destinationLabel: String = "",

    // URL
    val url: String = "",

    // APP
    val packageName: String = "",

    // SMS
    val smsBody: String = "",
) : Parcelable

// ─── Scheduled Task Types ────────────────────────────────────────────────────

enum class TaskType {
    SMS,                  // Send SMS at scheduled time
    WHATSAPP_MESSAGE,     // Open WhatsApp chat with text ready
    WHATSAPP_LOCATION,    // Share current GPS location via WhatsApp
}

enum class RepeatMode {
    NONE,       // One-time
    DAILY,      // Every day at same time
    WEEKLY,     // Selected days of week
}

@Parcelize
data class ScheduledTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String = "",
    val type: TaskType = TaskType.SMS,
    val isEnabled: Boolean = true,

    // Target contact
    val contactName: String = "",
    val phoneNumber: String = "",   // phone number for SMS/WhatsApp

    // Message body (for SMS / WhatsApp message)
    val messageBody: String = "",

    // Timing
    val hourOfDay: Int = 8,
    val minute: Int = 0,

    // Recurrence
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val daysOfWeek: List<Int> = emptyList(), // 1=Sun … 7=Sat (Calendar constants)

    // One-time: epoch millis of next trigger
    val triggerAtMillis: Long = 0L,
) : Parcelable

// ─── Day-of-week helper ──────────────────────────────────────────────────────

val DAY_LABELS = listOf("א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳")
val DAY_FULL   = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")
// Calendar.SUNDAY=1 … Calendar.SATURDAY=7
