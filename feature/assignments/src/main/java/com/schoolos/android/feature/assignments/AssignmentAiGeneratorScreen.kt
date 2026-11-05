package com.schoolos.android.feature.assignments

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.GeneratedCurriculumQuestion
import com.schoolos.android.domain.model.GeneratedCurriculumResult
import com.schoolos.android.domain.model.findClass
import com.schoolos.android.domain.model.groupParallelClasses
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class GeneratorStep {
    CONFIGURE,
    GENERATING,
    VERIFY
}

@Composable
fun AssignmentAiGeneratorScreen(
    availableSubjects: List<AcademicSubject>,
    currentSubjectName: String,
    availableClasses: List<AcademicClass> = emptyList(),
    currentClassName: String = "",
    isSubjectLocked: Boolean = false,
    onBack: () -> Unit,
    onApply: (GeneratedCurriculumResult) -> Unit,
    onGenerateRequest: suspend (
        format: String,
        subjectId: String,
        subjectName: String,
        topic: String,
        gradeLevel: String,
        difficulty: String,
        numMcq: Int,
        numEssay: Int,
        includeImages: Boolean,
    ) -> Result<GeneratedCurriculumResult>,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    val pageBg = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val subSurfaceBg = if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9)
    val subSurfaceBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val emeraldAccent = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val emeraldButton = Color(0xFF059669)
    val emeraldSoftBg = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
    val emeraldSoftBorder = if (isDark) Color(0xFF059669).copy(alpha = 0.45f) else Color(0xFFA7F3D0)

    val purpleAccent = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
    val purpleSoftBg = if (isDark) Color(0xFF3B0764).copy(alpha = 0.35f) else Color(0xFFF3E8FF)
    val purpleSoftBorder = if (isDark) Color(0xFF9333EA).copy(alpha = 0.45f) else Color(0xFFD8B4FE)

    val amberAccent = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    val amberSoftBg = if (isDark) Color(0xFF451A03).copy(alpha = 0.4f) else Color(0xFFFEF3C7)
    val amberSoftBorder = if (isDark) Color(0xFFD97706).copy(alpha = 0.45f) else Color(0xFFFDE68A)

    var currentStep by remember { mutableStateOf(GeneratorStep.CONFIGURE) }
    var generatedResult by remember { mutableStateOf<GeneratedCurriculumResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val unifiedClasses = remember(availableClasses) {
        availableClasses.groupParallelClasses()
    }
    val gradeOptions = remember(unifiedClasses) {
        val fromClasses = unifiedClasses.map { it.displayName }
        val defaults = listOf(
            "PAKET A 4 (Setara SD)",
            "PAKET A 5 (Setara SD)",
            "PAKET A 6 (Setara SD)",
            "PAKET B 7 (Setara SMP)",
            "PAKET B 8 (Setara SMP)",
            "PAKET B 9 (Setara SMP)",
            "PAKET C 10 (Setara SMA)",
            "PAKET C 11 (Setara SMA)",
            "PAKET C 12 (Setara SMA)"
        )
        if (fromClasses.isNotEmpty()) (fromClasses + defaults).distinct() else defaults
    }

    var selectedFormat by remember { mutableStateOf("STRUCTURED_QUESTIONS") }
    var customTopic by remember { mutableStateOf("") }
    var selectedGrade by remember(currentClassName, gradeOptions) {
        mutableStateOf(
            currentClassName.takeIf { it.isNotBlank() }
                ?: gradeOptions.firstOrNull()
                ?: "PAKET B 8 (Setara SMP)"
        )
    }
    var selectedDifficulty by remember { mutableStateOf("Sedang") }
    var numMcq by remember { mutableIntStateOf(4) }
    var numEssay by remember { mutableIntStateOf(2) }
    var customMcqInput by remember { mutableStateOf("4") }
    var customEssayInput by remember { mutableStateOf("2") }
    var includeImages by remember { mutableStateOf(false) }

    var selectedSubject by remember(currentSubjectName, availableSubjects) {
        val matched = availableSubjects.find { it.name.equals(currentSubjectName, ignoreCase = true) }
        mutableStateOf(matched ?: availableSubjects.firstOrNull())
    }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var currentGenJob by remember { mutableStateOf<Job?>(null) }

    BackHandler(enabled = currentStep == GeneratorStep.GENERATING) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) {
            currentGenJob?.cancel()
            currentStep = GeneratorStep.CONFIGURE
            Toast.makeText(context, "Proses AI dibatalkan", Toast.LENGTH_SHORT).show()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Tekan sekali lagi untuk membatalkan proses AI", Toast.LENGTH_SHORT).show()
        }
    }

    val handleStartGeneration = {
        val subject = selectedSubject
        if (subject == null && currentSubjectName.isBlank()) {
            errorMessage = "Silakan pilih mata pelajaran terlebih dahulu."
        } else {
            errorMessage = null
            currentStep = GeneratorStep.GENERATING

            val finalMcq = if (selectedFormat == "HOMEWORK_PR") 0 else numMcq
            val finalEssay = numEssay
            val subId = subject?.id ?: ""
            val subName = subject?.name ?: currentSubjectName
            val effectiveTopic = customTopic.trim().ifBlank { subName }

            currentGenJob = scope.launch {
                val res = onGenerateRequest(
                    selectedFormat,
                    subId,
                    subName,
                    effectiveTopic,
                    selectedGrade,
                    selectedDifficulty,
                    finalMcq,
                    finalEssay,
                    includeImages
                )
                res.onSuccess { result ->
                    generatedResult = result
                    currentStep = GeneratorStep.VERIFY
                }.onFailure { err ->
                    if (err !is CancellationException) {
                        errorMessage = err.toHumanReadableError(fallback = "Gagal menyusun penugasan dengan AI. Silakan periksa koneksi dan coba lagi.")
                        currentStep = GeneratorStep.CONFIGURE
                    }
                }
            }
        }
    }

    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Box(modifier = Modifier.fillMaxSize().background(pageBg)) {
        when (currentStep) {
            GeneratorStep.CONFIGURE -> {
                Scaffold(
                    containerColor = pageBg,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        ExecutiveTopBar(
                            title = "Studio AI Penugasan Guru",
                            subtitle = "Kurikulum Merdeka • Sintesis Soal & Rubrik Otomatis",
                            onBack = onBack
                        )
                    },
                    bottomBar = {
                        if (!isImeVisible) {
                            Surface(
                                color = cardBg,
                                tonalElevation = 0.dp,
                                shadowElevation = 12.dp,
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Button(
                                        onClick = { handleStartGeneration() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = emeraldButton,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Mulai Susun Tugas Otomatis",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = padding.calculateTopPadding(),
                                bottom = if (isImeVisible) 0.dp else padding.calculateBottomPadding()
                            )
                            .imePadding()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (errorMessage != null) {
                            item {
                                Surface(
                                    color = NeonError.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, NeonError.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NeonError, modifier = Modifier.size(18.dp))
                                        Text(errorMessage ?: "", color = NeonError, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // ── 1. KONTEKS KURIKULUM (MAPEL & JENJANG ROMBEL) ──
                        item {
                            Surface(
                                color = cardBg,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(emeraldAccent)
                                        )
                                        Text(
                                            "KONTEKS MAPEL & ROMBEL",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = emeraldAccent,
                                            letterSpacing = 0.6.sp
                                        )
                                    }

                                    val effectiveSubjectLocked = isSubjectLocked && availableSubjects.size <= 1
                                    val subNames = remember(availableSubjects, currentSubjectName) {
                                        val list = availableSubjects.map { it.name }
                                        if (list.isEmpty() && currentSubjectName.isNotBlank()) listOf(currentSubjectName) else list
                                    }

                                    ExecutiveBottomSheetSelector(
                                        value = selectedSubject?.name ?: currentSubjectName.ifBlank { "Pilih Mata Pelajaran" },
                                        label = "Mata Pelajaran",
                                        icon = Icons.Default.School,
                                        options = subNames,
                                        onSelect = { chosen ->
                                            selectedSubject = availableSubjects.find { it.name.equals(chosen, ignoreCase = true) }
                                        },
                                        sheetTitle = "Pilih Mata Pelajaran",
                                        sheetSubtitle = "Tentukan mata pelajaran untuk sintesis soal AI",
                                        accentColor = emeraldAccent,
                                        isLocked = effectiveSubjectLocked,
                                        lockedBadgeText = "Tutor Pengampu",
                                        onLockedClick = {
                                            Toast.makeText(context, "Mata pelajaran dikunci sesuai penugasan tutor", Toast.LENGTH_SHORT).show()
                                        }
                                    )

                                    ExecutiveBottomSheetSelector(
                                        value = selectedGrade,
                                        label = "Sasaran Rombel / Jenjang",
                                        icon = Icons.Default.Group,
                                        options = gradeOptions,
                                        onSelect = { selectedGrade = it },
                                        sheetTitle = "Pilih Rombel / Jenjang Kelas",
                                        sheetSubtitle = "Tingkat kesulitan bahasa akan disesuaikan dengan jenjang ini",
                                        accentColor = emeraldAccent
                                    )
                                }
                            }
                        }

                        // ── 2. TOPIK & KONSEP SPESIFIK ──
                        item {
                            Surface(
                                color = cardBg,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Topik / Pokok Bahasan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = textPrimary
                                        )
                                        Text(
                                            "Opsional",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textTertiary
                                        )
                                    }

                                    OutlinedTextField(
                                        value = customTopic,
                                        onValueChange = { customTopic = it },
                                        placeholder = {
                                            Text(
                                                "Contoh: Teks Eksposisi, Hukum Newton, Sejarah Kemerdekaan...",
                                                fontSize = 12.sp,
                                                color = textTertiary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = emeraldAccent, modifier = Modifier.size(18.dp))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        minLines = 2,
                                        maxLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = emeraldAccent,
                                            unfocusedBorderColor = subSurfaceBorder,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary,
                                            cursorColor = emeraldAccent
                                        )
                                    )

                                    Text(
                                        "Kosongkan jika ingin AI memilih topik inti secara otomatis sesuai kurikulum.",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                        }

                        // ── 3. METODE PENUGASAN & TINGKAT KESULITAN ──
                        item {
                            Surface(
                                color = cardBg,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Format Penugasan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = textPrimary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                Triple("STRUCTURED_QUESTIONS", "Terstruktur (PG & Esai)", "Koreksi otomatis & uraian"),
                                                Triple("HOMEWORK_PR", "Tugas Mandiri (PR)", "Instruksi lembar kerja & rubrik")
                                            ).forEach { (fmtKey, titleLabel, descLabel) ->
                                                val isSel = selectedFormat == fmtKey
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(13.dp))
                                                        .clickable { selectedFormat = fmtKey },
                                                    shape = RoundedCornerShape(13.dp),
                                                    color = if (isSel) emeraldSoftBg else subSurfaceBg,
                                                    border = BorderStroke(
                                                        width = if (isSel) 1.5.dp else 1.dp,
                                                        color = if (isSel) emeraldAccent else subSurfaceBorder
                                                    )
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(
                                                                titleLabel,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp,
                                                                color = if (isSel) emeraldAccent else textPrimary,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            if (isSel) {
                                                                Icon(
                                                                    Icons.Default.CheckCircle,
                                                                    contentDescription = null,
                                                                    tint = emeraldAccent,
                                                                    modifier = Modifier.size(15.dp)
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            descLabel,
                                                            fontSize = 10.5.sp,
                                                            color = textSecondary,
                                                            lineHeight = 14.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = cardBorder)

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Tingkat Kesulitan Kognitif",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = textPrimary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                "Mudah" to "Dasar (C1–C2)",
                                                "Sedang" to "Aplikasi (C3)",
                                                "HOTS" to "Analitis (C4–C6)"
                                            ).forEach { (diffKey, diffSub) ->
                                                val isSel = selectedDifficulty == diffKey
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable { selectedDifficulty = diffKey },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSel) emeraldSoftBg else subSurfaceBg,
                                                    border = BorderStroke(
                                                        width = if (isSel) 1.5.dp else 1.dp,
                                                        color = if (isSel) emeraldAccent else subSurfaceBorder
                                                    )
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            diffKey,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) emeraldAccent else textPrimary
                                                        )
                                                        Text(
                                                            diffSub,
                                                            fontSize = 9.5.sp,
                                                            color = textSecondary,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── 4. KOMPOSISI JUMLAH SOAL & VISUAL ──
                        item {
                            Surface(
                                color = cardBg,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    if (selectedFormat == "STRUCTURED_QUESTIONS") {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Jumlah Soal Pilihan Ganda (PG)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = textPrimary
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(emeraldSoftBg)
                                                        .border(0.5.dp, emeraldSoftBorder, RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        "$numMcq Butir PG",
                                                        color = emeraldAccent,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf(2, 4, 5, 10).forEach { count ->
                                                    val isSel = numMcq == count
                                                    Surface(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(38.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                numMcq = count
                                                                customMcqInput = count.toString()
                                                            },
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (isSel) emeraldButton else subSurfaceBg,
                                                        border = BorderStroke(1.dp, if (isSel) emeraldButton else subSurfaceBorder)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                "$count PG",
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSel) Color.White else textPrimary
                                                            )
                                                        }
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .width(56.dp)
                                                        .height(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(subSurfaceBg)
                                                        .border(1.dp, subSurfaceBorder, RoundedCornerShape(10.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    BasicTextField(
                                                        value = customMcqInput,
                                                        onValueChange = {
                                                            val filtered = it.filter { ch -> ch.isDigit() }.take(2)
                                                            customMcqInput = filtered
                                                            filtered.toIntOrNull()?.let { v -> if (v in 1..20) numMcq = v }
                                                        },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        singleLine = true,
                                                        cursorBrush = SolidColor(emeraldAccent),
                                                        textStyle = TextStyle(
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center,
                                                            color = textPrimary
                                                        ),
                                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = cardBorder)
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                if (selectedFormat == "HOMEWORK_PR") "Jumlah Butir Tugas Mandiri" else "Jumlah Soal Uraian / Esai",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = textPrimary
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(purpleSoftBg)
                                                    .border(0.5.dp, purpleSoftBorder, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    "$numEssay Butir Esai",
                                                    color = purpleAccent,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf(1, 2, 3, 5).forEach { count ->
                                                val isSel = numEssay == count
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            numEssay = count
                                                            customEssayInput = count.toString()
                                                        },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isSel) Color(0xFF7C3AED) else subSurfaceBg,
                                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF7C3AED) else subSurfaceBorder)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            "$count Esai",
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) Color.White else textPrimary
                                                        )
                                                    }
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(56.dp)
                                                    .height(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(subSurfaceBg)
                                                    .border(1.dp, subSurfaceBorder, RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                BasicTextField(
                                                    value = customEssayInput,
                                                    onValueChange = {
                                                        val filtered = it.filter { ch -> ch.isDigit() }.take(2)
                                                        customEssayInput = filtered
                                                        filtered.toIntOrNull()?.let { v -> if (v in 1..15) numEssay = v }
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    cursorBrush = SolidColor(purpleAccent),
                                                    textStyle = TextStyle(
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        color = textPrimary
                                                    ),
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = cardBorder)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(subSurfaceBg)
                                            .clickable { includeImages = !includeImages }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Image,
                                                contentDescription = null,
                                                tint = if (includeImages) emeraldAccent else textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    "Sertakan Ilustrasi / Diagram Soal",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = textPrimary
                                                )
                                                Text(
                                                    "Bagan kontekstual sains, geografi, atau tabel data",
                                                    fontSize = 10.5.sp,
                                                    color = textSecondary
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = includeImages,
                                            onCheckedChange = { includeImages = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = emeraldButton
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            GeneratorStep.GENERATING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageBg),
                    contentAlignment = Alignment.Center
                ) {
                    AiGenerationLoadingOverlay(
                        isVisible = true,
                        type = AiGenerationType.ASSIGNMENT,
                        subjectName = selectedSubject?.name ?: currentSubjectName,
                        topic = customTopic.ifBlank { selectedSubject?.name ?: "Umum" },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            GeneratorStep.VERIFY -> {
                val res = generatedResult
                if (res != null) {
                    val mcqCount = res.questions.count { it.questionType == "MULTIPLE_CHOICE" }
                    val essayCount = res.questions.count { it.questionType == "ESSAY" }

                    Scaffold(
                        containerColor = pageBg,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            ExecutiveTopBar(
                                title = "Verifikasi Hasil AI",
                                subtitle = "Tinjau paket soal & kunci jawaban sebelum diterapkan",
                                onBack = { currentStep = GeneratorStep.CONFIGURE }
                            )
                        },
                        bottomBar = {
                            Surface(
                                color = cardBg,
                                tonalElevation = 0.dp,
                                shadowElevation = 12.dp,
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = GeneratorStep.CONFIGURE },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                        border = BorderStroke(1.dp, cardBorder)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Susun Ulang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onApply(res) },
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = emeraldButton,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Terapkan ke Studio", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    ) { padding ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ── RINGKASAN PAKET TUGAS (ADAPTIVE DARK/LIGHT, ZERO HARSH GREEN) ──
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .background(emeraldSoftBg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Verified,
                                                        contentDescription = null,
                                                        tint = emeraldAccent,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                                Text(
                                                    "Paket Tugas Terverifikasi AI",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = emeraldAccent
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(subSurfaceBg)
                                                    .border(0.5.dp, subSurfaceBorder, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    "${res.questions.size} Butir Soal",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = textPrimary
                                                )
                                            }
                                        }

                                        Text(
                                            res.title,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = textPrimary,
                                            lineHeight = 22.sp
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (mcqCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(emeraldSoftBg)
                                                        .border(0.5.dp, emeraldSoftBorder, RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                                ) {
                                                    Text("$mcqCount Soal PG", color = emeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            if (essayCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(purpleSoftBg)
                                                        .border(0.5.dp, purpleSoftBorder, RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                                ) {
                                                    Text("$essayCount Soal Esai", color = purpleAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(amberSoftBg)
                                                    .border(0.5.dp, amberSoftBorder, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                                            ) {
                                                Text("KKTP: ${res.passingScore}", color = amberAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (!res.instructions.isNullOrBlank()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(subSurfaceBg)
                                                    .border(0.5.dp, subSurfaceBorder, RoundedCornerShape(12.dp))
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    "Panduan & Rubrik Penilaian",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textSecondary
                                                )
                                                Text(
                                                    res.instructions ?: "",
                                                    fontSize = 11.5.sp,
                                                    color = textPrimary.copy(alpha = 0.88f),
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Daftar Butir Soal (${res.questions.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = textPrimary
                                    )
                                    Text(
                                        "Total ${res.questions.sumOf { it.points }} Poin",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = emeraldAccent
                                    )
                                }
                            }

                            itemsIndexed(res.questions) { idx, q ->
                                QuestionVerificationCard(
                                    index = idx + 1,
                                    question = q,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    subSurfaceBg = subSurfaceBg,
                                    subSurfaceBorder = subSurfaceBorder,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    emeraldAccent = emeraldAccent,
                                    emeraldSoftBg = emeraldSoftBg,
                                    emeraldSoftBorder = emeraldSoftBorder,
                                    purpleAccent = purpleAccent,
                                    purpleSoftBg = purpleSoftBg,
                                    purpleSoftBorder = purpleSoftBorder
                                )
                            }

                            item {
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionVerificationCard(
    index: Int,
    question: GeneratedCurriculumQuestion,
    cardBg: Color,
    cardBorder: Color,
    subSurfaceBg: Color,
    subSurfaceBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    emeraldAccent: Color,
    emeraldSoftBg: Color,
    emeraldSoftBorder: Color,
    purpleAccent: Color,
    purpleSoftBg: Color,
    purpleSoftBorder: Color,
) {
    val isMcq = question.questionType == "MULTIPLE_CHOICE"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMcq) emeraldSoftBg else purpleSoftBg)
                            .border(0.5.dp, if (isMcq) emeraldSoftBorder else purpleSoftBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#$index",
                            color = if (isMcq) emeraldAccent else purpleAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        if (isMcq) "Pilihan Ganda" else "Uraian / Esai",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMcq) emeraldAccent else purpleAccent
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(subSurfaceBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${question.points} Poin",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = textSecondary
                    )
                }
            }

            Text(
                question.questionText,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = textPrimary,
                lineHeight = 19.sp
            )

            if (isMcq && question.choices.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    question.choices.forEachIndexed { cIdx, choice ->
                        val letter = ('A' + cIdx).toString()
                        val isCorrect = choice.isCorrect
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCorrect) emeraldSoftBg else subSurfaceBg)
                                .border(
                                    width = if (isCorrect) 1.dp else 0.5.dp,
                                    color = if (isCorrect) emeraldSoftBorder else subSurfaceBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isCorrect) emeraldAccent else cardBg)
                                    .border(0.5.dp, if (isCorrect) emeraldAccent else subSurfaceBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    letter,
                                    color = if (isCorrect) Color.White else textSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                choice.choiceText,
                                fontSize = 12.sp,
                                color = if (isCorrect) emeraldAccent else textPrimary,
                                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                                lineHeight = 16.sp
                            )
                            if (isCorrect) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = emeraldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!question.explanation.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(subSurfaceBg)
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = emeraldAccent,
                        modifier = Modifier.size(14.dp).padding(top = 1.dp)
                    )
                    Text(
                        question.explanation ?: "",
                        fontSize = 11.sp,
                        color = textSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
