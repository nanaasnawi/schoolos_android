package com.schoolos.android.core.reading

import android.content.Context
import android.content.SharedPreferences
import com.schoolos.android.domain.model.BookReadingItem
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("schoolos_reading_history", Context.MODE_PRIVATE)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    fun getReadingHistory(studentId: String): List<BookReadingItem> {
        val key = getKey(studentId)
        val rawJson = prefs.getString(key, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(rawJson)
            val list = mutableListOf<BookReadingItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    BookReadingItem(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        author = obj.optString("author").takeIf { it.isNotBlank() },
                        publisher = obj.optString("publisher").takeIf { it.isNotBlank() },
                        subjectName = obj.optString("subject_name").takeIf { it.isNotBlank() },
                        gradeLevelName = obj.optString("grade_level_name").takeIf { it.isNotBlank() },
                        coverUrl = obj.optString("cover_url").takeIf { it.isNotBlank() },
                        fileUrl = obj.optString("file_url").takeIf { it.isNotBlank() },
                        currentPage = obj.optInt("current_page", 1),
                        totalPages = obj.optInt("total_pages", 100),
                        startPage = if (obj.has("start_page") && !obj.isNull("start_page")) obj.getInt("start_page") else null,
                        endPage = if (obj.has("end_page") && !obj.isNull("end_page")) obj.getInt("end_page") else null,
                        isCompleted = obj.optBoolean("is_completed", false),
                        lastReadAt = obj.optString("last_read_at").takeIf { it.isNotBlank() },
                        materialId = obj.optString("material_id").takeIf { it.isNotBlank() },
                    )
                )
            }
            list.sortedByDescending { it.lastReadAt ?: "" }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveReadingProgress(studentId: String, item: BookReadingItem) {
        val currentList = getReadingHistory(studentId).toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == item.id || (item.fileUrl != null && it.fileUrl == item.fileUrl) }
        val updatedItem = item.copy(
            lastReadAt = dateFormat.format(Date())
        )
        if (existingIndex >= 0) {
            currentList[existingIndex] = updatedItem
        } else {
            currentList.add(0, updatedItem)
        }
        persistList(studentId, currentList)
    }

    fun markBookCompleted(studentId: String, bookId: String) {
        val currentList = getReadingHistory(studentId).toMutableList()
        val index = currentList.indexOfFirst { it.id == bookId }
        if (index >= 0) {
            val existing = currentList[index]
            currentList[index] = existing.copy(
                isCompleted = true,
                currentPage = existing.endPage ?: existing.totalPages,
                lastReadAt = dateFormat.format(Date())
            )
            persistList(studentId, currentList)
        }
    }

    private fun persistList(studentId: String, items: List<BookReadingItem>) {
        val jsonArray = JSONArray()
        items.take(20).forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("author", item.author ?: "")
                put("publisher", item.publisher ?: "")
                put("subject_name", item.subjectName ?: "")
                put("grade_level_name", item.gradeLevelName ?: "")
                put("cover_url", item.coverUrl ?: "")
                put("file_url", item.fileUrl ?: "")
                put("current_page", item.currentPage)
                put("total_pages", item.totalPages)
                if (item.startPage != null) put("start_page", item.startPage)
                if (item.endPage != null) put("end_page", item.endPage)
                put("is_completed", item.isCompleted)
                put("last_read_at", item.lastReadAt ?: "")
                put("material_id", item.materialId ?: "")
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(getKey(studentId), jsonArray.toString()).apply()
    }

    private fun getKey(studentId: String): String {
        val safeId = if (studentId.isNotBlank()) studentId else "default_student"
        return "reading_history_$safeId"
    }
}
