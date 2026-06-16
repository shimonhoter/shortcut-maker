package com.shimon.shortcutmaker.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class AppInfo(val name: String, val packageName: String)

@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (AppInfo) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val allApps = remember {
        val pm = context.packageManager
        val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val fromLauncher = pm.queryIntentActivities(launcherIntent, PackageManager.GET_META_DATA)
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }

        val fromInstalled = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { AppInfo(pm.getApplicationLabel(it).toString(), it.packageName) }

        (fromLauncher + fromInstalled)
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName && it.name.isNotBlank() }
            .sortedBy { it.name.lowercase() }
    }

    val filtered = remember(query) {
        if (query.isBlank()) allApps
        else allApps.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("בחר אפליקציה (${allApps.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("חיפוש...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(4.dp))
                Text("${filtered.size} תוצאות", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                Spacer(Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppSelected(app) }
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Text(app.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(app.packageName, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("ביטול")
                }
            }
        }
    }
}
