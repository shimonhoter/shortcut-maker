package com.shimon.shortcutmaker.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shimon.shortcutmaker.data.*
import com.shimon.shortcutmaker.shortcuts.ShortcutCreator
import com.shimon.shortcutmaker.receiver.SchedulerReceiver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val repo    = remember { AppRepository(context) }
    val scope   = rememberCoroutineScope()

    val shortcuts by repo.shortcutsFlow.collectAsState(initial = emptyList())
    val tasks     by repo.tasksFlow.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    var showShortcutDialog by remember { mutableStateOf(false) }
    var showTaskDialog     by remember { mutableStateOf(false) }
    var editingShortcut    by remember { mutableStateOf<ShortcutConfig?>(null) }
    var editingTask        by remember { mutableStateOf<ScheduledTask?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "⚡ ShortcutMaker",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        editingShortcut = null
                        showShortcutDialog = true
                    } else {
                        editingTask = null
                        showTaskDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "הוסף")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ─── Tab Row ─────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.surface,
                contentColor     = MaterialTheme.colorScheme.primary,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    text = { Text("קיצורי דרך") },
                    icon = { Icon(Icons.Default.Bolt, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    text = { Text("תזמון") },
                    icon = { Icon(Icons.Default.Schedule, null) }
                )
            }

            // ─── Content ──────────────────────────────────────────────────────
            when (selectedTab) {
                0 -> ShortcutsTab(
                    shortcuts = shortcuts,
                    onPin = { sc ->
                        val ok = ShortcutCreator.pinShortcut(context, sc)
                        val msg = if (ok) "קיצור נוסף למסך הבית ✓" else "הלאנצ׳ר לא תומך בהצמדה"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onEdit   = { editingShortcut = it; showShortcutDialog = true },
                    onDelete = { scope.launch { repo.deleteShortcut(it.id) } }
                )
                1 -> TasksTab(
                    tasks    = tasks,
                    onToggle = { task ->
                        scope.launch {
                            val updated = task.copy(isEnabled = !task.isEnabled)
                            repo.saveTask(updated)
                            if (updated.isEnabled) SchedulerReceiver.scheduleTask(context, updated)
                            else SchedulerReceiver.cancelTask(context, updated)
                        }
                    },
                    onEdit   = { editingTask = it; showTaskDialog = true },
                    onDelete = { task ->
                        scope.launch {
                            SchedulerReceiver.cancelTask(context, task)
                            repo.deleteTask(task.id)
                        }
                    }
                )
            }
        }
    }

    // ─── Dialogs ──────────────────────────────────────────────────────────────
    if (showShortcutDialog) {
        ShortcutDialog(
            initial  = editingShortcut,
            onDismiss = { showShortcutDialog = false },
            onSave   = { sc ->
                scope.launch {
                    repo.saveShortcut(sc)
                    showShortcutDialog = false
                }
            }
        )
    }

    if (showTaskDialog) {
        TaskDialog(
            initial  = editingTask,
            onDismiss = { showTaskDialog = false },
            onSave   = { task ->
                scope.launch {
                    repo.saveTask(task)
                    if (task.isEnabled) SchedulerReceiver.scheduleTask(context, task)
                    showTaskDialog = false
                }
            }
        )
    }
}

// ─── Shortcuts Tab ────────────────────────────────────────────────────────────

@Composable
fun ShortcutsTab(
    shortcuts: List<ShortcutConfig>,
    onPin: (ShortcutConfig) -> Unit,
    onEdit: (ShortcutConfig) -> Unit,
    onDelete: (ShortcutConfig) -> Unit,
) {
    if (shortcuts.isEmpty()) {
        EmptyState(
            icon    = Icons.Default.Bolt,
            message = "אין קיצורי דרך עדיין",
            hint    = "לחץ + כדי ליצור קיצור דרך חדש"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shortcuts) { sc ->
                ShortcutCard(sc, onPin, onEdit, onDelete)
            }
        }
    }
}

@Composable
fun ShortcutCard(
    sc: ShortcutConfig,
    onPin: (ShortcutConfig) -> Unit,
    onEdit: (ShortcutConfig) -> Unit,
    onDelete: (ShortcutConfig) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = iconForShortcut(sc.type),
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(sc.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    subtitleForShortcut(sc),
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Actions
            IconButton(onClick = { onPin(sc) }) {
                Icon(Icons.Default.PushPin, "הצמד", tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = { onEdit(sc) }) {
                Icon(Icons.Default.Edit, "ערוך", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { onDelete(sc) }) {
                Icon(Icons.Default.Delete, "מחק", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ─── Tasks Tab ────────────────────────────────────────────────────────────────

@Composable
fun TasksTab(
    tasks: List<ScheduledTask>,
    onToggle: (ScheduledTask) -> Unit,
    onEdit: (ScheduledTask) -> Unit,
    onDelete: (ScheduledTask) -> Unit,
) {
    if (tasks.isEmpty()) {
        EmptyState(
            icon    = Icons.Default.Schedule,
            message = "אין משימות מתוזמנות",
            hint    = "לחץ + כדי להוסיף משימה מתוזמנת"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tasks) { task ->
                TaskCard(task, onToggle, onEdit, onDelete)
            }
        }
    }
}

@Composable
fun TaskCard(
    task: ScheduledTask,
    onToggle: (ScheduledTask) -> Unit,
    onEdit: (ScheduledTask) -> Unit,
    onDelete: (ScheduledTask) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isEnabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (task.isEnabled) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForTask(task.type),
                    contentDescription = null,
                    tint = if (task.isEnabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    task.label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color = if (task.isEnabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    subtitleForTask(task),
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                // Schedule label
                Text(
                    scheduleLabel(task),
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Switch(
                checked  = task.isEnabled,
                onCheckedChange = { onToggle(task) }
            )

            IconButton(onClick = { onEdit(task) }) {
                Icon(Icons.Default.Edit, "ערוך", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { onDelete(task) }) {
                Icon(Icons.Default.Delete, "מחק", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(icon: ImageVector, message: String, hint: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                modifier           = Modifier.size(72.dp),
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                message,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                hint,
                fontSize  = 13.sp,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

fun iconForShortcut(type: ShortcutType): ImageVector = when (type) {
    ShortcutType.DIAL           -> Icons.Default.Call
    ShortcutType.NAVIGATE_MAPS  -> Icons.Default.Map
    ShortcutType.NAVIGATE_WAZE  -> Icons.Default.Navigation
    ShortcutType.OPEN_URL       -> Icons.Default.Language
    ShortcutType.OPEN_APP       -> Icons.Default.OpenInNew
    ShortcutType.SEND_SMS       -> Icons.Default.Sms
}

fun iconForTask(type: TaskType): ImageVector = when (type) {
    TaskType.SMS                -> Icons.Default.Sms
    TaskType.WHATSAPP_MESSAGE   -> Icons.Default.Chat
    TaskType.WHATSAPP_LOCATION  -> Icons.Default.MyLocation
}

fun subtitleForShortcut(sc: ShortcutConfig): String = when (sc.type) {
    ShortcutType.DIAL           -> "חיוג ל-${sc.phoneNumber}"
    ShortcutType.NAVIGATE_MAPS  -> "ניווט ל-${sc.destinationLabel}"
    ShortcutType.NAVIGATE_WAZE  -> "Waze → ${sc.destinationLabel}"
    ShortcutType.OPEN_URL       -> sc.url
    ShortcutType.OPEN_APP       -> sc.packageName
    ShortcutType.SEND_SMS       -> "SMS ל-${sc.phoneNumber}"
}

fun subtitleForTask(task: ScheduledTask): String = when (task.type) {
    TaskType.SMS               -> "SMS ל-${task.contactName}"
    TaskType.WHATSAPP_MESSAGE  -> "WhatsApp ל-${task.contactName}"
    TaskType.WHATSAPP_LOCATION -> "מיקום → ${task.contactName}"
}

fun scheduleLabel(task: ScheduledTask): String {
    val time = "%02d:%02d".format(task.hourOfDay, task.minute)
    return when (task.repeatMode) {
        RepeatMode.NONE    -> "פעם אחת ב-$time"
        RepeatMode.DAILY   -> "כל יום ב-$time"
        RepeatMode.WEEKLY  -> {
            val days = task.daysOfWeek.map { DAY_LABELS.getOrElse(it - 1) { "?" } }.joinToString(", ")
            "$days בשעה $time"
        }
    }
}
