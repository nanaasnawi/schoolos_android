package com.schoolos.android.feature.home

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.auth.isPkbmInstitution
import com.schoolos.android.core.chat.ChatManager
import com.schoolos.android.core.common.toHumanReadableError
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TeacherPerformanceDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val schoolName: String = "",
    val isPkbm: Boolean = false,
    val profile: TeacherSupervisionProfile? = null,
    val schoolAverageScore: Int = 0,
    val selectedTab: Int = 0, // 0 = Karya & Aktivitas, 1 = Rombel & Sesi, 2 = Biodata GTK
)

@HiltViewModel
class TeacherPerformanceDetailViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val authManager: AuthManager,
    private val chatManager: ChatManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val teacherId: String = Uri.decode(savedStateHandle.get<String>("teacherId") ?: "")

    private val _state = MutableStateFlow(TeacherPerformanceDetailUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load(isPullRefresh: Boolean = false) {
        if (teacherId.isBlank()) {
            _state.update { it.copy(isLoading = false, errorMessage = "ID Guru tidak valid.") }
            return
        }
        viewModelScope.launch {
            val auth = authManager.authState.first()
            val schoolName = auth.schoolName.orEmpty()
            _state.update {
                it.copy(
                    isLoading = !isPullRefresh && it.profile == null,
                    isRefreshing = isPullRefresh,
                    errorMessage = null,
                    schoolName = schoolName,
                    isPkbm = isPkbmInstitution(schoolName),
                )
            }

            // Load detail + school overview average cleanly
            analyticsRepository.getGtkSupervisionOverview()
                .onSuccess { overview ->
                    val matched = overview.teachers.firstOrNull { it.teacher.id == teacherId }
                    if (matched != null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                profile = matched,
                                schoolAverageScore = overview.averagePerformanceScore,
                                errorMessage = null,
                            )
                        }
                    } else {
                        loadDirectDetail(overview.averagePerformanceScore)
                    }
                }
                .onFailure {
                    loadDirectDetail(0)
                }
        }
    }

    private suspend fun loadDirectDetail(avgScore: Int) {
        analyticsRepository.getTeacherSupervisionDetail(teacherId)
            .onSuccess { detail ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        profile = detail,
                        schoolAverageScore = avgScore,
                        errorMessage = null,
                    )
                }
            }
            .onFailure { err ->
                Timber.e(err, "Failed loading teacher supervision detail")
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = err.toHumanReadableError("Gagal memuat detail performa guru."),
                    )
                }
            }
    }

    fun selectTab(index: Int) {
        _state.update { it.copy(selectedTab = index.coerceIn(0, 2)) }
    }

    fun clearUserMessage() {
        _state.update { it.copy(userMessage = null) }
    }

    fun openDirectChat(onOpenThread: (threadId: String, recipientName: String) -> Unit) {
        val profile = _state.value.profile ?: return
        val targetId = profile.teacher.userId?.takeIf { it.isNotBlank() } ?: profile.teacher.id
        val thread = chatManager.getOrCreateTeacherThread(
            teacherId = targetId,
            teacherName = profile.teacher.fullName,
            subjectName = profile.displaySubject,
        )
        onOpenThread(thread.id, profile.teacher.fullName)
    }

    fun exportTeacherPdf(context: Context) {
        val current = _state.value
        val profile = current.profile ?: run {
            _state.update { it.copy(userMessage = "Data guru belum tersedia untuk diekspor.") }
            return
        }

        val pdf = PdfDocument()
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val printDate = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID")).format(Date())
            val cleanName = profile.teacher.fullName.replace(Regex("[^a-zA-Z0-9]+"), "_").trim('_').take(28)
            val fileName = "Rapor_Supervisi_${cleanName}_$timeStamp.pdf"

            // A4 Portrait (595 x 842 pt)
            val pageWidth = 595
            val pageHeight = 842
            val margin = 36f
            val contentWidth = pageWidth - margin * 2f
            val footerY = pageHeight - 24f

            val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0284C7")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 9.2f
            }
            val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 9.2f
            }
            val cardBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val darkHeaderPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            fun fit(text: String, width: Float, paint: TextPaint): String {
                return TextUtils.ellipsize(text, paint, width.coerceAtLeast(12f), TextUtils.TruncateAt.END).toString()
            }

            var pageNum = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            var canvas = page.canvas
            var y = margin

            fun ensureSpace(neededHeight: Float) {
                if (y + neededHeight > pageHeight - 44f) {
                    canvas.drawText("Halaman $pageNum • Rapor Supervisi GTK • Akselerasi Edu", margin, footerY, labelPaint)
                    pdf.finishPage(page)
                    pageNum++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = page.canvas
                    y = margin
                    canvas.drawText("${profile.teacher.fullName} (Lanjutan)", margin, y + 8f, labelPaint)
                    y += 22f
                }
            }

            val schoolTitle = current.schoolName.ifBlank { "AKSELERASI EDU" }.uppercase(Locale.getDefault())
            canvas.drawText("$schoolTitle • RAPOR SUPERVISI KINERJA PENDIDIK", margin, y + 8f, brandPaint)
            y += 18f
            canvas.drawText(fit(profile.teacher.fullName, contentWidth, titlePaint), margin, y + 14f, titlePaint)
            y += 24f
            canvas.drawText("Dicetak: $printDate • Kategori: ${profile.tier.label} • Skor: ${profile.performanceScore}/100", margin, y + 8f, labelPaint)
            y += 18f

            // Identity Box
            canvas.drawRect(margin, y, margin + contentWidth, y + 88f, cardBgPaint)
            canvas.drawRect(margin, y, margin + contentWidth, y + 88f, borderPaint)
            val leftColX = margin + 12f
            val rightColX = margin + contentWidth / 2f + 8f
            val halfW = contentWidth / 2f - 24f
            var rowY = y + 18f
            canvas.drawText("Mapel / Bidang: ${fit(profile.displaySubject, halfW - 60f, valuePaint)}", leftColX, rowY, valuePaint)
            canvas.drawText("Status: ${if (profile.isActive) "Aktif" else "Nonaktif"}", rightColX, rowY, valuePaint)
            rowY += 18f
            canvas.drawText("NUPTK: ${profile.teacher.nuptk?.ifBlank { "-" } ?: "-"}", leftColX, rowY, bodyPaint)
            canvas.drawText("NIP: ${profile.teacher.nip?.ifBlank { "-" } ?: "-"}", rightColX, rowY, bodyPaint)
            rowY += 18f
            canvas.drawText("Kepegawaian: ${fit(profile.teacher.statusKepegawaian ?: "-", halfW - 60f, bodyPaint)}", leftColX, rowY, bodyPaint)
            canvas.drawText("Wali Rombel: ${fit(profile.homeroomClassName ?: "-", halfW - 60f, bodyPaint)}", rightColX, rowY, bodyPaint)
            rowY += 18f
            canvas.drawText("Kontak HP: ${profile.teacher.noHp?.ifBlank { "-" } ?: "-"}", leftColX, rowY, bodyPaint)
            canvas.drawText("Email: ${fit(profile.teacher.email ?: "-", halfW - 40f, bodyPaint)}", rightColX, rowY, bodyPaint)
            y += 104f

            // 4-Pillar Performance Breakdown
            canvas.drawText("A. Evaluasi Kinerja 4 Pilar (Total Skor: ${profile.performanceScore}/100)", margin, y + 10f, sectionPaint)
            y += 18f
            canvas.drawRect(margin, y, margin + contentWidth, y + 22f, darkHeaderPaint)
            canvas.drawText("Pilar Evaluasi", margin + 8f, y + 14.5f, headerTextPaint)
            canvas.drawText("Capaian Real", margin + 250f, y + 14.5f, headerTextPaint)
            canvas.drawText("Poin", margin + 455f, y + 14.5f, headerTextPaint)
            y += 22f

            val pillars = listOf(
                Triple("1. Modul & Bahan Ajar", "${profile.materialsCount} modul (${profile.totalReadingsCompleted} pembaca selesai)", "${profile.materialsScore}/30"),
                Triple("2. Penugasan Siswa / WB", "${profile.assignmentsCount} tugas diterbitkan", "${profile.assignmentsScore}/25"),
                Triple("3. Evaluasi & Kuis CBT", "${profile.quizzesCount} kuis CBT dibuat", "${profile.quizzesScore}/25"),
                Triple("4. Sesi Mengajar & Rombel", "${profile.completedSessionsCount}/${profile.sessionsCount} sesi • ${profile.assignedClasses.size} rombel", "${profile.sessionsScore}/20"),
            )
            pillars.forEachIndexed { idx, (name, metric, pts) ->
                if (idx % 2 == 1) canvas.drawRect(margin, y, margin + contentWidth, y + 20f, cardBgPaint)
                canvas.drawRect(margin, y, margin + contentWidth, y + 20f, borderPaint)
                canvas.drawText(name, margin + 8f, y + 13.5f, bodyPaint)
                canvas.drawText(fit(metric, 195f, bodyPaint), margin + 250f, y + 13.5f, bodyPaint)
                canvas.drawText(pts, margin + 455f, y + 13.5f, valuePaint)
                y += 20f
            }
            y += 18f

            // Insights
            if (profile.coachingInsights.isNotEmpty()) {
                ensureSpace(36f + profile.coachingInsights.size * 18f)
                canvas.drawText("B. Catatan Supervisi & Rekomendasi Kepala Sekolah", margin, y + 10f, sectionPaint)
                y += 18f
                profile.coachingInsights.forEach { insight ->
                    ensureSpace(20f)
                    canvas.drawText("• ${fit(insight, contentWidth - 16f, bodyPaint)}", margin + 6f, y + 10f, bodyPaint)
                    y += 16f
                }
                y += 10f
            }

            // Recent Works Table
            val works = buildList {
                profile.materials.take(15).forEach { add(Triple("Modul Ajar", it.title, it.subject)) }
                profile.assignments.take(15).forEach { add(Triple("Tugas Siswa", it.title, it.className ?: it.subjectName ?: "-")) }
                profile.quizzes.take(15).forEach { add(Triple("Kuis CBT", it.title, it.className ?: it.subjectName ?: "-")) }
            }
            if (works.isNotEmpty()) {
                ensureSpace(64f)
                canvas.drawText("C. Daftar Karya & Aktivitas Terbaru (${works.size} item)", margin, y + 10f, sectionPaint)
                y += 18f
                canvas.drawRect(margin, y, margin + contentWidth, y + 22f, darkHeaderPaint)
                canvas.drawText("Jenis", margin + 8f, y + 14.5f, headerTextPaint)
                canvas.drawText("Judul Modul / Tugas / Kuis", margin + 96f, y + 14.5f, headerTextPaint)
                canvas.drawText("Keterangan", margin + 390f, y + 14.5f, headerTextPaint)
                y += 22f

                works.forEachIndexed { idx, (kind, title, meta) ->
                    if (y + 20f > pageHeight - 44f) {
                        canvas.drawText("Halaman $pageNum • Rapor Supervisi GTK • Akselerasi Edu", margin, footerY, labelPaint)
                        pdf.finishPage(page)
                        pageNum++
                        page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                        canvas = page.canvas
                        y = margin
                        canvas.drawRect(margin, y, margin + contentWidth, y + 22f, darkHeaderPaint)
                        canvas.drawText("Jenis", margin + 8f, y + 14.5f, headerTextPaint)
                        canvas.drawText("Judul Modul / Tugas / Kuis", margin + 96f, y + 14.5f, headerTextPaint)
                        canvas.drawText("Keterangan", margin + 390f, y + 14.5f, headerTextPaint)
                        y += 22f
                    }
                    if (idx % 2 == 1) canvas.drawRect(margin, y, margin + contentWidth, y + 20f, cardBgPaint)
                    canvas.drawRect(margin, y, margin + contentWidth, y + 20f, borderPaint)
                    canvas.drawText(kind, margin + 8f, y + 13.5f, bodyPaint)
                    canvas.drawText(fit(title, 284f, bodyPaint), margin + 96f, y + 13.5f, bodyPaint)
                    canvas.drawText(fit(meta, 125f, bodyPaint), margin + 390f, y + 13.5f, bodyPaint)
                    y += 20f
                }
            }

            canvas.drawText("Halaman $pageNum • Rapor Supervisi GTK • Akselerasi Edu", margin, footerY, labelPaint)
            pdf.finishPage(page)

            val outStream = ByteArrayOutputStream()
            pdf.writeTo(outStream)
            saveAndOpenSupervisionPdf(
                context = context,
                fileName = fileName,
                bytes = outStream.toByteArray(),
                subject = "Rapor Supervisi ${profile.teacher.fullName}",
                chooserTitle = "Buka / Bagikan Rapor PDF",
            )

            _state.update { it.copy(userMessage = "Rapor PDF berhasil disimpan: $fileName") }
        } catch (e: Exception) {
            Timber.e(e, "Error exporting individual teacher PDF")
            _state.update { it.copy(userMessage = e.toHumanReadableError("Gagal mengekspor rapor PDF guru.")) }
        } finally {
            pdf.close()
        }
    }
}
