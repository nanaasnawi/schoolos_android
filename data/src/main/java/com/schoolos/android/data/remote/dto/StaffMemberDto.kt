package com.schoolos.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StaffMemberDto(
    val id: String,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("full_name") val fullName: String,
    val nuptk: String? = null,
    val jk: String? = null,
    @SerialName("tempat_lahir") val tempatLahir: String? = null,
    @SerialName("tanggal_lahir") val tanggalLahir: String? = null,
    val nip: String? = null,
    @SerialName("status_kepegawaian") val statusKepegawaian: String? = null,
    @SerialName("jenis_ptk") val jenisPtk: String? = null,
    val agama: String? = null,
    @SerialName("alamat_jalan") val alamatJalan: String? = null,
    @SerialName("no_hp") val noHp: String? = null,
    val email: String? = null,
    @SerialName("job_title") val jobTitle: String = "Tenaga Kependidikan",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)
