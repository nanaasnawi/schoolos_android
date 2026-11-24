package com.schoolos.android.feature.home

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.auth.isPkbmInstitution
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.domain.model.GtkSupervisionOverview
import com.schoolos.android.domain.model.StaffMember
import com.schoolos.android.domain.model.TeacherProductivityTier
import com.schoolos.android.domain.model.TeacherSupervisionProfile
import com.schoolos.android.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

data class TeacherStaffSupervisionUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val schoolName: String = "",
    val isPkbm: Boolean = false,
    val overview: GtkSupervisionOverview = GtkSupervisionOverview(),
    val selectedTab: Int = 0, // 0 = Guru/Tutor, 1 = Tenaga Kependidikan
    val searchQuery: String = "",
    val selectedTierFilter: String = "ALL", // ALL, SANGAT_PRODUKTIF, AKTIF, CUKUP, PERLU_PEMBINAAN
    val selectedSortMode: String = "SCORE_DESC", // SCORE_DESC, SCORE_ASC, NAME_ASC
    val selectedStaffFilter: String = "ALL", // ALL, ACTIVE, INACTIVE
    val selectedStaffMember: StaffMember? = null,
) {
    val filteredTeachers: List<TeacherSupervisionProfile>
        get() {
            val q = searchQuery.trim().lowercase()
            val filtered = overview.teachers.filter { item ->
                val matchesSearch = q.isBlank() ||
                    item.teacher.fullName.lowercase().contains(q) ||
                    (item.teacher.nip?.lowercase()?.contains(q) == true) ||
                    (item.teacher.nuptk?.lowercase()?.contains(q) == true) ||
                    item.displaySubject.lowercase().contains(q) ||
                    (item.homeroomClassName?.lowercase()?.contains(q) == true) ||
                    (item.teacher.statusKepegawaian?.lowercase()?.contains(q) == true)

                val matchesTier = when (selectedTierFilter) {
                    "SANGAT_PRODUKTIF" -> item.tier == TeacherProductivityTier.SANGAT_PRODUKTIF
                    "AKTIF" -> item.tier == TeacherProductivityTier.AKTIF
                    "CUKUP" -> item.tier == TeacherProductivityTier.CUKUP
                    "PERLU_PEMBINAAN" -> item.tier == TeacherProductivityTier.PERLU_PEMBINAAN
                    else -> true
                }
                matchesSearch && matchesTier
            }

            return when (selectedSortMode) {
                "SCORE_ASC" -> filtered.sortedWith(
                    compareBy<TeacherSupervisionProfile> { it.performanceScore }
                        .thenBy { it.totalWorksCount }
                        .thenBy { it.teacher.fullName.lowercase() }
                )
                "NAME_ASC" -> filtered.sortedBy { it.teacher.fullName.lowercase() }
                else -> filtered.sortedWith(
                    compareByDescending<TeacherSupervisionProfile> { it.isActive }
                        .thenByDescending { it.performanceScore }
                        .thenByDescending { it.totalWorksCount }
                        .thenBy { it.teacher.fullName.lowercase() }
                )
            }
        }

    val filteredStaff: List<StaffMember>
        get() {
            val q = searchQuery.trim().lowercase()
            return overview.staff.filter { member ->
                val matchesSearch = q.isBlank() ||
                    member.fullName.lowercase().contains(q) ||
                    member.jobTitle.lowercase().contains(q) ||
                    (member.jenisPtk?.lowercase()?.contains(q) == true) ||
                    (member.nuptk?.lowercase()?.contains(q) == true) ||
                    (member.nip?.lowercase()?.contains(q) == true) ||
                    (member.statusKepegawaian?.lowercase()?.contains(q) == true)

                val matchesStatus = when (selectedStaffFilter) {
                    "ACTIVE" -> member.isActive
                    "INACTIVE" -> !member.isActive
                    else -> true
                }
                matchesSearch && matchesStatus
            }
        }
}

@HiltViewModel
class TeacherStaffSupervisionViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherStaffSupervisionUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load(isPullRefresh: Boolean = false) {
        viewModelScope.launch {
            val auth = authManager.authState.first()
            val schoolName = auth.schoolName.orEmpty()
            _state.update {
                it.copy(
                    isLoading = !isPullRefresh && it.overview.teachers.isEmpty() && it.overview.staff.isEmpty(),
                    isRefreshing = isPullRefresh,
                    errorMessage = null,
                    schoolName = schoolName,
                    isPkbm = isPkbmInstitution(schoolName),
                )
            }

            analyticsRepository.getGtkSupervisionOverview()
                .onSuccess { overview ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            overview = overview,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { err ->
                    Timber.e(err, "Failed loading GTK supervision overview")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = err.toHumanReadableError("Gagal memuat data supervisi guru & tendik."),
                        )
                    }
                }
        }
    }

    fun selectTab(tabIndex: Int) {
        _state.update { it.copy(selectedTab = tabIndex.coerceIn(0, 1)) }
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun selectTierFilter(filter: String) {
        _state.update { it.copy(selectedTierFilter = filter) }
    }

    fun selectSortMode(sortMode: String) {
        _state.update { it.copy(selectedSortMode = sortMode) }
    }

    fun selectStaffFilter(filter: String) {
        _state.update { it.copy(selectedStaffFilter = filter) }
    }

    fun openStaffDetail(member: StaffMember) {
        _state.update { it.copy(selectedStaffMember = member) }
    }

    fun closeStaffDetail() {
        _state.update { it.copy(selectedStaffMember = null) }
    }

    fun clearUserMessage() {
        _state.update { it.copy(userMessage = null) }
    }

    fun exportSupervisionPdf(context: Context) {
        val current = _state.value
        val teachers = current.filteredTeachers
        val staff = current.filteredStaff
        if (teachers.isEmpty() && staff.isEmpty()) {
            _state.update { it.copy(userMessage = "Tidak ada data guru maupun tendik untuk diekspor.") }
            return
        }

        val pdf = PdfDocument()
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val printDate = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID")).format(Date())
            val fileName = "Rekap_Supervisi_GTK_$timeStamp.pdf"

            val pageWidth = 842
            val pageHeight = 595
            val margin = 32f
            val tableWidth = pageWidth - margin * 2f
            val footerReserve = margin + 22f

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
            val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 11f
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
                textSize = 8.2f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 8.2f
            }

            fun fitCell(text: String, width: Float, paint: TextPaint): String {
                return TextUtils.ellipsize(text, paint, (width - 8f).coerceAtLeast(10f), TextUtils.TruncateAt.END).toString()
            }

            fun drawRowCells(
                canvas: android.graphics.Canvas,
                y: Float,
                rowHeight: Float,
                values: List<String>,
                colWidths: List<Float>,
                paint: TextPaint,
                drawVerticalDividers: Boolean,
                baselineOffset: Float,
            ) {
                var cellX = margin
                values.forEachIndexed { idx, value ->
                    canvas.drawText(fitCell(value, colWidths[idx], paint), cellX + 4f, y + baselineOffset, paint)
                    if (drawVerticalDividers && idx > 0) {
                        canvas.drawLine(cellX, y, cellX, y + rowHeight, borderPaint)
                    }
                    cellX += colWidths[idx]
                }
            }

            var pageNum = 1
            val headerRowHeight = 22f
            val rowHeight = 20f

            // Page group 1: Pendidik / Guru / Tutor Performance Table
            if (teachers.isNotEmpty()) {
                val teacherHeaders = listOf("No", "Nama Lengkap", "NUPTK / NIP", "Mapel / Wali", "Modul", "Tugas", "Kuis", "Sesi", "Skor", "Kategori")
                val teacherRatios = listOf(0.04f, 0.22f, 0.14f, 0.19f, 0.06f, 0.06f, 0.06f, 0.06f, 0.06f, 0.11f)
                val teacherWidths = teacherRatios.map { it * tableWidth }

                var tIdx = 0
                while (tIdx < teachers.size) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    val page = pdf.startPage(pageInfo)
                    val canvas = page.canvas
                    var y = margin

                    if (pageNum == 1) {
                        val schoolHeader = current.schoolName.ifBlank { "AKSELERASI EDU" }
                        canvas.drawText("${schoolHeader.uppercase(Locale.getDefault())} • LAPORAN SUPERVISI GTK", margin, y + 8f, brandPaint)
                        y += 16f
                        canvas.drawText("Rekapitulasi Supervisi & Kinerja Guru / Tutor dan Tendik", margin, y + 12f, titlePaint)
                        y += 20f
                        canvas.drawText(
                            "Dicetak: $printDate • Guru/Tutor: ${teachers.size} • Tendik: ${staff.size} • Rata-rata Skor: ${current.overview.averagePerformanceScore}/100",
                            margin,
                            y + 8f,
                            metaPaint,
                        )
                        y += 18f
                    }
                    canvas.drawText(
                        if (current.isPkbm) "A. Kinerja Tutor & Pamong Belajar" else "A. Kinerja Pendidik / Guru",
                        margin,
                        y + 9f,
                        sectionPaint,
                    )
                    y += 16f

                    canvas.drawRect(margin, y, margin + tableWidth, y + headerRowHeight, headerBgPaint)
                    drawRowCells(canvas, y, headerRowHeight, teacherHeaders, teacherWidths, headerTextPaint, drawVerticalDividers = false, baselineOffset = 14.5f)
                    y += headerRowHeight

                    while (tIdx < teachers.size && y + rowHeight <= pageHeight - footerReserve) {
                        val p = teachers[tIdx]
                        if (tIdx % 2 == 1) canvas.drawRect(margin, y, margin + tableWidth, y + rowHeight, zebraBgPaint)
                        canvas.drawRect(margin, y, margin + tableWidth, y + rowHeight, borderPaint)

                        val idDoc = p.teacher.nuptk?.takeIf { it.isNotBlank() } ?: p.teacher.nip?.takeIf { it.isNotBlank() } ?: "-"
                        val roleText = if (!p.homeroomClassName.isNullOrBlank()) "${p.displaySubject} (${p.homeroomClassName})" else p.displaySubject
                        val rowValues = listOf(
                            "${tIdx + 1}",
                            p.teacher.fullName,
                            idDoc,
                            roleText,
                            "${p.materialsCount}",
                            "${p.assignmentsCount}",
                            "${p.quizzesCount}",
                            "${p.sessionsCount}",
                            "${p.performanceScore}",
                            p.tier.shortLabel,
                        )
                        drawRowCells(canvas, y, rowHeight, rowValues, teacherWidths, cellPaint, drawVerticalDividers = true, baselineOffset = 13.5f)
                        y += rowHeight
                        tIdx++
                    }

                    canvas.drawText("Halaman $pageNum • Dokumen PDF Supervisi GTK Akselerasi Edu", margin, pageHeight - 18f, metaPaint)
                    pdf.finishPage(page)
                    pageNum++
                }
            }

            // Page group 2: Tenaga Kependidikan (Tendik) Table
            if (staff.isNotEmpty()) {
                val staffHeaders = listOf("No", "Nama Tendik", "NIP / NUPTK", "Jabatan / Tugas", "Kepegawaian", "No. HP", "Status")
                val staffRatios = listOf(0.05f, 0.24f, 0.15f, 0.22f, 0.13f, 0.13f, 0.08f)
                val staffWidths = staffRatios.map { it * tableWidth }

                var sIdx = 0
                while (sIdx < staff.size) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    val page = pdf.startPage(pageInfo)
                    val canvas = page.canvas
                    var y = margin

                    if (pageNum == 1) {
                        val schoolHeader = current.schoolName.ifBlank { "AKSELERASI EDU" }
                        canvas.drawText("${schoolHeader.uppercase(Locale.getDefault())} • LAPORAN SUPERVISI GTK", margin, y + 8f, brandPaint)
                        y += 16f
                        canvas.drawText("Rekapitulasi Tenaga Kependidikan (Tendik)", margin, y + 12f, titlePaint)
                        y += 20f
                        canvas.drawText("Dicetak: $printDate • Total Tendik: ${staff.size}", margin, y + 8f, metaPaint)
                        y += 18f
                    }
                    canvas.drawText("B. Daftar Tenaga Kependidikan (Tendik)", margin, y + 9f, sectionPaint)
                    y += 16f

                    canvas.drawRect(margin, y, margin + tableWidth, y + headerRowHeight, headerBgPaint)
                    drawRowCells(canvas, y, headerRowHeight, staffHeaders, staffWidths, headerTextPaint, drawVerticalDividers = false, baselineOffset = 14.5f)
                    y += headerRowHeight

                    while (sIdx < staff.size && y + rowHeight <= pageHeight - footerReserve) {
                        val m = staff[sIdx]
                        if (sIdx % 2 == 1) canvas.drawRect(margin, y, margin + tableWidth, y + rowHeight, zebraBgPaint)
                        canvas.drawRect(margin, y, margin + tableWidth, y + rowHeight, borderPaint)

                        val idDoc = m.nip?.takeIf { it.isNotBlank() } ?: m.nuptk?.takeIf { it.isNotBlank() } ?: "-"
                        val rowValues = listOf(
                            "${sIdx + 1}",
                            m.fullName,
                            idDoc,
                            m.jobTitle,
                            m.statusKepegawaian ?: m.jenisPtk ?: "-",
                            m.noHp?.takeIf { it.isNotBlank() } ?: m.email?.takeIf { it.isNotBlank() } ?: "-",
                            if (m.isActive) "Aktif" else "Nonaktif",
                        )
                        drawRowCells(canvas, y, rowHeight, rowValues, staffWidths, cellPaint, drawVerticalDividers = true, baselineOffset = 13.5f)
                        y += rowHeight
                        sIdx++
                    }

                    canvas.drawText("Halaman $pageNum • Dokumen PDF Supervisi GTK Akselerasi Edu", margin, pageHeight - 18f, metaPaint)
                    pdf.finishPage(page)
                    pageNum++
                }
            }

            val outStream = ByteArrayOutputStream()
            pdf.writeTo(outStream)
            saveAndOpenSupervisionPdf(
                context = context,
                fileName = fileName,
                bytes = outStream.toByteArray(),
                subject = "Rekapitulasi Supervisi Guru & Tendik",
                chooserTitle = "Buka / Bagikan Rekap GTK (PDF)",
            )

            _state.update { it.copy(userMessage = "Dokumen PDF berhasil disimpan: $fileName") }
        } catch (e: Exception) {
            Timber.e(e, "Error exporting supervision PDF")
            _state.update { it.copy(userMessage = e.toHumanReadableError("Gagal mengekspor laporan PDF GTK.")) }
        } finally {
            pdf.close()
        }
    }
}

internal fun saveAndOpenSupervisionPdf(
    context: Context,
    fileName: String,
    bytes: ByteArray,
    subject: String,
    chooserTitle: String,
) {
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
        val shareIntent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            chooserTitle,
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        runCatching { context.startActivity(viewIntent) }.onFailure {
            context.startActivity(shareIntent)
        }
    }
}
