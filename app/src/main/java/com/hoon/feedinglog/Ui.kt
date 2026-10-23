package com.hoon.feedinglog

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant

/** 밤에 봐도 눈부시지 않게: 거의 검정 바탕, 따뜻한 회백색 글씨, 채도 낮은 호박색 하나 */
object C {
    val bg = Color(0xFF0E1012)
    val surface = Color(0xFF1A1D21)
    val surface2 = Color(0xFF24272C)
    val field = Color(0xFF121417)
    val line = Color(0xFF1F2226)
    val border = Color(0xFF2E3237)
    val text = Color(0xFFD3CEC4)
    val text2 = Color(0xFFC9C4BA)
    val soft = Color(0xFFA9A49A)
    val muted = Color(0xFF8A867E)
    val faint = Color(0xFF6F6B64)
    val accent = Color(0xFFB8915E)
    val onAccent = Color(0xFF17140F)
    val selBg = Color(0xFF3A3125)
    val selText = Color(0xFFE0C9A6)
    val scrim = Color(0xB8040506)
}

@Composable
fun FeedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.accent,
            onPrimary = C.onAccent,
            background = C.bg,
            onBackground = C.text,
            surface = C.surface,
            onSurface = C.text,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides C.text, content = content)
    }
}

@Composable
fun Dot(t: FeedType) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(t.color))
}

@Composable
fun IconBtn(icon: ImageVector, desc: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = desc, tint = if (enabled) C.muted else C.faint.copy(alpha = 0.4f))
    }
}

@Composable
fun TextLink(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 14.sp, color = C.soft)
    }
}

@Composable
fun SheetButton(label: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(54.dp)
            .clip(shape)
            .then(if (primary) Modifier.background(C.accent) else Modifier.border(1.dp, Color(0xFF33373D), shape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.Normal,
            color = if (primary) C.onAccent else C.soft,
        )
    }
}

@Composable
private fun RoundButton(label: String, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(C.surface2)
            .clickable(onClick = onClick)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 26.sp, color = C.text2)
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .height(44.dp)
            .clip(shape)
            .then(if (selected) Modifier.background(C.selBg) else Modifier.border(1.dp, C.border, shape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 15.sp, color = if (selected) C.selText else C.soft)
    }
}

/** 양 입력: − / + 는 10씩, 아래 칩 5개는 설정에서 바꾼 값. 같은 칩을 다시 누르면 비운다. */
@Composable
fun AmountEditor(type: FeedType, amount: Int?, presets: List<Int>, onChange: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row {
            Text("양", fontSize = 13.sp, color = C.muted)
            Text(" · 선택", fontSize = 13.sp, color = C.faint)
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RoundButton("−", "10${type.unit} 줄이기") {
                val n = (amount ?: 0) - 10
                onChange(if (n <= 0) null else n)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(amount?.toString() ?: "—", fontSize = 48.sp, fontWeight = FontWeight.Light, color = C.text)
                Spacer(Modifier.width(6.dp))
                Text(type.unit, fontSize = 18.sp, color = C.muted, modifier = Modifier.padding(bottom = 12.dp))
            }
            RoundButton("+", "10${type.unit} 늘리기") { onChange((amount ?: 0) + 10) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { v ->
                Chip(v.toString(), selected = amount == v, modifier = Modifier.weight(1f)) {
                    onChange(if (amount == v) null else v)
                }
            }
        }
    }
}

/** 화면 아래에서 올라오는 창. 바깥(어두운 부분)을 누르면 onDismiss. */
@Composable
fun BottomSheetFrame(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(C.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(C.surface)
                // 창 안을 누른 건 바깥 누름으로 치지 않는다
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            content = content,
        )
    }
}

/** 시각만 고른다. 날짜는 그대로 두되, 고른 시각이 미래면 하루 전으로 본다(자정 넘어 고칠 때). */
fun pickTime(ctx: Context, current: Long, onPicked: (Long) -> Unit) {
    val z = Instant.ofEpochMilli(current).atZone(zone())
    TimePickerDialog(
        ctx,
        R.style.Theme_Feed_Dialog,
        { _, h, m ->
            var t = z.withHour(h).withMinute(m).withSecond(0).withNano(0)
            if (t.toInstant().toEpochMilli() > System.currentTimeMillis() + 60_000) t = t.minusDays(1)
            onPicked(t.toInstant().toEpochMilli())
        },
        z.hour,
        z.minute,
        true,
    ).show()
}
