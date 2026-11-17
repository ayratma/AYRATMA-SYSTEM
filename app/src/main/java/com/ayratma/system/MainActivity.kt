package com.ayratma.system

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AyratmaApp() }
    }
}

private val Night = Color(0xFF080B12)
private val Panel = Color(0xFF121927)
private val Accent = Color(0xFF69D5C5)
private val Muted = Color(0xFF9AA8BC)

@Composable
private fun AyratmaApp() {
    val selected = remember { mutableStateOf("Genel Bakış") }
    val sections = listOf(
        "Genel Bakış", "CORE AI", "Uzman Ajanlar", "Hafıza",
        "Dosya Sistemi", "Kod Motoru", "Web Intelligence", "Sistem İzleme"
    )
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Night,
            surface = Panel,
            primary = Accent,
            onPrimary = Night,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Night) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("AYRATMA", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("SYSTEM", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("v0.1.0", color = Muted, fontSize = 12.sp)
                }

                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CORE STATUS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Başlangıç arayüzü hazır", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Uygulama kabuğu çalışıyor. AI bağlantısı ve cihaz modülleri sonraki aşamada etkinleştirilecek.", color = Muted, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("● ", color = Accent)
                            Text("Yerel arayüz aktif", color = Accent, fontSize = 12.sp)
                        }
                    }
                }

                Text("MODÜLLER", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                sections.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (selected.value == item) Color(0xFF1B3038) else Panel),
                        shape = RoundedCornerShape(16.dp),
                        onClick = { selected.value = item }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(if (item == "Genel Bakış") "●" else "›", color = Accent, fontSize = 20.sp)
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(selected.value, color = Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            when (selected.value) {
                                "CORE AI" -> "Ana yapay zekâ merkezi. Model bağlantısı henüz kurulmadı."
                                "Uzman Ajanlar" -> "ANALYST, ENGINEER ve GUARD ajanları için hazırlık alanı."
                                "Hafıza" -> "Yerel SQLite hafıza altyapısı sonraki sürümde bağlanacak."
                                "Dosya Sistemi" -> "SAF tabanlı kullanıcı tarafından seçilen dosya erişimi planlandı."
                                "Kod Motoru" -> "Kod projeleri için güvenli çalışma alanı hazırlanacak."
                                "Web Intelligence" -> "Web araştırma bağlantısı henüz etkin değil."
                                "Sistem İzleme" -> "Android'in izin verdiği sistem bilgileri gösterilecek."
                                else -> "AYRATMA SYSTEM kontrol paneline hoş geldin."
                            },
                            color = Muted, fontSize = 13.sp
                        )
                        OutlinedButton(onClick = { selected.value = "Genel Bakış" }) {
                            Text("Ana panele dön")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("MADE BY AYRATMA", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
