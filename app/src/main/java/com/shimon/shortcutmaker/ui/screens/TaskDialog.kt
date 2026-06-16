package com.shimon.shortcutmaker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

    val isEdit = initial != null
    val title  = if (isEdit) "עריכת משימה" else "משימה מתוזמנת חדשה"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                // ─── Label ───────────────────────────────────────────────────
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("שם המשימה") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))

                // ─── Task Type ───────────────────────────────────────────────
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = labelForTaskType(type),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("סוג המשימה") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        TaskType.values().forEach { t ->
                            DropdownMenuItem(
                                text = { Text(labelForTaskType(t)) },
                                onClick = { type = t; typeExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // ─── Contact ─────────────────────────────────────────────────
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("שם איש קשר") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("מספר טלפון (עם קידומת מדינה, למשל +972)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                // ─── Message Body (not for location) ─────────────────────────
                if (type != TaskType.WHATSAPP_LOCATION) {
                    OutlinedTextField(
                        value = messageBody,
                        onValueChange = { messageBody = it },
                        label = { Text("תוכן ההודעה") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // ─── Time ────────────────────────────────────────────────────
                Text("שעת שליחה", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = "%02d".format(hour),
                        onValueChange = { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour },
                        label = { Text("שעה") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = "%02d".format(minute),
                        onValueChange = { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute },
                        label = { Text("דקה") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(12.dp))

                // ─── Repeat Mode ─────────────────────────────────────────────
                ExposedDropdownMenuBox(
                    expanded = repeatExpanded,
                    onExpandedChange = { repeatExpanded = !repeatExpanded }
                ) {
                    OutlinedTextField(
                        value = labelForRepeat(repeatMode),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("חזרה") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(repeatExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = repeatExpanded,
                        onDismissRequest = { repeatExpanded = false }
                    ) {
                        RepeatMode.values().forEach { r ->
                            DropdownMenuItem(
                                text = { Text(labelForRepeat(r)) },
                                onClick = { repeatMode = r; repeatExpanded = false }
                            )
                        }
                    }
                }

                // ─── Days of Week (for WEEKLY) ────────────────────────────────
                if (repeatMode == RepeatMode.WEEKLY) {
                    Spacer(Modifier.height(12.dp))
                    Text("ימים בשבוע", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    // Calendar.SUNDAY=1 … SATURDAY=7; display Sun-Sat in Hebrew
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..7).forEach { day ->
                            val isSelected = day in selectedDays
                            FilterChip(
                                selected = isSelected,
                                onClick  = {
                                    selectedDays = if (isSelected)
                                        selectedDays - day
                                    else
                                        selectedDays + day
                                },
                                label = { Text(DAY_LABELS[day - 1], fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ─── Buttons ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("ביטול") }

                    Button(
                        onClick = {
                            val triggerMillis = if (repeatMode == RepeatMode.NONE) {
                                SchedulerReceiver.nextOccurrenceMillis(hour, minute, emptyList())
                            } else 0L

                            val task = ScheduledTask(
                                id              = initial?.id ?: java.util.UUID.randomUUID().toString(),
                                label           = label.trim(),
                                type            = type,
                                isEnabled       = true,
                                contactName     = contactName.trim(),
                                phoneNumber     = phoneNumber.trim(),
                                messageBody     = messageBody.trim(),
                                hourOfDay       = hour,
                                minute          = minute,
                                repeatMode      = repeatMode,
                                daysOfWeek      = selectedDays.sorted(),
                                triggerAtMillis = triggerMillis,
                            )
                            onSave(task)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = label.isNotBlank() && phoneNumber.isNotBlank()
                    ) { Text(if (isEdit) "שמור" else "הוסף") }
                }
            }
        }
    }
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
