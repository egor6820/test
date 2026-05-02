package com.example.tossday.data.repository

import com.example.tossday.data.local.QuickNoteDao
import com.example.tossday.data.local.TaskDao
import com.example.tossday.data.local.entities.QuickNoteEntity
import com.example.tossday.data.local.entities.TaskEntity
import com.example.tossday.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Резервне копіювання у JSON-файл. Формат містить schemaVersion, тому майбутні зміни
 * структури БД не зламають можливість прочитати старі бекапи.
 *
 * Стратегія імпорту: спершу повна валідація JSON (parse + перевірка типів) — якщо файл
 * пошкоджений або несумісний, повертаємо ImportResult.Error БЕЗ зміни існуючих даних.
 * Лише після успішної валідації виконуємо атомарну заміну: wipe → insert.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val quickNoteDao: QuickNoteDao,
    private val quickNoteRepository: QuickNoteRepository,
    private val settingsRepository: SettingsRepository
) {

    /** Експортує всі дані в JSON-рядок. Не блокує UI — виконує читання на IO-потоці. */
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val tasks = taskDao.getAll()
        val noteText = quickNoteRepository.getNote().first()

        val tasksArr = JSONArray()
        for (t in tasks) {
            tasksArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("text", t.text)
                    put("date", t.date ?: JSONObject.NULL)
                    put("time", t.time ?: JSONObject.NULL)
                    put("durationMinutes", t.durationMinutes ?: JSONObject.NULL)
                    put("status", t.status)
                    put("energy", t.energy)
                    put("itemOrder", t.itemOrder)
                    put("createdAt", t.createdAt)
                }
            )
        }

        val settingsObj = JSONObject().apply {
            put("theme", settingsRepository.appTheme.value.name)
            put("haptic", settingsRepository.isHapticEnabled.value)
            put("noteBackground", settingsRepository.noteBackground.value.name)
            put("pinnedDate", settingsRepository.pinnedDate.value?.toString() ?: JSONObject.NULL)
        }

        JSONObject().apply {
            put("schemaVersion", SCHEMA_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("tasks", tasksArr)
            put("quickNote", noteText)
            put("settings", settingsObj)
        }.toString(2)
    }

    /**
     * Імпортує дані з JSON. ВАЖЛИВО: викликач має скасувати alarms ПЕРЕД викликом
     * (бо id поточних завдань зникнуть) і запланувати нові ПІСЛЯ — повертаємо список
     * завдань-з-часом, які треба пере-schedulити.
     */
    suspend fun importFromJson(json: String): ImportResult = withContext(Dispatchers.IO) {
        // Етап 1 — валідація. Парсимо все в проміжні структури БЕЗ зміни існуючих даних.
        val parsed = try {
            parseAndValidate(json)
        } catch (e: ImportValidationException) {
            return@withContext ImportResult.Error(e.message ?: "Невалідний файл")
        } catch (e: JSONException) {
            return@withContext ImportResult.Error("Файл не схожий на JSON: ${e.message}")
        }

        // Етап 2 — атомарна заміна. Тут вже валідовані дані, тому можна wipe + insert.
        try {
            taskDao.deleteAll()
            quickNoteDao.clearNote()

            if (parsed.tasks.isNotEmpty()) {
                taskDao.insertAll(parsed.tasks)
            }
            if (parsed.quickNote.isNotEmpty()) {
                quickNoteDao.upsert(
                    QuickNoteEntity(
                        id = 1,
                        text = parsed.quickNote,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            // Налаштування — пишемо через SettingsRepository (там SharedPreferences + StateFlow).
            settingsRepository.setAppTheme(parsed.theme)
            settingsRepository.setHapticEnabled(parsed.haptic)
            settingsRepository.setNoteBackground(parsed.noteBackground)
            settingsRepository.setPinnedDate(parsed.pinnedDate)

            ImportResult.Success(
                taskCount = parsed.tasks.size,
                tasksWithAlarms = parsed.tasks.filter { it.time != null && it.date != null }
            )
        } catch (e: Exception) {
            ImportResult.Error("Помилка запису: ${e.message}")
        }
    }

    private fun parseAndValidate(json: String): ParsedBackup {
        val root = JSONObject(json)
        val schema = root.optInt("schemaVersion", -1)
        if (schema == -1) throw ImportValidationException("Файл не містить schemaVersion")
        if (schema > SCHEMA_VERSION) {
            throw ImportValidationException("Файл створено новішою версією застосунку (v$schema)")
        }

        val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
        val tasks = mutableListOf<TaskEntity>()
        for (i in 0 until tasksArr.length()) {
            val o = tasksArr.getJSONObject(i)
            val text = o.optString("text", "").trim()
            if (text.isEmpty()) continue // пропускаємо порожні записи

            val statusStr = o.optString("status", "TODO")
            // Валідуємо enum-и — якщо невідоме значення, fallback на дефолти, не падаємо.
            val status = runCatching { com.example.tossday.domain.model.Status.valueOf(statusStr) }
                .getOrDefault(com.example.tossday.domain.model.Status.TODO).name
            val energyStr = o.optString("energy", "MEDIUM")
            val energy = runCatching { com.example.tossday.domain.model.EnergyLevel.valueOf(energyStr) }
                .getOrDefault(com.example.tossday.domain.model.EnergyLevel.MEDIUM).name

            tasks.add(
                TaskEntity(
                    id = o.optLong("id", 0L),
                    text = text,
                    date = o.optStringOrNull("date")?.takeIf { runCatching { LocalDate.parse(it) }.isSuccess },
                    time = o.optStringOrNull("time")?.takeIf {
                        runCatching { java.time.LocalTime.parse(it) }.isSuccess
                    },
                    durationMinutes = if (o.isNull("durationMinutes")) null else o.optInt("durationMinutes").let { if (it == 0) null else it },
                    status = status,
                    energy = energy,
                    itemOrder = o.optInt("itemOrder", 0),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val quickNote = root.optString("quickNote", "")

        val settingsObj = root.optJSONObject("settings") ?: JSONObject()
        val theme = runCatching { AppTheme.valueOf(settingsObj.optString("theme", "OCEAN")) }
            .getOrDefault(AppTheme.OCEAN)
        val haptic = settingsObj.optBoolean("haptic", true)
        val noteBackground = runCatching {
            NoteBackground.valueOf(settingsObj.optString("noteBackground", "NONE"))
        }.getOrDefault(NoteBackground.NONE)
        val pinnedDate = settingsObj.optStringOrNull("pinnedDate")
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        return ParsedBackup(
            tasks = tasks,
            quickNote = quickNote,
            theme = theme,
            haptic = haptic,
            noteBackground = noteBackground,
            pinnedDate = pinnedDate
        )
    }

    private data class ParsedBackup(
        val tasks: List<TaskEntity>,
        val quickNote: String,
        val theme: AppTheme,
        val haptic: Boolean,
        val noteBackground: NoteBackground,
        val pinnedDate: LocalDate?
    )

    private class ImportValidationException(message: String) : RuntimeException(message)

    companion object {
        const val SCHEMA_VERSION = 1
    }
}

sealed class ImportResult {
    data class Success(
        val taskCount: Int,
        val tasksWithAlarms: List<TaskEntity>
    ) : ImportResult()

    data class Error(val reason: String) : ImportResult()
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }
