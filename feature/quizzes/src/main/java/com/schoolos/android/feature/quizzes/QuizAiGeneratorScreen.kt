package com.schoolos.android.feature.quizzes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schoolos.android.core.common.toHumanReadableError
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.GeneratedCurriculumQuestion
import com.schoolos.android.domain.model.GeneratedCurriculumResult
import com.schoolos.android.domain.model.groupParallelClasses
import kotlinx.coroutines.launch

private enum class QuizGenStep {
    CONFIGURE,
    GENERATING,
    VERIFY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizAiGeneratorScreen(
    availableSubjects: List<AcademicSubject>,
    availableClasses: List<AcademicClass>,
    currentSubjectName: String,
    currentClassName: String,
    isSubjectLocked: Boolean = false,
    onBack: () -> Unit,
    onApply: (GeneratedCurriculumResult) -> Unit,
    onGenerateRequest: suspend (
        type: String,
        subjectId: String,
        subjectName: String,
        classId: String?,
        className: String,
        topic: String,
        numQuestions: Int,
        numMcq: Int,
        numEssay: Int,
        difficulty: String,
        includeImages: Boolean,
    ) -> Result<GeneratedCurriculumResult>,
) {
    val isDark = LocalIsDarkTheme.current
    val scope = rememberCoroutineScope()

    val pageBg = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val surfaceMuted = if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9)
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569)
    val textMuted = if (isDark) Color(0xFF71717A) else Color(0xFF64748B)
    val cyan = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val emerald = if (isDark) Color(0xFF10B981) else Color(0xFF059669)
    val purple = if (isDark) Color(0xFFA855F7) else Color(0xFF7C3AED)
    val amber = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)

    var currentStep by remember { mutableStateOf(QuizGenStep.CONFIGURE) }
    var generatedResult by remember { mutableStateOf<GeneratedCurriculumResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val groupedClasses = remember(availableClasses) { availableClasses.groupParallelClasses() }
    val gradeOptions = remember(groupedClasses) {
        if (groupedClasses.isNotEmpty()) {
            groupedClasses.map { it.displayName }
        } else {
            listOf(
                "PAKET A 4", "PAKET A 5", "PAKET A 6",
                "PAKET B 7", "PAKET B 8", "PAKET B 9",
                "PAKET C 10", "PAKET C 11", "PAKET C 12"
            )
        }
    }

    var selectedFormat by remember { mutableStateOf("MIXED") } // "MCQ_ONLY" | "MIXED"
    var examScope by remember { mutableStateOf("TOPIC_QUIZ") } // "TOPIC_QUIZ" | "MONTHLY_EXAM"
    var customTopic by remember { mutableStateOf("") }
    var selectedGrade by remember(currentClassName, gradeOptions) {
        val matched = gradeOptions.firstOrNull { it.equals(currentClassName, ignoreCase = true) || it.startsWith(currentClassName, ignoreCase = true) }
        mutableStateOf(matched ?: gradeOptions.firstOrNull() ?: "PAKET A 4")
    }
    var selectedDifficulty by remember { mutableStateOf("Sedang") }

    var numMcq by remember { mutableIntStateOf(10) }
    var numEssay by remember { mutableIntStateOf(5) }
    var customMcqInput by remember { mutableStateOf("10") }
    var customEssayInput by remember { mutableStateOf("5") }
    var includeImages by remember { mutableStateOf(false) }

    var selectedSubject by remember(currentSubjectName, availableSubjects) {
        val matched = availableSubjects.find { it.name.equals(currentSubjectName, ignoreCase = true) }
        mutableStateOf(matched ?: availableSubjects.firstOrNull())
    }

    var selectedClass by remember(currentClassName, availableClasses) {
        val matched = availableClasses.find { it.name.equals(currentClassName, ignoreCase = true) }
        mutableStateOf(matched ?: availableClasses.firstOrNull())
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var currentGenJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    androidx.activity.compose.BackHandler(enabled = currentStep == QuizGenStep.GENERATING) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) {
            currentGenJob?.cancel()
            currentStep = QuizGenStep.CONFIGURE
            android.widget.Toast.makeText(context, "Proses AI dibatalkan", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            lastBackPressTime = currentTime
            android.widget.Toast.makeText(context, "Tekan sekali lagi untuk membatalkan proses AI", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val handleStartGeneration = {
        val subject = selectedSubject
        if (subject == null) {
            errorMessage = "Silakan pilih mata pelajaran terlebih dahulu."
        } else {
            errorMessage = null
            currentStep = QuizGenStep.GENERATING

            val finalMcq = numMcq
            val finalEssay = if (selectedFormat == "MIXED") numEssay else 0
            val totalQ = finalMcq + finalEssay
            val effectiveTopic = customTopic.trim().ifBlank { subject.name }
            val typeStr = if (examScope == "MONTHLY_EXAM") "EXAM_MONTHLY" else (if (selectedFormat == "MIXED") "QUIZ_MCQ_ESSAY" else "QUIZ_MCQ_ONLY")

            currentGenJob = scope.launch {
                val res = onGenerateRequest(
                    typeStr,
                    subject.id,
                    subject.name,
                    selectedClass?.id,
                    selectedGrade,
                    effectiveTopic,
                    totalQ,
                    finalMcq,
                    finalEssay,
                    selectedDifficulty,
                    includeImages
                )
                res.onSuccess { result ->
                    generatedResult = result
                    currentStep = QuizGenStep.VERIFY
                }.onFailure { err ->
                    if (err !is kotlinx.coroutines.CancellationException) {
                        errorMessage = err.toHumanReadableError(fallback = "Gagal menyusun paket kuis CBT dengan AI. Silakan periksa koneksi dan coba lagi.")
                        currentStep = QuizGenStep.CONFIGURE
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(pageBg)) {
        when (currentStep) {
            QuizGenStep.CONFIGURE -> {
                Scaffold(
                    containerColor = pageBg,
                    topBar = {
                        ExecutiveTopBar(
                            title = "Studio AI Kuis & Ujian CBT",
                            subtitle = "Kurikulum Merdeka • Sintesis Bank Soal Otomatis",
                            onBack = onBack
                        )
                    },
                    bottomBar = {
                        Surface(
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            color = cardBg,
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
                                        containerColor = cyan,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Generate Paket Kuis CBT Sekarang",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (errorMessage != null) {
                            item {
                                Surface(
                                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444))
                                        Spacer(Modifier.width(8.dp))
                                        Text(errorMessage ?: "", color = textPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Unified Configuration Container
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
                                    val effectiveSubjectLocked = isSubjectLocked && availableSubjects.size <= 1
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        ExecutiveBottomSheetSelector(
                                            value = selectedGrade,
                                            label = "Rombel / Jenjang",
                                            icon = Icons.Default.Groups,
                                            options = gradeOptions,
                                            onSelect = { selected ->
                                                selectedGrade = selected
                                                val foundGroup = groupedClasses.find { it.displayName == selected }
                                                if (foundGroup != null) {
                                                    selectedClass = AcademicClass(
                                                        id = foundGroup.primaryId,
                                                        name = foundGroup.displayName
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            accentColor = cyan
                                        )

                                        ExecutiveBottomSheetSelector(
                                            value = selectedSubject?.name ?: currentSubjectName.ifBlank { "Pilih Mapel" },
                                            label = "Mata Pelajaran",
                                            icon = Icons.Default.MenuBook,
                                            options = availableSubjects.map { it.name },
                                            onSelect = { name ->
                                                selectedSubject = availableSubjects.find { it.name == name }
                                            },
                                            modifier = Modifier.weight(1f),
                                            accentColor = cyan,
                                            isLocked = effectiveSubjectLocked,
                                            lockedBadgeText = "Terkunci"
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            "Topik / Materi Spesifik Soal (Opsional)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = textSecondary
                                        )
                                        OutlinedTextField(
                                            value = customTopic,
                                            onValueChange = { customTopic = it },
                                            placeholder = {
                                                Text(
                                                    "Contoh: Dinamika Gerak Lurus, Peredaran Darah...",
                                                    fontSize = 12.sp,
                                                    color = textMuted
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = cyan,
                                                unfocusedBorderColor = cardBorder,
                                                focusedTextColor = textPrimary,
                                                unfocusedTextColor = textPrimary,
                                                focusedContainerColor = surfaceMuted.copy(alpha = 0.45f),
                                                unfocusedContainerColor = surfaceMuted.copy(alpha = 0.45f)
                                            )
                                        )
                                    }

                                    // Difficulty Segmented Selector
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Tingkat Kesulitan Soal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textSecondary)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(surfaceMuted)
                                                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf("Mudah", "Sedang", "HOTS").forEach { diff ->
                                                val isSel = selectedDifficulty == diff
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp)
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(if (isSel) cyan else Color.Transparent)
                                                        .clickable { selectedDifficulty = diff },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        diff,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                        color = if (isSel) Color.White else textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = cardBorder)

                                    // Format Ujian Segmented Selector
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Komposisi Format Ujian", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textSecondary)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(surfaceMuted)
                                                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf(
                                                "MCQ_ONLY" to "Full Pilihan Ganda",
                                                "MIXED" to "PG + Uraian (Campuran)"
                                            ).forEach { (key, label) ->
                                                val isSel = selectedFormat == key
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(if (isSel) cardBg else Color.Transparent)
                                                        .border(
                                                            width = if (isSel) 1.dp else 0.dp,
                                                            color = if (isSel) cyan.copy(alpha = 0.5f) else Color.Transparent,
                                                            shape = RoundedCornerShape(9.dp)
                                                        )
                                                        .clickable { selectedFormat = key }
                                                        .padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                                                        color = if (isSel) cyan else textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Jumlah Soal PG & Essay
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Jumlah Soal Pilihan Ganda (PG)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                                            Surface(
                                                color = emerald.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "$numMcq PG",
                                                    color = emerald,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            listOf(5, 10, 15, 20).forEach { count ->
                                                val isSel = numMcq == count
                                                Surface(
                                                    modifier = Modifier.clickable {
                                                        numMcq = count
                                                        customMcqInput = count.toString()
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) emerald else surfaceMuted,
                                                    border = BorderStroke(1.dp, if (isSel) emerald else cardBorder)
                                                ) {
                                                    Text(
                                                        "$count PG",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSel) Color.White else textSecondary,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(54.dp)
                                                    .height(34.dp)
                                                    .background(surfaceMuted, RoundedCornerShape(8.dp))
                                                    .border(1.dp, cardBorder, RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                androidx.compose.foundation.text.BasicTextField(
                                                    value = customMcqInput,
                                                    onValueChange = {
                                                        customMcqInput = it
                                                        it.toIntOrNull()?.let { v -> if (v in 1..30) numMcq = v }
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    textStyle = androidx.compose.ui.text.TextStyle(
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        color = textPrimary
                                                    ),
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                                                )
                                            }
                                        }

                                        if (selectedFormat == "MIXED") {
                                            HorizontalDivider(color = cardBorder)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Jumlah Soal Uraian / Essay", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                                                Surface(
                                                    color = purple.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        "$numEssay Essay",
                                                        color = purple,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                listOf(2, 3, 5, 10).forEach { count ->
                                                    val isSel = numEssay == count
                                                    Surface(
                                                        modifier = Modifier.clickable {
                                                            numEssay = count
                                                            customEssayInput = count.toString()
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isSel) purple else surfaceMuted,
                                                        border = BorderStroke(1.dp, if (isSel) purple else cardBorder)
                                                    ) {
                                                        Text(
                                                            "$count Essay",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) Color.White else textSecondary,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                                        )
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .width(54.dp)
                                                        .height(34.dp)
                                                        .background(surfaceMuted, RoundedCornerShape(8.dp))
                                                        .border(1.dp, cardBorder, RoundedCornerShape(8.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    androidx.compose.foundation.text.BasicTextField(
                                                        value = customEssayInput,
                                                        onValueChange = {
                                                            customEssayInput = it
                                                            it.toIntOrNull()?.let { v -> if (v in 1..20) numEssay = v }
                                                        },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        singleLine = true,
                                                        textStyle = androidx.compose.ui.text.TextStyle(
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                            color = textPrimary
                                                        ),
                                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = cardBorder)

                                    // Diagram toggle
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(surfaceMuted.copy(alpha = 0.5f))
                                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                                            .clickable { includeImages = !includeImages }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.Image,
                                                contentDescription = null,
                                                tint = if (includeImages) cyan else textMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    "Sertakan Soal Bergambar / Diagram",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = textPrimary
                                                )
                                                Text(
                                                    "Ilustrasi sains, geometri & grafik edukatif",
                                                    fontSize = 10.sp,
                                                    color = textMuted
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = includeImages,
                                            onCheckedChange = { includeImages = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = cyan
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            QuizGenStep.GENERATING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageBg),
                    contentAlignment = Alignment.Center
                ) {
                    AiGenerationLoadingOverlay(
                        isVisible = true,
                        type = AiGenerationType.QUIZ,
                        subjectName = selectedSubject?.name ?: currentSubjectName,
                        topic = customTopic.ifBlank { selectedSubject?.name ?: "Umum" },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            QuizGenStep.VERIFY -> {
                val res = generatedResult
                if (res != null) {
                    val mcqCount = res.questions.count { it.questionType == "MULTIPLE_CHOICE" }
                    val essayCount = res.questions.count { it.questionType == "ESSAY" }

                    Scaffold(
                        containerColor = pageBg,
                        topBar = {
                            ExecutiveTopBar(
                                title = "Verifikasi Paket Soal CBT",
                                subtitle = "Periksa butir soal & kunci sebelum diterapkan",
                                onBack = { currentStep = QuizGenStep.CONFIGURE }
                            )
                        },
                        bottomBar = {
                            Surface(
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                color = cardBg,
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
                                        onClick = { currentStep = QuizGenStep.CONFIGURE },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                        border = BorderStroke(1.dp, cardBorder)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Ubah", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onApply(res) },
                                        modifier = Modifier.weight(1.4f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = cyan,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Terapkan ke Kuis", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
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
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, cyan.copy(alpha = 0.45f)),
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
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Verified, contentDescription = null, tint = cyan, modifier = Modifier.size(18.dp))
                                                Text(
                                                    "Paket Soal CBT Siap Pakai",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp,
                                                    color = cyan
                                                )
                                            }
                                            Surface(
                                                color = cyan.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    "${res.questions.size} Butir Soal",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp,
                                                    color = cyan,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            res.title,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = textPrimary
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (mcqCount > 0) {
                                                Surface(color = emerald.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                                    Text("$mcqCount PG", color = emerald, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                                }
                                            }
                                            if (essayCount > 0) {
                                                Surface(color = purple.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                                    Text("$essayCount Essay", color = purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                                }
                                            }
                                            Surface(color = amber.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                                Text("KKTP: ${res.passingScore}", color = amber, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                            }
                                            Surface(color = cyan.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                                Text("${res.timeLimitMinutes} Menit", color = cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                            }
                                        }

                                        if (!res.description.isNullOrBlank()) {
                                            Text(
                                                res.description ?: "",
                                                fontSize = 12.sp,
                                                color = textSecondary,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }

                            itemsIndexed(res.questions) { idx, q ->
                                QuizQuestionVerificationCard(
                                    index = idx + 1,
                                    question = q,
                                    isDark = isDark,
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    surfaceMuted = surfaceMuted,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    cyan = cyan,
                                    emerald = emerald,
                                    purple = purple
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizQuestionVerificationCard(
    index: Int,
    question: GeneratedCurriculumQuestion,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    surfaceMuted: Color,
    textPrimary: Color,
    textSecondary: Color,
    cyan: Color,
    emerald: Color,
    purple: Color
) {
    val isMcq = question.questionType == "MULTIPLE_CHOICE"
    val accent = if (isMcq) cyan else purple

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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(index.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Surface(
                        color = accent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            if (isMcq) "Pilihan Ganda" else "Uraian / Essay",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    "${question.points} Poin",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            Text(
                question.questionText,
                fontWeight = FontWeight.SemiBold,
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
                                .background(
                                    if (isCorrect) emerald.copy(alpha = if (isDark) 0.14f else 0.10f)
                                    else surfaceMuted.copy(alpha = 0.55f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isCorrect) emerald.copy(alpha = 0.55f) else cardBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isCorrect) emerald else cardBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    letter,
                                    color = if (isCorrect) Color.White else textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                choice.choiceText,
                                fontSize = 12.sp,
                                color = if (isCorrect) emerald else textPrimary,
                                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCorrect) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = emerald, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            } else if (!isMcq && !question.explanation.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(purple.copy(alpha = 0.10f))
                        .border(1.dp, purple.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Panduan Jawaban & Rubrik:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = purple)
                    Text(
                        question.explanation ?: "",
                        fontSize = 11.sp,
                        color = textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
