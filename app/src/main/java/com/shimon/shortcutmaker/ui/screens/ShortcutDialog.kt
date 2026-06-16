package com.shimon.shortcutmaker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    var lat          by remember { mutableStateOf(initial?.latitude?.takeIf { it != 0.0 }?.toString() ?: "") }
    var lng          by remember { mutableStateOf(initial?.longitude?.takeIf { it != 0.0 }?.toString() ?: "") }
    var destLabel    by remember { mutableStateOf(initial?.destinationLabel ?: "") }
    var url          by remember { mutableStateOf(initial?.url ?: "") }
    var packageName  by remember { mutableStateOf(initial?.packageName ?: "") }
    var smsBody      by remember { mutableStateOf(initial?.smsBody ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }

    var showAppPicker      by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }

    val isEdit = initial != null

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
                Text(
                    if (isEdit) "עריכת קיצור דרך" else "קיצור דרך חדש",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                // ── Label ────────────────────────────────────────────────────
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("שם קיצור הדרך") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))

                // ── Type ─────────────────────────────────────────────────────
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = labelForType(type),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("סוג פעולה") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        ShortcutType.values().forEach { t ->
                            DropdownMenuItem(
                                text = { Text(labelForType(t)) },
                                onClick = { type = t; typeExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // ── Type-specific fields ──────────────────────────────────────
                when (type) {
                    ShortcutType.DIAL, ShortcutType.SEND_SMS -> {
                        PhoneField(
                            phone = phoneNumber,
                            onPhoneChange = { phoneNumber = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (type == ShortcutType.SEND_SMS) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = smsBody,
                                onValueChange = { smsBody = it },
                                label = { Text("תוכן ההודעה") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 4
                            )
                        }
                    }

                    ShortcutType.NAVIGATE_MAPS, ShortcutType.NAVIGATE_WAZE -> {
                        // Destination label
                        OutlinedTextField(
                            value = destLabel,
                            onValueChange = { destLabel = it },
                            label = { Text("שם היעד") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))

                        // Coordinates row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = lat,
                                onValueChange = { lat = it },
                                label = { Text("קו רוחב") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = lng,
                                onValueChange = { lng = it },
                                label = { Text("קו אורך") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        // Map picker button
                        OutlinedButton(
                            onClick = { showLocationPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Map, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (lat.isNotBlank()) "📍 $lat, $lng" else "בחר במפה / חפש כתובת")
                        }
                    }

                    ShortcutType.OPEN_URL -> {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("כתובת URL") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    ShortcutType.OPEN_APP -> {
                        OutlinedTextField(
                            value = packageName,
                            onValueChange = { packageName = it },
                            label = { Text("Package Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showAppPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Apps, null)
                            Spacer(Modifier.width(8.dp))
                            Text("בחר מרשימת האפליקציות")
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Action buttons ────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("ביטול")
                    }
                    Button(
                        onClick = {
                            onSave(ShortcutConfig(
                                id               = initial?.id ?: java.util.UUID.randomUUID().toString(),
                                label            = label.trim(),
                                type             = type,
                                phoneNumber      = phoneNumber.trim(),
                                latitude         = lat.toDoubleOrNull() ?: 0.0,
                                longitude        = lng.toDoubleOrNull() ?: 0.0,
                                destinationLabel = destLabel.trim(),
                                url              = url.trim(),
                                packageName      = packageName.trim(),
                                smsBody          = smsBody.trim(),
                            ))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = label.isNotBlank()
                    ) {
                        Text(if (isEdit) "שמור" else "צור")
                    }
                }
            }
        }
    }

    // ── App picker ────────────────────────────────────────────────────────────
    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { app ->
                packageName = app.packageName
                if (label.isBlank()) label = app.name
                showAppPicker = false
            }
        )
    }

    // ── Location picker ───────────────────────────────────────────────────────
    if (showLocationPicker) {
        LocationPickerDialog(
            onDismiss = { showLocationPicker = false },
            onLocationPicked = { loc ->
                lat = loc.lat.toString()
                lng = loc.lng.toString()
                if (destLabel.isBlank()) destLabel = loc.label
                showLocationPicker = false
            }
        )
    }
}

private fun labelForType(type: ShortcutType): String = when (type) {
    ShortcutType.DIAL           -> "📞 חיוג ישיר"
    ShortcutType.NAVIGATE_MAPS  -> "🗺 מפות גוגל"
    ShortcutType.NAVIGATE_WAZE  -> "🚗 Waze"
    ShortcutType.OPEN_URL       -> "🌐 פתיחת כתובת"
    ShortcutType.OPEN_APP       -> "📱 הפעלת אפליקציה"
    ShortcutType.SEND_SMS       -> "✉️ שליחת SMS"
}
