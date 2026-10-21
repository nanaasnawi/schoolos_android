package com.schoolos.android.domain.model

data class CanonicalHabit(
    val key: String,
    val number: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val exampleNote: String,
)

data class HabitEntry(
    val id: String,
    val studentId: String,
    val studentName: String? = null,
    val className: String? = null,
    val entryDate: String, // YYYY-MM-DD
    val academicYear: String = "2026/2027",
    val semester: String = "GANJIL",
    val habits: Map<String, Boolean> = emptyMap(),
    val habitsNotes: Map<String, String> = emptyMap(),
    val completedCount: Int = 0,
    val complianceRate: Double = 0.0,
    val verificationStatus: String = "PENDING", // PENDING, PARENT_VERIFIED, TEACHER_OVERRIDE, SYSTEM_EXPIRED
    val verifiedByRole: String? = null,
    val verifiedAt: String? = null,
    val parentFeedback: String? = null,
    val teacherNotes: String? = null,
    val overrideReason: String? = null,
)

object CanonicalHabitsList {
    val items = listOf(
        CanonicalHabit(
            key = "bangun_pagi",
            number = 1,
            title = "Bangun Pagi",
            subtitle = "Sebelum fajar / subuh",
            description = "Bangun pagi tepat waktu sebelum fajar/subuh dengan penuh semangat dan merapikan tempat tidur secara mandiri.",
            exampleNote = "Bangun jam 04.30 WIB dan merapikan kasur",
        ),
        CanonicalHabit(
            key = "beribadah",
            number = 2,
            title = "Beribadah",
            subtitle = "Tepat waktu & khusyuk",
            description = "Melaksanakan ibadah wajib tepat waktu, berdoa harian, dan membaca kitab suci sesuai keyakinan agama.",
            exampleNote = "Sholat Subuh & berdoa pagi",
        ),
        CanonicalHabit(
            key = "berolahraga",
            number = 3,
            title = "Berolahraga",
            subtitle = "Aktivitas fisik 15-30 menit",
            description = "Beraktivitas fisik atau berolahraga ringan minimal 15-30 menit untuk menjaga kebugaran jasmani.",
            exampleNote = "Senam pagi & jalan sehat 20 menit",
        ),
        CanonicalHabit(
            key = "makan_sehat",
            number = 4,
            title = "Makan Sehat & Bergizi",
            subtitle = "Gizi seimbang & air putih",
            description = "Mengonsumsi makanan bergizi seimbang (sayur, buah, protein, karbohidrat cukup) dan minum air putih yang memadai.",
            exampleNote = "Sarapan telur, sayur bening, dan buah pisang",
        ),
        CanonicalHabit(
            key = "gemar_belajar",
            number = 5,
            title = "Gemar Belajar & Membaca",
            subtitle = "Literasi mandiri",
            description = "Membaca buku literasi, mengulang modul pelajaran Kurikulum Merdeka, atau mengeksplorasi wawasan baru secara mandiri.",
            exampleNote = "Membaca modul Geografi & buku literasi 30 menit",
        ),
        CanonicalHabit(
            key = "bermasyarakat",
            number = 6,
            title = "Bermasyarakat & Gotong Royong",
            subtitle = "Membantu sesama & santun",
            description = "Membantu orang tua di rumah, bersosialisasi dengan santun, menjaga kebersihan lingkungan, atau gotong royong.",
            exampleNote = "Membantu menyapu teras dan menyiram bunga",
        ),
        CanonicalHabit(
            key = "tidur_tepat_waktu",
            number = 7,
            title = "Tidur Tepat Waktu",
            subtitle = "Istirahat cukup (tanpa begadang)",
            description = "Beristirahat malam tepat waktu (tidak begadang/main gadget berlebihan) demi kesehatan mental dan metabolisme tubuh.",
            exampleNote = "Tidur malam pukul 21.00 WIB",
        ),
    )
}
