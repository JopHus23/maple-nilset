package com.mapherdf.maplenilset.ui.bili

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.mapherdf.maplenilset.bili.api.BiliApiService
import com.mapherdf.maplenilset.bili.api.BiliCookieStore
import com.mapherdf.maplenilset.bili.api.EncryptedBiliCookiePersistence
import com.mapherdf.maplenilset.bili.api.BiliHttpClientFactory
import com.mapherdf.maplenilset.bili.api.BiliWbiSigner
import com.mapherdf.maplenilset.bili.download.BiliDownloadEngine
import com.mapherdf.maplenilset.bili.download.BiliDownloadRequest
import com.mapherdf.maplenilset.bili.download.BiliSnapshotStore
import com.mapherdf.maplenilset.bili.download.BiliTaskState
import com.mapherdf.maplenilset.bili.auth.BiliLoginManager
import com.mapherdf.maplenilset.bili.auth.BiliLoginApi
import com.mapherdf.maplenilset.bili.auth.BiliLoginLevel
import com.mapherdf.maplenilset.bili.auth.BiliLoginState
import com.mapherdf.maplenilset.bili.auth.SystemBiliWebCookieStore
import com.mapherdf.maplenilset.BuildConfig
import com.mapherdf.maplenilset.bili.model.BiliAudioQuality
import com.mapherdf.maplenilset.bili.model.BiliQuality
import com.mapherdf.maplenilset.bili.model.BiliStreamSelector
import com.mapherdf.maplenilset.bili.model.BiliVideoReference
import com.mapherdf.maplenilset.const.BiliSettings
import com.mapherdf.maplenilset.data.repository.BiliNilRepository
import com.mapherdf.maplenilset.di.ApplicationScope
import java.io.File
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiliVideoEngineProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope,
) {
    @Volatile private var engine: BiliDownloadEngine? = null
    @Volatile private var apiService: BiliApiService? = null
    @Volatile private var cookieStore: BiliCookieStore? = null
    @Volatile private var client: OkHttpClient? = null
    @Volatile private var loginManager: BiliLoginManager? = null
    private val settingsPreferences = context.getSharedPreferences(
        BiliSettings.STORE_NAME,
        Context.MODE_PRIVATE,
    )
    private val _maxConcurrentTasks = MutableStateFlow(
        settingsPreferences.getInt(
            BiliSettings.CONCURRENT_TASKS_KEY,
            BiliSettings.DEFAULT_CONCURRENT_TASKS,
        ).coerceIn(BiliSettings.MIN_CONCURRENT_TASKS, BiliSettings.MAX_CONCURRENT_TASKS),
    )
    val maxConcurrentTasks: StateFlow<Int> = _maxConcurrentTasks.asStateFlow()

    fun getEngine(): BiliDownloadEngine { if (engine == null) init(); return engine!! }
    fun getApi(): BiliApiService { if (apiService == null) init(); return apiService!! }
    fun getLoginManager(): BiliLoginManager {
        init()
        return loginManager!!
    }
    fun isBiliLoggedIn(): Boolean {
        init()
        return cookieStore?.hasLoginCookie() == true
    }

    fun setMaxConcurrentTasks(value: Int) {
        val normalized = value.coerceIn(BiliSettings.MIN_CONCURRENT_TASKS, BiliSettings.MAX_CONCURRENT_TASKS)
        _maxConcurrentTasks.value = normalized
        settingsPreferences.edit()
            .putInt(BiliSettings.CONCURRENT_TASKS_KEY, normalized)
            .apply()
        init()
        externalScope.launch { engine?.setMaxConcurrentTasks(normalized) }
    }

    @Synchronized private fun init() {
        if (engine != null) return
        com.mapherdf.maplenilset.bili.api.BiliLogger.isEnabled = BuildConfig.DEBUG
        if (engine != null) return
        cookieStore = BiliCookieStore(EncryptedBiliCookiePersistence(context.applicationContext))
        val factory = BiliHttpClientFactory.create(cookieStore!!)
        client = factory.create()
        val signer = BiliWbiSigner(client!!)
        val api = BiliApiService(client!!, signer)
        val tempDir = File(context.cacheDir, "bili_nil_download")
        val snapshotFile = File(context.filesDir, "bili_dl_snapshots.json")
        apiService = api
        val merger = com.mapherdf.maplenilset.bili.mux.MediaMuxerMerger()
        val exporter = com.mapherdf.maplenilset.bili.store.BiliMediaExporter(context)
        engine = BiliDownloadEngine(
            client = client!!,
            apiService = api,
            snapshotStore = BiliSnapshotStore(snapshotFile),
            tempDirectory = tempDir,
            merger = merger,
            exporter = exporter,
            maxConcurrentTasks = _maxConcurrentTasks.value,
        )
        loginManager = BiliLoginManager(
            cookieStore = cookieStore!!,
            loginApi = BiliLoginApi(client!!),
            webCookieStore = SystemBiliWebCookieStore(),
        )
    }
}

@HiltViewModel
class BiliVideoViewModel @Inject constructor(
    private val provider: BiliVideoEngineProvider,
    private val coverRepository: BiliNilRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BiliVideoUiState())
    val uiState: StateFlow<BiliVideoUiState> = _uiState.asStateFlow()
    val maxConcurrentTasks: StateFlow<Int> = provider.maxConcurrentTasks
    private val _biliAvatar = MutableStateFlow<ImageBitmap?>(null)
    val biliAvatar: StateFlow<ImageBitmap?> = _biliAvatar.asStateFlow()
    private var observeJob: Job? = null
    private var resolveJob: Job? = null
    private var coverPreviewJob: Job? = null
    private var biliAvatarJob: Job? = null
    private var biliAvatarUrl: String? = null

    init {
        observeTasks()
        viewModelScope.launch {
            provider.getLoginManager().loginState.collect { state ->
                _uiState.update { current ->
                    if (state.level == BiliLoginLevel.LOGGED_OUT) {
                        current.copy(showLoginPrompt = true)
                    } else {
                        current.copy(showLoginPrompt = false)
                    }
                }
                updateBiliAvatar(state.avatarUrl)
            }
        }
    }

    fun updateInput(value: String) { _uiState.update { it.copy(inputText = value) } }

    fun setMaxConcurrentTasks(value: Int) = provider.setMaxConcurrentTasks(value)

    val biliLoginState: StateFlow<BiliLoginState>
        get() = provider.getLoginManager().loginState

    fun refreshBiliLoginState() {
        viewModelScope.launch { provider.getLoginManager().refreshLoginState() }
    }

    suspend fun importBiliWebViewCookies(): Boolean =
        provider.getLoginManager().importWebViewCookies().success

    fun logoutFromBili() {
        viewModelScope.launch { provider.getLoginManager().logout() }
    }

    private fun updateBiliAvatar(url: String?) {
        if (url == biliAvatarUrl) {
            return
        }
        biliAvatarUrl = url
        biliAvatarJob?.cancel()
        _biliAvatar.value = null
        if (url == null) {
            return
        }
        biliAvatarJob = viewModelScope.launch {
            runCatching {
                val file = coverRepository.downloadCoverByUrl(url, "bili_avatar")
                BiliCoverImageLoader.decode(file.file, maxDimension = 256)
            }.getOrNull()?.let { avatar ->
                if (biliAvatarUrl == url) {
                    _biliAvatar.value = avatar
                }
            }
        }
    }

    fun selectQuality(q: BiliQuality) {
        if (q.code !in _uiState.value.downloadableQualityCodes) {
            _uiState.update { it.copy(errorMessage = "该画质需要登录B站账号") }
            return
        }
        _uiState.update { it.copy(selectedQuality = q, errorMessage = null) }
    }

    fun resolve() {
        val s = _uiState.value
        if (s.isResolving || s.inputText.isBlank()) return
        resolveJob?.cancel()
        coverPreviewJob?.cancel()
        _uiState.update {
            it.copy(
                isResolving = true,
                errorMessage = null,
                videoInfo = null,
                coverPreview = null,
                isCoverLoading = false,
                availableQualities = emptyList(),
                downloadableQualityCodes = emptySet(),
            )
        }
        resolveJob = viewModelScope.launch {
            try {
                val bvid = resolveInput(s.inputText)
                val api = provider.getApi()
                val info = api.resolveVideo(bvid)
                val cid = info.pages.firstOrNull()?.cid ?: 0
                val play = api.fetchPlayUrl(bvid, cid)
                val qs = play.accept_quality.map { BiliQuality.fromCode(it) }.filter { it != BiliQuality.UNKNOWN }
                val downloadable = play.dash?.video?.map { it.id }?.toSet() ?: emptySet()
                val highestQuality = qs.filter { it.code in downloadable }.maxByOrNull { it.code } ?: BiliQuality.Q_360P
                _uiState.update {
                    it.copy(
                        isResolving = false, videoInfo = info,
                        availableQualities = qs, downloadableQualityCodes = downloadable,
                        resolvedCid = cid, resolvedTitle = info.title ?: "",
                        cachedPlayUrl = play,
                        selectedQuality = highestQuality,
                    )
                }
                info.pic?.let { coverUrl ->
                    coverPreviewJob = viewModelScope.launch {
                        try {
                            val preview = runCatching {
                                val coverFile = coverRepository.downloadCoverByUrl(coverUrl, bvid)
                                BiliCoverImageLoader.decode(coverFile.file)
                            }.getOrNull()
                            if (preview != null && _uiState.value.videoInfo?.bvid == info.bvid) {
                                _uiState.update { it.copy(coverPreview = preview) }
                            }
                        } finally {
                            _uiState.update { it.copy(isCoverLoading = false) }
                        }
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(isResolving = false, errorMessage = e.message) } }
        }
    }

    fun enqueueDownload() {
        val s = _uiState.value
        if (s.videoInfo == null || s.isEnqueuing) return
        if (s.selectedQuality.code !in s.downloadableQualityCodes) {
            _uiState.update { it.copy(errorMessage = "该画质需要登录 B 站账号后才能下载，当前未登录仅支持 360P/480P") }
            return
        }
        viewModelScope.launch {
            try {
                val bvid = resolveInput(s.inputText)
                // Use cached playUrl from resolve
                val selector = BiliStreamSelector()
                val audioP = listOf(BiliAudioQuality.A_192K, BiliAudioQuality.A_132K, BiliAudioQuality.A_64K)
                val play = s.cachedPlayUrl ?: throw IllegalStateException("PlayUrl not cached")
                val qualityP = listOf(s.selectedQuality, BiliQuality.Q_1080P, BiliQuality.Q_720P, BiliQuality.Q_480P, BiliQuality.Q_360P)
                val sel = selector.select(play.dash!!, qualityP, audioP, true)
                val req = BiliDownloadRequest(
                    reference = BiliVideoReference(bvid = bvid),
                    qualityPriority = listOf(s.selectedQuality.code, 64, 32, 16),
                    preResolvedCid = s.resolvedCid,
                    preResolvedVideoUrl = sel.videoStream.resolvedUrl,
                    preResolvedTitle = s.resolvedTitle,
                    preResolvedQualityLabel = sel.selectedQuality.label,
                    preResolvedAudioUrl = sel.audioStream?.resolvedUrl,
                )
                val id = provider.getEngine().enqueue(req)
                _uiState.update { it.copy(isEnqueuing = false, activeTaskId = id) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(isEnqueuing = false, errorMessage = e.message) } }
        }
    }

    fun pauseTask() { _uiState.value.activeTaskId?.let { provider.getEngine().pause(it) } }
    fun resumeTask() { _uiState.value.activeTaskId?.let { provider.getEngine().resume(it) } }
    fun cancelTask() { _uiState.value.activeTaskId?.let { provider.getEngine().cancel(it, true) }; _uiState.update { BiliVideoUiState(inputText = it.inputText) } }

    private fun observeTasks() {
        observeJob = viewModelScope.launch {
            provider.getEngine().tasks.collect { tasks ->
                val id = _uiState.value.activeTaskId ?: return@collect
                val t = tasks.firstOrNull { it.id == id } ?: return@collect
                _uiState.update { it.copy(taskState = t.state, taskProgress = t.progress, mergeOutcome = t.mergeOutcome, downgradeReason = t.downgradeReason, completedOutputPath = if (t.state == BiliTaskState.COMPLETED) t.outputUri else null) }
            }
        }
    }

    private suspend fun resolveInput(input: String): String {
        val t = input.trim()
        Regex("""BV([0-9A-Za-z]+)""", RegexOption.IGNORE_CASE).find(t)?.let { return "BV${it.groupValues[1]}" }
        Regex("""^av([0-9]+)$""", RegexOption.IGNORE_CASE).find(t)?.let { return it.groupValues[1] }
        Regex("""b23\.tv/([0-9A-Za-z]+)""", RegexOption.IGNORE_CASE).find(t)?.let { return provider.getApi().resolveShortLink(it.groupValues[1]) }
        return t
    }

    override fun onCleared() { observeJob?.cancel(); resolveJob?.cancel(); super.onCleared() }
}
