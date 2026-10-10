package com.schoolos.android.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolos.android.domain.model.AcademicClass
import com.schoolos.android.domain.model.AcademicSubject
import com.schoolos.android.domain.model.MaterialType
import com.schoolos.android.domain.repository.AcademicRepository
import com.schoolos.android.domain.repository.LearningMaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID

enum class ContentBlockType { TEXT, IMAGE }

@Serializable
data class ContentBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: ContentBlockType,
    var content: String,
)

data class YoutubeVideoResult(
    val videoId: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val channelTitle: String,
)

data class MaterialCreatorUiState(
    val isLoading: Boolean = false,
    val isUploadingFile: Boolean = false,
    val isGeneratingAi: Boolean = false,
    val uploadedFileName: String? = null,
    val uploadedFileUrl: String? = null,
    val success: Boolean = false,
    val error: String? = null,
    val availableClasses: List<AcademicClass> = emptyList(),
    val availableSubjects: List<AcademicSubject> = emptyList(),
    val availableBooks: List<com.schoolos.android.domain.model.LibraryBook> = emptyList(),
    val recommendedBooks: List<com.schoolos.android.domain.model.LibraryBook> = emptyList(),
    val isLoadingAcademicData: Boolean = false,
    val isLoadingBooks: Boolean = false,
    
    // Youtube Search State
    val isSearchingYoutube: Boolean = false,
    val youtubeSearchResults: List<YoutubeVideoResult> = emptyList(),
    val youtubeSearchError: String? = null,
)

@HiltViewModel
class MaterialCreatorViewModel @Inject constructor(
    private val repository: LearningMaterialRepository,
    private val academicRepository: AcademicRepository,
    private val generatorRepository: com.schoolos.android.domain.repository.CurriculumGeneratorRepository,
) : ViewModel() {

    private val httpClient = OkHttpClient()

    private val _state = MutableStateFlow(MaterialCreatorUiState())
    val state = _state.asStateFlow()

    init {
        loadAcademicData()
        loadLibraryBooks()
    }

    val contentBlocks = MutableStateFlow<List<ContentBlock>>(listOf(ContentBlock(type = ContentBlockType.TEXT, content = "")))

    fun addBlock(type: ContentBlockType) {
        val current = contentBlocks.value.toMutableList()
        current.add(ContentBlock(type = type, content = ""))
        contentBlocks.value = current
    }

    fun updateBlock(id: String, newContent: String) {
        contentBlocks.value = contentBlocks.value.map {
            if (it.id == id) it.copy(content = newContent) else it
        }
    }

    fun removeBlock(id: String) {
        val current = contentBlocks.value.toMutableList()
        current.removeAll { it.id == id }
        if (current.isEmpty()) {
            current.add(ContentBlock(type = ContentBlockType.TEXT, content = ""))
        }
        contentBlocks.value = current
    }

    fun addBlockWithContent(type: ContentBlockType, content: String) {
        val current = contentBlocks.value.toMutableList()
        current.add(ContentBlock(type = type, content = content))
        contentBlocks.value = current
    }

    fun moveBlockUp(id: String) {
        val list = contentBlocks.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            contentBlocks.value = list
        }
    }

    fun moveBlockDown(id: String) {
        val list = contentBlocks.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            contentBlocks.value = list
        }
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
    }

    fun searchYoutube(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            _state.value = _state.value.copy(youtubeSearchResults = emptyList(), youtubeSearchError = null)
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _state.value = _state.value.copy(isSearchingYoutube = true, youtubeSearchError = null)
            try {
                // Gunakan YouTube API Key dari local.properties via BuildConfig
                val apiKey = BuildConfig.YOUTUBE_API_KEY
                if (apiKey.isBlank()) {
                    _state.value = _state.value.copy(
                        isSearchingYoutube = false,
                        youtubeSearchError = "YouTube API Key belum dikonfigurasi. Hubungi administrator."
                    )
                    return@launch
                }

                // Batasi pencarian max 10 untuk menghemat kuota API request (Cost saving)
                val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=10&q=${java.net.URLEncoder.encode(cleanQuery, "UTF-8")}&type=video&key=$apiKey"
                val request = Request.Builder().url(url).get().build()
                val response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val root = JSONObject(bodyString)
                    val items = root.optJSONArray("items") ?: org.json.JSONArray()
                    val results = mutableListOf<YoutubeVideoResult>()

                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val snippet = item.optJSONObject("snippet") ?: continue
                        val idObj = item.optJSONObject("id") ?: continue
                        val videoId = idObj.optString("videoId")
                        
                        if (videoId.isNotBlank()) {
                            results.add(
                                YoutubeVideoResult(
                                    videoId = videoId,
                                    title = unescapeHtml(snippet.optString("title")),
                                    description = unescapeHtml(snippet.optString("description")),
                                    channelTitle = unescapeHtml(snippet.optString("channelTitle")),
                                    thumbnailUrl = snippet.optJSONObject("thumbnails")?.optJSONObject("medium")?.optString("url")
                                        ?: snippet.optJSONObject("thumbnails")?.optJSONObject("default")?.optString("url") ?: ""
                                )
                            )
                        }
                    }
                    _state.value = _state.value.copy(
                        isSearchingYoutube = false,
                        youtubeSearchResults = results,
                        youtubeSearchError = if (results.isEmpty()) "Tidak ditemukan video untuk kata kunci \"$cleanQuery\"" else null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isSearchingYoutube = false,
                        youtubeSearchError = "Gagal mengambil data dari YouTube (HTTP ${response.code})"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSearchingYoutube = false,
                    youtubeSearchError = e.message ?: "Terjadi kesalahan koneksi"
                )
            }
        }
    }

    fun updateRecommendation(className: String?, subjectName: String?) {
        val books = _state.value.availableBooks
        if (books.isEmpty() || (className.isNullOrBlank() && subjectName.isNullOrBlank())) {
            _state.value = _state.value.copy(recommendedBooks = emptyList())
            return
        }

        val s = (subjectName ?: "").lowercase()
        val c = (className ?: "").lowercase()

        // Ekstraksi token kata kunci mapel dinamis untuk SEMUA mata pelajaran (tanpa stop words)
        val stopWords = setOf("dan", "atau", "untuk", "mata", "pelajaran", "mapel", "kelas", "tingkat", "fase", "sd", "smp", "sma", "smk", "wajib", "peminatan", "umum", "dasar", "lanjut", "lanjutan")
        val subjectTokens = s.split(Regex("[^a-zA-Z0-9]+"))
            .map { it.trim() }
            .filter { it.length >= 2 && !stopWords.contains(it) }

        val matches = books.filter { b ->
            val title = b.title.lowercase()
            val bookSubj = (b.subjectName ?: "").lowercase()

            val subjMatch = if (s.isBlank()) {
                true
            } else {
                // 1. Direct contains (nama lengkap mapel)
                val isDirect = title.contains(s) || bookSubj.contains(s) || (bookSubj.isNotBlank() && s.contains(bookSubj))

                // 2. Token keyword matching untuk SEMUA mata pelajaran secara dinamis
                val isTokenMatched = subjectTokens.isNotEmpty() && subjectTokens.any { token ->
                    title.contains(token) || bookSubj.contains(token)
                }

                // 3. Toleransi alias nasional untuk singkatan umum (PAI, PJOK, PPKn, IPAS, TIK, SBK)
                val isAliasMatched = when {
                    (s.contains("pai") || s.contains("agama")) ->
                        title.contains("agama") || bookSubj.contains("agama") || title.contains("budi pekerti") || bookSubj.contains("budi pekerti")
                    (s.contains("pjok") || s.contains("penjas") || s.contains("jasmani") || s.contains("olahraga")) ->
                        title.contains("pjok") || title.contains("jasmani") || title.contains("olahraga") || bookSubj.contains("pjok") || bookSubj.contains("jasmani")
                    (s.contains("pkn") || s.contains("ppkn") || s.contains("pancasila") || s.contains("kewarganegaraan")) ->
                        title.contains("pancasila") || title.contains("ppkn") || title.contains("kewarganegaraan") || bookSubj.contains("pancasila") || bookSubj.contains("ppkn")
                    (s.contains("tik") || s.contains("komputer") || s.contains("informatika") || s.contains("koding")) ->
                        title.contains("informatika") || title.contains("koding") || title.contains("komputer") || bookSubj.contains("informatika")
                    (s.contains("ipas") || s.contains("ipa") || s.contains("sains")) ->
                        title.contains("ipa") || title.contains("ipas") || title.contains("sains") || title.contains("alam") || bookSubj.contains("ipa") || bookSubj.contains("ipas")
                    (s.contains("ips") || s.contains("sosial")) ->
                        title.contains("ips") || title.contains("sosial") || bookSubj.contains("ips") || bookSubj.contains("sosial")
                    (s.contains("seni") || s.contains("sbk") || s.contains("budaya") || s.contains("prakarya")) ->
                        title.contains("seni") || title.contains("budaya") || title.contains("prakarya") || bookSubj.contains("seni") || bookSubj.contains("budaya")
                    else -> false
                }

                isDirect || isTokenMatched || isAliasMatched
            }

            val classNumber = Regex("\\d+").find(c)?.value?.toIntOrNull()
            val classMatch = if (classNumber != null) {
                b.classLevel == classNumber ||
                b.gradeLevelName?.contains(classNumber.toString()) == true ||
                title.contains("kelas $classNumber") ||
                (classNumber == 1 && (title.contains("kelas i") || title.contains("kelas 1"))) ||
                (classNumber == 2 && (title.contains("kelas ii") || title.contains("kelas 2"))) ||
                (classNumber == 3 && (title.contains("kelas iii") || title.contains("kelas 3"))) ||
                (classNumber == 4 && (title.contains("kelas iv") || title.contains("kelas 4"))) ||
                (classNumber == 5 && (title.contains("kelas v") || title.contains("kelas 5"))) ||
                (classNumber == 6 && (title.contains("kelas vi") || title.contains("kelas 6"))) ||
                (classNumber == 7 && (title.contains("kelas vii") || title.contains("kelas 7"))) ||
                (classNumber == 8 && (title.contains("kelas viii") || title.contains("kelas 8"))) ||
                (classNumber == 9 && (title.contains("kelas ix") || title.contains("kelas 9"))) ||
                (classNumber == 10 && (title.contains("kelas x") || title.contains("kelas 10"))) ||
                (classNumber == 11 && (title.contains("kelas xi") || title.contains("kelas 11"))) ||
                (classNumber == 12 && (title.contains("kelas xii") || title.contains("kelas 12")))
            } else if (c.contains("10") || c.contains(" x") || c.contains("x ")) {
                b.classLevel == 10 || title.contains("kelas x") || title.contains("kelas 10")
            } else if (c.contains("11") || c.contains(" xi") || c.contains("xi ")) {
                b.classLevel == 11 || title.contains("kelas xi") || title.contains("kelas 11")
            } else if (c.contains("12") || c.contains(" xii") || c.contains("xii ")) {
                b.classLevel == 12 || title.contains("kelas xii") || title.contains("kelas 12")
            } else if (c.contains("7") || c.contains("vii")) {
                b.classLevel == 7 || title.contains("kelas vii") || title.contains("kelas 7")
            } else if (c.contains("8") || c.contains("viii")) {
                b.classLevel == 8 || title.contains("kelas viii") || title.contains("kelas 8")
            } else if (c.contains("9") || c.contains("ix")) {
                b.classLevel == 9 || title.contains("kelas ix") || title.contains("kelas 9")
            } else true

            subjMatch && classMatch
        }.sortedByDescending { b ->
            val title = b.title.lowercase()
            if (!title.contains("panduan guru") && !title.contains("buku guru")) 2 else 1
        }

        _state.value = _state.value.copy(recommendedBooks = matches)
    }

    fun loadAcademicData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingAcademicData = true)
            val classesResult = academicRepository.getClasses()
            val subjectsResult = academicRepository.getSubjects()

            _state.value = _state.value.copy(
                isLoadingAcademicData = false,
                availableClasses = classesResult.getOrDefault(emptyList()),
                availableSubjects = subjectsResult.getOrDefault(emptyList()),
            )
        }
    }

    fun uploadFile(bytes: ByteArray, fileName: String, mimeType: String, onUploaded: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingFile = true, error = null)
            repository.uploadMaterialFile(bytes, fileName, mimeType)
                .onSuccess { url ->
                    _state.value = _state.value.copy(
                        isUploadingFile = false,
                        uploadedFileName = fileName,
                        uploadedFileUrl = url,
                    )
                    onUploaded?.invoke(url)
                }
                .onFailure { err ->
                    _state.value = _state.value.copy(
                        isUploadingFile = false,
                        error = err.message ?: "Gagal mengunggah file materi: ${err.localizedMessage}"
                    )
                }
        }
    }

    fun clearUploadedFile() {
        _state.value = _state.value.copy(
            uploadedFileName = null,
            uploadedFileUrl = null,
        )
    }

    fun createMaterial(
        title: String,
        description: String?,
        materialType: MaterialType,
        mediaUrl: String?,
        subject: String,
        classId: String?,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            // Serialize content blocks if type is Article or Image(Infografis)
            val finalContentBody = if (materialType == MaterialType.ARTICLE || materialType == MaterialType.IMAGE) {
                try {
                    Json.encodeToString(contentBlocks.value)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

            repository.createMaterial(
                title = title,
                description = description,
                materialType = materialType,
                contentBody = finalContentBody,
                mediaUrl = mediaUrl,
                subject = subject,
                classId = classId,
            ).onSuccess {
                _state.value = _state.value.copy(isLoading = false, success = true)
            }.onFailure { err ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = err.message ?: "Gagal membuat materi ajar."
                )
            }
        }
    }

    fun loadLibraryBooks(search: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingBooks = true)
            repository.getLibraryBooks(search = search)
                .onSuccess { books ->
                    _state.value = _state.value.copy(
                        isLoadingBooks = false,
                        availableBooks = books
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(isLoadingBooks = false)
                }
        }
    }

    fun assignReadingBook(
        book: com.schoolos.android.domain.model.LibraryBook,
        startPage: Int,
        endPage: Int,
        instructions: String?,
        classId: String,
        subjectId: String?
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val title = "Materi Bacaan: ${book.title} (Hal. $startPage–$endPage)"
            repository.assignReadingMaterial(
                bookId = book.id,
                title = title,
                instructions = instructions,
                classId = classId,
                subjectId = subjectId,
                startPage = startPage,
                endPage = endPage
            ).onSuccess {
                _state.value = _state.value.copy(isLoading = false, success = true)
            }.onFailure { err ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = err.message ?: "Gagal menugaskan materi buku perpustakaan."
                )
            }
        }
    }

    fun generateMaterialWithAi(
        mode: String,
        topic: String,
        gradeLevel: String = "Kelas 5 SD",
        subjectName: String,
        onSuccess: (title: String, description: String) -> Unit = { _, _ -> },
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isGeneratingAi = true, error = null)
            generatorRepository.generateMaterial(
                mode = mode,
                topic = topic,
                gradeLevel = gradeLevel,
                subjectName = subjectName
            ).onSuccess { res ->
                val newBlocks = res.blocks.map { b ->
                    ContentBlock(
                        id = b.id,
                        type = if (b.type.equals("IMAGE", ignoreCase = true)) ContentBlockType.IMAGE else ContentBlockType.TEXT,
                        content = b.content
                    )
                }
                if (newBlocks.isNotEmpty()) {
                    contentBlocks.value = newBlocks
                }
                _state.value = _state.value.copy(isGeneratingAi = false)
                onSuccess(res.title, res.description)
            }.onFailure { err ->
                _state.value = _state.value.copy(isGeneratingAi = false, error = err.message)
                onError(err.message ?: "Gagal menyusun materi dengan AI NVIDIA NIM")
            }
        }
    }

    fun resetState() {
        _state.value = _state.value.copy(
            isLoading = false,
            isUploadingFile = false,
            isGeneratingAi = false,
            uploadedFileName = null,
            uploadedFileUrl = null,
            success = false,
            error = null,
        )
    }
}
