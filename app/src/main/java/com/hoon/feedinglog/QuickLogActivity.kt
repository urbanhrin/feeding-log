package com.hoon.feedinglog

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 알림 버튼을 누르면 뜬다. 뜨자마자 지금 시각으로 기록하고, 양은 골라도 되고 안 골라도 된다.
 * 5초 동안 손대지 않으면 양 없이 닫힌다.
 */
class QuickLogActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TYPE = "type"
    }

    private var entryId by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true)
        else window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        val dark = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = dark, navigationBarStyle = dark)

        // 화면이 다시 만들어질 때(회전 등) 두 번 기록하지 않도록
        entryId = savedInstanceState?.getLong("id") ?: record(intent)
        if (entryId == 0L) {
            finish()
            return
        }
        setContent {
            FeedTheme {
                key(entryId) { QuickSheet(entryId, onDone = { finish() }) }
            }
        }
    }

    // 창이 떠 있는 동안 알림 버튼을 또 누른 경우
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val id = record(intent)
        if (id != 0L) entryId = id
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("id", entryId)
    }

    private fun record(i: Intent): Long {
        val t = FeedType.entries.getOrNull(i.getIntExtra(EXTRA_TYPE, -1)) ?: return 0L
        return Repo.add(t, System.currentTimeMillis()).id
    }
}

@Composable
private fun QuickSheet(id: Long, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val entries by Repo.entries.collectAsState()
    val options by Repo.options.collectAsState()
    val entry = entries.find { it.id == id }
    if (entry == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }

    var amount by remember { mutableStateOf(entry.amount) }
    var touched by remember { mutableStateOf(false) }
    var left by remember { mutableIntStateOf(5) }

    LaunchedEffect(touched) {
        if (touched) return@LaunchedEffect
        while (left > 0) {
            delay(1000)
            left--
        }
        onDone()
    }

    BottomSheetFrame(
        onDismiss = onDone,
        // 어디든 한 번 누르면 자동 닫힘을 멈춘다
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                touched = true
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF2B2620)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = C.accent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${entry.type.label} 기록됨", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = C.text)
                Text(whenLabel(entry.time), fontSize = 14.sp, color = C.muted)
            }
            TextLink("시간 수정") {
                pickTime(ctx, entry.time) { t -> Repo.find(id)?.let { Repo.update(it.copy(time = t)) } }
            }
        }

        AmountEditor(entry.type, amount, options.presets(entry.type)) { amount = it }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetButton("건너뛰기", primary = false, modifier = Modifier.weight(1f), onClick = onDone)
            SheetButton("저장", primary = true, modifier = Modifier.weight(1f)) {
                Repo.find(id)?.let { Repo.update(it.copy(amount = amount)) }
                onDone()
            }
        }

        Text(
            if (touched) "양은 나중에 기록 화면에서도 넣을 수 있어요" else "${left}초 뒤 양 없이 저장돼요",
            fontSize = 12.sp,
            color = C.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
