package com.schoolos.android.feature.assignments

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.AssignmentQuestion
import com.schoolos.android.domain.model.findClass
import com.schoolos.android.domain.model.groupParallelClasses
import java.time.Instant
import java.time.temporal.ChronoUnit

private val CyanAccent = Color(0xFF0891B2)

@Composable
fun AssignmentCreatorScreen(
    initialClass: String? = null,
    initialSubject: String? = null,
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {},
    viewModel: AssignmentCreatorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val isDark = LocalIsDarkTheme.current

    val pageBg = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val surfaceBg = if (isDark) Color(0xFF111113) else Color.White
    val surfaceBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val subSurfaceBg = if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9)
    val subSurfaceBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textTertiary = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val emeraldAccent = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val emeraldSoftBg = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
    val emeraldSoftBorder = if (isDark) Color(0xFF059669).copy(alpha = 0.45f) else Color(0xFFA7F3D0)

    val unifiedClasses = remember(state.availableClasses) {
        state.availableClasses.groupParallelClasses()
    }
    val classOptions = remember(unifiedClasses) {
        unifiedClasses.map { it.displayName }
    }
    val subjectOptions = remember(state.availableSubjects) {
        state.availableSubjects.map { it.name }
    }

    var selectedClass by remember {
        mutableStateOf(
            unifiedClasses.findClass(initialClass)?.displayName
                ?: initialClass
                ?: ""
        )
    }
    var selectedSubject by remember { mutableStateOf(initialSubject ?: "") }

    LaunchedEffect(classOptions) {
        if (selectedClass.isEmpty() && classOptions.isNotEmpty()) {
            selectedClass = classOptions.first()
        } else if (selectedClass.isNotEmpty()) {
            unifiedClasses.findClass(selectedClass)?.let {
                selectedClass = it.displayName
            }
        }
    }

    LaunchedEffect(subjectOptions, state.isSubjectLocked, state.teacherSubject) {
        if (state.isSubjectLocked && !state.teacherSubject.isNullOrBlank()) {
            val matched = subjectOptions.firstOrNull {
                it.equals(state.teacherSubject!!, ignoreCase = true) ||
                    it.contains(state.teacherSubject!!, ignoreCase = true) ||
                    state.teacherSubject!!.contains(it, ignoreCase = true)
            }
            selectedSubject = matched ?: subjectOptions.firstOrNull() ?: state.teacherSubject!!
        } else if (selectedSubject.isEmpty() && subjectOptions.isNotEmpty()) {
            selectedSubject = subjectOptions.first()
        }
    }

    LaunchedEffect(selectedClass, subjectOptions) {
        if (subjectOptions.size > 1 && selectedClass.isNotBlank()) {
            val upper = selectedClass.uppercase()
            val autoSubject = if (upper.contains("PAKET C") || upper.contains(" 10") || upper.contains(" 11") || upper.contains(" 12")) {
                subjectOptions.firstOrNull { it.contains("ekonomi", ignoreCase = true) || it.contains("geografi", ignoreCase = true) || it.contains("sosiologi", ignoreCase = true) }
            } else if (upper.contains("PAKET B") || upper.contains(" 7") || upper.contains(" 8") || upper.contains(" 9")) {
                subjectOptions.firstOrNull { it.contains("cirebon", ignoreCase = true) || it.contains("ipa", ignoreCase = true) || it.contains("ips", ignoreCase = true) }
            } else null
            if (autoSubject != null && autoSubject != selectedSubject) {
                selectedSubject = autoSubject
            }
        }
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var customMaxScore by remember { mutableStateOf("100") }
    var selectedDueDays by remember { mutableIntStateOf(7) }
    var showPrePublishReview by remember { mutableStateOf(false) }
    var showAiGenerateDialog by remember { mutableStateOf(false) }
    var activeQuestionIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.questions.size) {
        if (activeQuestionIndex >= state.questions.size && state.questions.isNotEmpty()) {
            activeQuestionIndex = state.questions.lastIndex
        }
    }

    val totalQuestionPoints = remember(state.questions) {
        state.questions.sumOf { it.points ?: 10 }
    }

    val displayMaxScore = remember(state.assignmentFormat, totalQuestionPoints, customMaxScore) {
        if (state.assignmentFormat != "HOMEWORK_PR" && totalQuestionPoints > 0) {
            totalQuestionPoints.toString()
        } else {
            customMaxScore
        }
    }

    LaunchedEffect(state.success) {
        if (state.success) {
            Toast.makeText(context, "Tugas berhasil diterbitkan ke siswa!", Toast.LENGTH_SHORT).show()
            onSuccess()
            viewModel.resetState()
        }
    }

    if (showAiGenerateDialog) {
        AssignmentAiGeneratorScreen(
            availableSubjects = state.availableSubjects,
            currentSubjectName = selectedSubject,
            availableClasses = state.availableClasses,
            currentClassName = selectedClass,
            isSubjectLocked = state.isSubjectLocked,
            onBack = { showAiGenerateDialog = false },
            onApply = { generated ->
                title = generated.title
                instructions = generated.instructions ?: instructions
                if (!state.isSubjectLocked) {
                    selectedSubject = generated.subjectName.ifBlank { selectedSubject }
                }
                viewModel.applyGeneratedAssignment(generated)
                activeQuestionIndex = 0
                showAiGenerateDialog = false
                Toast.makeText(context, "Berhasil menerapkan ${generated.questions.size} butir soal terverifikasi!", Toast.LENGTH_SHORT).show()
            },
            onGenerateRequest = { format, subId, subName, topic, gradeLevel, difficulty, numMcq, numEssay, includeImages ->
                viewModel.generateAssignmentAsync(
                    format = format,
                    subjectId = subId,
                    subjectName = subName,
                    topic = topic,
                    gradeLevel = gradeLevel,
                    difficulty = difficulty,
                    numMcq = numMcq,
                    numEssay = numEssay,
                    includeImages = includeImages,
                )
            }
        )
        return
    }

    if (showPrePublishReview) {
        val calculatedDueDate = Instant.now().plus(selectedDueDays.toLong(), ChronoUnit.DAYS).toString()
        val matchedUnified = unifiedClasses.findClass(selectedClass)
        val targetClassIds = if (matchedUnified != null && matchedUnified.memberClassIds.isNotEmpty()) {
            matchedUnified.memberClassIds
        } else {
            val singleId = state.availableClasses.find { it.name == selectedClass }?.id ?: selectedClass
            listOf(singleId)
        }
        val targetClassId = targetClassIds.firstOrNull() ?: selectedClass
        val parsedScore = customMaxScore.toIntOrNull() ?: 100
        val fullDesc = "$selectedSubject • $selectedClass • $description"

        AssignmentReviewAndPublishScreen(
            title = title.trim(),
            description = fullDesc.trim(),
            instructions = instructions.ifBlank { "Kerjakan tugas secara teliti dan kumpulkan sebelum batas waktu berakhir." },
            selectedClass = selectedClass,
            selectedSubject = selectedSubject,
            dueDays = selectedDueDays,
            maxScore = displayMaxScore,
            assignmentFormat = state.assignmentFormat,
            questions = state.questions,
            isLoading = state.isLoading,
            onBackToEdit = { showPrePublishReview = false },
            onConfirmPublish = { isScheduled, releaseAtIso ->
                viewModel.createAssignment(
                    title = title.trim(),
                    description = fullDesc.trim(),
                    instructions = instructions.ifBlank { "Kerjakan tugas secara teliti dan kumpulkan sebelum batas waktu berakhir." },
                    customMaxScore = parsedScore,
                    dueAt = calculatedDueDate,
                    classId = targetClassId,
                    classIds = targetClassIds,
                    status = if (isScheduled) "draft" else "published",
                    releaseAt = releaseAtIso,
                )
            }
        )
        return
    }

    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        containerColor = pageBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ExecutiveTopBar(
                title = "Studio Penugasan Guru",
                subtitle = "Rancang & Terbitkan Tugas (PG & Esai)",
                onBack = onBack,
            )
        },
        bottomBar = {
            if (!isImeVisible) {
                Surface(
                    color = surfaceBg,
                    tonalElevation = 0.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, surfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    Toast.makeText(context, "Judul tugas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                showPrePublishReview = true
                            },
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF059669).copy(alpha = 0.35f)
                            )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                            } else {
                                Text("Pratinjau & Terbitkan Tugas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = if (isImeVisible) 0.dp else padding.calculateBottomPadding()
                )
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. SLEEK AI ASSISTANT BAR ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(emeraldSoftBg)
                    .border(1.dp, emeraldSoftBorder, RoundedCornerShape(16.dp))
                    .clickable { showAiGenerateDialog = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF059669)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Susun Tugas Otomatis (AI)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF059669))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Cepat",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Buat soal PG, uraian & kunci jawaban otomatis",
                            fontSize = 11.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = emeraldAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            // ── 2. UNIFIED WORKSPACE FORM (SASARAN, FORMAT, JUDUL & TENGGAT) ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = surfaceBg,
                border = BorderStroke(1.dp, surfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // A. Sasaran Rombel & Mapel (BottomSheet Selectors — NO vanilla Dropdown!)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "SASARAN ROMBEL & MATA PELAJARAN",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = emeraldAccent,
                            letterSpacing = 0.6.sp
                        )

                        ExecutiveBottomSheetSelector(
                            value = selectedClass,
                            label = "Rombel / Kelas Target *",
                            icon = Icons.Default.Group,
                            options = classOptions,
                            onSelect = { selectedClass = it },
                            sheetTitle = "Pilih Rombel Target",
                            sheetSubtitle = "Tugas akan diterbitkan ke seluruh siswa di rombel ini",
                            accentColor = emeraldAccent
                        )

                        val isSubLocked = state.isSubjectLocked && subjectOptions.size <= 1
                        ExecutiveBottomSheetSelector(
                            value = selectedSubject,
                            label = "Mata Pelajaran *",
                            icon = Icons.Default.School,
                            options = subjectOptions,
                            onSelect = { selectedSubject = it },
                            sheetTitle = "Pilih Mata Pelajaran",
                            sheetSubtitle = "Pilih mata pelajaran yang Anda ampu",
                            accentColor = emeraldAccent,
                            isLocked = isSubLocked,
                            lockedBadgeText = "Guru Pengampu",
                            onLockedClick = {
                                Toast.makeText(context, "Mata pelajaran dikunci untuk guru pengampu: $selectedSubject", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    HorizontalDivider(color = surfaceBorder)

                    // B. Metode Pengisian Tugas (Segmented Bar)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "METODE PENGERJAAN SISWA",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textSecondary,
                            letterSpacing = 0.6.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(subSurfaceBg)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "STRUCTURED_QUESTIONS" to "Soal PG & Esai",
                                "HOMEWORK_PR" to "Upload PR / Foto",
                                "HYBRID" to "Kombinasi"
                            ).forEach { (formatKey, label) ->
                                val isSelected = state.assignmentFormat == formatKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (isSelected) surfaceBg else Color.Transparent)
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) emeraldSoftBorder else Color.Transparent,
                                            shape = RoundedCornerShape(9.dp)
                                        )
                                        .clickable { viewModel.setAssignmentFormat(formatKey) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) emeraldAccent else textSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = surfaceBorder)

                    // C. Judul & Instruksi Tugas
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "INFORMASI & PETUNJUK TUGAS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textSecondary,
                            letterSpacing = 0.6.sp
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Judul Tugas *", fontSize = 12.sp) },
                            placeholder = { Text("Contoh: Evaluasi Bab 2: Teks Eksposisi", fontSize = 12.sp, color = textTertiary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldAccent,
                                unfocusedBorderColor = subSurfaceBorder,
                                focusedLabelColor = emeraldAccent,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            )
                        )

                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            label = { Text("Petunjuk / Rubrik Pengerjaan", fontSize = 12.sp) },
                            placeholder = { Text("Tuliskan instruksi pengerjaan atau rubrik penilaian singkat...", fontSize = 12.sp, color = textTertiary) },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldAccent,
                                unfocusedBorderColor = subSurfaceBorder,
                                focusedLabelColor = emeraldAccent,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            )
                        )
                    }

                    HorizontalDivider(color = surfaceBorder)

                    // D. Tenggat Waktu Pengumpulan
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BATAS WAKTU PENGUMPULAN",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textSecondary,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Maks $displayMaxScore Poin • KKTP 75",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = emeraldAccent
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                1 to "1 Hari",
                                3 to "3 Hari",
                                7 to "1 Minggu",
                                14 to "2 Minggu"
                            ).forEach { (days, label) ->
                                val isSelected = selectedDueDays == days
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) emeraldSoftBg else subSurfaceBg)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) emeraldAccent else subSurfaceBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedDueDays = days }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (isSelected) emeraldAccent else textSecondary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 3. DAFTAR BUTIR SOAL (CLEAN TABBED QUESTION WORKSPACE — ZERO NESTED CARD CLUTTER) ──
            val isStructuredMode = state.assignmentFormat == "STRUCTURED_QUESTIONS" || state.assignmentFormat == "HYBRID"
            if (isStructuredMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = surfaceBg,
                    border = BorderStroke(1.dp, surfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Top Header + Add Question Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Butir Soal (${state.questions.size} Soal)",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Total Skor: $totalQuestionPoints Poin",
                                    fontSize = 11.5.sp,
                                    color = emeraldAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(emeraldSoftBg)
                                        .border(1.dp, emeraldSoftBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.addMultipleChoiceQuestion()
                                            activeQuestionIndex = state.questions.size
                                        }
                                        .padding(horizontal = 11.dp, vertical = 7.dp)
                                ) {
                                    Text("+ Soal PG", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = emeraldAccent)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CyanAccent.copy(alpha = 0.14f))
                                        .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.addEssayQuestion()
                                            activeQuestionIndex = state.questions.size
                                        }
                                        .padding(horizontal = 11.dp, vertical = 7.dp)
                                ) {
                                    Text("+ Esai", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                                }
                            }
                        }

                        // Horizontal Question Number Selector Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            state.questions.forEachIndexed { idx, q ->
                                val isSelected = idx == activeQuestionIndex
                                val isMcq = q.questionType == "MULTIPLE_CHOICE"
                                val hasContent = q.questionText.isNotBlank()
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> Color(0xFF059669)
                                                hasContent -> emeraldSoftBg
                                                else -> subSurfaceBg
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = when {
                                                isSelected -> Color(0xFF059669)
                                                hasContent -> emeraldSoftBorder
                                                else -> subSurfaceBorder
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { activeQuestionIndex = idx }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "#${idx + 1} ${if (isMcq) "PG" else "Esai"}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isSelected -> Color.White
                                            hasContent -> emeraldAccent
                                            else -> textSecondary
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = surfaceBorder)

                        // Active Question Flat Editor (Zero Card-in-Card!)
                        val safeIdx = activeQuestionIndex.coerceIn(0, (state.questions.size - 1).coerceAtLeast(0))
                        val currentQuestion = state.questions.getOrNull(safeIdx)
                        if (currentQuestion != null) {
                            FlatQuestionEditor(
                                questionIndex = safeIdx,
                                totalQuestions = state.questions.size,
                                question = currentQuestion,
                                canDelete = state.questions.size > 1,
                                subSurfaceBg = subSurfaceBg,
                                subSurfaceBorder = subSurfaceBorder,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                textTertiary = textTertiary,
                                emeraldAccent = emeraldAccent,
                                emeraldSoftBg = emeraldSoftBg,
                                emeraldSoftBorder = emeraldSoftBorder,
                                onDelete = { viewModel.removeQuestion(safeIdx) },
                                onTextChange = { viewModel.updateQuestionText(safeIdx, it) },
                                onPointsChange = { viewModel.updateQuestionPoints(safeIdx, it) },
                                onChoiceTextChange = { cIdx, text -> viewModel.updateChoiceText(safeIdx, cIdx, text) },
                                onCorrectChoiceSelect = { cIdx -> viewModel.setCorrectChoice(safeIdx, cIdx) },
                                onPrevQuestion = if (safeIdx > 0) ({ activeQuestionIndex = safeIdx - 1 }) else null,
                                onNextQuestion = if (safeIdx < state.questions.lastIndex) ({ activeQuestionIndex = safeIdx + 1 }) else null,
                            )
                        }
                    }
                }
            }

            if (!state.error.isNullOrBlank()) {
                Surface(
                    color = NeonError.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeonError.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = NeonError, modifier = Modifier.size(18.dp))
                        Text(state.error!!, color = NeonError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FlatQuestionEditor(
    questionIndex: Int,
    totalQuestions: Int,
    question: AssignmentQuestion,
    canDelete: Boolean,
    subSurfaceBg: Color,
    subSurfaceBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textTertiary: Color,
    emeraldAccent: Color,
    emeraldSoftBg: Color,
    emeraldSoftBorder: Color,
    onDelete: () -> Unit,
    onTextChange: (String) -> Unit,
    onPointsChange: (Int) -> Unit,
    onChoiceTextChange: (Int, String) -> Unit,
    onCorrectChoiceSelect: (Int) -> Unit,
    onPrevQuestion: (() -> Unit)?,
    onNextQuestion: (() -> Unit)?,
) {
    val isMultipleChoice = question.questionType == "MULTIPLE_CHOICE"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Question Top Metadata Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Butir Soal #${questionIndex + 1} dari $totalQuestions",
                    color = textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isMultipleChoice) emeraldSoftBg else CyanAccent.copy(alpha = 0.14f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        if (isMultipleChoice) "Pilihan Ganda" else "Uraian Esai",
                        color = if (isMultipleChoice) emeraldAccent else CyanAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Bobot:", color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(subSurfaceBg)
                        .border(1.dp, subSurfaceBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = (question.points ?: 10).toString(),
                        onValueChange = { newVal ->
                            val p = newVal.filter { it.isDigit() }.take(3).toIntOrNull() ?: 10
                            onPointsChange(p)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = textPrimary
                        ),
                        cursorBrush = SolidColor(emeraldAccent),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    )
                }

                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Soal",
                            tint = NeonError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Clean Question Text Input
        OutlinedTextField(
            value = question.questionText,
            onValueChange = onTextChange,
            placeholder = {
                Text(
                    if (isMultipleChoice) "Ketik naskah pertanyaan pilihan ganda di sini..." else "Ketik instruksi atau pertanyaan uraian esai di sini...",
                    fontSize = 12.5.sp,
                    color = textTertiary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = emeraldAccent,
                unfocusedBorderColor = subSurfaceBorder,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                unfocusedContainerColor = subSurfaceBg.copy(alpha = 0.5f),
                focusedContainerColor = subSurfaceBg.copy(alpha = 0.5f)
            )
        )

        // Clean Flat Multiple Choice Rows (No nested OutlinedTextField inside boxes!)
        if (isMultipleChoice) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Pilihan Jawaban",
                        color = textSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Ketuk huruf A/B/C/D untuk memilih Kunci Jawaban",
                        color = emeraldAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                question.choices.forEachIndexed { cIdx, choice ->
                    val isCorrect = choice.isCorrect == true
                    val letter = ('A' + cIdx).toString()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCorrect) emeraldSoftBg else subSurfaceBg)
                            .border(
                                width = if (isCorrect) 1.5.dp else 1.dp,
                                color = if (isCorrect) emeraldAccent else subSurfaceBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tappable Letter Badge to select Answer Key
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isCorrect) Color(0xFF059669) else Color.Transparent)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isCorrect) Color(0xFF059669) else textTertiary,
                                    shape = CircleShape
                                )
                                .clickable { onCorrectChoiceSelect(cIdx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                color = if (isCorrect) Color.White else textSecondary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }

                        // Direct Clean Text Input (No inner border!)
                        Box(modifier = Modifier.weight(1f)) {
                            if (choice.choiceText.isEmpty()) {
                                Text(
                                    text = "Ketik opsi jawaban $letter...",
                                    color = textTertiary,
                                    fontSize = 12.5.sp
                                )
                            }
                            BasicTextField(
                                value = choice.choiceText,
                                onValueChange = { onChoiceTextChange(cIdx, it) },
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Medium,
                                    color = textPrimary,
                                    lineHeight = 18.sp
                                ),
                                cursorBrush = SolidColor(emeraldAccent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Answer Key Pill Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCorrect) Color(0xFF059669) else Color.Transparent)
                                .clickable { onCorrectChoiceSelect(cIdx) }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isCorrect) "✓ Kunci" else "Pilih",
                                color = if (isCorrect) Color.White else textTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyanAccent.copy(alpha = 0.1f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.EditNote, null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                Text(
                    "Siswa akan mengetik jawaban uraian langsung pada aplikasi dan guru dapat memberi nilai pada menu Koreksi Tugas.",
                    color = textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // Quick Prev / Next Question Navigation Footer
        if (totalQuestions > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onPrevQuestion?.invoke() },
                    enabled = onPrevQuestion != null,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "← Soal Sebelumnya",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (onPrevQuestion != null) textSecondary else textTertiary.copy(alpha = 0.4f)
                    )
                }
                TextButton(
                    onClick = { onNextQuestion?.invoke() },
                    enabled = onNextQuestion != null,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Soal Berikutnya →",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (onNextQuestion != null) emeraldAccent else textTertiary.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AssignmentReviewAndPublishScreen(
    title: String,
    description: String,
    instructions: String,
    selectedClass: String,
    selectedSubject: String,
    dueDays: Int,
    maxScore: String,
    assignmentFormat: String,
    questions: List<AssignmentQuestion>,
    isLoading: Boolean,
    onBackToEdit: () -> Unit,
    onConfirmPublish: (isScheduled: Boolean, releaseAtIso: String?) -> Unit,
) {
    BackHandler(onBack = onBackToEdit)
    val isDark = LocalIsDarkTheme.current
    val pageBg = if (isDark) Color(0xFF09090B) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF111113) else Color.White
    val cardBorder = if (isDark) Color(0xFF232326) else Color(0xFFE2E8F0)
    val subSurfaceBg = if (isDark) Color(0xFF18181B) else Color(0xFFF1F5F9)
    val subSurfaceBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val emeraldAccent = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val emeraldSoftBg = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
    val emeraldSoftBorder = if (isDark) Color(0xFF059669).copy(alpha = 0.45f) else Color(0xFFA7F3D0)

    var isScheduled by remember { mutableStateOf(false) }
    var scheduledDate by remember { mutableStateOf(java.time.LocalDate.now().plusDays(1)) }
    var scheduledTime by remember { mutableStateOf(java.time.LocalTime.of(7, 0)) }

    Scaffold(
        containerColor = pageBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ExecutiveTopBar(
                title = "Pratinjau Penugasan",
                subtitle = "Konfirmasi akhir sebelum diterbitkan ke kelas",
                onBack = onBackToEdit
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBackToEdit,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Tugas", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val releaseAtIso = if (isScheduled) {
                                buildScheduledUtcIso(scheduledDate, scheduledTime)
                            } else null
                            onConfirmPublish(isScheduled, releaseAtIso)
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.4f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScheduled) NeonWarning else Color(0xFF059669),
                            contentColor = if (isScheduled) Color(0xFF0F172A) else Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                if (isScheduled) Icons.Default.Schedule else Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isScheduled) "Jadwalkan Tugas" else "Terbitkan Sekarang",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Unified Publish Summary Card
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Metadata Pills Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(emeraldSoftBg)
                                .border(0.5.dp, emeraldSoftBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(selectedClass, color = emeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(subSurfaceBg)
                                .border(0.5.dp, subSurfaceBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(selectedSubject, color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(subSurfaceBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Tenggat $dueDays Hari • $maxScore Poin", color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Text(
                        text = title.ifBlank { "(Tanpa Judul)" },
                        color = textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 23.sp
                    )

                    if (instructions.isNotBlank()) {
                        Text(
                            text = instructions,
                            color = textSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }

                    HorizontalDivider(color = cardBorder)

                    PublishingScheduleSelector(
                        isScheduled = isScheduled,
                        onScheduleModeChange = { isScheduled = it },
                        scheduledDate = scheduledDate,
                        onDateChange = { scheduledDate = it },
                        scheduledTime = scheduledTime,
                        onTimeChange = { scheduledTime = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (questions.isNotEmpty() && assignmentFormat != "HOMEWORK_PR") {
                Text(
                    "Ringkasan Butir Soal (${questions.size} Soal)",
                    color = textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Surface(
                    color = cardBg,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        questions.forEachIndexed { idx, q ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = cardBorder,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Soal #${idx + 1} • ${if (q.questionType == "MULTIPLE_CHOICE") "Pilihan Ganda" else "Esai"}",
                                        color = emeraldAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                    Text(
                                        "${q.points ?: 10} Poin",
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    q.questionText.ifBlank { "(Pertanyaan belum diisi)" },
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
                                )

                                if (q.choices.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        q.choices.forEachIndexed { cIdx, choice ->
                                            val letter = ('A' + cIdx).toString()
                                            val isCorrect = choice.isCorrect == true
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isCorrect) emeraldSoftBg else Color.Transparent)
                                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    "$letter.",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCorrect) emeraldAccent else textSecondary
                                                )
                                                Text(
                                                    choice.choiceText.ifBlank { "-" },
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCorrect) emeraldAccent else textPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isCorrect) {
                                                    Text(
                                                        "Kunci",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = emeraldAccent
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
