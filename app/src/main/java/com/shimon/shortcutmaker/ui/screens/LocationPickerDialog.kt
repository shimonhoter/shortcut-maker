package com.shimon.shortcutmaker.ui.screens

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.*

data class PickedLocation(val lat: Double, val lng: Double, val label: String)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LocationPickerDialog(
    onDismiss: () -> Unit,
    onLocationPicked: (PickedLocation) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var pickedLat   by remember { mutableStateOf<Double?>(null) }
    var pickedLng   by remember { mutableStateOf<Double?>(null) }
    var pickedLabel by remember { mutableStateOf("") }
    var webViewRef  by remember { mutableStateOf<WebView?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    // HTML map page using Leaflet (OpenStreetMap) – no API key needed
    val mapHtml = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
  body { margin:0; padding:0; }
  #map { width:100vw; height:100vh; }
</style>
</head>
<body>
<div id="map"></div>
<script>
  var map = L.map('map').setView([31.7683, 35.2137], 8);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap'
  }).addTo(map);

  var marker = null;

  map.on('click', function(e) {
    var lat = e.latlng.lat.toFixed(6);
    var lng = e.latlng.lng.toFixed(6);
    if (marker) marker.remove();
    marker = L.marker([lat, lng]).addTo(map);
    // Call Android interface
    Android.onLocationPicked(parseFloat(lat), parseFloat(lng));
  });

  function jumpTo(lat, lng, label) {
    map.setView([lat, lng], 15);
    if (marker) marker.remove();
    marker = L.marker([lat, lng]).addTo(map).bindPopup(label).openPopup();
    Android.onLocationPicked(lat, lng);
  }
</script>
</body>
</html>
    """.trimIndent()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("בחר מיקום במפה", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))

                // ── Search bar ──────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("חיפוש כתובת...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                isSearching = true
                                searchAddress(searchQuery, webViewRef) { label ->
                                    isSearching = false
                                    if (label != null) pickedLabel = label
                                }
                            }
                        }
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, "חיפוש")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Map WebView ─────────────────────────────────────────────
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                                addJavascriptInterface(object : Any() {
                                    @JavascriptInterface
                                    fun onLocationPicked(lat: Double, lng: Double) {
                                        pickedLat = lat
                                        pickedLng = lng
                                        if (pickedLabel.isBlank()) {
                                            pickedLabel = "%.4f, %.4f".format(lat, lng)
                                        }
                                    }
                                }, "Android")
                                loadDataWithBaseURL(
                                    "https://openstreetmap.org",
                                    mapHtml, "text/html", "utf-8", null
                                )
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // ── Picked location display ─────────────────────────────────
                if (pickedLat != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "📍 ${"%.4f".format(pickedLat)}, ${"%.4f".format(pickedLng)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Buttons ─────────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("ביטול")
                    }
                    Button(
                        onClick = {
                            val lat = pickedLat ?: return@Button
                            val lng = pickedLng ?: return@Button
                            onLocationPicked(PickedLocation(lat, lng,
                                pickedLabel.ifBlank { searchQuery }))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = pickedLat != null
                    ) {
                        Text("אישור")
                    }
                }
            }
        }
    }
}

/**
 * Geocode address via Nominatim (OpenStreetMap) – no API key.
 * On success calls jumpTo() in the WebView JS.
 */
private fun searchAddress(
    query: String,
    webView: WebView?,
    onResult: (String?) -> Unit,
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1"
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "ShortcutMaker/1.0")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val arr = org.json.JSONArray(response)
            if (arr.length() > 0) {
                val obj   = arr.getJSONObject(0)
                val lat   = obj.getDouble("lat")
                val lng   = obj.getDouble("lon")
                val label = obj.getString("display_name").split(",").take(2).joinToString(", ")
                withContext(Dispatchers.Main) {
                    webView?.evaluateJavascript("jumpTo($lat, $lng, '$label')", null)
                    onResult(label)
                }
            } else {
                withContext(Dispatchers.Main) { onResult(null) }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onResult(null) }
        }
    }
}
