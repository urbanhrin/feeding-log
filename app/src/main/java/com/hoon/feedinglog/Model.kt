package com.hoon.feedinglog

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class FeedType(val label: String, val unit: String, val color: Color) {
    FORMULA("분유", "ml", Color(0xFFB8915E)),
    WEAN("이유식", "g", Color(0xFF7F9479)),
    TODDLER("유아식", "g", Color(0xFFC49A8E)),
}

/** 수유 한 번. 시작 시각만 기록하고, 양은 없을 수 있다. */
data class Entry(
    val id: Long,
    val time: Long,
    val type: FeedType,
    val amount: Int?,
    val memo: String,
) {
    fun describe(): String = if (amount == null) type.label else "${type.label} $amount${type.unit}"
}

data class Options(
    /** FeedType 순서대로, 알림에 버튼을 보일지 */
    val show: List<Boolean> = listOf(true, true, false),
    val mlPresets: List<Int> = listOf(120, 150, 180, 210, 240),
    val gPresets: List<Int> = listOf(40, 60, 80, 100, 120),
) {
    fun presets(t: FeedType): List<Int> = if (t.unit == "ml") mlPresets else gPresets
}

private val HM = DateTimeFormatter.ofPattern("HH:mm")

fun zone(): ZoneId = ZoneId.systemDefault()

fun hm(t: Long): String = Instant.ofEpochMilli(t).atZone(zone()).format(HM)

fun dateOf(t: Long): LocalDate = Instant.ofEpochMilli(t).atZone(zone()).toLocalDate()

fun dayLabel(d: LocalDate): String {
    val today = LocalDate.now()
    return when (d) {
        today -> "오늘"
        today.minusDays(1) -> "어제"
        else -> "${d.monthValue}월 ${d.dayOfMonth}일"
    }
}

fun whenLabel(t: Long): String = "${dayLabel(dateOf(t))} ${hm(t)}"
