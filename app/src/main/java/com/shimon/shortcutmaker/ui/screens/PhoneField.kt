package com.shimon.shortcutmaker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * שדה טלפון עם כפתור לבחירת איש קשר.
 * משמש ב-ShortcutDialog וב-TaskDialog.
 */
@Composable
fun PhoneField(
    phone: String,
    onPhoneChange: (String) -> Unit,
    contactName: String = "",
    onContactNameChange: ((String) -> Unit)? = null,
    label: String = "מספר טלפון",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showContactPicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showContactPicker = true
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text(label) },
            placeholder = { Text(if (contactName.isNotBlank()) contactName else "+972...") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(1f),
            singleLine = true,
            supportingText = if (contactName.isNotBlank()) {
                { Text(contactName) }
            } else null
        )

        IconButton(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) showContactPicker = true
                else permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                Icons.Default.Contacts,
                contentDescription = "בחר איש קשר",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showContactPicker) {
        ContactPickerDialog(
            onDismiss = { showContactPicker = false },
            onContactSelected = { contact ->
                onPhoneChange(contact.phone)
                onContactNameChange?.invoke(contact.name)
                showContactPicker = false
            }
        )
    }
}
