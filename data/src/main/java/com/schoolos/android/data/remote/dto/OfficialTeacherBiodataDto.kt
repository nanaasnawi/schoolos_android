package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OfficialTeacherBiodataDto(
    val id: String,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val nip: String? = null,
    @SerialName("full_name") val fullName: String,
    val nuptk: String? = null,
    val jk: String? = null,
    @SerialName("tempat_lahir") val tempatLahir: String? = null,
    @SerialName("tanggal_lahir") val tanggalLahir: String? = null,
    @SerialName("status_kepegawaian") val statusKepegawaian: String? = null,
    @SerialName("jenis_ptk") val jenisPtk: String? = null,
    val agama: String? = null,
    @SerialName("alamat_jalan") val alamatJalan: String? = null,
    val rt: String? = null,
    val rw: String? = null,
    val dusun: String? = null,
    val kelurahan: String? = null,
    val kecamatan: String? = null,
    @SerialName("kode_pos") val kodePos: String? = null,
    @SerialName("no_hp") val noHp: String? = null,
    val email: String? = null,
    val subject: String? = null,
    @SerialName("classes_assigned") val classesAssigned: String? = null,
    val status: String? = null,
    @SerialName("foto_profile") val fotoProfile: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
)
