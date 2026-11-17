package com.ayratma.system

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

private val Bg = Color(0xFF080B12)
private val Panel = Color(0xFF121927)
private val Mint = Color(0xFF69D5C5)
private val Muted = Color(0xFF9AA8BC)
private val Pages = listOf("Genel Bakış", "CORE AI", "Ajanlar", "Hafıza", "Görevler", "Dosya Merkezi", "Sistem", "Onaylar", "Evolution", "Cloud", "Proje Merkezi")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AyratmaApp() }
    }
}

private object CoreApi {
    private const val BASE = "http://127.0.0.1:8765"
    fun request(path: String, method: String = "GET", body: JSONObject? = null): JSONObject {
        val conn = (URL(BASE + path).openConnection() as HttpURLConnection)
        conn.requestMethod = method
        conn.connectTimeout = 2500
        conn.readTimeout = 90000
        conn.setRequestProperty("Accept", "application/json")
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        try {
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            return JSONObject(text).put("_http", conn.responseCode)
        } finally { conn.disconnect() }
    }
}

@Composable
private fun AyratmaApp() {
    val context = LocalContext.current
    var page by remember { mutableStateOf(Pages[0]) }
    var status by remember { mutableStateOf("CORE bağlantısı kontrol ediliyor…") }
    var connected by remember { mutableStateOf(false) }
    var agents by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var tasks by remember { mutableStateOf(listOf<String>()) }
    var memory by remember { mutableStateOf(listOf<String>()) }
    var approvals by remember { mutableStateOf(listOf<String>()) }
    var chatInput by remember { mutableStateOf("") }
    var chatOutput by remember { mutableStateOf("AYRATMA CORE'a mesaj gönder. Yerel model çalışmıyorsa bağlantı hatası gösterilir.") }
    var taskInput by remember { mutableStateOf("") }
    var memoryInput by remember { mutableStateOf("") }
    var approvalInput by remember { mutableStateOf("") }
    var agentName by remember { mutableStateOf("") }
    var agentRole by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var editorText by remember { mutableStateOf("") }
    var openedName by remember { mutableStateOf("Dosya seçilmedi") }
    var info by remember { mutableStateOf("Hazır") }
    var refresh by remember { mutableIntStateOf(0) }

    fun runApi(path: String, method: String = "GET", body: JSONObject? = null, done: (JSONObject?, String?) -> Unit) {
        busy = true
        thread {
            try {
                val result = CoreApi.request(path, method, body)
                Handler(Looper.getMainLooper()).post {
                    busy = false
                    if (result.optInt("_http", 200) >= 400) done(null, result.optString("error", "HTTP " + result.optInt("_http")))
                    else done(result, null)
                }
            } catch (e: Exception) {
                runOnUiThread { busy = false; done(null, e.message ?: "CORE bağlantısı yok") }
            }
        }
    }

    fun refreshCore() {
        runApi("/health") { result, error ->
            connected = result != null
            status = if (result != null) "CORE bağlı • sürüm " + result.optString("version") else "CORE çevrimdışı • Termux backend başlatılmalı"
        }
        runApi("/agents") { result, _ ->
            if (result != null) {
                val arr = result.optJSONArray("agents") ?: JSONArray()
                agents = (0 until arr.length()).map { i ->
                    val a = arr.getJSONObject(i)
                    a.optString("id") to a.optJSONObject("role")?.toString().orEmpty().ifBlank { a.optString("role") }
                }
            }
        }
        runApi("/tasks") { result, _ ->
            if (result != null) {
                val arr = result.optJSONArray("tasks") ?: JSONArray()
                tasks = (0 until arr.length()).map { i -> arr.optJSONObject(i)?.toString() ?: arr.optString(i) }
            }
        }
        runApi("/memory") { result, _ ->
            if (result != null) {
                val arr = result.optJSONArray("memory") ?: JSONArray()
                memory = (0 until arr.length()).map { i -> arr.optJSONObject(i)?.toString() ?: arr.optString(i) }
            }
        }
        runApi("/approvals") { result, _ ->
            if (result != null) {
                val arr = result.optJSONArray("approvals") ?: JSONArray()
                approvals = (0 until arr.length()).map { i -> arr.optJSONObject(i)?.toString() ?: arr.optString(i) }
            }
        }
    }

    LaunchedEffect(Unit) { refreshCore() }
    LaunchedEffect(refresh) { if (refresh > 0) refreshCore() }

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { editorText = it.readText() }
            openedName = queryName(context, uri)
            info = "Dosya okundu. Düzenleyip yeni dosya olarak kaydedebilirsin."
        } catch (e: Exception) { info = "Okuma hatası: ${e.message}" }
    }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri != null) try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(editorText) }
            info = "Dosya kaydedildi."
        } catch (e: Exception) { info = "Kaydetme hatası: ${e.message}" }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, background = Bg, surface = Panel, onSurface = Color.White)) {
        Row(Modifier.fillMaxSize().background(Bg)) {
            Column(Modifier.width(104.dp).fillMaxHeight().background(Color(0xFF0D1420)).verticalScroll(rememberScrollState()).padding(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("AYRATMA", color = Mint, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp))
                Pages.forEach { label ->
                    val selected = page == label
                    TextButton(onClick = { page = label }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 5.dp, vertical = 8.dp), colors = ButtonDefaults.textButtonColors(contentColor = if (selected) Mint else Muted)) {
                        Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, lineHeight = 12.sp)
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("SYSTEM", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                        Text(page, color = Mint, fontSize = 12.sp)
                    }
                    Text(if (connected) "● CORE" else "○ CORE", color = if (connected) Mint else Color(0xFFFFB86B), fontSize = 10.sp)
                }
                when (page) {
                    "Genel Bakış" -> {
                        PanelCard("AYRATMA CORE", status)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard("Ajan", agents.size.toString(), Modifier.weight(1f))
                            MetricCard("Görev", tasks.size.toString(), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard("Hafıza", memory.size.toString(), Modifier.weight(1f))
                            MetricCard("Onay", approvals.size.toString(), Modifier.weight(1f))
                        }
                        PanelCard("Çalışma mimarisi", "Tek bir model yönlendiricisi + uzman ajan tanımları + araçlar + kalıcı hafıza. Yeni ajanlar katalog üzerinden eklenebilir; binlerce ayrı model indirilmez.")
                        ActionButton("Durumu yenile") { refresh++ }
                        PanelCard("Güvenlik", "Root yok. Dosya erişimi Android dosya seçicisiyle. Kritik işlemler onay gerektirir; uygulama gizli komut çalıştırmaz.")
                    }
                    "CORE AI" -> {
                        PanelCard("AYRATMA CORE CHAT", "Mesajlar Termux'ta çalışan yerel CORE API'ye gider. LLM için ayrıca localhost:8080 üzerinde llama.cpp sunucusu açık olmalıdır.")
                        EditField(chatInput, { chatInput = it }, "CORE'a mesajını yaz…", 3)
                        ActionButton(if (busy) "İşleniyor…" else "Gönder") {
                            if (chatInput.isNotBlank() && !busy) {
                                val prompt = chatInput
                                chatOutput = "Yanıt bekleniyor…"
                                runApi("/chat", "POST", JSONObject().put("message", prompt)) { result, error ->
                                    chatOutput = if (result != null) "[" + result.optString("agent") + "] " + result.optString("answer") else "CORE yanıt veremedi: " + error
                                }
                            }
                        }
                        PanelCard("Yanıt", chatOutput)
                    }
                    "Ajanlar" -> {
                        PanelCard("AI Agent Factory", "Yeni uzman ajan profilleri oluştur. Profiller SQLite'a kaydedilir ve katalogda kalıcı görünür; bunlar ayrı eğitilmiş modeller değil, CORE'un paylaştığı model için uzman rol tanımlarıdır.")
                        EditField(agentName, { agentName = it }, "Ajan adı (örn. DESIGNER)")
                        EditField(agentRole, { agentRole = it }, "Ajanın uzmanlık ve görev tanımı…", 3)
                        ActionButton("Yeni ajan oluştur") {
                            if (agentName.isNotBlank() && agentRole.isNotBlank() && !busy) {
                                runApi("/agents", "POST", JSONObject().put("name", agentName).put("role", agentRole)) { result, error ->
                                    info = if (result != null) "Ajan profili oluşturuldu." else "Ajan oluşturulamadı: $error"
                                    if (result != null) { agentName = ""; agentRole = ""; refresh++ }
                                }
                            }
                        }
                        PanelCard("Yerleşik uzmanlar", "ANALYST • ENGINEER • GUARD • RESEARCHER • ANDROID_EXPERT • DEBUGGER")
                        if (agents.isEmpty()) PanelCard("Bağlantı gerekli", "CORE başlatıldığında ajan kataloğu burada görünür.")
                        agents.forEach { (name, role) -> PanelCard(name, role) }
                    }
                    "Hafıza" -> {
                        EditField(memoryInput, { memoryInput = it }, "CORE hafızasına not ekle…", 2)
                        ActionButton("Hafızaya kaydet") {
                            if (memoryInput.isNotBlank() && !busy) runApi("/memory", "POST", JSONObject().put("body", memoryInput)) { result, error ->
                                info = if (result != null) "Hafızaya kaydedildi." else "Kaydedilemedi: $error"
                                if (result != null) { memoryInput = ""; refresh++ }
                            }
                        }
                        memory.forEach { PanelCard("Hafıza kaydı", it) }
                        if (memory.isEmpty()) PanelCard("Kayıt yok", "CORE veritabanında henüz kayıt bulunmuyor.")
                    }
                    "Görevler" -> {
                        EditField(taskInput, { taskInput = it }, "Yeni görev tanımı…", 2)
                        ActionButton("Görevi planla") {
                            if (taskInput.isNotBlank() && !busy) runApi("/tasks/plan", "POST", JSONObject().put("title", taskInput)) { result, error ->
                                info = if (result != null) "Görev planı oluşturuldu." else "Plan oluşturulamadı: $error"
                                if (result != null) { taskInput = ""; refresh++ }
                            }
                        }
                        tasks.forEach { PanelCard("Görev kaydı", it) }
                        if (tasks.isEmpty()) PanelCard("Görev bulunamadı", "Yeni görev ekleyerek CORE planlayıcısını kullan.")
                    }
                    "Dosya Merkezi" -> {
                        PanelCard("SAF dosya erişimi", "Sadece Android dosya seçicisinden senin seçtiğin dosyaları açar. Tüm depolama izni istemez.")
                        Text(openedName, color = Mint, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionButton("Dosya aç", Modifier.weight(1f)) { openFile.launch(arrayOf("text/*", "application/json", "application/xml")) }
                            OutlinedButton(onClick = { saveFile.launch("ayratma-output.txt") }, modifier = Modifier.weight(1f)) { Text("Yeni dosya kaydet", fontSize = 11.sp) }
                        }
                        EditField(editorText, { editorText = it }, "Dosya içeriği / kod…", 12)
                        PanelCard("Güvenli düzenleme", "Bu sürüm dosyayı açıp yeni hedefe kaydeder. Orijinal dosyayı değiştirme ve kod çalıştırma otomasyonu kapalıdır.")
                    }
                    "Sistem" -> {
                        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        val mi = ActivityManager.MemoryInfo()
                        am.getMemoryInfo(mi)
                        val stat = StatFs(Environment.getDataDirectory().path)
                        PanelCard("Cihaz", "${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE} • API ${Build.VERSION.SDK_INT}")
                        PanelCard("RAM", "Toplam: ${mi.totalMem / 1048576} MB\nKullanılabilir: ${mi.availMem / 1048576} MB")
                        PanelCard("Depolama", "Toplam: ${stat.totalBytes / 1073741824} GB\nBoş: ${stat.availableBytes / 1073741824} GB")
                        ActionButton("Sistem bilgilerini yenile") { refresh++ }
                    }
                    "Onaylar" -> {
                        EditField(approvalInput, { approvalInput = it }, "Onay kaydı başlığı…", 2)
                        ActionButton("Onay isteği kaydet") {
                            if (approvalInput.isNotBlank() && !busy) runApi("/approvals", "POST", JSONObject().put("title", approvalInput).put("detail", "Kullanıcı tarafından uygulama içinden oluşturuldu.")) { result, error ->
                                info = if (result != null) "Onay kaydı oluşturuldu." else "İşlem başarısız: $error"
                                if (result != null) { approvalInput = ""; refresh++ }
                            }
                        }
                        approvals.forEach { PanelCard("Onay kaydı", it) }
                        if (approvals.isEmpty()) PanelCard("Bekleyen kayıt yok", "Hassas işlemler için onay kaydı burada tutulur.")
                    }
                    "Evolution" -> {
                        PanelCard("Evolution Engine", "Bu modül şu an güvenli geliştirme altyapısı ekranıdır; kendini gizlice değiştirmez.")
                        PanelCard("Mevcut", "CORE API sağlık kontrolü • görev planlama • SQLite hafıza • ajan yönlendirme.")
                        PanelCard("Sonraki entegrasyon", "Kod değişikliği önerisi → sandbox testi → kullanıcı onayı → yedek/geri alma → sürüm.")
                        PanelCard("Otomatik güncelleme", "İmzalı sürüm doğrulaması ve kullanıcı onayı olmadan APK güncellenmez.")
                    }
                    "Cloud" -> {
                        PanelCard("AYRATMA Cloud", "Bulut servisi henüz kurulmadı. Bu ekran bağlantı varmış gibi davranmaz.")
                        PanelCard("Güvenli bulut mimarisi", "Kimlik doğrulama, şifreli aktarım, kullanıcı kontrollü senkronizasyon ve maliyet/onay sınırları.")
                        PanelCard("Durum", "Sunucu: yapılandırılmadı • Senkronizasyon: kapalı")
                    }
                    "Proje Merkezi" -> {
                        PanelCard("AI Project Builder", "Proje gereksinimlerini görevlere ayıran merkez. Kod üretimi için yerel model bağlantısı gereklidir.")
                        EditField(taskInput, { taskInput = it }, "Örn. Android için not uygulaması oluştur…", 3)
                        ActionButton("Proje planını oluştur") {
                            if (taskInput.isNotBlank() && !busy) runApi("/tasks/plan", "POST", JSONObject().put("title", "PROJE: " + taskInput)) { result, error ->
                                info = if (result != null) "Proje planı CORE görevlerine eklendi." else "Planlama başarısız: $error"
                                if (result != null) { taskInput = ""; refresh++ }
                            }
                        }
                    }
                }
                if (info != "Hazır") Text(info, color = Mint, fontSize = 11.sp)
                Text("MADE BY AYRATMA • v0.3.0", color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun PanelCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Mint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(body, color = Color(0xFFCAD3E0), fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = Mint, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ActionButton(label: String, modifier: Modifier = Modifier, action: () -> Unit) {
    Button(onClick = action, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(label, fontSize = 12.sp) }
}

@Composable
private fun EditField(value: String, onChange: (String) -> Unit, hint: String, minLines: Int = 1) {
    Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        BasicTextField(value, onChange, modifier = Modifier.fillMaxWidth().padding(12.dp).heightIn(min = (minLines * 22).dp), textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp), decorationBox = { inner ->
            if (value.isEmpty()) Text(hint, color = Muted, fontSize = 12.sp)
            inner()
        })
    }
}

private fun queryName(context: Context, uri: Uri): String {
    var name = uri.lastPathSegment ?: "Seçilen dosya"
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val index = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && c.moveToFirst()) name = c.getString(index)
    }
    return name
}
