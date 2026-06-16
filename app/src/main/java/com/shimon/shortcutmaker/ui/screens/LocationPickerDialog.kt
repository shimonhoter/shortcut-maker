package com.shimon.shortcutmaker.ui.screens

import android.annotation.SuppressLint
import android.webkit.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.*

data class PickedLocation(val lat: Double, val lng: Double, val label: String)
data class NominatimResult(val lat: Double, val lng: Double, val short: String, val detail: String)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LocationPickerDialog(
    onDismiss: () -> Unit,
    onLocationPicked: (PickedLocation) -> Unit,
) {
    val context = LocalContext.current
    var searchQuery   by remember { mutableStateOf("") }
    var isSearching   by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<NominatimResult>>(emptyList()) }
    var errorMsg      by remember { mutableStateOf("") }
    var pickedLat     by remember { mutableStateOf<Double?>(null) }
    var pickedLng     by remember { mutableStateOf<Double?>(null) }
    var pickedLabel   by remember { mutableStateOf("") }
    var webViewRef    by remember { mutableStateOf<WebView?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("בחר מיקום", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))

                // ── חיפוש ─────────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        placeholder = { Text("חפש כתובת, עיר, מקום...") },
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = {
                        if (searchQuery.isNotBlank()) {
                            isSearching = true; errorMsg = ""; searchResults = emptyList()
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val enc = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                                    val url = "https://nominatim.openstreetmap.org/search?q=$enc&format=json&limit=5"
                                    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                                    conn.setRequestProperty("User-Agent", "ShortcutMaker/1.0")
                                    conn.connectTimeout = 8000; conn.readTimeout = 8000
                                    val resp = conn.inputStream.bufferedReader().readText()
                                    conn.disconnect()
                                    val arr = org.json.JSONArray(resp)
                                    val results = (0 until arr.length()).map { i ->
                                        val o = arr.getJSONObject(i)
                                        NominatimResult(
                                            lat   = o.getDouble("lat"),
                                            lng   = o.getDouble("lon"),
                                            short = o.getString("display_name").split(",").take(2).joinToString(", "),
                                            detail = o.getString("display_name").split(",").drop(2).take(2).joinToString(", ")
                                        )
                                    }
                                    withContext(Dispatchers.Main) {
                                        isSearching = false
                                        searchResults = results
                                        if (results.isEmpty()) errorMsg = "לא נמצאו תוצאות"
                                        else {
                                            // קפוץ לתוצאה ראשונה במפה
                                            val r = results[0]
                                            val js = "jumpTo(${r.lat}, ${r.lng}, '${r.short.replace("'","\\'")}');"
                                            webViewRef?.evaluateJavascript(js, null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { isSearching = false; errorMsg = "שגיאת חיפוש" }
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
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp))
                }

                // ── תוצאות חיפוש ──────────────────────────────────────────────
                if (searchResults.size > 1) {
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 130.dp)) {
                        items(searchResults) { r ->
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val js = "jumpTo(${r.lat}, ${r.lng}, '${r.short.replace("'","\\'")}');"
                                    webViewRef?.evaluateJavascript(js, null)
                                    searchResults = emptyList()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(r.short, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    if (r.detail.isNotBlank())
                                        Text(r.detail, fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ── WebView עם Leaflet מ-assets ───────────────────────────────
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).also { wv ->
                                wv.settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    userAgentString = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/91.0 Mobile Safari/537.36"
                                }
                                wv.webChromeClient = WebChromeClient()
                                wv.webViewClient = WebViewClient()
                                wv.addJavascriptInterface(object : Any() {
                                    @JavascriptInterface
                                    fun onPick(lat: Double, lng: Double, label: String) {
                                        pickedLat = lat
                                        pickedLng = lng
                                        pickedLabel = label
                                    }
                                }, "Android")
                                // טוען map.html מ-assets – Leaflet טעון מקומית
                                wv.loadUrl("file:///android_asset/map.html")
                                webViewRef = wv
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // ── מיקום נבחר ────────────────────────────────────────────────
                if (pickedLat != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("📍 $pickedLabel", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("ביטול") }
                    Button(
                        onClick = {
                            onLocationPicked(PickedLocation(
                                pickedLat ?: 0.0, pickedLng ?: 0.0,
                                pickedLabel.ifBlank { searchQuery }
                            ))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = pickedLat != null
                    ) { Text("אישור") }
                }
            }
        }
    }
}
