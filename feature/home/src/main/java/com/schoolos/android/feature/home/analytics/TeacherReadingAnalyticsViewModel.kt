package com.schoolos.android.feature.home.analytics

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.domain.model.MaterialSummaryItem
import com.schoolos.android.domain.model.StudentReadingRow
import com.schoolos.android.domain.model.TeacherReadingMetrics
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.LearningMaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ReadingStatusCounts(
    val all: Int = 0,
    val completed: Int = 0,
    val inProgress: Int = 0,
    val notStarted: Int = 0,
)

data class TeacherReadingAnalyticsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val userMessage: String? = null,
    val materials: List<MaterialSummaryItem> = emptyList(),
    val allReadingRows: List<StudentReadingRow> = emptyList(),
    val rawMetrics: TeacherReadingMetrics = TeacherReadingMetrics(),
    val availableClasses: List<String> = emptyList(),
    val selectedClassFilter: String = "ALL", // "ALL" or class name
    val selectedMaterialFilter: String = "ALL", // "ALL" or materialId
    val selectedStatusFilter: String = "ALL", // "ALL" | "COMPLETED" | "IN_PROGRESS" | "NOT_STARTED"
    val searchQuery: String = "",
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
) {
    val scopedRows: List<StudentReadingRow>
        get() {
            return allReadingRows.filter { r ->
                val matchesClass = if (selectedClassFilter == "ALL" || selectedClassFilter.isBlank()) true
                else r.className.equals(selectedClassFilter, ignoreCase = true)

                val matchesMaterial = if (selectedMaterialFilter == "ALL" || selectedMaterialFilter.isBlank()) true
                else r.materialId.equals(selectedMaterialFilter, ignoreCase = true)

                matchesClass && matchesMaterial
            }
        }

    val statusCounts: ReadingStatusCounts
        get() {
            val rows = scopedRows
            var completed = 0
            var inProgress = 0
            var notStarted = 0
            for (r in rows) {
                if (r.isCompleted) {
                    completed++
                } else if (r.currentPage > 0) {
                    inProgress++
                } else {
                    notStarted++
                }
            }
            return ReadingStatusCounts(
                all = rows.size,
                completed = completed,
                inProgress = inProgress,
                notStarted = notStarted,
            )
        }

    val dynamicMetrics: TeacherReadingMetrics
        get() {
            val rows = scopedRows
            val uniqueMaterials = rows.map { it.materialId }.distinct()
            val totalMat = if (selectedMaterialFilter != "ALL") 1L
            else if (selectedClassFilter != "ALL") uniqueMaterials.size.toLong()
            else rawMetrics.totalMaterials.coerceAtLeast(materials.size.toLong())

            val totalReaders = rows.size.toLong()
            val completedReaders = rows.count { it.isCompleted }.toLong()
            val overallRate = if (totalReaders > 0) ((completedReaders * 100) / totalReaders).toInt() else 0
            val totalPages = rows.sumOf { it.currentPage.toLong() }

            return TeacherReadingMetrics(
                totalMaterials = totalMat,
                totalReaders = totalReaders,
                completedReaders = completedReaders,
                overallCompletionRate = overallRate,
                totalPagesRead = totalPages,
            )
        }

    val filteredRows: List<StudentReadingRow>
        get() {
            return scopedRows.filter { r ->
                // Status Filter
                val matchesStatus = when (selectedStatusFilter) {
                    "COMPLETED" -> r.isCompleted
                    "IN_PROGRESS" -> !r.isCompleted && r.currentPage > 0
                    "NOT_STARTED" -> !r.isCompleted && r.currentPage == 0
                    else -> true
                }
                if (!matchesStatus) return@filter false

                // Search Query
                if (searchQuery.isNotBlank()) {
                    val q = searchQuery.trim().lowercase()
                    val matchesName = r.studentName.lowercase().contains(q)
                    val matchesNisn = r.nisn.lowercase().contains(q)
                    val matchesClass = r.className.lowercase().contains(q)
                    val matchesTitle = r.materialTitle.lowercase().contains(q)
                    if (!matchesName && !matchesNisn && !matchesClass && !matchesTitle) {
                        return@filter false
                    }
                }
                true
            }
        }
}

@HiltViewModel
class TeacherReadingAnalyticsViewModel @Inject constructor(
    private val materialRepository: LearningMaterialRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherReadingAnalyticsUiState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val classesResult = academicRepository.getClasses()
                val classList = classesResult.getOrDefault(emptyList()).map { it.name }

                val summaryResult = materialRepository.getReadingSummary()
                summaryResult.onSuccess { summary ->
                    val allClasses = (classList + summary.readingRows.map { it.className } + summary.materials.map { it.className })
                        .filter { it.isNotBlank() && !it.equals("Semua Rombel", ignoreCase = true) && !it.equals("Kelas Umum", ignoreCase = true) }
                        .distinct()
                        .sorted()

                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            materials = summary.materials,
                            allReadingRows = summary.readingRows,
                            rawMetrics = summary.metrics,
                            availableClasses = allClasses,
                            lastUpdatedMillis = System.currentTimeMillis(),
                        )
                    }
                }.onFailure { e ->
                    Timber.e(e, "Failed to load teacher reading summary")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = e.toHumanReadableError("Gagal memuat analitik literasi guru. Periksa koneksi internet Anda."),
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error loading teacher reading summary")
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.toHumanReadableError("Gagal memuat analitik literasi guru. Periksa koneksi internet Anda."),
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            loadData()
        }
    }

    fun onSelectClassFilter(className: String) {
        _state.update { it.copy(selectedClassFilter = className) }
    }

    fun onSelectMaterialFilter(materialId: String) {
        _state.update { it.copy(selectedMaterialFilter = materialId) }
    }

    fun onSelectStatusFilter(status: String) {
        _state.update { it.copy(selectedStatusFilter = status) }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun clearUserMessage() {
        _state.update { it.copy(userMessage = null) }
    }

    fun exportCsv(context: Context) {
        val currentRows = _state.value.filteredRows
        if (currentRows.isEmpty()) {
            _state.update { it.copy(userMessage = "Tidak ada data pembaca untuk diekspor.") }
            return
        }

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Rekap_Literasi_Membaca_$timeStamp.csv"
            val file = File(context.cacheDir, fileName)

            val header = "No,NISN,Nama Siswa,Rombel,Materi/Buku,Halaman Dibaca,Total Halaman,Progress (%),Status,Terakhir Dibaca\n"
            val sb = StringBuilder()
            sb.append("\uFEFF") // UTF-8 BOM for Excel
            sb.append(header)

            currentRows.forEachIndexed { idx, r ->
                val status = if (r.isCompleted) "Selesai" else if (r.currentPage > 0) "Sedang Baca" else "Belum Mulai"
                val lastRead = r.lastReadAt ?: "-"
                sb.append("${idx + 1},\"${r.nisn}\",\"${r.studentName}\",\"${r.className}\",\"${r.materialTitle}\",${r.currentPage},${r.totalPages},${r.completionPercentage}%,$status,\"$lastRead\"\n")
            }

            file.writeText(sb.toString())

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Rekapitulasi Literasi Membaca Siswa")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan Rekap Literasi (CSV)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            _state.update { it.copy(userMessage = "File rekap CSV berhasil disiapkan.") }
        } catch (e: Exception) {
            Timber.e(e, "Error exporting CSV")
            _state.update { it.copy(userMessage = e.toHumanReadableError("Gagal menyiapkan berkas rekap CSV.")) }
        }
    }
}

