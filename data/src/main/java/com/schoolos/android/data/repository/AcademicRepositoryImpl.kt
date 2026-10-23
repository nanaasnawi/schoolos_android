package com.schoolos.android.data.repository

import com.schoolos.android.data.mapper.toDomain
import com.schoolos.android.data.remote.SchoolOsApi
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.ClassStudent
import com.schoolos.android.domain.model.OfficialStudentBiodata
import com.schoolos.android.domain.repository.AcademicRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcademicRepositoryImpl @Inject constructor(
    private val api: SchoolOsApi,
) : AcademicRepository {

    override suspend fun getClasses(): Result<List<AcademicClass>> = runCatching {
        val response = api.getClasses(pageSize = 100)
        (response.data ?: emptyList()).map { dto ->
            AcademicClass(
                id = dto.id,
                name = dto.name,
                gradeLevelId = dto.gradeLevelId,
                homeroomTeacherId = dto.homeroomTeacherId,
            )
        }
    }

    override suspend fun getSubjects(): Result<List<AcademicSubject>> = runCatching {
        val response = api.getSubjects()
        (response.data ?: emptyList()).map { dto ->
            AcademicSubject(
                id = dto.id,
                name = dto.name,
                code = dto.code,
            )
        }
    }

    override suspend fun getClassStudents(className: String): Result<List<ClassStudent>> = runCatching {
        val trimmed = className.trim()
        val isUuid = try {
            java.util.UUID.fromString(trimmed)
            true
        } catch (_: Exception) {
            false
        }
        val response = if (isUuid) {
            api.getClassStudents(classId = trimmed)
        } else if (trimmed.equals("ALL", ignoreCase = true) || trimmed.isBlank()) {
            api.getClassStudents(className = null, classId = null)
        } else {
            api.getClassStudents(className = trimmed)
        }
        if (!response.success) {
            throw Exception(response.error?.message ?: "Gagal memuat daftar murid")
        }
        (response.data ?: emptyList()).map { dto ->
            ClassStudent(
                id = dto.id.toString(),
                fullName = dto.fullName,
                nisn = dto.nisn,
                gender = dto.gender,
                status = dto.status,
                noHp = dto.noHp,
                email = dto.email,
                classId = dto.classId.toString(),
                className = dto.className,
            )
        }
    }

    override suspend fun getStudentProgress(studentId: String): Result<com.schoolos.android.domain.model.Progress> = runCatching {
        val response = api.getStudentProgress(studentId.trim())
        val data = response.data ?: throw Exception(response.error?.message ?: "Gagal memuat progres akademik murid.")
        data.toDomain()
    }

    override suspend fun getStudentProfile(studentId: String): Result<OfficialStudentBiodata> = runCatching {
        val response = api.getStudentProfile(studentId.trim())
        val data = response.data ?: throw Exception(
            response.error?.message ?: "Gagal memuat profil resmi siswa Dapodik."
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
}
