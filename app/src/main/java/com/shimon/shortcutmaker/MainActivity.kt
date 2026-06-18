package com.shimon.shortcutmaker

import android.Manifest
import android.app.AlarmManager
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.shimon.shortcutmaker.data.AppRepository
import com.shimon.shortcutmaker.data.RepeatMode
import com.shimon.shortcutmaker.data.ScheduledTask
import com.shimon.shortcutmaker.data.ShortcutConfig
import com.shimon.shortcutmaker.data.ShortcutType
import com.shimon.shortcutmaker.data.TaskType
import com.shimon.shortcutmaker.receiver.SchedulerReceiver
import com.shimon.shortcutmaker.shortcuts.ShortcutCreator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var repo: AppRepository
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { reloadWebView() }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repo = AppRepository(this)

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/91.0 Mobile Safari/537.36"
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    android.util.Log.d("WebViewConsole",
                        "${message.message()} -- line ${message.lineNumber()} of ${message.sourceId()}")
                    return true
                }
            }
            webViewClient = WebViewClient()
            addJavascriptInterface(AppBridge(), "Android")
            loadUrl("file:///android_asset/app.html")
        }

        setContentView(webView)
        requestAllPermissions()
        checkExactAlarmPermission()
    }

    private fun checkExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java) ?: return
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this,
                    "יש לאשר הרשאת 'תזכורות והתראות' כדי שמשימות מתוזמנות יישלחו",
                    Toast.LENGTH_LONG).show()
                try {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Could not open exact alarm settings: ${e.message}")
                }
            }
        }
    }

    private fun requestAllPermissions() {
        val perms = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ).apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissions.launch(missing.toTypedArray())
    }

    private fun reloadWebView() = webView.reload()

    // ── Bridge ────────────────────────────────────────────────────────────────
    inner class AppBridge {

        @JavascriptInterface
        fun getData(): String {
            return runBlocking {
                val shortcuts = repo.shortcutsFlow.firstValue()
                val tasks     = repo.tasksFlow.firstValue()
                val contacts  = loadContacts()
                val apps      = loadApps()

                JSONObject().apply {
                    put("shortcuts", JSONArray(shortcuts.map { it.toJson() }))
                    put("tasks",     JSONArray(tasks.map { it.toJson() }))
                    put("contacts",  contacts)
                    put("apps",      apps)
                }.toString()
            }
        }

        @JavascriptInterface
        fun saveShortcut(json: String) {
            val o = JSONObject(json)
            val sc = ShortcutConfig(
                id               = o.getString("id"),
                label            = o.getString("label"),
                type             = when(o.getString("type")) {
                    "DIAL"  -> ShortcutType.DIAL
                    "SMS"   -> ShortcutType.SEND_SMS
                    "MAPS"  -> ShortcutType.NAVIGATE_MAPS
                    "WAZE"  -> ShortcutType.NAVIGATE_WAZE
                    "URL"   -> ShortcutType.OPEN_URL
                    "APP"   -> ShortcutType.OPEN_APP
                    else    -> ShortcutType.DIAL
                },
                phoneNumber      = o.optString("phone"),
                latitude         = o.optDouble("lat", 0.0),
                longitude        = o.optDouble("lng", 0.0),
                destinationLabel = o.optString("destLabel"),
                url              = o.optString("url"),
                packageName      = o.optString("packageName"),
                smsBody          = o.optString("smsBody"),
            )
            scope.launch { repo.saveShortcut(sc) }
        }

        @JavascriptInterface
        fun deleteShortcut(json: String) {
            val id = JSONObject(json).getString("id")
            scope.launch { repo.deleteShortcut(id) }
        }

        @JavascriptInterface
        fun pinShortcut(json: String) {
            val id = JSONObject(json).getString("id")
            scope.launch {
                val sc = repo.shortcutsFlow.firstValue().find { it.id == id } ?: return@launch
                val ok = ShortcutCreator.pinShortcut(this@MainActivity, sc)
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        if (ok) "קיצור נוסף למסך הבית ✓" else "הלאנצ'ר לא תומך בהצמדה",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun saveTask(json: String) {
            val o = JSONObject(json)
            val daysArr = o.optJSONArray("days")
            val days = (0 until (daysArr?.length() ?: 0)).map { daysArr!!.getInt(it) }
            scope.launch {
                // אם זו עריכת משימה קיימת, שמר את הסטטוס וההיסטוריה הקיימים
                val existing = repo.tasksFlow.firstValue().find { it.id == o.optString("id") }
                val task = ScheduledTask(
                    id               = o.getString("id"),
                    label            = o.getString("label"),
                    type             = when(o.getString("type")) {
                        "SMS"    -> TaskType.SMS
                        "WA_MSG" -> TaskType.WHATSAPP_MESSAGE
                        "WA_LOC" -> TaskType.WHATSAPP_LOCATION
                        else     -> TaskType.SMS
                    },
                    isEnabled        = true,
                    contactName      = o.optString("contactName"),
                    phoneNumber      = o.optString("phone"),
                    messageBody      = o.optString("message"),
                    hourOfDay        = o.optInt("hour", 8),
                    minute           = o.optInt("minute", 0),
                    targetDateMillis = o.optLong("targetDateMillis", 0L),
                    repeatMode       = when(o.optString("repeat")) {
                        "DAILY"  -> RepeatMode.DAILY
                        "WEEKLY" -> RepeatMode.WEEKLY
                        else     -> RepeatMode.NONE
                    },
                    daysOfWeek       = days,
                    // שמירת היסטוריית סטטוס בעריכה; משימה חדשה מתחילה כ-PENDING
                    status           = existing?.status ?: com.shimon.shortcutmaker.data.TaskStatus.PENDING,
                    createdAtMillis  = existing?.createdAtMillis ?: System.currentTimeMillis(),
                    sentAtMillis     = existing?.sentAtMillis ?: 0L,
                    errorReason      = "",
                    retryCount       = existing?.retryCount ?: 0,
                )
                repo.saveTask(task)
                if (task.isEnabled) SchedulerReceiver.scheduleTask(this@MainActivity, task)
            }
        }

        @JavascriptInterface
        fun deleteTask(json: String) {
            val id = JSONObject(json).getString("id")
            scope.launch { repo.deleteTask(id) }
        }

        @JavascriptInterface
        fun toggleTask(json: String) {
            val o = JSONObject(json)
            val id = o.getString("id"); val enabled = o.getBoolean("enabled")
            scope.launch {
                val task = repo.tasksFlow.firstValue().find { it.id == id } ?: return@launch
                val updated = task.copy(isEnabled = enabled)
                repo.saveTask(updated)
                if (enabled) SchedulerReceiver.scheduleTask(this@MainActivity, updated)
                else SchedulerReceiver.cancelTask(this@MainActivity, updated)
            }
        }
    }

    // ── Contacts ──────────────────────────────────────────────────────────────
    private fun loadContacts(): JSONArray {
        val arr = JSONArray()
        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            ) ?: return arr
            cursor.use {
                val ni = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val pi = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    arr.put(JSONObject().apply {
                        put("name",  it.getString(ni) ?: "")
                        put("phone", it.getString(pi)?.replace(" ","")?.replace("-","") ?: "")
                    })
                }
            }
        } catch (e: Exception) {}
        return arr
    }

    // ── Apps ──────────────────────────────────────────────────────────────────
    private fun loadApps(): JSONArray {
        val arr = JSONArray()
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            .map { Pair(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.second != packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
            .forEach { (name, pkg) ->
                arr.put(JSONObject().apply { put("name", name); put("pkg", pkg) })
            }
        return arr
    }

    // ── Extension ─────────────────────────────────────────────────────────────
    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstValue(): T =
        take(1).toList()[0]

    private fun ShortcutConfig.toJson() = JSONObject().apply {
        put("id", id); put("label", label)
        put("type", when(type) {
            ShortcutType.DIAL          -> "DIAL"
            ShortcutType.SEND_SMS      -> "SMS"
            ShortcutType.NAVIGATE_MAPS -> "MAPS"
            ShortcutType.NAVIGATE_WAZE -> "WAZE"
            ShortcutType.OPEN_URL      -> "URL"
            ShortcutType.OPEN_APP      -> "APP"
        })
        put("phone", phoneNumber); put("contactName", "")
        put("lat", latitude); put("lng", longitude); put("destLabel", destinationLabel)
        put("url", url); put("packageName", packageName); put("appName", "")
        put("smsBody", smsBody)
    }.toString()

    private fun ScheduledTask.toJson() = JSONObject().apply {
        put("id", id); put("label", label); put("enabled", isEnabled)
        put("type", when(type) {
            TaskType.SMS                -> "SMS"
            TaskType.WHATSAPP_MESSAGE   -> "WA_MSG"
            TaskType.WHATSAPP_LOCATION  -> "WA_LOC"
        })
        put("phone", phoneNumber); put("contactName", contactName)
        put("message", messageBody); put("hour", hourOfDay); put("minute", minute)
        put("targetDateMillis", targetDateMillis)
        put("repeat", when(repeatMode) {
            RepeatMode.NONE   -> "NONE"
            RepeatMode.DAILY  -> "DAILY"
            RepeatMode.WEEKLY -> "WEEKLY"
        })
        put("days", JSONArray(daysOfWeek))
        put("status", status.name)
        put("createdAtMillis", createdAtMillis)
        put("sentAtMillis", sentAtMillis)
        put("errorReason", errorReason)
        put("retryCount", retryCount)
    }.toString()

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
