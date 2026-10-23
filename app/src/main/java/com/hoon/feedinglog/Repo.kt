package com.hoon.feedinglog

import android.content.Context
import android.content.SharedPreferences
import android.util.AtomicFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 기록은 앱 전용 폴더의 feeds.json에, 설정은 SharedPreferences에. 둘 다 휴대폰 안에만 있다. */
object Repo {
    private lateinit var app: Context
    private lateinit var file: AtomicFile
    private lateinit var prefs: SharedPreferences

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    /** 최신순 */
    val entries: StateFlow<List<Entry>> = _entries

    private val _options = MutableStateFlow(Options())
    val options: StateFlow<Options> = _options

    fun init(ctx: Context) {
        if (::app.isInitialized) return
        app = ctx.applicationContext
        file = AtomicFile(File(app.filesDir, "feeds.json"))
        prefs = app.getSharedPreferences("options", Context.MODE_PRIVATE)
        _entries.value = load()
        _options.value = loadOptions()
    }

    fun find(id: Long): Entry? = _entries.value.find { it.id == id }

    fun add(type: FeedType, time: Long): Entry {
        val id = (_entries.value.maxOfOrNull { it.id } ?: 0L) + 1
        val e = Entry(id, time, type, amount = null, memo = "")
        commit(_entries.value + e)
        return e
    }

    fun update(e: Entry) = commit(_entries.value.map { if (it.id == e.id) e else it })

    fun delete(id: Long) = commit(_entries.value.filterNot { it.id == id })

    private fun commit(list: List<Entry>) {
        val sorted = list.sortedByDescending { it.time }
        _entries.value = sorted
        save(sorted)
        Notif.post(app)
    }

    private fun load(): List<Entry> {
        if (!file.baseFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(String(file.readFully(), Charsets.UTF_8))
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Entry(
                    id = o.getLong("id"),
                    time = o.getLong("t"),
                    type = FeedType.valueOf(o.getString("k")),
                    amount = if (o.has("a")) o.getInt("a") else null,
                    memo = o.optString("m", ""),
                )
            }.sortedByDescending { it.time }
        } catch (e: Exception) {
            // 읽을 수 없는 파일은 덮어쓰기 전에 따로 남겨 둔다
            file.baseFile.copyTo(File(app.filesDir, "feeds-broken-${System.currentTimeMillis()}.json"), overwrite = true)
            emptyList()
        }
    }

    private fun save(list: List<Entry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("t", e.time)
                put("k", e.type.name)
                if (e.amount != null) put("a", e.amount)
                if (e.memo.isNotEmpty()) put("m", e.memo)
            })
        }
        val out = file.startWrite()
        try {
            out.write(arr.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
        }
    }

    private fun loadOptions(): Options {
        val d = Options()
        fun ints(key: String, def: List<Int>): List<Int> =
            prefs.getString(key, null)?.split(",")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 5 } ?: def
        return Options(
            show = FeedType.entries.map { prefs.getBoolean("show_${it.name}", d.show[it.ordinal]) },
            mlPresets = ints("ml", d.mlPresets),
            gPresets = ints("g", d.gPresets),
        )
    }

    fun setShow(t: FeedType, on: Boolean) {
        val o = _options.value
        val show = o.show.toMutableList().also { it[t.ordinal] = on }
        if (show.none { it }) return // 버튼이 하나도 없으면 알림이 쓸모없으니 최소 하나는 남긴다
        prefs.edit().putBoolean("show_${t.name}", on).apply()
        _options.value = o.copy(show = show)
        Notif.post(app)
    }

    fun setPreset(unit: String, index: Int, value: Int) {
        val o = _options.value
        val n = if (unit == "ml") {
            o.copy(mlPresets = o.mlPresets.toMutableList().also { it[index] = value })
        } else {
            o.copy(gPresets = o.gPresets.toMutableList().also { it[index] = value })
        }
        val list = if (unit == "ml") n.mlPresets else n.gPresets
        prefs.edit().putString(unit, list.joinToString(",")).apply()
        _options.value = n
    }
}
