# 🚀 Catatan Rilis — Akselerasi-Edu Platform (v1.2.8 / Build 29)

**Tanggal Rilis:** 31 Oktober 2026  
**Status Build:** Production Stable  
**Target Distribusi:** Google Play Console (AAB) & Instalasi Langsung (APK)  

---

## 📦 Paket Rilis yang Tersedia
* **Android App Bundle (AAB):** `android/app/build/outputs/bundle/release/app-release.aab` *(~22.5 MB, Signed)*
* **Android APK Release:** `android/app/build/outputs/apk/release/app-release.apk` *(~27.7 MB, Signed)*

---

## 🌟 Sorotan Pembaruan Utama (Highlights)

### 1. 🎨 Modernisasi Tampilan Antarmuka (UI/UX Redesign)
* **Pintasan Sumber Daya Kelas (Detail Jadwal Guru):**
  * Tiga kartu aksi utama (*Beri Tugas, Buat Kuis, Unggah Materi*) kini mengadopsi desain *modern squircle* (`RoundedCornerShape(16.dp)`) dengan *soft elevation*, gradien aksen halus, kontainer ikon 44dp, dan *pill badge* yang rapi.
  * Header bagian diperbarui menjadi **"Kelola Sumber Daya Kelas"** dengan subjudul **"Pintasan Aksi"** untuk navigasi yang lebih intuitif.
* **Metrik Warga Belajar (Profil Detail Siswa):**
  * Empat kartu ringkasan (*Rata-rata, Kuis, Tugas, Hadir*) didesain ulang menggunakan konsep *Bento Grid*.
  * Tipografi angka kini menggunakan kontras tinggi dengan bobot ekstra (*ExtraBold/Black*), memberikan kejelasan informasi performa belajar siswa dalam satu lirikan.
* **Pratinjau Modul AI (Infografis Majalah):**
  * Blok bertipe `GAMBAR / DIAGRAM` kini langsung menampilkan visual gambar resolusi proporsional menggunakan *engine caching* Coil (`SubcomposeAsyncImage`) dengan status *loading indicator* dan penanganan *fallback* gambar.

---

### 2. 📢 Perbaikan Pusat Siaran Sekolah & Notifikasi (Kepala Sekolah)
* **Eliminasi Error Autentikasi (HTTP 401):**
  * Memperbaiki kendala kegagalan kirim siaran akibat rotasi rahasia token (`JWT_SECRET`) di server.
  * Mengganti tampilan kode teknis mentah (*raw code* seperti `HTTP 401`) dengan banner informasi ramah pengguna dalam bahasa Indonesia yang membimbing pengguna untuk memperbarui sesi login.
* **Integrasi Push Notification FCM & Real-Time SSE:**
  * Pengumuman resmi dari Kepala Sekolah kini secara instan disiarkan ke tiga target civitas (*Siswa, Dewan Guru, Orang Tua/Wali Murid*) baik sebagai notifikasi *in-app* maupun notifikasi *banner* perangkat via Firebase Cloud Messaging (FCM).

---

### 3. 🧠 Modul Ajar AI & Kurasi Visual Ensiklopedia (Backend)
* **Integrasi Wikimedia / Wikipedia API:**
  * Generator modul AI materi ajar kini mencari diagram dan gambar edukatif resmi melalui Wikipedia API (`fetch_wikipedia_educational_image`) untuk mata pelajaran dan topik kurikulum Indonesia.
  * Mencegah kemunculan foto stok acak atau tautan gambar rusak (*broken links*).
* **Filter Cerdas Buku SIBI Kemendikdasmen:**
  * Katalog buku teks Kurikulum Merdeka difilter secara otomatis di tingkat backend berdasarkan mata pelajaran yang diampu guru serta tingkat kelas/fase rombelnya (memenuhi kepatuhan **ADR-0011** dan **ADR-0008**).

---

### 4. 🔒 Pengamanan Kredensial Super Admin & Eliminasi Kebocoran
* **Enkripsi Kredensial via Environment Variables:**
  * Kredensial Super Admin platform (`SYSADMIN_EMAIL` dan `SYSADMIN_PASSWORD`) kini diisolasi sepenuhnya melalui *Environment Variables* terenkripsi di Railway.
* **Penghapusan Hardcoded Fallback & Default Password:**
  * Menghapus seluruh nilai sandi bawaan (`sysadmin123`) dari kode sumber backend. Jika variabel *environment* tidak terpasang, sistem langsung menolak login demi mencegah akses tanpa izin (*zero backdoor policy*).
  * Membersihkan template `.env.example` dari kredensial riil.

---

## 🛠️ Petunjuk Pembaruan untuk Pengguna

1. **Bagi Kepala Sekolah & Guru di Android:**
   * Unduh dan pasang file APK rilis terbaru.
   * Lakukan **Keluar (Logout)** 1 kali melalui menu profil, lalu **Masuk (Login)** kembali untuk mendapatkan token sesi baru yang tersinkronisasi dengan server.
2. **Bagi Super Admin Web:**
   * Masuk melalui portal `/system-admin/login` menggunakan email dan kata sandi baru yang telah Anda konfigurasikan di Railway Variables.

