package com.shimon.shortcutmaker.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*

data class AppPermission(
    val permission: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
)

val requiredPermissions = buildList {
    add(AppPermission(Manifest.permission.CALL_PHONE,          Icons.Default.Call,       "חיוג",         "לחיוג ישיר ממסך הבית"))
    add(AppPermission(Manifest.permission.SEND_SMS,            Icons.Default.Sms,        "SMS",          "לשליחת הודעות מתוזמנות"))
    add(AppPermission(Manifest.permission.READ_CONTACTS,       Icons.Default.Contacts,   "אנשי קשר",    "לבחירת מספרים מהרשימה"))
    add(AppPermission(Manifest.permission.ACCESS_FINE_LOCATION,Icons.Default.MyLocation, "מיקום",        "לשיתוף מיקום אוטומטי"))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(AppPermission(Manifest.permission.POST_NOTIFICATIONS, Icons.Default.Notifications, "התראות", "להתראות על משימות מתוזמנות"))
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(onAllGranted: () -> Unit) {
    val permissionStates = rememberMultiplePermissionsState(
        permissions = requiredPermissions.map { it.permission }
    )

    // אם כולן מאושרות – עבור ישירות
    LaunchedEffect(permissionStates.allPermissionsGranted) {
        if (permissionStates.allPermissionsGranted) onAllGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Bolt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text("ShortcutMaker", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "האפליקציה צריכה מספר הרשאות כדי לפעול",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        // רשימת הרשאות
        requiredPermissions.forEach { perm ->
            val state = permissionStates.permissions.find { it.permission == perm.permission }
            val granted = state?.status?.isGranted == true

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (granted)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        perm.icon, null,
                        tint = if (granted) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(perm.title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(perm.description, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    }
                    if (granted) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { permissionStates.launchMultiplePermissionRequest() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (permissionStates.allPermissionsGranted) "הכל מאושר ✓"
                else "אשר הרשאות",
                fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
        }

        if (!permissionStates.allPermissionsGranted) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAllGranted) {
                Text("דלג (חלק מהפיצ׳רים לא יעבדו)", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}
