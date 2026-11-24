package com.schoolos.android.feature.home.analytics

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
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

    fun exportPdf(context: Context) {
        val currentRows = _state.value.filteredRows
        if (currentRows.isEmpty()) {
            _state.update { it.copy(userMessage = "Tidak ada data pembaca untuk diekspor.") }
            return
        }

        val pdf = PdfDocument()
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val printDate = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID")).format(Date())
            val fileName = "Rekap_Literasi_Membaca_$timeStamp.pdf"

            // A4 Landscape dimensions in PostScript points
            val pageWidth = 842
            val pageHeight = 595
            val margin = 32f
            val tableWidth = pageWidth - margin * 2f

            val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0284C7")
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 8.5f
            }
            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            val zebraBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 0.7f
            }
            val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 8.3f
            }

            // Column proportions summing cleanly to tableWidth (~778pt)
            val colHeaders = listOf("No", "NISN", "Nama Siswa", "Rombel", "Materi / Buku", "Hal.", "%", "Status", "Terakhir")
            val colRatios = listOf(0.04f, 0.11f, 0.21f, 0.11f, 0.25f, 0.08f, 0.05f, 0.08f, 0.07f)
            val colWidths = colRatios.map { it * tableWidth }
            val rowHeight = 20f
            val headerRowHeight = 22f

            fun fitCell(text: String, width: Float, paint: TextPaint): String {
                return TextUtils.ellipsize(text, paint, (width - 8f).coerceAtLeast(10f), TextUtils.TruncateAt.END).toString()
            }

            var rowIdx = 0
            var pageNum = 1
            val rowsPerFirstPage = 19
            val rowsPerLaterPage = 23
            val totalPages = if (currentRows.size <= rowsPerFirstPage) 1 else 1 + ((currentRows.size - rowsPerFirstPage + rowsPerLaterPage - 1) / rowsPerLaterPage)

            while (rowIdx < currentRows.size) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas
                var y = margin

                if (pageNum == 1) {
                    canvas.drawText("AKSELERASI EDU • REKAPITULASI LITERASI & MEMBACA", margin, y + 8f, brandPaint)
                    y += 16f
                    canvas.drawText("Laporan Progress Membaca Peserta Didik", margin, y + 12f, titlePaint)
                    y += 20f
                    canvas.drawText("Dicetak: $printDate • Total Rekaman: ${currentRows.size} siswa", margin, y + 8f, metaPaint)
                    y += 18f
                } else {
                    canvas.drawText("Laporan Progress Membaca Peserta Didik (Halaman $pageNum / $totalPages)", margin, y + 8f, metaPaint)
                    y += 18f
                }

                // Draw table header
                canvas.drawRect(margin, y, margin + tableWidth, y + headerRowHeight, headerBgPaint)
                var x = margin
                colHeaders.forEachIndexed { idx, label ->
                    canvas.drawText(fitCell(label, colWidths[idx], headerTextPaint), x + 4f, y + 14.5f, headerTextPaint)
                    x += colWidths[idx]
                }
                y += headerRowHeight

                val footerReserve = margin + 22f
                while (rowIdx < currentRows.size && y + rowHeight <= pageHeight - footerReserve) {
                    val r = currentRows[rowIdx]
                    if (rowIdx % 2 == 1) {
                        canvas.drawRect(margin, y, margin + tableWidth, y + rowHeight, zebraBgPaint)
                    }
                    canvas.drawRect(margin, y, margin + tableWidth, y + rowHeight, borderPaint)

                    val status = if (r.isCompleted) "Selesai" else if (r.currentPage > 0) "Baca" else "Belum"
                    val values = listOf(
                        "${rowIdx + 1}",
                        r.nisn.ifBlank { "-" },
                        r.studentName,
                        r.className,
                        r.materialTitle,
                        "${r.currentPage}/${r.totalPages}",
                        "${r.completionPercentage}%",
                        status,
                        r.lastReadAt ?: "-"
                    )

                    var cellX = margin
                    values.forEachIndexed { cIdx, v ->
                        canvas.drawText(fitCell(v, colWidths[cIdx], cellPaint), cellX + 4f, y + 13.5f, cellPaint)
                        if (cIdx > 0) {
                            canvas.drawLine(cellX, y, cellX, y + rowHeight, borderPaint)
                        }
                        cellX += colWidths[cIdx]
                    }
                    y += rowHeight
                    rowIdx++
                }

                canvas.drawText("Halaman $pageNum dari $totalPages • Dokumen PDF Resmi Akselerasi Edu", margin, pageHeight - 18f, metaPaint)
                pdf.finishPage(page)
                pageNum++
            }

            val outStream = ByteArrayOutputStream()
            pdf.writeTo(outStream)
            val bytes = outStream.toByteArray()

            val targetUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Gagal membuat berkas PDF di folder Downloads.")
                resolver.openOutputStream(uri)?.use { it.write(bytes); it.flush() }
                    ?: throw IllegalStateException("Gagal menulis isi PDF.")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                val file = File(context.cacheDir, fileName)
                FileOutputStream(file).use { it.write(bytes); it.flush() }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

            targetUri?.let { uri ->
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Rekapitulasi Literasi Membaca Siswa")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { context.startActivity(viewIntent) }.onFailure {
                    context.startActivity(
                        Intent.createChooser(shareIntent, "Buka / Bagikan Rekap Literasi (PDF)").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            }

            _state.update { it.copy(userMessage = "Dokumen PDF berhasil disimpan: $fileName") }
        } catch (e: Exception) {
            Timber.e(e, "Error exporting PDF")
            _state.update { it.copy(userMessage = e.toHumanReadableError("Gagal menyiapkan dokumen PDF.")) }
        } finally {
            pdf.close()
        }
    }
}

