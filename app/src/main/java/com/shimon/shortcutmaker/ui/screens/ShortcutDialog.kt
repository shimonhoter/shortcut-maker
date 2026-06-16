package com.shimon.shortcutmaker.ui.screens

import androidx.compose.foundation.clickable
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
import com.shimon.shortcutmaker.data.ShortcutConfig
import com.shimon.shortcutmaker.data.ShortcutType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutDialog(
    initial: ShortcutConfig?,
    onDismiss: () -> Unit,
    onSave: (ShortcutConfig) -> Unit,
) {
    var label        by remember { mutableStateOf(initial?.label ?: "") }
    var type         by remember { mutableStateOf(initial?.type ?: ShortcutType.DIAL) }
    var phoneNumber  by remember { mutableStateOf(initial?.phoneNumber ?: "") }
    var contactName  by remember { mutableStateOf("") }
    var lat          by remember { mutableStateOf(initial?.latitude?.takeIf { it != 0.0 }?.toString() ?: "") }
    var lng          by remember { mutableStateOf(initial?.longitude?.takeIf { it != 0.0 }?.toString() ?: "") }
    var destLabel    by remember { mutableStateOf(initial?.destinationLabel ?: "") }
    var url          by remember { mutableStateOf(initial?.url ?: "") }
    var packageName  by remember { mutableStateOf(initial?.packageName ?: "") }
    var appName      by remember { mutableStateOf("") }
    var smsBody      by remember { mutableStateOf(initial?.smsBody ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }
    var showAppPicker      by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showContactPicker  by remember { mutableStateOf(false) }
    var showUrlPicker      by remember { mutableStateOf(false) }

    val isEdit = initial != null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(if (isEdit) "עריכת קיצור" else "קיצור דרך חדש",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                // ── שם (הזנה חופשית) ──────────────────────────────────────────
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("שם קיצור הדרך") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(12.dp))

                // ── סוג (חלונית בחירה) ────────────────────────────────────────
                Text("סוג פעולה", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                    OutlinedTextField(
                        value = labelForType(type), onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        ShortcutType.values().forEach { t ->
                            DropdownMenuItem(
                                text = { Text(labelForType(t)) },
                                onClick = { type = t; typeExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ── שדות לפי סוג ─────────────────────────────────────────────
                when (type) {

                    ShortcutType.DIAL, ShortcutType.SEND_SMS -> {
                        // בחירת איש קשר
                        PickerField(
                            label = "איש קשר / מספר טלפון",
                            value = if (contactName.isNotBlank()) "$contactName  ($phoneNumber)" else phoneNumber,
                            icon = Icons.Default.Contacts,
                            placeholder = "לחץ לבחירת איש קשר",
                            onClick = { showContactPicker = true }
                        )
                        if (type == ShortcutType.SEND_SMS) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = smsBody, onValueChange = { smsBody = it },
                                label = { Text("תוכן ההודעה") },
                                modifier = Modifier.fillMaxWidth(), maxLines = 4
                            )
                        }
                    }

                    ShortcutType.NAVIGATE_MAPS, ShortcutType.NAVIGATE_WAZE -> {
                        PickerField(
                            label = "יעד",
                            value = if (destLabel.isNotBlank()) destLabel else "",
                            icon = Icons.Default.LocationOn,
                            placeholder = "לחץ לחיפוש יעד במפה",
                            onClick = { showLocationPicker = true }
                        )
                        if (lat.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("📍 $lat, $lng", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    ShortcutType.OPEN_URL -> {
                        PickerField(
                            label = "כתובת אינטרנט",
                            value = url,
                            icon = Icons.Default.Language,
                            placeholder = "הזן כתובת URL",
                            onClick = { showUrlPicker = true }
                        )
                    }

                    ShortcutType.OPEN_APP -> {
                        PickerField(
                            label = "אפליקציה",
                            value = if (appName.isNotBlank()) appName else packageName,
                            icon = Icons.Default.Apps,
                            placeholder = "לחץ לבחירת אפליקציה",
                            onClick = { showAppPicker = true }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("ביטול") }
                    Button(
                        onClick = {
                            onSave(ShortcutConfig(
                                id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                                label = label.trim(), type = type,
                                phoneNumber = phoneNumber.trim(),
                                latitude = lat.toDoubleOrNull() ?: 0.0,
                                longitude = lng.toDoubleOrNull() ?: 0.0,
                                destinationLabel = destLabel.trim(),
                                url = url.trim(), packageName = packageName.trim(),
                                smsBody = smsBody.trim(),
                            ))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = label.isNotBlank()
                    ) { Text(if (isEdit) "שמור" else "צור") }
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

    if (showAppPicker) {
        AppPickerDialog(onDismiss = { showAppPicker = false }, onAppSelected = { app ->
            packageName = app.packageName; appName = app.name
            if (label.isBlank()) label = app.name
            showAppPicker = false
        })
    }

    if (showLocationPicker) {
        LocationPickerDialog(onDismiss = { showLocationPicker = false }, onLocationPicked = { loc ->
            lat = loc.lat.toString(); lng = loc.lng.toString(); destLabel = loc.label
            if (label.isBlank()) label = loc.label.split(",").first().trim()
            showLocationPicker = false
        })
    }

    if (showUrlPicker) {
        UrlInputDialog(
            initial = url,
            onDismiss = { showUrlPicker = false },
            onConfirm = { u -> url = u; showUrlPicker = false }
        )
    }
}

// ── קומפוננטה: שדה בחירה אחיד ────────────────────────────────────────────────
@Composable
fun PickerField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String,
    onClick: () -> Unit,
) {
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = value.ifBlank { placeholder },
                    fontSize = 15.sp,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ── דיאלוג URL ────────────────────────────────────────────────────────────────
@Composable
fun UrlInputDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val suggestions = listOf("https://", "https://www.google.com", "https://waze.com", "https://youtube.com")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הזן כתובת URL") },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it },
                    label = { Text("URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                suggestions.forEach { s ->
                    TextButton(onClick = { text = s }, modifier = Modifier.fillMaxWidth()) {
                        Text(s, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("אישור") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}

fun labelForType(type: ShortcutType): String = when (type) {
    ShortcutType.DIAL          -> "📞 חיוג ישיר"
    ShortcutType.NAVIGATE_MAPS -> "🗺 מפות גוגל"
    ShortcutType.NAVIGATE_WAZE -> "🚗 Waze"
    ShortcutType.OPEN_URL      -> "🌐 פתיחת כתובת"
    ShortcutType.OPEN_APP      -> "📱 הפעלת אפליקציה"
    ShortcutType.SEND_SMS      -> "✉️ שליחת SMS"
}
