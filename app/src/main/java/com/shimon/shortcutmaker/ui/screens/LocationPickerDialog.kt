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
    var statusMsg    by remember { mutableStateOf("לחץ על המפה לבחירת מיקום") }

    // HTML עם OpenStreetMap ישירות דרך iframe של umap - גישה פשוטה יותר
    val mapHtml = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"/>
<style>
*{margin:0;padding:0;box-sizing:border-box}
html,body{width:100%;height:100%;overflow:hidden;background:#1B2E45}
#map{width:100%;height:100%;position:absolute;top:0;left:0}
#info{position:absolute;bottom:8px;left:50%;transform:translateX(-50%);
  background:rgba(0,0,0,0.7);color:#fff;padding:6px 12px;border-radius:20px;
  font-size:12px;z-index:1000;pointer-events:none;white-space:nowrap}
.marker{width:32px;height:32px;position:absolute;transform:translate(-50%,-100%)}
</style>
</head>
<body>
<div id="map"></div>
<div id="info">טוען מפה...</div>
<script>
// OSM tile map without external library - pure canvas approach
var canvas = document.createElement('canvas');
canvas.style.cssText = 'width:100%;height:100%;position:absolute;top:0;left:0';
document.getElementById('map').appendChild(canvas);

var ctx = canvas.getContext('2d');
var zoom = 13;
var centerLat = 31.7683;
var centerLng = 35.2137;
var isDragging = false;
var lastX, lastY;
var markerLat = null, markerLng = null;
var tiles = {};
var pendingTiles = 0;

function resize(){
  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;
  draw();
}

function lat2y(lat,z){ return Math.floor((1-Math.log(Math.tan(lat*Math.PI/180)+1/Math.cos(lat*Math.PI/180))/Math.PI)/2*Math.pow(2,z)); }
function lng2x(lng,z){ return Math.floor((lng+180)/360*Math.pow(2,z)); }
function tile2lat(y,z){ var n=Math.PI-2*Math.PI*y/Math.pow(2,z); return 180/Math.PI*Math.atan(0.5*(Math.exp(n)-Math.exp(-n))); }
function tile2lng(x,z){ return x/Math.pow(2,z)*360-180; }

function pixToLatLng(px,py){
  var tileSize=256;
  var centerTileX=lng2x(centerLng,zoom);
  var centerTileY=lat2y(centerLat,zoom);
  var cx=canvas.width/2, cy=canvas.height/2;
  var tx=centerTileX+(px-cx)/tileSize;
  var ty=centerTileY+(py-cy)/tileSize;
  var n=Math.PI-2*Math.PI*ty/Math.pow(2,zoom);
  var lat=180/Math.PI*Math.atan(0.5*(Math.exp(n)-Math.exp(-n)));
  var lng=tx/Math.pow(2,zoom)*360-180;
  return {lat:Math.round(lat*1e6)/1e6, lng:Math.round(lng*1e6)/1e6};
}

function loadTile(x,y,z){
  var key=z+'/'+x+'/'+y;
  if(tiles[key]) return tiles[key];
  var img=new Image();
  img.crossOrigin='anonymous';
  var s=['a','b','c'][Math.abs(x+y)%3];
  img.src='https://'+s+'.tile.openstreetmap.org/'+z+'/'+x+'/'+y+'.png';
  img.onload=function(){ tiles[key]=img; draw(); };
  tiles[key]='loading';
  return null;
}

function draw(){
  ctx.fillStyle='#1B2E45';
  ctx.fillRect(0,0,canvas.width,canvas.height);
  var tileSize=256;
  var centerTX=lng2x(centerLng,zoom);
  var centerTY=lat2y(centerLat,zoom);
  var cx=canvas.width/2, cy=canvas.height/2;
  var tilesX=Math.ceil(canvas.width/tileSize)+2;
  var tilesY=Math.ceil(canvas.height/tileSize)+2;
  for(var dx=-tilesX;dx<=tilesX;dx++){
    for(var dy=-tilesY;dy<=tilesY;dy++){
      var tx=centerTX+dx, ty=centerTY+dy;
      var px=cx+(dx)*tileSize, py=cy+(dy)*tileSize;
      var img=loadTile(tx,ty,zoom);
      if(img && img!=='loading') ctx.drawImage(img,px,py,tileSize,tileSize);
      else{ ctx.fillStyle='#243B55'; ctx.fillRect(px,py,tileSize-1,tileSize-1); }
    }
  }
  // מרקר
  if(markerLat!==null){
    var mtx=lng2x(markerLng,zoom), mty=lat2y(markerLat,zoom);
    var mpx=cx+(mtx-centerTX)*tileSize, mpy=cy+(mty-centerTY)*tileSize;
    ctx.beginPath();
    ctx.arc(mpx,mpy-16,10,0,2*Math.PI);
    ctx.fillStyle='#FF4444'; ctx.fill();
    ctx.strokeStyle='white'; ctx.lineWidth=2; ctx.stroke();
    ctx.beginPath(); ctx.moveTo(mpx,mpy-6); ctx.lineTo(mpx,mpy);
    ctx.strokeStyle='#FF4444'; ctx.lineWidth=2; ctx.stroke();
  }
}

// Touch events
canvas.addEventListener('touchstart',function(e){
  e.preventDefault();
  if(e.touches.length===1){ isDragging=true; lastX=e.touches[0].clientX; lastY=e.touches[0].clientY; }
},{ passive:false });

canvas.addEventListener('touchmove',function(e){
  e.preventDefault();
  if(isDragging && e.touches.length===1){
    var dx=e.touches[0].clientX-lastX, dy=e.touches[0].clientY-lastY;
    var tileSize=256;
    centerLng-=dx/tileSize*360/Math.pow(2,zoom);
    var n=Math.PI-2*Math.PI*(lat2y(centerLat,zoom)-dy/tileSize)/Math.pow(2,zoom);
    centerLat=180/Math.PI*Math.atan(0.5*(Math.exp(n)-Math.exp(-n)));
    lastX=e.touches[0].clientX; lastY=e.touches[0].clientY;
    draw();
  }
},{ passive:false });

canvas.addEventListener('touchend',function(e){
  e.preventDefault();
  isDragging=false;
},{ passive:false });

canvas.addEventListener('click',function(e){
  var rect=canvas.getBoundingClientRect();
  var px=e.clientX-rect.left, py=e.clientY-rect.top;
  var ll=pixToLatLng(px,py);
  markerLat=ll.lat; markerLng=ll.lng;
  draw();
  document.getElementById('info').textContent=ll.lat+', '+ll.lng;
  Android.onPick(ll.lat,ll.lng,ll.lat+', '+ll.lng);
});

// Pinch zoom
var lastDist=0;
canvas.addEventListener('touchstart',function(e){
  if(e.touches.length===2){
    lastDist=Math.hypot(e.touches[0].clientX-e.touches[1].clientX,e.touches[0].clientY-e.touches[1].clientY);
  }
},{ passive:false });
canvas.addEventListener('touchmove',function(e){
  if(e.touches.length===2){
    var dist=Math.hypot(e.touches[0].clientX-e.touches[1].clientX,e.touches[0].clientY-e.touches[1].clientY);
    if(Math.abs(dist-lastDist)>20){
      if(dist>lastDist && zoom<18){ zoom++; tiles={}; }
      else if(dist<lastDist && zoom>3){ zoom--; tiles={}; }
      lastDist=dist; draw();
    }
  }
},{ passive:false });

function jumpTo(lat,lng,label){
  centerLat=lat; centerLng=lng; zoom=15;
  markerLat=lat; markerLng=lng;
  tiles={}; draw();
  document.getElementById('info').textContent=label;
  Android.onPick(lat,lng,label);
}

window.addEventListener('resize',resize);
resize();
document.getElementById('info').textContent='לחץ על המפה לבחירת נקודה';
</script>
</body>
</html>""".trimIndent()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("בחר מיקום", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        placeholder = { Text("חפש כתובת...") },
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = {
                        if (searchQuery.isNotBlank()) {
                            isSearching = true
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val enc = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                                    val url = "https://nominatim.openstreetmap.org/search?q=$enc&format=json&limit=1"
                                    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                                    conn.setRequestProperty("User-Agent","ShortcutMaker/1.0")
                                    conn.connectTimeout=8000; conn.readTimeout=8000
                                    val resp = conn.inputStream.bufferedReader().readText()
                                    conn.disconnect()
                                    val arr = org.json.JSONArray(resp)
                                    withContext(Dispatchers.Main) {
                                        isSearching = false
                                        if (arr.length()>0){
                                            val o=arr.getJSONObject(0)
                                            val lat=o.getDouble("lat"); val lng=o.getDouble("lon")
                                            val lbl=o.getString("display_name").split(",").take(2).joinToString(", ")
                                            statusMsg = lbl
                                            webViewRef?.evaluateJavascript("jumpTo($lat,$lng,'${lbl.replace("'","")}');",null)
                                        } else statusMsg="לא נמצא"
                                    }
                                } catch(e:Exception){ withContext(Dispatchers.Main){ isSearching=false; statusMsg="שגיאה" } }
                            }
                        }
                    }) {
                        if (isSearching) CircularProgressIndicator(modifier=Modifier.size(20.dp),strokeWidth=2.dp)
                        else Icon(Icons.Default.Search,"חיפוש")
                    }
                }

                Text(statusMsg, fontSize=12.sp, color=MaterialTheme.colorScheme.primary,
                    modifier=Modifier.padding(top=4.dp))

                Spacer(Modifier.height(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(factory = { ctx ->
                        WebView(ctx).also { wv ->
                            wv.settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/91.0.4472.120 Mobile Safari/537.36"
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = false
                                displayZoomControls = false
                            }
                            wv.setBackgroundColor(android.graphics.Color.parseColor("#1B2E45"))
                            wv.webChromeClient = WebChromeClient()
                            wv.webViewClient = WebViewClient()
                            wv.addJavascriptInterface(object : Any() {
                                @JavascriptInterface
                                fun onPick(lat: Double, lng: Double, label: String) {
                                    pickedLat = lat; pickedLng = lng; pickedLabel = label
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        statusMsg = "📍 ${"%.5f".format(lat)}, ${"%.5f".format(lng)}"
                                    }
                                }
                            }, "Android")
                            wv.loadDataWithBaseURL("https://tile.openstreetmap.org", mapHtml, "text/html", "utf-8", null)
                            webViewRef = wv
                        }
                    }, modifier = Modifier.fillMaxSize())
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick=onDismiss, modifier=Modifier.weight(1f)){ Text("ביטול") }
                    Button(
                        onClick={ onLocationPicked(PickedLocation(pickedLat?:0.0,pickedLng?:0.0,pickedLabel.ifBlank{searchQuery})) },
                        modifier=Modifier.weight(1f), enabled=pickedLat!=null
                    ){ Text("אישור") }
                }
            }
        }
    }
}
