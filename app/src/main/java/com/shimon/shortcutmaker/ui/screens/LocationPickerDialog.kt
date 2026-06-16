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
    var searchQuery  by remember { mutableStateOf("") }
    var pickedLat    by remember { mutableStateOf<Double?>(null) }
    var pickedLng    by remember { mutableStateOf<Double?>(null) }
    var pickedLabel  by remember { mutableStateOf("") }
    var webViewRef   by remember { mutableStateOf<WebView?>(null) }
    var isSearching  by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf("") }

    val mapHtml = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1"/>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
* { margin:0; padding:0; box-sizing:border-box; }
html,body,#map { width:100%; height:100%; }
</style>
</head>
<body>
<div id="map"></div>
<script>
var map = L.map('map',{zoomControl:true}).setView([31.7683,35.2137],8);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{
  attribution:'© OSM',maxZoom:19
}).addTo(map);
var marker=null;
function setMarker(lat,lng){
  if(marker) marker.remove();
  marker=L.marker([lat,lng]).addTo(map);
}
map.on('click',function(e){
  var lat=Math.round(e.latlng.lat*1e6)/1e6;
  var lng=Math.round(e.latlng.lng*1e6)/1e6;
  setMarker(lat,lng);
  Android.onPick(lat,lng,''+lat+', '+lng);
});
function jumpTo(lat,lng,label){
  map.setView([lat,lng],15);
  setMarker(lat,lng);
  Android.onPick(lat,lng,label);
}
</script>
</body>
</html>
""".trimIndent()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("בחר מיקום", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))

                // ── חיפוש כתובת ──────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("חפש כתובת...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = {
                        if (searchQuery.isNotBlank()) {
                            isSearching = true
                            searchResult = ""
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val enc = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                                    val url = "https://nominatim.openstreetmap.org/search?q=$enc&format=json&limit=1"
                                    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                                    conn.setRequestProperty("User-Agent", "ShortcutMaker/1.0")
                                    conn.connectTimeout = 8000; conn.readTimeout = 8000
                                    val resp = conn.inputStream.bufferedReader().readText()
                                    conn.disconnect()
                                    val arr = org.json.JSONArray(resp)
                                    withContext(Dispatchers.Main) {
                                        isSearching = false
                                        if (arr.length() > 0) {
                                            val o = arr.getJSONObject(0)
                                            val lat = o.getDouble("lat")
                                            val lng = o.getDouble("lon")
                                            val lbl = o.getString("display_name").split(",").take(2).joinToString(", ")
                                            searchResult = lbl
                                            webViewRef?.evaluateJavascript("jumpTo($lat,$lng,'${lbl.replace("'","")}')", null)
                                        } else {
                                            searchResult = "לא נמצא"
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { isSearching = false; searchResult = "שגיאת חיפוש" }
                                }
                            }
                        }
                    }) {
                        if (isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Search, "חיפוש")
                    }
                }

                if (searchResult.isNotBlank()) {
                    Text(searchResult, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(Modifier.height(8.dp))

                // ── מפה ──────────────────────────────────────────────────────
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).also { wv ->
                                wv.settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    userAgentString = "Mozilla/5.0 (Android) ShortcutMaker"
                                }
                                wv.webViewClient = object : WebViewClient() {
                                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                        // ignore tile errors
                                    }
                                }
                                wv.webChromeClient = WebChromeClient()
                                wv.addJavascriptInterface(object : Any() {
                                    @JavascriptInterface
                                    fun onPick(lat: Double, lng: Double, label: String) {
                                        pickedLat = lat
                                        pickedLng = lng
                                        pickedLabel = label
                                    }
                                }, "Android")
                                wv.loadDataWithBaseURL(
                                    "https://openstreetmap.org", mapHtml,
                                    "text/html", "utf-8", null
                                )
                                webViewRef = wv
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // ── מיקום נבחר ────────────────────────────────────────────────
                if (pickedLat != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "📍 ${"%.5f".format(pickedLat)}, ${"%.5f".format(pickedLng)}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("ביטול") }
                    Button(
                        onClick = {
                            onLocationPicked(PickedLocation(
                                pickedLat ?: 0.0,
                                pickedLng ?: 0.0,
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
