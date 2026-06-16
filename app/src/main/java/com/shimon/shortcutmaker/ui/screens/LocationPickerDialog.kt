package com.shimon.shortcutmaker.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.*

data class PickedLocation(val lat: Double, val lng: Double, val label: String)

@Composable
fun LocationPickerDialog(
    onDismiss: () -> Unit,
    onLocationPicked: (PickedLocation) -> Unit,
) {
    val context = LocalContext.current
    var searchQuery  by remember { mutableStateOf("") }
    var isSearching  by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<NominatimResult>>(emptyList()) }
    var errorMsg     by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("בחר מיקום", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))

                // ── חיפוש ─────────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("חפש כתובת, עיר, מקום...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = {
                        if (searchQuery.isNotBlank()) {
                            isSearching = true
                            errorMsg = ""
                            searchResults = emptyList()
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val enc = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                                    val url = "https://nominatim.openstreetmap.org/search?q=$enc&format=json&limit=5&addressdetails=1"
                                    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                                    conn.setRequestProperty("User-Agent", "ShortcutMaker/1.0")
                                    conn.connectTimeout = 8000; conn.readTimeout = 8000
                                    val resp = conn.inputStream.bufferedReader().readText()
                                    conn.disconnect()
                                    val arr = org.json.JSONArray(resp)
                                    val results = (0 until arr.length()).map { i ->
                                        val o = arr.getJSONObject(i)
                                        NominatimResult(
                                            lat = o.getDouble("lat"),
                                            lng = o.getDouble("lon"),
                                            display = o.getString("display_name"),
                                            short = o.getString("display_name").split(",").take(2).joinToString(", ")
                                        )
                                    }
                                    withContext(Dispatchers.Main) {
                                        isSearching = false
                                        searchResults = results
                                        if (results.isEmpty()) errorMsg = "לא נמצאו תוצאות"
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isSearching = false
                                        errorMsg = "שגיאת חיפוש – בדוק חיבור לאינטרנט"
                                    }
                                }
                            }
                        }
                    }) {
                        if (isSearching)
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        else
                            Icon(Icons.Default.Search, "חיפוש")
                    }
                }

                if (errorMsg.isNotBlank()) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(Modifier.height(8.dp))

                // ── תוצאות חיפוש ──────────────────────────────────────────────
                if (searchResults.isNotEmpty()) {
                    Text("תוצאות:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(searchResults) { result ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        onLocationPicked(PickedLocation(result.lat, result.lng, result.short))
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(result.short, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(result.display.split(",").drop(2).take(2).joinToString(", "),
                                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ── אפשרויות ידניות ──────────────────────────────────────────
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        Spacer(Modifier.height(12.dp))
                        Text("חפש כתובת או מיקום בשורת החיפוש",
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(Modifier.height(20.dp))

                        // פתיחת Google Maps לבחירת מיקום
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW,
                                    Uri.parse("geo:31.7683,35.2137?q=31.7683,35.2137"))
                                intent.setPackage("com.google.android.apps.maps")
                                try { context.startActivity(intent) } catch (e: Exception) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://maps.google.com")))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Map, null)
                            Spacer(Modifier.width(8.dp))
                            Text("פתח Google Maps לחיפוש")
                        }
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

data class NominatimResult(val lat: Double, val lng: Double, val display: String, val short: String)
