package com.shimon.shortcutmaker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shortcut_maker_prefs")

class AppRepository(private val context: Context) {

    companion object {
        val KEY_SHORTCUTS = stringPreferencesKey("shortcuts_json")
        val KEY_TASKS     = stringPreferencesKey("tasks_json")
    }

    // ─── Shortcuts ────────────────────────────────────────────────────────────

    val shortcutsFlow: Flow<List<ShortcutConfig>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_SHORTCUTS] ?: "[]"
        parseShortcuts(json)
    }

    suspend fun saveShortcut(shortcut: ShortcutConfig) {
        context.dataStore.edit { prefs ->
            val current = parseShortcuts(prefs[KEY_SHORTCUTS] ?: "[]").toMutableList()
            val idx = current.indexOfFirst { it.id == shortcut.id }
            if (idx >= 0) current[idx] = shortcut else current.add(shortcut)
            prefs[KEY_SHORTCUTS] = serializeShortcuts(current)
        }
    }

    suspend fun deleteShortcut(id: String) {
        context.dataStore.edit { prefs ->
            val current = parseShortcuts(prefs[KEY_SHORTCUTS] ?: "[]").filter { it.id != id }
            prefs[KEY_SHORTCUTS] = serializeShortcuts(current)
        }
    }

    // ─── Scheduled Tasks ─────────────────────────────────────────────────────

    val tasksFlow: Flow<List<ScheduledTask>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_TASKS] ?: "[]"
        parseTasks(json)
    }

    suspend fun saveTask(task: ScheduledTask) {
        context.dataStore.edit { prefs ->
            val current = parseTasks(prefs[KEY_TASKS] ?: "[]").toMutableList()
            val idx = current.indexOfFirst { it.id == task.id }
            if (idx >= 0) current[idx] = task else current.add(task)
            prefs[KEY_TASKS] = serializeTasks(current)
        }
    }

    suspend fun deleteTask(id: String) {
        context.dataStore.edit { prefs ->
            val current = parseTasks(prefs[KEY_TASKS] ?: "[]").filter { it.id != id }
            prefs[KEY_TASKS] = serializeTasks(current)
        }
    }

    suspend fun getAllTasks(): List<ScheduledTask> {
        val prefs = context.dataStore.data
        var result = emptyList<ScheduledTask>()
        prefs.collect { result = parseTasks(it[KEY_TASKS] ?: "[]") }
        return result
    }

    // ─── Serialization ────────────────────────────────────────────────────────

    private fun serializeShortcuts(list: List<ShortcutConfig>): String {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("label", s.label)
                put("type", s.type.name)
                put("phoneNumber", s.phoneNumber)
                put("latitude", s.latitude)
                put("longitude", s.longitude)
                put("destinationLabel", s.destinationLabel)
                put("url", s.url)
                put("packageName", s.packageName)
                put("smsBody", s.smsBody)
            })
        }
        return arr.toString()
    }

    private fun parseShortcuts(json: String): List<ShortcutConfig> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ShortcutConfig(
                    id               = o.getString("id"),
                    label            = o.getString("label"),
                    type             = ShortcutType.valueOf(o.getString("type")),
                    phoneNumber      = o.optString("phoneNumber"),
                    latitude         = o.optDouble("latitude", 0.0),
                    longitude        = o.optDouble("longitude", 0.0),
                    destinationLabel = o.optString("destinationLabel"),
                    url              = o.optString("url"),
                    packageName      = o.optString("packageName"),
                    smsBody          = o.optString("smsBody"),
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun serializeTasks(list: List<ScheduledTask>): String {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("label", t.label)
                put("type", t.type.name)
                put("isEnabled", t.isEnabled)
                put("contactName", t.contactName)
                put("phoneNumber", t.phoneNumber)
                put("messageBody", t.messageBody)
                put("hourOfDay", t.hourOfDay)
                put("minute", t.minute)
                put("repeatMode", t.repeatMode.name)
                put("daysOfWeek", JSONArray(t.daysOfWeek))
                put("triggerAtMillis", t.triggerAtMillis)
            })
        }
        return arr.toString()
    }

    private fun parseTasks(json: String): List<ScheduledTask> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val days = (0 until o.getJSONArray("daysOfWeek").length())
                    .map { o.getJSONArray("daysOfWeek").getInt(it) }
                ScheduledTask(
                    id              = o.getString("id"),
                    label           = o.getString("label"),
                    type            = TaskType.valueOf(o.getString("type")),
                    isEnabled       = o.getBoolean("isEnabled"),
                    contactName     = o.optString("contactName"),
                    phoneNumber     = o.optString("phoneNumber"),
                    messageBody     = o.optString("messageBody"),
                    hourOfDay       = o.getInt("hourOfDay"),
                    minute          = o.getInt("minute"),
                    repeatMode      = RepeatMode.valueOf(o.optString("repeatMode", "NONE")),
                    daysOfWeek      = days,
                    triggerAtMillis = o.optLong("triggerAtMillis", 0L),
                )
            }
        } catch (e: Exception) { emptyList() }
    }
}
