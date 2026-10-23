package com.hoon.feedinglog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

// ───────────── 기록 화면 ─────────────

@Composable
fun LogScreen(onSettings: () -> Unit) {
    val entries by Repo.entries.collectAsState()
    val options by Repo.options.collectAsState()
    var dayOffset by rememberSaveable { mutableIntStateOf(0) } // 0 = 오늘
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val day = LocalDate.now().minusDays(dayOffset.toLong())
    val list = entries.filter { dateOf(it.time) == day }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    dayLabel(day),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = C.text,
                    modifier = Modifier.weight(1f),
                )
                IconBtn(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "이전 날") { dayOffset++ }
                IconBtn(Icons.AutoMirrored.Filled.KeyboardArrowRight, "다음 날", enabled = dayOffset > 0) { dayOffset-- }
                IconBtn(Icons.Filled.Settings, "설정", onClick = onSettings)
            }
            Text(
                summary(day, list),
                fontSize = 14.sp,
                color = C.muted,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(list, key = { it.id }) { e -> EntryRow(e) { editingId = e.id } }
            }
            Text(
                "항목을 누르면 메모 · 양 · 시간 수정",
                fontSize = 12.sp,
                color = C.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 12.dp),
            )
        }

        val editing = editingId?.let { id -> entries.find { it.id == id } }
        if (editing != null) EditSheet(editing, options, onClose = { editingId = null })
    }
}

private fun summary(d: LocalDate, list: List<Entry>): String {
    val parts = mutableListOf("${d.monthValue}월 ${d.dayOfMonth}일")
    if (list.isEmpty()) {
        parts += "기록 없음"
    } else {
        parts += "${list.size}회"
        FeedType.entries.forEach { t ->
            val sum = list.filter { it.type == t }.sumOf { it.amount ?: 0 }
            if (sum > 0) parts += "${t.label} $sum${t.unit}"
        }
    }
    return parts.joinToString(" · ")
}

@Composable
private fun EntryRow(e: Entry, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(hm(e.time), fontSize = 17.sp, color = C.text, modifier = Modifier.width(52.dp))
            Box(Modifier.padding(top = 8.dp)) { Dot(e.type) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(e.type.label, fontSize = 16.sp, color = C.text2)
                if (e.memo.isNotEmpty()) Text(e.memo, fontSize = 13.sp, color = C.soft)
            }
            // 양을 안 넣었으면 비워 둔다
            if (e.amount != null) Text("${e.amount}${e.type.unit}", fontSize = 16.sp, color = C.text)
        }
        HorizontalDivider(color = C.line)
    }
}

/** 기록 하나 고치기. 닫을 때(완료·바깥 누름·뒤로) 고친 내용을 저장한다. */
@Composable
private fun EditSheet(e: Entry, options: Options, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var amount by remember(e.id) { mutableStateOf(e.amount) }
    var memo by remember(e.id) { mutableStateOf(e.memo) }
    var confirmDelete by remember(e.id) { mutableStateOf(false) }

    fun save() {
        val cur = Repo.find(e.id)
        if (cur != null && (cur.amount != amount || cur.memo != memo.trim())) {
            Repo.update(cur.copy(amount = amount, memo = memo.trim()))
        }
        onClose()
    }

    BackHandler { save() }
    BottomSheetFrame(onDismiss = { save() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${whenLabel(e.time)} ${e.type.label}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = C.text,
                modifier = Modifier.weight(1f),
            )
            TextLink("시간 수정") {
                pickTime(ctx, e.time) { t -> Repo.find(e.id)?.let { Repo.update(it.copy(time = t)) } }
            }
        }

        AmountEditor(e.type, amount, options.presets(e.type)) { amount = it }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("메모", fontSize = 13.sp, color = C.muted)
            val shape = RoundedCornerShape(14.dp)
            BasicTextField(
                value = memo,
                onValueChange = { memo = it },
                textStyle = TextStyle(color = C.text, fontSize = 15.sp),
                cursorBrush = SolidColor(C.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .clip(shape)
                    .background(C.field)
                    .border(1.dp, C.border, shape)
                    .padding(12.dp),
                decorationBox = { inner ->
                    Box {
                        if (memo.isEmpty()) Text("예) 반쯤 먹고 잠듦", fontSize = 15.sp, color = C.faint)
                        inner()
                    }
                },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetButton(
                if (confirmDelete) "한 번 더 누르면 삭제" else "삭제",
                primary = false,
                modifier = Modifier.weight(1f),
            ) {
                if (confirmDelete) {
                    Repo.delete(e.id)
                    onClose()
                } else {
                    confirmDelete = true
                }
            }
            SheetButton("완료", primary = true, modifier = Modifier.weight(1f)) { save() }
        }
    }
}

// ───────────── 설정 화면 ─────────────

@Composable
fun SettingsScreen(notifOk: Boolean, onBack: () -> Unit, onFixNotif: () -> Unit) {
    val options by Repo.options.collectAsState()
    val card = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.surface)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", onClick = onBack)
            Text("설정", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = C.text)
        }

        if (!notifOk) {
            Column(card.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("알림이 꺼져 있어요", fontSize = 16.sp, color = C.text)
                Text("잠금화면 버튼을 쓰려면 이 앱의 알림을 허용해 주세요", fontSize = 13.sp, color = C.muted)
                SheetButton("알림 설정 열기", primary = true, modifier = Modifier.fillMaxWidth(), onClick = onFixNotif)
            }
        }

        Section("알림에 보일 버튼") {
            Column(card.padding(horizontal = 16.dp, vertical = 4.dp)) {
                FeedType.entries.forEach { t ->
                    val on = options.show[t.ordinal]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .toggleable(value = on, role = Role.Switch) { Repo.setShow(t, it) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Dot(t)
                        Text(t.label, fontSize = 16.sp, color = C.text, modifier = Modifier.weight(1f))
                        Switch(
                            checked = on,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = C.selText,
                                checkedTrackColor = Color(0xFF8A6E48),
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = C.faint,
                                uncheckedTrackColor = C.border,
                                uncheckedBorderColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
            Text("최소 하나는 켜져 있어야 해요", fontSize = 12.sp, color = C.faint)
        }

        Section("분유 빠른 선택 (ml)") { PresetRow("ml", options.mlPresets) }
        Section("이유식 · 유아식 빠른 선택 (g)") { PresetRow("g", options.gPresets) }

        Column(card.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("기록은 이 폰에만 저장돼요", fontSize = 15.sp, color = C.text)
            Text(
                "잠금화면에서 알림 버튼이 안 보이면, 휴대폰 설정에서 잠금화면 알림을 아이콘만이 아니라 ‘상세히’ 보이도록 바꿔 주세요.",
                fontSize = 13.sp,
                color = C.muted,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 13.sp, color = C.muted)
        content()
    }
}

@Composable
private fun PresetRow(unit: String, values: List<Int>) {
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEachIndexed { i, v ->
            var text by remember(unit, i) { mutableStateOf(v.toString()) }
            BasicTextField(
                value = text,
                onValueChange = { s ->
                    val digits = s.filter(Char::isDigit).take(3)
                    text = digits
                    digits.toIntOrNull()?.takeIf { it > 0 }?.let { Repo.setPreset(unit, i, it) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(color = C.text, fontSize = 15.sp, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(C.accent),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(shape)
                    .background(C.surface)
                    .border(1.dp, C.border, shape),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() }
                },
            )
        }
    }
}
