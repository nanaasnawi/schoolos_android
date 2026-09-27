package com.schoolos.android.domain.model

/**
 * Lightweight model holding all school contact & identification fields
 * fetched from /api/v1/schools/profile. Used by the Help & Contact screen.
 */
data class SchoolContactInfo(
    val name: String = "",
    val npsn: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val accreditation: String? = null,
    val website: String? = null,
)
