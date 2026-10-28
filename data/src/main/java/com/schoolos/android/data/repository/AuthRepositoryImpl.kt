package com.schoolos.android.data.repository

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.schoolos.android.core.auth.AuthManager
import com.schoolos.android.core.database.AppDatabase
import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.data.remote.dto.LoginRequest
import com.schoolos.android.domain.model.OfficialStudentBiodata
import com.schoolos.android.domain.model.OfficialTeacherBiodata
import com.schoolos.android.domain.model.SchoolContactInfo
import com.schoolos.android.domain.model.StudentDocumentItem
import com.schoolos.android.domain.model.StudentDossier
import com.schoolos.android.domain.model.User
import com.schoolos.android.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val api: SchoolOsApi,
    private val authManager: AuthManager,
    private val appDatabase: AppDatabase,
    private val chatManager: com.schoolos.android.core.chat.ChatManager,
) : AuthRepository {

    private suspend fun resolveClassId(explicitClassId: String?, className: String?): String? {
        if (!explicitClassId.isNullOrBlank()) return explicitClassId
        if (className.isNullOrBlank()) return null
        return try {
            val classesResp = api.getClasses(pageSize = 100)
            classesResp.data?.find { it.name.equals(className, ignoreCase = true) }?.id
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun login(username: String, password: String): Result<User> = runCatching {
        val response = api.login(LoginRequest(username = username, password = password))
        val data = response.data ?: throw Exception(
            response.error?.message ?: "Login gagal. Silakan periksa kredensial email/username & password Anda."
        )

        authManager.saveSession(
            accessToken = data.accessToken,
            refreshToken = data.refreshToken,
            userId = data.userId,
            tenantId = data.tenantId,
            name = data.name,
            email = data.email,
            role = data.role,
            identifier = data.identifier,
            className = data.className,
            classId = resolveClassId(data.classId, data.className),
            childName = data.childName,
            childId = data.childId,
            avatarUrl = data.avatarUrl,
            subject = data.subject,
        )

        if (!data.schoolName.isNullOrBlank()) {
            authManager.saveSchoolProfile(name = data.schoolName, logoUrl = data.schoolLogoUrl)
        } else {
            try {
                val profileResponse = api.getSchoolProfile()
                profileResponse.data?.let { profile ->
                    authManager.saveSchoolProfile(name = profile.name, logoUrl = profile.logoUrl)
                }
            } catch (e: Exception) {
                // Log or ignore profile fetch failure so it doesn't break login
            }
        }

        User(id = data.userId, name = data.name, email = data.email, role = data.role, avatarUrl = data.avatarUrl)
    }

    override suspend fun loginWithQr(token: String): Result<User> = runCatching {
        val response = api.loginWithQr(com.schoolos.android.data.remote.dto.QrLoginRequest(token = token))
        val data = response.data ?: throw Exception(
            response.error?.message ?: "QR Login gagal. Pastikan QR Code valid dan masih aktif."
        )

        authManager.saveSession(
            accessToken = data.accessToken,
            refreshToken = data.refreshToken,
            userId = data.userId,
            tenantId = data.tenantId,
            name = data.name,
            email = data.email,
            role = data.role,
            identifier = data.identifier,
            className = data.className,
            classId = resolveClassId(data.classId, data.className),
            childName = data.childName,
            childId = data.childId,
            avatarUrl = data.avatarUrl,
            subject = data.subject,
        )

        if (!data.schoolName.isNullOrBlank()) {
            authManager.saveSchoolProfile(name = data.schoolName, logoUrl = data.schoolLogoUrl)
        } else {
            try {
                val profileResponse = api.getSchoolProfile()
                profileResponse.data?.let { profile ->
                    authManager.saveSchoolProfile(name = profile.name, logoUrl = profile.logoUrl)
                }
            } catch (e: Exception) {
                // Log or ignore profile fetch failure so it doesn't break login
            }
        }

        User(id = data.userId, name = data.name, email = data.email, role = data.role, avatarUrl = data.avatarUrl)
    }

    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            try {
                chatManager.clearOnLogout()
            } catch (e: Exception) {
                Timber.w(e, "Failed clearing ChatManager on logout")
            }

            try {
                // Purge all user-cached tables while preserving unsynced offline submissions in submission_queue
                appDatabase.gradeDao().clearAll()
                appDatabase.assignmentDao().clearAll()
                appDatabase.quizDao().clearAllQuestions()
                appDatabase.quizDao().clearAll()
                appDatabase.learningMaterialDao().clearAll()
                appDatabase.notificationDao().clearAll()
                appDatabase.sessionAttendanceDao().clearAll()
                appDatabase.sessionDao().clearAll()
                appDatabase.submissionQueueDao().clearSynced()
                Timber.i("Local user database tables purged on logout (unsynced submissions preserved)")
            } catch (e: Exception) {
                Timber.e(e, "Error clearing local database tables during logout")
            }

            // Clear user-scoped SharedPreferences caches
            val prefsToClear = listOf(
                "schoolos_reading_history",
                "schoolos_notif_sync",
                "schoolos_notif_dedup",
                "teacher_schedule_reminders",
            )
            prefsToClear.forEach { prefName ->
                try {
                    context.getSharedPreferences(prefName, android.content.Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply()
                } catch (_: Exception) {}
            }

            authManager.clearSession()
        }
    }

    override suspend fun refreshToken(): Result<String> = runCatching {
        val refreshToken = authManager.getRefreshToken() ?: throw Exception("No refresh token stored")
        val response = api.refreshToken(
            com.schoolos.android.data.remote.dto.RefreshTokenRequest(refreshToken)
        )
        val data = response.data ?: throw Exception("Token refresh failed")
        authManager.updateTokens(
            accessToken = data.accessToken,
            refreshToken = data.refreshToken,
        )
        data.accessToken
    }

    override suspend fun isLoggedIn(): Boolean = authManager.isLoggedIn

    override suspend fun getCurrentUser(): Result<User> = runCatching {
        val response = api.getCurrentUser()
        val data = response.data ?: throw Exception(
            response.error?.message ?: "Gagal memuat profil pengguna."
        )

        authManager.updateUserProfile(
            name = data.fullName,
            email = data.email,
            role = data.role,
            identifier = data.identifier,
            className = data.className,
            classId = resolveClassId(data.classId, data.className),
            childName = data.childName,
            childId = data.childId,
            avatarUrl = data.avatarUrl,
            subject = data.subject,
        )
        authManager.updateUserEditableDetails(
            email = data.email.takeIf { it.isNotBlank() },
            phone = data.phone?.takeIf { it.isNotBlank() },
            identifier = data.identifier?.takeIf { it.isNotBlank() },
            about = data.about?.takeIf { it.isNotBlank() },
        )

        var schoolName = data.schoolName
        var schoolLogo = data.schoolLogoUrl

        // Also fetch latest school profile from /api/v1/schools/profile to ensure dynamic tenant logo changes from Next.js are instantly captured!
        try {
            val schoolProfile = api.getSchoolProfile().data
            if (schoolProfile != null) {
                if (!schoolProfile.name.isNullOrBlank()) schoolName = schoolProfile.name
                if (!schoolProfile.logoUrl.isNullOrBlank()) schoolLogo = schoolProfile.logoUrl
            }
        } catch (_: Exception) {}

        if (!schoolName.isNullOrBlank()) {
            authManager.saveSchoolProfile(name = schoolName, logoUrl = schoolLogo)
        }

        User(id = data.id, name = data.fullName, email = data.email, role = data.role, avatarUrl = data.avatarUrl)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        val response = api.changePassword(
            com.schoolos.android.data.remote.dto.ChangePasswordRequestDto(
                currentPassword = currentPassword,
                newPassword = newPassword,
            )
        )
        if (!response.success) {
            throw Exception(response.error?.message ?: "Gagal memperbarui kata sandi.")
        }
    }

    override suspend fun getSchoolContactInfo(): Result<SchoolContactInfo> = runCatching {
        val response = api.getSchoolProfile()
        val data = response.data ?: throw Exception(
            response.error?.message ?: "Gagal memuat informasi kontak sekolah."
        )
        SchoolContactInfo(
            name = data.name,
            npsn = data.npsn?.takeIf { it.isNotBlank() },
            phone = data.phoneNumber?.takeIf { it.isNotBlank() },
            email = data.email?.takeIf { it.isNotBlank() },
            address = data.address?.takeIf { it.isNotBlank() },
            accreditation = data.accreditation?.takeIf { it.isNotBlank() },
            website = data.dapodikUrl?.takeIf { it.isNotBlank() },
        )
    }

    override suspend fun uploadAvatar(
        bytes: ByteArray,
        filename: String,
        mimeType: String
    ): Result<String> = runCatching {
        if (bytes.size > 5 * 1024 * 1024) {
            throw Exception("Ukuran foto profil melebihi batas maksimal 5 MB.")
        }
        val mediaType = mimeType.toMediaTypeOrNull()
        val requestBody = bytes.toRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("avatar", filename, requestBody)
        val response = api.uploadAvatar(part)
        val data = response.data ?: throw Exception(response.error?.message ?: "Gagal mengunggah foto profil.")
        val separator = if (data.avatarUrl.contains("?")) "&" else "?"
        val bustedUrl = "${data.avatarUrl}${separator}v=${System.currentTimeMillis()}"
        authManager.updateUserAvatar(bustedUrl)
        bustedUrl
    }

    override suspend fun updateProfile(
        fullName: String?,
        avatarUrl: String?,
        email: String?,
        phone: String?,
        identifier: String?,
        about: String?,
    ): Result<Unit> = runCatching {
        val response = api.updateProfile(
            com.schoolos.android.data.remote.dto.UpdateProfileRequestDto(
                fullName = fullName,
                avatarUrl = avatarUrl,
                email = email,
                phone = phone,
                identifier = identifier,
                about = about,
            )
        )
        if (!response.success) {
            throw Exception(response.error?.message ?: "Gagal memperbarui profil.")
        }
        authManager.updateUserEditableDetails(
            email = email,
            phone = phone,
            identifier = identifier,
            about = about,
        )
        if (!avatarUrl.isNullOrBlank()) {
            authManager.updateUserAvatar(avatarUrl)
        }
        try {
            getCurrentUser()
        } catch (_: Exception) {}
    }

    override suspend fun getMyQrBadge(): Result<String> = runCatching {
        val response = api.getMyQrBadge()
        val data = response.data ?: throw Exception(
            response.error?.message ?: "Gagal memuat QR Badge login."
        )
        if (data.rawToken.isBlank()) {
            throw Exception("QR Badge token kosong.")
        }
        data.rawToken
    }

    override suspend fun getOfficialBiodata(): Result<OfficialStudentBiodata> = runCatching {
        val response = api.getMyOfficialBiodata()
        val data = response.data ?: throw Exception(
            response.error?.message ?: "Gagal memuat biodata resmi Dapodik dari database."
        )
        OfficialStudentBiodata(
            id = data.id,
            nisn = data.nisn,
            fullName = data.fullName,
            nik = data.nik,
            nipd = data.nipd,
            gender = data.gender,
            placeOfBirth = data.placeOfBirth,
            dateOfBirth = data.dateOfBirth,
            religion = data.religion,
            className = data.className,
            alamatJalan = data.alamatJalan,
            rt = data.rt,
            rw = data.rw,
            dusun = data.dusun,
            kelurahan = data.kelurahan,
            kecamatan = data.kecamatan,
            kodePos = data.kodePos,
            noHp = data.noHp,
            email = data.email,
            namaAyah = data.namaAyah,
            pekerjaanAyah = data.pekerjaanAyah,
            penghasilanAyah = data.penghasilanAyah,
            namaIbu = data.namaIbu,
            pekerjaanIbu = data.pekerjaanIbu,
            penghasilanIbu = data.penghasilanIbu,
            namaWali = data.namaWali,
            pekerjaanWali = data.pekerjaanWali,
            penghasilanWali = data.penghasilanWali,
            penerimaKps = data.penerimaKps,
            noKps = data.noKps,
            penerimaKip = data.penerimaKip,
            nomorKip = data.nomorKip,
            namaDiKip = data.namaDiKip,
            nomorKks = data.nomorKks,
            layakPip = data.layakPip,
            alasanLayakPip = data.alasanLayakPip,
            bank = data.bank,
            nomorRekeningBank = data.nomorRekeningBank,
            rekeningAtasNama = data.rekeningAtasNama,
            fotoProfile = data.fotoProfile,
        )
    }

    override suspend fun getTeacherOfficialBiodata(): Result<OfficialTeacherBiodata> = runCatching {
        val currentAuth = authManager.authState.first()
        val currentUserId = currentAuth.userId ?: ""
        val currentUserEmail = currentAuth.email ?: ""
        val currentUserName = currentAuth.name ?: ""

        // Strategy 1: Call /api/v1/teachers/me/official-profile directly
        try {
            val response = api.getMyTeacherOfficialBiodata()
            if (response.success && response.data != null) {
                val data = response.data
                return@runCatching OfficialTeacherBiodata(
                    id = data.id,
                    tenantId = data.tenantId,
                    userId = data.userId ?: currentUserId,
                    nip = data.nip ?: currentAuth.identifier,
                    fullName = data.fullName.ifBlank { currentUserName },
                    nuptk = data.nuptk,
                    jk = data.jk,
                    tempatLahir = data.tempatLahir,
                    tanggalLahir = data.tanggalLahir,
                    statusKepegawaian = data.statusKepegawaian ?: "Pendidik & Tenaga Kependidikan",
                    jenisPtk = data.jenisPtk ?: if (currentAuth.isPrincipal) "Kepala Sekolah" else "Guru / Tutor",
                    agama = data.agama,
                    alamatJalan = data.alamatJalan,
                    rt = data.rt,
                    rw = data.rw,
                    dusun = data.dusun,
                    kelurahan = data.kelurahan,
                    kecamatan = data.kecamatan,
                    kodePos = data.kodePos,
                    noHp = data.noHp ?: currentAuth.phone,
                    email = data.email ?: currentUserEmail,
                    subject = data.subject ?: currentAuth.subject,
                    classesAssigned = data.classesAssigned ?: currentAuth.className,
                    status = data.status ?: "Active",
                    fotoProfile = data.fotoProfile ?: currentAuth.avatarUrl,
                )
            }
        } catch (_: Exception) {}

        // Strategy 2: Call /api/v1/teachers/me fallback
        try {
            val response = api.getMyTeacherProfile()
            if (response.success && response.data != null) {
                val data = response.data
                return@runCatching OfficialTeacherBiodata(
                    id = data.id,
                    tenantId = data.tenantId,
                    userId = data.userId ?: currentUserId,
                    nip = data.nip ?: currentAuth.identifier,
                    fullName = data.fullName.ifBlank { currentUserName },
                    nuptk = data.nuptk,
                    jk = data.jk,
                    tempatLahir = data.tempatLahir,
                    tanggalLahir = data.tanggalLahir,
                    statusKepegawaian = data.statusKepegawaian ?: "Pendidik & Tenaga Kependidikan",
                    jenisPtk = data.jenisPtk ?: if (currentAuth.isPrincipal) "Kepala Sekolah" else "Guru / Tutor",
                    agama = data.agama,
                    alamatJalan = data.alamatJalan,
                    rt = data.rt,
                    rw = data.rw,
                    dusun = data.dusun,
                    kelurahan = data.kelurahan,
                    kecamatan = data.kecamatan,
                    kodePos = data.kodePos,
                    noHp = data.noHp ?: currentAuth.phone,
                    email = data.email ?: currentUserEmail,
                    subject = data.subject ?: currentAuth.subject,
                    classesAssigned = data.classesAssigned ?: currentAuth.className,
                    status = data.status ?: "Active",
                    fotoProfile = data.fotoProfile ?: currentAuth.avatarUrl,
                )
            }
        } catch (_: Exception) {}

        // Strategy 3: Query /api/v1/teachers list and match by userId, email, or name
        try {
            val teachersResp = api.getTeachers(pageSize = 100)
            val teacherRecord = teachersResp.data?.find { t ->
                (currentUserId.isNotBlank() && t.userId == currentUserId) ||
                (currentUserEmail.isNotBlank() && t.email?.equals(currentUserEmail, ignoreCase = true) == true) ||
                (currentUserName.isNotBlank() && t.fullName.trim().equals(currentUserName.trim(), ignoreCase = true))
            }
            if (teacherRecord != null) {
                return@runCatching OfficialTeacherBiodata(
                    id = teacherRecord.id,
                    tenantId = teacherRecord.tenantId,
                    userId = teacherRecord.userId ?: currentUserId,
                    nip = teacherRecord.nip ?: currentAuth.identifier,
                    fullName = teacherRecord.fullName.ifBlank { currentUserName },
                    nuptk = teacherRecord.nuptk,
                    jk = teacherRecord.jk,
                    tempatLahir = teacherRecord.tempatLahir,
                    tanggalLahir = teacherRecord.tanggalLahir,
                    statusKepegawaian = teacherRecord.statusKepegawaian ?: "Pendidik & Tenaga Kependidikan",
                    jenisPtk = teacherRecord.jenisPtk ?: if (currentAuth.isPrincipal) "Kepala Sekolah" else "Guru / Tutor",
                    agama = teacherRecord.agama,
                    alamatJalan = teacherRecord.alamatJalan,
                    rt = teacherRecord.rt,
                    rw = teacherRecord.rw,
                    dusun = teacherRecord.dusun,
                    kelurahan = teacherRecord.kelurahan,
                    kecamatan = teacherRecord.kecamatan,
                    kodePos = teacherRecord.kodePos,
                    noHp = teacherRecord.noHp ?: currentAuth.phone,
                    email = teacherRecord.email ?: currentUserEmail,
                    subject = teacherRecord.subject ?: currentAuth.subject,
                    classesAssigned = teacherRecord.classesAssigned ?: currentAuth.className,
                    status = teacherRecord.status ?: "Active",
                    fotoProfile = teacherRecord.fotoProfile ?: currentAuth.avatarUrl,
                )
            }
        } catch (_: Exception) {}

        // Strategy 4: Resilient fallback from current session auth state so screen always renders
        OfficialTeacherBiodata(
            id = if (currentUserId.isNotBlank()) currentUserId else "gtk-current",
            tenantId = currentAuth.tenantId,
            userId = currentUserId.ifBlank { null },
            nip = currentAuth.identifier?.takeIf { it.isNotBlank() },
            fullName = if (currentUserName.isNotBlank()) currentUserName else "Pendidik / Tenaga Kependidikan",
            nuptk = null,
            jk = null,
            tempatLahir = null,
            tanggalLahir = null,
            statusKepegawaian = "Pendidik & Tenaga Kependidikan",
            jenisPtk = if (currentAuth.isPrincipal) "Kepala Sekolah" else "Guru / Tutor",
            agama = null,
            alamatJalan = null,
            rt = null,
            rw = null,
            dusun = null,
            kelurahan = null,
            kecamatan = null,
            kodePos = null,
            noHp = currentAuth.phone?.takeIf { it.isNotBlank() },
            email = currentUserEmail.ifBlank { null },
            subject = currentAuth.subject?.takeIf { it.isNotBlank() },
            classesAssigned = currentAuth.className?.takeIf { it.isNotBlank() },
            status = "Active",
            fotoProfile = currentAuth.avatarUrl,
        )
    }

    override suspend fun getStudentDossier(): Result<StudentDossier> = runCatching {
        val response = api.getMyStudentDossier()
        val data = response.data ?: throw Exception(response.error?.message ?: "Data berkas tidak ditemukan")
        StudentDossier(
            studentId = data.studentId,
            nisn = data.nisn,
            fullName = data.fullName,
            className = data.className,
            programTier = data.programTier,
            programTierLabel = data.programTierLabel,
            isComplete = data.isComplete,
            completenessPercentage = data.completenessPercentage,
            requiredCount = data.requiredCount,
            verifiedCount = data.verifiedCount,
            pendingCount = data.pendingCount,
            rejectedCount = data.rejectedCount,
            documents = data.documents.map { doc ->
                StudentDocumentItem(
                    id = doc.id,
                    documentType = doc.documentType,
                    label = doc.label,
                    description = doc.description,
                    isRequired = doc.isRequired,
                    status = doc.status,
                    fileUrl = doc.fileUrl,
                    fileName = doc.fileName,
                    fileSizeBytes = doc.fileSizeBytes,
                    mimeType = doc.mimeType,
                    verifiedAt = doc.verifiedAt,
                    rejectionReason = doc.rejectionReason,
                    notes = doc.notes,
                    updatedAt = doc.updatedAt
                )
            }
        )
    }

    override suspend fun uploadStudentDocument(
        documentType: String,
        bytes: ByteArray,
        filename: String,
        mimeType: String,
        notes: String?
    ): Result<StudentDocumentItem> = runCatching {
        if (bytes.size > 5 * 1024 * 1024) {
            throw Exception("Ukuran berkas persyaratan melebihi batas maksimal 5 MB.")
        }
        val typePart = documentType.toRequestBody("text/plain".toMediaTypeOrNull())
        val notesPart = notes?.toRequestBody("text/plain".toMediaTypeOrNull())
        val fileReqBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", filename, fileReqBody)

        val response = api.uploadMyStudentDocument(typePart, filePart, notesPart)
        val data = response.data ?: throw Exception(response.error?.message ?: "Gagal mengunggah berkas")
        StudentDocumentItem(
            id = data.id,
            documentType = data.documentType,
            label = data.label,
            description = data.description,
            isRequired = data.isRequired,
            status = data.status,
            fileUrl = data.fileUrl,
            fileName = data.fileName,
            fileSizeBytes = data.fileSizeBytes,
            mimeType = data.mimeType,
            verifiedAt = data.verifiedAt,
            rejectionReason = data.rejectionReason,
            notes = data.notes,
            updatedAt = data.updatedAt
        )
    }
}
