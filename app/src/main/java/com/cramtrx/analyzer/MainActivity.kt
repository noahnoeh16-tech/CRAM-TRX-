package com.cramtrx.analyzer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.cramtrx.analyzer.data.AppDatabase
import com.cramtrx.analyzer.data.RoundEntity
import com.cramtrx.analyzer.engine.CramAnalysisEngine
import com.cramtrx.analyzer.engine.AnalysisReport
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val DarkBg = Color(0xFF09090C)
val SurfaceBg = Color(0xFF14141A)
val CardBg = Color(0xFF1A1A22)
val NeonRed = Color(0xFFFF0F30)
val DarkRed = Color(0xFF9E0013)
val MutedText = Color(0xFF757585)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)

        setContent {
            var currentTab by remember { mutableIntStateOf(0) }
            val rounds by db.roundDao().getAllRoundsFlow().collectAsState(initial = emptyList())
            val report = remember(rounds) {
                CramAnalysisEngine.compute(rounds.sortedBy { it.timestampMillis })
            }

            Scaffold(
                bottomBar = {
                    NavigationBar(containerColor = SurfaceBg) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = { Icon(Icons.Default.PostAdd, contentDescription = null) },
                            label = { Text("Saisie") }
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                            label = { Text("CRAM 1-4") }
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = { Icon(Icons.Default.History, contentDescription = null) },
                            label = { Text("Historique") }
                        )
                    }
                }
            ) { paddingVals ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBg)
                        .padding(paddingVals)
                        .padding(16.dp)
                ) {
                    when (currentTab) {
                        0 -> InputView { timeStr, multiplier ->
                            lifecycleScope.launch {
                                val now = Calendar.getInstance()
                                val parts = timeStr.split(":")
                                now.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                                now.set(Calendar.MINUTE, parts[1].toInt())
                                now.set(Calendar.SECOND, parts[2].toInt())
                                val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                                db.roundDao().insertRound(
                                    RoundEntity(
                                        timestampMillis = now.timeInMillis,
                                        roundTimeStr = timeStr,
                                        multiplier = multiplier,
                                        observedDateStr = dStr
                                    )
                                )
                            }
                        }
                        1 -> AnalysisView(report)
                        2 -> HistoryView(rounds) { r ->
                            lifecycleScope.launch { db.roundDao().deleteRound(r) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InputView(onSave: (String, Double) -> Unit) {
    val ctx = LocalContext.current
    var timeInput by remember {
        mutableStateOf(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
    }
    var multInput by remember { mutableStateOf("") }

    Column {
        Text("SAISIE DU ROUND AVIATOR", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("Enregistrez les crashs constates", color = MutedText, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = timeInput,
            onValueChange = { timeInput = it },
            label = { Text("Heure (HH:MM:SS)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = multInput,
            onValueChange = { multInput = it },
            label = { Text("Multiplicateur (ex: 2.45)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val parsed = multInput.replace(",", ".").toDoubleOrNull()
                if (parsed != null && timeInput.split(":").size == 3) {
                    onSave(timeInput, parsed)
                    multInput = ""
                    timeInput = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    Toast.makeText(ctx, "Round enregistre !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(ctx, "Entrez une heure (HH:MM:SS) et une cote valides.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("ENREGISTRER LE ROUND 🔥", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun AnalysisView(report: AnalysisReport) {
    if (!report.isSufficientData) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceBg),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("DONNEES EN ATTENTE", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(report.statusMessage, color = Color.LightGray, fontSize = 13.sp)
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(report.statusMessage, color = Color(0xFF00E676), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }
            items(report.estimations) { est ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(1.dp, NeonRed, RoundedCornerShape(10.dp))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(est.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                            Text("INDICE : " + est.confidenceRate, color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Heure estimee :", color = MutedText, fontSize = 12.sp)
                        Text(
                            text = est.estimatedTime + if (est.isNextDay) " (J+1)" else "",
                            color = NeonRed,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("Methode : " + est.methodFormula, color = Color.LightGray, fontSize = 11.sp)
                        Text("Estimation statistique non deterministe.", color = MutedText, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryView(rounds: List<RoundEntity>, onDelete: (RoundEntity) -> Unit) {
    if (rounds.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucun round enregistre.", color = MutedText)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(rounds) { r ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(r.roundTimeStr + "  (" + r.observedDateStr + ")", color = Color.White, fontSize = 13.sp)
                            Text("Crash : " + r.multiplier + "x", color = if (r.multiplier >= 2.0) NeonRed else Color.LightGray, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { onDelete(r) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MutedText)
                        }
                    }
                }
            }
        }
    }
}
