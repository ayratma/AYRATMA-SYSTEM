package com.ayratma.system

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF080B12)
private val Panel = Color(0xFF121927)
private val Mint = Color(0xFF69D5C5)
private val Muted = Color(0xFF9AA8BC)

class MainActivity : ComponentActivity() {
    private lateinit var db: LocalStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = LocalStore(this)
        setContent { AyratmaApp(db) }
    }
}

private class LocalStore(context: Context) : android.database.sqlite.SQLiteOpenHelper(context, "ayratma.db", null, 1) {
    override fun onCreate(db: android.database.sqlite.SQLiteDatabase) {
        db.execSQL("CREATE TABLE memory(id INTEGER PRIMARY KEY AUTOINCREMENT, body TEXT NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, status TEXT NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE approvals(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, detail TEXT NOT NULL, status TEXT NOT NULL, created_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: android.database.sqlite.SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun add(table: String, title: String, detail: String = "") {
        val values = android.content.ContentValues().apply {
            if (table == "memory") { put("body", title); put("created_at", System.currentTimeMillis()) }
            else if (table == "tasks") { put("title", title); put("status", "Bekliyor"); put("created_at", System.currentTimeMillis()) }
            else { put("title", title); put("detail", detail); put("status", "Onay bekliyor"); put("created_at", System.currentTimeMillis()) }
        }
        writableDatabase.insert(table, null, values)
    }
    fun rows(table: String): List<String> {
        val column = if (table == "memory") "body" else "title"
        val result = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT $column FROM $table ORDER BY id DESC LIMIT 50", null).use { c ->
            while (c.moveToNext()) result.add(c.getString(0))
        }
        return result
    }
}

@Composable
private fun AyratmaApp(db: LocalStore) {
    var page by remember { mutableStateOf("Kontrol Merkezi") }
    var memoryInput by remember { mutableStateOf("") }
    var taskInput by remember { mutableStateOf("") }
    var editorText by remember { mutableStateOf("") }
    var openedName by remember { mutableStateOf("Henüz dosya seçilmedi") }
    var info by remember { mutableStateOf("Hazır") }
    var refresh by remember { mutableIntStateOf(0) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { editorText = it.readText() }
                openedName = queryName(ctx, uri)
                info = "Dosya okundu. Düzenleyip Kaydet ile yeni dosya oluşturabilirsin."
            } catch (e: Exception) { info = "Dosya okunamadı: ${e.message}" }
        }
    }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri != null) try {
            ctx.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(editorText) }
            info = "Dosya kaydedildi."
        } catch (e: Exception) { info = "Kaydetme hatası: ${e.message}" }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, background = Bg, surface = Panel, onSurface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("AYRATMA", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("SYSTEM", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("v0.2.0", color = Muted)
                }
                Text("Yerel kontrol merkezi • ${Build.MODEL}", color = Muted, fontSize = 12.sp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Kontrol Merkezi", "CORE AI", "Uzman Ajanlar", "Hafıza", "Dosya / Kod", "Sistem İzleme", "Görevler", "Onay Merkezi", "Evolution Engine", "Cloud").chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { label ->
                                OutlinedButton(onClick = { page = label }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp)) {
                                    Text(label, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                when (page) {
                    "Kontrol Merkezi" -> {
                        PanelCard("Sistem durumu", "Uygulama ve yerel SQLite hafızası aktif. AI modeli henüz bağlanmadı.")
                        PanelCard("Çalışan yerel özellikler", "Dosya seçme/okuma • metin düzenleme/kaydetme • kalıcı notlar • görev listesi • cihaz bilgileri")
                        PanelCard("Güvenlik", "Dosyalar yalnızca Android dosya seçicisiyle senin seçtiğin konumdan açılır. Kod otomatik çalıştırılmaz. Kritik işlemler onay gerektirir.")
                    }
                    "CORE AI" -> {
                        PanelCard("AYRATMA CORE", "Orkestrasyon arayüzü hazır; gerçek yanıt üretimi için yerel model veya yapılandırılmış bir AI sağlayıcısı gerekiyor.")
                        PanelCard("Çalışma modu", "Çevrimdışı: model kurulana kadar kullanılamaz. Çevrimiçi: sağlayıcı bağlantısı henüz yapılandırılmadı.")
                        Text("API anahtarını bu ekrana veya sohbete yazma. Güvenli sağlayıcı ayarları ayrı bir aşamada eklenecek.", color = Muted, fontSize = 12.sp)
                    }
                    "Uzman Ajanlar" -> {
                        PanelCard("ANALYST", "Analiz ve planlama ajanı — model bağlantısı bekliyor.")
                        PanelCard("ENGINEER", "Kod inceleme ve geliştirme ajanı — model bağlantısı bekliyor.")
                        PanelCard("GUARD", "İzin, güvenlik ve işlem denetimi ajanı — temel onay kayıtları yerel olarak tutuluyor.")
                    }
                    "Hafıza" -> {
                        Text("Kalıcı yerel notlar (SQLite)", color = Mint, fontWeight = FontWeight.Bold)
                        EditField(memoryInput, { memoryInput = it }, "Hatırlanacak not...")
                        Button(onClick = { if (memoryInput.isNotBlank()) { db.add("memory", memoryInput); memoryInput = ""; refresh++ } }, modifier = Modifier.fillMaxWidth()) { Text("Hafızaya kaydet") }
                        Text("Kayıtlar", color = Muted)
                        db.rows("memory").forEach { PanelCard("Kayıt", it) }
                    }
                    "Dosya / Kod" -> {
                        PanelCard("Dosya erişimi", "Android sistem dosya seçicisi kullanılır. Uygulama tüm dosyalarına gizlice erişmez.")
                        Text(openedName, color = Mint, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { openFile.launch(arrayOf("text/*", "application/json", "application/xml")) }, modifier = Modifier.weight(1f)) { Text("Dosya aç") }
                            OutlinedButton(onClick = { saveFile.launch("ayratma-notes.txt") }, modifier = Modifier.weight(1f)) { Text("Yeni dosya kaydet") }
                        }
                        EditField(editorText, { editorText = it }, "Dosya içeriği / kod...", minLines = 10)
                        Text("Not: Kaydetme, seçtiğin hedefe yeni dosya yazar; orijinal dosyanın üzerine yazma sonraki güvenli sürümde eklenecek.", color = Muted, fontSize = 11.sp)
                    }
                    "Sistem İzleme" -> {
                        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        val mi = ActivityManager.MemoryInfo()
                        am.getMemoryInfo(mi)
                        val stat = StatFs(Environment.getDataDirectory().path)
                        PanelCard("Cihaz", "${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                        PanelCard("Bellek", "Toplam RAM: ${mi.totalMem / 1024 / 1024} MB\nKullanılabilir RAM: ${mi.availMem / 1024 / 1024} MB")
                        PanelCard("Depolama", "Toplam: ${stat.totalBytes / 1024 / 1024 / 1024} GB\nBoş: ${stat.availableBytes / 1024 / 1024 / 1024} GB")
                        Button(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Bilgileri yenile") }
                    }
                    "Görevler" -> {
                        EditField(taskInput, { taskInput = it }, "Yeni görev...")
                        Button(onClick = { if (taskInput.isNotBlank()) { db.add("tasks", taskInput); taskInput = ""; refresh++ } }, modifier = Modifier.fillMaxWidth()) { Text("Görev ekle") }
                        db.rows("tasks").forEach { PanelCard("Görev", it) }
                    }
                    "Onay Merkezi" -> PanelCard("İşlem onayı", "Bu sürümde henüz cihaz komutu veya arka plan eylemi çalıştırılmaz. İleride hassas işlemler burada açık onayla yürütülecek.")
                    "Evolution Engine" -> PanelCard("Kontrollü gelişim", "Şimdilik sürüm bilgisi ve yerel proje notları. Kendi kodunu değiştirme, test etme ve güncelleme otomasyonu henüz bağlı değil.")
                    "Cloud" -> PanelCard("AYRATMA Cloud", "Bulut sunucusu henüz kurulmadı. Kimlik doğrulama ve güvenli sunucu altyapısı sonraki aşamada geliştirilecek.")
                }
                Text(info, color = Mint, fontSize = 12.sp)
                Text("MADE BY AYRATMA", color = Muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PanelCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Mint, fontWeight = FontWeight.Bold)
            Text(body, color = Color(0xFFCAD3E0), fontSize = 13.sp)
        }
    }
}

@Composable
private fun EditField(value: String, onChange: (String) -> Unit, hint: String, minLines: Int = 1) {
    Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        BasicTextField(value, onChange, modifier = Modifier.fillMaxWidth().padding(12.dp).heightIn(min = (minLines * 22).dp), textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp), decorationBox = { inner ->
            if (value.isEmpty()) Text(hint, color = Muted, fontSize = 13.sp)
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
