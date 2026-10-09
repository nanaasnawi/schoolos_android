package com.schoolos.android.data.remote

import okhttp3.MultipartBody
import com.schoolos.android.data.remote.dto.*
import retrofit2.http.*

interface SchoolOsApi {

    // Auth
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @POST("auth/qr-login")
    suspend fun loginWithQr(@Body request: QrLoginRequest): ApiResponse<LoginResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): ApiResponse<RefreshTokenResponse>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequestDto): ApiResponse<Map<String, String>>

    @PUT("auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): ApiResponse<UpdateProfileResponseDto>

    @Multipart
    @POST("auth/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): ApiResponse<UploadAvatarResponse>

    // Public school info — no token required, used on login screen
    @GET("schools/info")
    suspend fun getSchoolPublicInfo(
        @Query("npsn") npsn: String? = null,
    ): ApiResponse<SchoolPublicInfoResponse>

    // School Profile (authenticated)
    @GET("schools/profile")
    suspend fun getSchoolProfile(): ApiResponse<SchoolProfileResponse>

    // Current User Profile (authenticated)
    @GET("auth/me")
    suspend fun getCurrentUser(): ApiResponse<UserDto>

    @GET("auth/qr-tokens/my-badge")
    suspend fun getMyQrBadge(): ApiResponse<QrBadgeDto>

    // Academic (Classes & Subjects)
    @GET("academic/classes")
    suspend fun getClasses(
        @Query("academic_year_id") academicYearId: String? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
    ): ApiResponse<List<ClassDto>>

    @GET("academic/subjects")
    suspend fun getSubjects(): ApiResponse<List<SubjectDto>>

    @GET("academic/classes/students")
    suspend fun getClassStudents(
        @Query("class_name") className: String? = null,
        @Query("class_id") classId: String? = null,
        @Query("search") search: String? = null,
    ): ApiResponse<List<ClassStudentDto>>

    // Materials
    @GET("learning/materials")
    suspend fun getMaterials(
        @Query("class_id") classId: String? = null,
        @Query("class_name") className: String? = null,
    ): ApiResponse<List<MaterialDto>>

    @Multipart
    @POST("learning/materials/upload")
    suspend fun uploadMaterialFile(
        @Part file: okhttp3.MultipartBody.Part,
    ): ApiResponse<FileUploadResponse>

    // Assignments
    @GET("learning/assignments")
    suspend fun getAssignments(@Query("class_id") classId: String? = null): ApiResponse<List<AssignmentDto>>

    @GET("learning/assignments/{id}")
    suspend fun getAssignment(@Path("id") id: String): ApiResponse<AssignmentDto>

    @POST("learning/assignments/{id}/submit")
    suspend fun submitAssignment(
        @Path("id") id: String,
        @Body request: SubmitAssignmentRequest,
        @Header("X-Idempotency-Key") idempotencyKey: String? = null,
    ): ApiResponse<SubmissionDto>

    @GET("learning/assignments/{id}/submissions")
    suspend fun getSubmissions(@Path("id") id: String): ApiResponse<List<SubmissionDto>>

    @POST("learning/assignments/{id}/submissions/{subId}/grade")
    suspend fun gradeSubmission(
        @Path("id") assignmentId: String,
        @Path("subId") submissionId: String,
        @Body request: GradeSubmissionRequest,
        @Header("X-Idempotency-Key") idempotencyKey: String? = null,
    ): ApiResponse<SubmissionDto>

    // Quizzes
    @GET("learning/quizzes")
    suspend fun getQuizzes(@Query("class_id") classId: String? = null): ApiResponse<List<QuizDto>>

    @GET("learning/quizzes/{id}")
    suspend fun getQuiz(@Path("id") id: String): ApiResponse<QuizDto>

    @GET("learning/quizzes/{id}/questions")
    suspend fun getQuizQuestions(@Path("id") id: String): ApiResponse<List<QuizQuestionDto>>

    @POST("learning/quizzes/{id}/questions")
    suspend fun addQuizQuestion(
        @Path("id") id: String,
        @Body request: CreateQuizQuestionRequestDto,
    ): ApiResponse<QuizQuestionDto>

    @POST("learning/quizzes/{id}/verify-token")
    suspend fun verifyQuizToken(
        @Path("id") id: String,
        @Body request: VerifyQuizTokenRequestDto,
    ): ApiResponse<VerifyQuizTokenResponseDto>

    @POST("learning/quizzes/{id}/attempts")
    suspend fun startAttempt(
        @Path("id") id: String,
        @Body request: StartAttemptRequest,
    ): ApiResponse<QuizAttemptDto>

    @GET("learning/quizzes/{id}/attempts")
    suspend fun getQuizAttempts(
        @Path("id") id: String,
    ): ApiResponse<List<QuizAttemptDto>>

    @GET("learning/quizzes/{id}/attempts/{attempt_id}")
    suspend fun getQuizAttempt(
        @Path("id") quizId: String,
        @Path("attempt_id") attemptId: String,
    ): ApiResponse<QuizAttemptDto>

    @POST("learning/quizzes/{id}/attempts/{attempt_id}/submit")
    suspend fun submitAttempt(
        @Path("id") quizId: String,
        @Path("attempt_id") attemptId: String,
        @Body request: SubmitAttemptRequest,
        @Header("X-Idempotency-Key") idempotencyKey: String? = null,
    ): ApiResponse<QuizAttemptDto>

    @POST("learning/quizzes/{id}/attempts/{attempt_id}/grade")
    suspend fun gradeQuizAttempt(
        @Path("id") quizId: String,
        @Path("attempt_id") attemptId: String,
        @Body request: GradeQuizAttemptRequest,
    ): ApiResponse<QuizAttemptDto>


    // Sessions
    @GET("learning/sessions")
    suspend fun getSessions(@Query("class_id") classId: String? = null): ApiResponse<List<LearningSessionDto>>

    @GET("learning/sessions/{id}")
    suspend fun getSession(@Path("id") id: String): ApiResponse<LearningSessionDto>

    @GET("learning/sessions/{id}/attendance")
    suspend fun getSessionAttendance(@Path("id") id: String): ApiResponse<List<SessionAttendanceDto>>

    @POST("learning/sessions/{id}/attendance")
    suspend fun recordAttendance(
        @Path("id") id: String,
        @Body request: RecordAttendanceRequestDto,
        @Header("X-Idempotency-Key") idempotencyKey: String? = null,
    ): ApiResponse<SessionAttendanceDto>

    @POST("learning/sessions/{id}/attendance/bulk")
    suspend fun recordAttendanceBulk(
        @Path("id") id: String,
        @Body request: List<RecordAttendanceRequestDto>,
        @Header("X-Idempotency-Key") idempotencyKey: String? = null,
    ): ApiResponse<List<SessionAttendanceDto>>

    @POST("learning/sessions/{id}/cancel")
    suspend fun cancelSession(
        @Path("id") id: String,
        @Body request: CancelSessionRequestDto,
    ): ApiResponse<LearningSessionDto>

    @POST("learning/sessions/{id}/substitute")
    suspend fun substituteTeacher(
        @Path("id") id: String,
        @Body request: SubstituteTeacherRequestDto,
    ): ApiResponse<LearningSessionDto>


    @GET("learning/materials/{id}")
    suspend fun getMaterial(@Path("id") id: String): ApiResponse<com.schoolos.android.data.remote.dto.MaterialDto>

    @POST("learning/materials/{id}/toggle-complete")
    suspend fun toggleMaterialComplete(
        @Path("id") id: String
    ): ApiResponse<com.schoolos.android.data.remote.dto.MaterialCompletionToggleDto>

    @GET("learning/materials/{id}/completions")
    suspend fun getMaterialCompletions(
        @Path("id") id: String
    ): ApiResponse<List<com.schoolos.android.data.remote.dto.MaterialStudentCompletionDto>>

    // Library Books (Katalog Buku Kurikulum & Perpustakaan Digital)
    @GET("learning/library/books")
    suspend fun getLibraryBooks(
        @Query("subject_id") subjectId: String? = null,
        @Query("grade_level_id") gradeLevelId: String? = null,
        @Query("search") search: String? = null,
    ): ApiResponse<List<com.schoolos.android.data.remote.dto.LibraryBookDto>>

    @POST("learning/library/assign")
    suspend fun assignReadingMaterial(
        @Body request: com.schoolos.android.data.remote.dto.AssignReadingMaterialRequestDto,
    ): ApiResponse<String>

    // Grades
    @GET("learning/assessment/gradebook")
    suspend fun getGradebook(
        @Query("class_id") classId: String? = null,
        @Query("subject_id") subjectId: String? = null,
    ): ApiResponse<List<GradeEntryDto>>

    // Progress
    @GET("learning/progress/me")
    suspend fun getMyProgress(): ApiResponse<ProgressDto>

    @GET("learning/progress/student/{student_id}")
    suspend fun getStudentProgress(
        @Path("student_id") studentId: String
    ): ApiResponse<ProgressDto>

    @GET("learning/progress/{student_id}/{class_id}/{subject_id}")
    suspend fun getProgress(
        @Path("student_id") studentId: String,
        @Path("class_id") classId: String,
        @Path("subject_id") subjectId: String,
    ): ApiResponse<ProgressDto>

    // Achievements
    @GET("learning/achievements/student/{student_id}")
    suspend fun getStudentAchievements(@Path("student_id") studentId: String): ApiResponse<List<AchievementDto>>

    // Notifications
    @GET("notifications")
    suspend fun getNotifications(@Query("page") page: Int = 1): ApiResponse<com.schoolos.android.data.remote.dto.PaginatedNotificationResponse>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): ApiResponse<UnreadCountResponse>

    @POST("learning/assignments")
    suspend fun createAssignment(@Body request: CreateAssignmentRequestDto): ApiResponse<AssignmentDto>

    @POST("learning/quizzes")
    suspend fun createQuiz(@Body request: CreateQuizRequestDto): ApiResponse<QuizDto>

    @POST("learning/materials")
    suspend fun createMaterial(@Body request: CreateMaterialRequestDto): ApiResponse<com.schoolos.android.data.remote.dto.MaterialDto>

    @PATCH("learning/materials/{id}")
    suspend fun updateMaterial(
        @Path("id") id: String,
        @Body request: UpdateMaterialRequestDto
    ): ApiResponse<com.schoolos.android.data.remote.dto.MaterialDto>

    @DELETE("learning/materials/{id}")
    suspend fun deleteMaterial(@Path("id") id: String): ApiResponse<Unit>

    @POST("learning/quizzes/{id}/publish")
    suspend fun publishQuiz(@Path("id") id: String): ApiResponse<QuizDto>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): ApiResponse<Unit>

    @PATCH("notifications/read-all")
    suspend fun markAllNotificationsRead(): ApiResponse<Unit>

    @POST("announcements")
    suspend fun createAnnouncement(@Body request: CreateAnnouncementRequest): ApiResponse<CreateAnnouncementResponse>

    @GET("announcements/{id}")
    suspend fun getAnnouncementById(@Path("id") id: String): ApiResponse<AnnouncementDto>

    // Analytics & Headmaster Compliance Monitoring
    @GET("analytics/schedule-compliance")
    suspend fun getScheduleCompliance(
        @Query("date") date: String? = null,
    ): ApiResponse<ScheduleComplianceResponseDto>
}

@kotlinx.serialization.Serializable
data class AnnouncementDto(
    val id: String,
    val title: String,
    val content: String,
    val category: String = "PENGUMUMAN",
    val target: String = "",
    val author: String = "Pihak Sekolah",
    @kotlinx.serialization.SerialName("is_pinned") val isPinned: Boolean = false,
    @kotlinx.serialization.SerialName("push_status") val pushStatus: Boolean = false,
    val date: String = "",
    @kotlinx.serialization.SerialName("created_at") val createdAt: String = "",
)

@kotlinx.serialization.Serializable
data class CreateAnnouncementResponse(
    val notifications_sent: Int = 0
)

@kotlinx.serialization.Serializable
data class CreateAnnouncementRequest(
    val title: String,
    val content: String,
    val category: String? = null,
    val target: String? = null,
    val author: String? = null,
    @kotlinx.serialization.SerialName("is_pinned") val isPinned: Boolean? = null,
    @kotlinx.serialization.SerialName("send_push") val sendPush: Boolean? = null
)

@kotlinx.serialization.Serializable
data class GradeSubmissionRequest(
    val score: Int,
    val feedback: String? = null,
)

@kotlinx.serialization.Serializable
data class UnreadCountResponse(val count: Int)

@kotlinx.serialization.Serializable
data class StartAttemptRequest(
    @kotlinx.serialization.SerialName("student_id") val studentId: String,
)

@kotlinx.serialization.Serializable
data class CreateAssignmentRequestDto(
    @kotlinx.serialization.SerialName("lesson_id") val lessonId: String? = null,
    val title: String,
    val description: String? = null,
    val instructions: String? = null,
    @kotlinx.serialization.SerialName("max_score") val maxScore: Int? = 100,
    @kotlinx.serialization.SerialName("due_at") val dueAt: String? = null,
    @kotlinx.serialization.SerialName("assignment_type") val assignmentType: String = "individual",
    @kotlinx.serialization.SerialName("class_id") val classId: String? = null,
    val questions: List<com.schoolos.android.data.remote.dto.AssignmentQuestionDto>? = null,
)

@kotlinx.serialization.Serializable
data class CreateQuizRequestDto(
    @kotlinx.serialization.SerialName("lesson_id") val lessonId: String? = null,
    val title: String,
    val description: String? = null,
    @kotlinx.serialization.SerialName("duration_minutes") val durationMinutes: Int? = 30,
    @kotlinx.serialization.SerialName("passing_score") val passingScore: Int = 70,
    @kotlinx.serialization.SerialName("max_attempts") val maxAttempts: Int = 1,
    @kotlinx.serialization.SerialName("class_id") val classId: String? = null,
)

@kotlinx.serialization.Serializable
data class CreateMaterialRequestDto(
    @kotlinx.serialization.SerialName("lesson_id") val lessonId: String? = null,
    @kotlinx.serialization.SerialName("material_type") val materialType: String,
    val title: String,
    val description: String? = null,
    @kotlinx.serialization.SerialName("storage_key") val storageKey: String? = null,
    @kotlinx.serialization.SerialName("external_url") val externalUrl: String? = null,
    @kotlinx.serialization.SerialName("order_index") val orderIndex: Int = 0,
    val visibility: String = "published",
    @kotlinx.serialization.SerialName("class_id") val classId: String? = null,
)

@kotlinx.serialization.Serializable
data class UpdateMaterialRequestDto(
    val title: String? = null,
    val description: String? = null,
    @kotlinx.serialization.SerialName("storage_key") val storageKey: String? = null,
    @kotlinx.serialization.SerialName("external_url") val externalUrl: String? = null,
    val visibility: String? = null,
    @kotlinx.serialization.SerialName("start_page") val startPage: Int? = null,
    @kotlinx.serialization.SerialName("end_page") val endPage: Int? = null,
)

@kotlinx.serialization.Serializable
data class CreateQuizQuestionRequestDto(
    @kotlinx.serialization.SerialName("question_text") val questionText: String,
    @kotlinx.serialization.SerialName("question_type") val questionType: String = "multiple_choice",
    val points: Int = 10,
    @kotlinx.serialization.SerialName("order_index") val orderIndex: Int = 1,
    @kotlinx.serialization.SerialName("image_url") val imageUrl: String? = null,
    val choices: List<CreateQuizOptionRequestDto> = emptyList(),
)

@kotlinx.serialization.Serializable
data class CreateQuizOptionRequestDto(
    @kotlinx.serialization.SerialName("choice_text") val choiceText: String,
    @kotlinx.serialization.SerialName("is_correct") val isCorrect: Boolean = false,
    @kotlinx.serialization.SerialName("order_index") val orderIndex: Int = 1,
)

@kotlinx.serialization.Serializable
data class FileUploadResponse(
    val url: String,
)
