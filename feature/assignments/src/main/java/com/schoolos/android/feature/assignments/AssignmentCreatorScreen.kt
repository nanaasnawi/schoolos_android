package com.schoolos.android.feature.assignments

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolos.android.core.designsystem.*
import com.schoolos.android.domain.model.AssignmentChoice
import com.schoolos.android.domain.model.AssignmentQuestion
import java.time.Instant
import java.time.temporal.ChronoUnit

private val IndigoGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4338CA), Color(0xFF2563EB), Color(0xFF0891B2))
)
private val IndigoPrimary  = Color(0xFF4338CA)
private val IndigoAccent   = Color(0xFF2563EB)
private val CyanAccent     = Color(0xFF0891B2)

@OptIn(ExperimentalMaterial3Api::class)
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

    val classOptions = remember(state.availableClasses) {
        state.availableClasses.map { it.name }
    }
    val subjectOptions = remember(state.availableSubjects) {
        state.availableSubjects.map { it.name }
    }

    var selectedClass by remember { mutableStateOf(initialClass ?: "") }
    var selectedSubject by remember { mutableStateOf(initialSubject ?: "") }
    var isClassMenuExpanded by remember { mutableStateOf(false) }
    var isSubjectMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(classOptions) {
        if (selectedClass.isEmpty() && classOptions.isNotEmpty()) {
            selectedClass = classOptions.first()
        }
    }

    LaunchedEffect(subjectOptions) {
        if (selectedSubject.isEmpty() && subjectOptions.isNotEmpty()) {
            selectedSubject = subjectOptions.first()
        }
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var customMaxScore by remember { mutableStateOf("100") }
    var selectedDueDays by remember { mutableStateOf(7) }
    var showPreview by remember { mutableStateOf(false) }

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
            Toast.makeText(context, "✓ Tugas berhasil diterbitkan ke siswa!", Toast.LENGTH_SHORT).show()
            onSuccess()
            viewModel.resetState()
        }
    }

    Scaffold(
        containerColor = CosmicBlack,
        topBar = {
            ExecutiveTopBar(
                title = "Studio Penugasan Guru",
                subtitle = "Rancang & Terbitkan Tugas (PG & Esai)",
                onBack = onBack,
            )
        },
        bottomBar = {
            Surface(
                color = CosmicNavy,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder),
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
                        onClick = { showPreview = !showPreview },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TeacherNeon),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TeacherNeon.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            if (showPreview) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (showPreview) "Tutup" else "Preview",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Judul tugas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val fullDesc = "$selectedSubject • $selectedClass • $description"
                            val calculatedDueDate = Instant.now().plus(selectedDueDays.toLong(), ChronoUnit.DAYS).toString()
                            val targetClassId = state.availableClasses.find { it.name == selectedClass }?.id ?: selectedClass
                            val parsedScore = customMaxScore.toIntOrNull() ?: 100
                            viewModel.createAssignment(
                                title = title.trim(),
                                description = fullDesc.trim(),
                                instructions = instructions.ifBlank { "Kerjakan tugas secara teliti dan kumpulkan sebelum batas waktu berakhir." },
                                customMaxScore = parsedScore,
                                dueAt = calculatedDueDate,
                                classId = targetClassId
                            )
                        },
                        enabled = title.isNotBlank() && !state.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.4f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TeacherNeon,
                            contentColor = Color.White
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                        } else {
                            Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Terbitkan Tugas", fontWeight = FontWeight.Black, fontSize = 13.sp)
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
            // ── LIVE STUDENT PREVIEW CARD ──
            AnimatedVisibility(visible = showPreview) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(TeacherNeon.copy(alpha = 0.05f))
                        .border(1.dp, TeacherNeon.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TeacherNeon))
                                Text("Preview Tampilan Siswa", color = TeacherNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            AssignmentChip("Tenggat: $selectedDueDays Hari", NeonWarning)
                        }
                        Text(
                            text = title.ifBlank { "Judul Tugas Anda Akan Muncul Di Sini" },
                            color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black
                        )
                        Text(
                            text = description.ifBlank { "Deskripsi dan panduan ringkas pengerjaan tugas oleh guru..." },
                            color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            AssignmentChip(selectedSubject.ifBlank { "Mata Pelajaran" }, NeonBlue)
                            AssignmentChip(selectedClass.ifBlank { "Rombel" }, TeacherNeon)
                            if (state.assignmentFormat != "HOMEWORK_PR") {
                                val count = state.questions.size
                                AssignmentChip("$count Soal Terstruktur", CyanAccent)
                            }
                            AssignmentChip("Maks $displayMaxScore Poin", NeonSuccess)
                        }
                    }
                }
            }

            // ── SECTION 1: SASARAN & ROMBEL ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AssignmentSectionHeader(
                        number = 1,
                        title = "Sasaran & Rombel Belajar",
                        subtitle = "Tentukan rombel kelas dan mata pelajaran target",
                        numberColor = TeacherNeon
                    )

                    AssignmentDropdown(
                        value = selectedClass,
                        label = "Rombel Target *",
                        icon = Icons.Default.Group,
                        expanded = isClassMenuExpanded,
                        accentColor = TeacherNeon,
                        onExpand = { isClassMenuExpanded = true },
                        onDismiss = { isClassMenuExpanded = false },
                        options = classOptions,
                        onSelect = { selectedClass = it }
                    )

                    AssignmentDropdown(
                        value = selectedSubject,
                        label = "Mata Pelajaran *",
                        icon = Icons.Default.School,
                        expanded = isSubjectMenuExpanded,
                        accentColor = TeacherNeon,
                        onExpand = { isSubjectMenuExpanded = true },
                        onDismiss = { isSubjectMenuExpanded = false },
                        options = subjectOptions,
                        onSelect = { selectedSubject = it }
                    )
                }
            }

            // ── SECTION 2: FORMAT / METODE PENGISIAN TUGAS ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AssignmentSectionHeader(
                        number = 2,
                        title = "Format / Metode Pengisian Tugas",
                        subtitle = "Pilih cara siswa mengumpulkan jawaban tugas",
                        numberColor = NeonBlue
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("STRUCTURED_QUESTIONS", "📝 PG & Essay", "Dikerjakan online"),
                            Triple("HOMEWORK_PR", "📄 PR / Berkas", "Upload Foto/PDF"),
                            Triple("HYBRID", "🔄 Kombinasi", "Soal + Upload")
                        ).forEach { (formatKey, label, sublabel) ->
                            val isSelected = state.assignmentFormat == formatKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) TeacherNeon.copy(alpha = 0.12f) else CosmicSurface2
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) TeacherNeon else GlassBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setAssignmentFormat(formatKey) }
                                    .padding(vertical = 12.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (isSelected) TeacherNeon else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        sublabel,
                                        color = if (isSelected) TeacherNeon.copy(alpha = 0.8f) else TextTertiary,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── SECTION 3: KONTEN & PANDUAN TUGAS ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AssignmentSectionHeader(
                        number = 3,
                        title = "Konten & Panduan Tugas",
                        subtitle = "Tulis judul, deskripsi, dan instruksi pengerjaan",
                        numberColor = CyanAccent
                    )

                    AssignmentTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Judul Tugas *",
                        placeholder = "Contoh: Latihan Bab 3 Fisika: Hukum Gerak Newton",
                        icon = Icons.Default.Title,
                        accentColor = TeacherNeon
                    )

                    AssignmentTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Deskripsi / Latar Belakang Tugas",
                        placeholder = "Uraian singkat tujuan dan cakupan materi tugas...",
                        icon = Icons.Default.Notes,
                        accentColor = TeacherNeon,
                        minHeight = 80.dp
                    )

                    AssignmentTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = "Instruksi Langkah Pengerjaan",
                        placeholder = "Contoh: Kerjakan soal pilihan ganda berikut atau tulis penyelesaian pada buku tugas...",
                        icon = Icons.Default.FormatListNumbered,
                        accentColor = TeacherNeon,
                        minHeight = 90.dp
                    )
                }
            }

            // ── SECTION 4: DAFTAR BUTIR SOAL (Pilihan Ganda & Esai) ──
            val isStructuredMode = state.assignmentFormat == "STRUCTURED_QUESTIONS" || state.assignmentFormat == "HYBRID"
            if (isStructuredMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicNavy)
                        .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Section header with action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssignmentSectionHeader(
                                number = 4,
                                title = "Daftar Butir Soal (${state.questions.size} Butir)",
                                subtitle = "Total Poin: $totalQuestionPoints Poin",
                                numberColor = NeonSuccess
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { viewModel.addMultipleChoiceQuestion() },
                                    colors = ButtonDefaults.buttonColors(containerColor = TeacherNeon),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("+ PG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = { viewModel.addEssayQuestion() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("+ Esai", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        // List of questions
                        state.questions.forEachIndexed { qIdx, question ->
                            QuestionBuilderCard(
                                questionIndex = qIdx,
                                question = question,
                                canDelete = state.questions.size > 1,
                                onDelete = { viewModel.removeQuestion(qIdx) },
                                onTextChange = { viewModel.updateQuestionText(qIdx, it) },
                                onPointsChange = { viewModel.updateQuestionPoints(qIdx, it) },
                                onChoiceTextChange = { cIdx, text -> viewModel.updateChoiceText(qIdx, cIdx, text) },
                                onCorrectChoiceSelect = { cIdx -> viewModel.setCorrectChoice(qIdx, cIdx) }
                            )
                        }
                    }
                }
            }

            // ── SECTION 5: PENGUMPULAN & BATAS WAKTU ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicNavy)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AssignmentSectionHeader(
                        number = if (isStructuredMode) 5 else 4,
                        title = "Pengumpulan & Batas Waktu",
                        subtitle = "Atur deadline dan nilai maksimal",
                        numberColor = NeonWarning
                    )

                    // Deadline presets
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Batas Waktu Pengumpulan:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                1 to "+1 Hari",
                                3 to "+3 Hari",
                                7 to "+1 Minggu",
                                14 to "+2 Minggu"
                            ).forEach { (days, label) ->
                                val isSelected = selectedDueDays == days
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) TeacherNeon.copy(alpha = 0.15f) else CosmicSurface2
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) TeacherNeon else GlassBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedDueDays = days }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (isSelected) TeacherNeon else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Score presets & display
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Nilai Maksimal Tugas:",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isStructuredMode && totalQuestionPoints > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonSuccess.copy(alpha = 0.12f))
                                        .border(0.5.dp, NeonSuccess.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "Otomatis dari Butir Soal ($totalQuestionPoints Poin)",
                                        color = NeonSuccess,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (!isStructuredMode || totalQuestionPoints == 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("100", "80", "50").forEach { score ->
                                    val isSelected = customMaxScore == score
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) NeonSuccess.copy(alpha = 0.15f)
                                                else CosmicSurface2
                                            )
                                            .border(
                                                1.5.dp,
                                                if (isSelected) NeonSuccess else GlassBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { customMaxScore = score }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            score,
                                            color = if (isSelected) NeonSuccess else TextSecondary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(Modifier.weight(1f))

                                OutlinedTextField(
                                    value = customMaxScore,
                                    onValueChange = { customMaxScore = it },
                                    label = { Text("Custom", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(90.dp).height(54.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonSuccess,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedLabelColor = NeonSuccess,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                            }
                        }
                    }

                    // KKM info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonWarning.copy(alpha = 0.08f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = NeonWarning, modifier = Modifier.size(16.dp))
                        Text("KKM Kelulusan: 75 Poin", color = NeonWarning, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Error Display
            if (!state.error.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonError.copy(alpha = 0.12f))
                        .border(1.dp, NeonError.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = NeonError, modifier = Modifier.size(18.dp))
                        Text(state.error!!, color = NeonError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

// ─── Question Builder Card Component ──────────────────────────────────────────

@Composable
private fun QuestionBuilderCard(
    questionIndex: Int,
    question: AssignmentQuestion,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onTextChange: (String) -> Unit,
    onPointsChange: (Int) -> Unit,
    onChoiceTextChange: (Int, String) -> Unit,
    onCorrectChoiceSelect: (Int) -> Unit,
) {
    val isMultipleChoice = question.questionType == "MULTIPLE_CHOICE"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmicSurface2)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row: Question number, type badge, points, and delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Soal #${questionIndex + 1}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isMultipleChoice) IndigoAccent.copy(alpha = 0.15f) else CyanAccent.copy(alpha = 0.15f))
                            .border(0.5.dp, if (isMultipleChoice) IndigoAccent.copy(alpha = 0.3f) else CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (isMultipleChoice) "🔘 Pilihan Ganda (PG)" else "📝 Uraian / Esai",
                            color = if (isMultipleChoice) IndigoAccent else CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Poin:", color = TextTertiary, fontSize = 11.sp)
                    OutlinedTextField(
                        value = (question.points ?: 10).toString(),
                        onValueChange = { newVal ->
                            val p = newVal.filter { it.isDigit() }.toIntOrNull() ?: 10
                            onPointsChange(p)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(60.dp).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonSuccess,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
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

            // Question text field
            OutlinedTextField(
                value = question.questionText,
                onValueChange = onTextChange,
                label = { Text("Teks Pertanyaan Soal #${questionIndex + 1} *", fontSize = 11.sp) },
                placeholder = {
                    Text(
                        if (isMultipleChoice) "Tuliskan pertanyaan pilihan ganda..." else "Tuliskan narasi pertanyaan atau instruksi soal esai...",
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isMultipleChoice) IndigoAccent else CyanAccent,
                    unfocusedBorderColor = GlassBorder,
                    focusedLabelColor = if (isMultipleChoice) IndigoAccent else CyanAccent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    unfocusedLabelColor = TextTertiary
                )
            )

            // Multiple Choice Options Builder
            if (isMultipleChoice) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Opsi Jawaban (Klik radio untuk menandai Kunci Jawaban):",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    question.choices.forEachIndexed { cIdx, choice ->
                        val isCorrect = choice.isCorrect == true
                        val letter = ('A' + cIdx).toString()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCorrect) NeonSuccess.copy(alpha = 0.08f) else CosmicBlack)
                                .border(
                                    width = if (isCorrect) 1.2.dp else 0.8.dp,
                                    color = if (isCorrect) NeonSuccess else GlassBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = isCorrect,
                                onClick = { onCorrectChoiceSelect(cIdx) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = NeonSuccess,
                                    unselectedColor = TextTertiary
                                )
                            )

                            Text(
                                text = "$letter.",
                                color = if (isCorrect) NeonSuccess else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            OutlinedTextField(
                                value = choice.choiceText,
                                onValueChange = { onChoiceTextChange(cIdx, it) },
                                placeholder = { Text("Pilihan $letter", fontSize = 11.sp, color = TextTertiary) },
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isCorrect) NeonSuccess else IndigoAccent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            if (isCorrect) {
                                Text(
                                    "✓ Kunci",
                                    color = NeonSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            } else {
                // Essay Note
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyanAccent.copy(alpha = 0.08f))
                        .border(0.5.dp, CyanAccent.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(
                            "Siswa akan mengetikkan teks jawaban esai langsung pada aplikasi Android dan dapat dikoreksi nilainya oleh guru.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── Local UI Helpers ─────────────────────────────────────────────────────────

@Composable
private fun AssignmentChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .border(0.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AssignmentSectionHeader(number: Int, title: String, subtitle: String, numberColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(numberColor.copy(alpha = 0.12f))
                .border(1.dp, numberColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(number.toString(), color = numberColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
        Column {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = TextTertiary, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignmentDropdown(
    value: String,
    label: String,
    icon: ImageVector,
    expanded: Boolean,
    accentColor: Color,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (!expanded) onExpand() else onDismiss() }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = GlassBorder,
                focusedLabelColor = accentColor,
                focusedLeadingIconColor = accentColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                unfocusedLabelColor = TextTertiary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.background(CosmicNavy)
        ) {
            options.forEach { option ->
                val isSelected = option == value
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (isSelected) accentColor else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { onSelect(option); onDismiss() },
                    leadingIcon = if (isSelected) ({
                        Icon(Icons.Default.Check, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }) else null
                )
            }
        }
    }
}

@Composable
private fun AssignmentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    accentColor: Color,
    minHeight: androidx.compose.ui.unit.Dp = 56.dp
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, fontSize = 12.sp, color = TextTertiary) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = GlassBorder,
            focusedLabelColor = accentColor,
            focusedLeadingIconColor = accentColor,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            unfocusedLabelColor = TextTertiary
        )
    )
}
