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
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            .map { ri ->
                AppInfo(
                    name = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
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
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("בחר אפליקציה (${allApps.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("חיפוש...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(app.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text(
                                    app.packageName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
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
