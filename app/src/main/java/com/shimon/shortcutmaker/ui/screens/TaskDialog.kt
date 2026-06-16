package com.shimon.shortcutmaker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shimon.shortcutmaker.data.*
import com.shimon.shortcutmaker.receiver.SchedulerReceiver
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    initial: ScheduledTask?,
    onDismiss: () -> Unit,
    onSave: (ScheduledTask) -> Unit,
) {
    var label          by remember { mutableStateOf(initial?.label ?: "") }
    var type           by remember { mutableStateOf(initial?.type ?: TaskType.SMS) }
    var contactName    by remember { mutableStateOf(initial?.contactName ?: "") }
    var phoneNumber    by remember { mutableStateOf(initial?.phoneNumber ?: "") }
    var messageBody    by remember { mutableStateOf(initial?.messageBody ?: "") }
    var hour           by remember { mutableIntStateOf(initial?.hourOfDay ?: 8) }
    var minute         by remember { mutableIntStateOf(initial?.minute ?: 0) }
    var repeatMode     by remember { mutableStateOf(initial?.repeatMode ?: RepeatMode.DAILY) }
    var selectedDays   by remember { mutableStateOf(initial?.daysOfWeek?.toSet() ?: setOf(Calendar.SUNDAY)) }
    var typeExpanded   by remember { mutableStateOf(false) }
    var repeatExpanded by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var showTimePicker    by remember { mutableStateOf(false) }

    val isEdit = initial != null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(if (isEdit) "עריכת משימה" else "משימה מתוזמנת חדשה",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                // ── שם ────────────────────────────────────────────────────────
                OutlinedTextField(value = label, onValueChange = { label = it },
                    label = { Text("שם המשימה") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(12.dp))

                // ── סוג ──────────────────────────────────────────────────────
                Text("סוג משימה", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                    OutlinedTextField(value = labelForTaskType(type), onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        TaskType.values().forEach { t ->
                            DropdownMenuItem(text = { Text(labelForTaskType(t)) },
                                onClick = { type = t; typeExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // ── איש קשר ──────────────────────────────────────────────────
                PickerField(
                    label = "איש קשר",
                    value = if (contactName.isNotBlank()) "$contactName  ($phoneNumber)" else phoneNumber,
                    icon = Icons.Default.Contacts,
                    placeholder = "לחץ לבחירת איש קשר",
                    onClick = { showContactPicker = true }
                )
                Spacer(Modifier.height(12.dp))

                // ── הודעה ─────────────────────────────────────────────────────
                if (type != TaskType.WHATSAPP_LOCATION) {
                    OutlinedTextField(value = messageBody, onValueChange = { messageBody = it },
                        label = { Text("תוכן ההודעה") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                    Spacer(Modifier.height(12.dp))
                }

                // ── שעה (חלונית בחירה) ────────────────────────────────────────
                PickerField(
                    label = "שעת שליחה",
                    value = "%02d:%02d".format(hour, minute),
                    icon = Icons.Default.Schedule,
                    placeholder = "בחר שעה",
                    onClick = { showTimePicker = true }
                )
                Spacer(Modifier.height(12.dp))

                // ── חזרה ─────────────────────────────────────────────────────
                Text("חזרה", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                ExposedDropdownMenuBox(expanded = repeatExpanded, onExpandedChange = { repeatExpanded = !repeatExpanded }) {
                    OutlinedTextField(value = labelForRepeat(repeatMode), onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(repeatExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = repeatExpanded, onDismissRequest = { repeatExpanded = false }) {
                        RepeatMode.values().forEach { r ->
                            DropdownMenuItem(text = { Text(labelForRepeat(r)) },
                                onClick = { repeatMode = r; repeatExpanded = false })
                        }
                    }
                }

                // ── ימים בשבוע ────────────────────────────────────────────────
                if (repeatMode == RepeatMode.WEEKLY) {
                    Spacer(Modifier.height(12.dp))
                    Text("ימים בשבוע", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..7).forEach { day ->
                            val selected = day in selectedDays
                            FilterChip(
                                selected = selected,
                                onClick = { selectedDays = if (selected) selectedDays - day else selectedDays + day },
                                label = { Text(DAY_LABELS[day - 1], fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("ביטול") }
                    Button(
                        onClick = {
                            val triggerMillis = if (repeatMode == RepeatMode.NONE)
                                SchedulerReceiver.nextOccurrenceMillis(hour, minute, emptyList()) else 0L
                            onSave(ScheduledTask(
                                id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                                label = label.trim(), type = type, isEnabled = true,
                                contactName = contactName.trim(), phoneNumber = phoneNumber.trim(),
                                messageBody = messageBody.trim(), hourOfDay = hour, minute = minute,
                                repeatMode = repeatMode, daysOfWeek = selectedDays.sorted(),
                                triggerAtMillis = triggerMillis,
                            ))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = label.isNotBlank() && phoneNumber.isNotBlank()
                    ) { Text(if (isEdit) "שמור" else "הוסף") }
                }
            }
        }
    }

    if (showContactPicker) {
        ContactPickerDialog(onDismiss = { showContactPicker = false }, onContactSelected = { c ->
            phoneNumber = c.phone; contactName = c.name
            if (label.isBlank()) label = c.name
            showContactPicker = false
        })
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = hour, initialMinute = minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m -> hour = h; minute = m; showTimePicker = false }
        )
    }
}

// ── Time Picker Dialog ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int, initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("בחר שעה") },
        text = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                TimePicker(state = state)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("אישור") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}

private fun labelForTaskType(type: TaskType): String = when (type) {
    TaskType.SMS               -> "✉️ SMS"
    TaskType.WHATSAPP_MESSAGE  -> "💬 הודעת WhatsApp"
    TaskType.WHATSAPP_LOCATION -> "📍 שיתוף מיקום WhatsApp"
}

private fun labelForRepeat(mode: RepeatMode): String = when (mode) {
    RepeatMode.NONE   -> "פעם אחת"
    RepeatMode.DAILY  -> "כל יום"
    RepeatMode.WEEKLY -> "ימים נבחרים בשבוע"
}
