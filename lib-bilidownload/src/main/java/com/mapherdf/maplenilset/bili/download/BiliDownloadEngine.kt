package com.mapherdf.maplenilset.bili.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import com.mapherdf.maplenilset.bili.api.BiliApiException
import com.mapherdf.maplenilset.bili.api.BiliApiService
import com.mapherdf.maplenilset.bili.api.BiliLogger
import com.mapherdf.maplenilset.bili.model.BiliAudioQuality
import com.mapherdf.maplenilset.bili.model.BiliMergeOutcome
import com.mapherdf.maplenilset.bili.model.BiliQuality
import com.mapherdf.maplenilset.bili.model.BiliStreamSelector
import com.mapherdf.maplenilset.bili.mux.BiliMerger
import com.mapherdf.maplenilset.bili.mux.BiliMergeResult
import com.mapherdf.maplenilset.bili.store.BiliMediaExporter
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class BiliDownloadEngine(
    private val client: OkHttpClient,
    private val apiService: BiliApiService,
    private val snapshotStore: BiliSnapshotStore,
    private val tempDirectory: File,
    private val merger: BiliMerger,
    private val exporter: BiliMediaExporter,
    maxConcurrentTasks: Int = DEFAULT_CONCURRENT_TASKS,
    externalScope: CoroutineScope? = null,
) {
    private val scope = externalScope ?: CoroutineScope(SupervisorJob())
    private val chunkDownloader = BiliChunkDownloader(client)
    private val streamSelector = BiliStreamSelector()
    private val tasksMutex = Mutex()
    private val concurrencyMutex = Mutex()
    private var currentMaxConcurrentTasks = maxConcurrentTasks.coerceIn(MIN_CONCURRENT_TASKS, MAX_CONCURRENT_TASKS)
    private val semaphore = Semaphore(MAX_CONCURRENT_TASKS).apply {
        repeat(MAX_CONCURRENT_TASKS - currentMaxConcurrentTasks) {
            check(tryAcquire())
        }
    }
    private val _tasks = MutableStateFlow<List<BiliDownloadTask>>(emptyList())
    val tasks: StateFlow<List<BiliDownloadTask>> = _tasks.asStateFlow()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    suspend fun enqueue(request: BiliDownloadRequest): String {
        val taskId = "task_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
        val task = BiliDownloadTask(taskId, request, "", BiliTaskState.QUEUED, BiliTaskProgress(), BiliQuality.UNKNOWN, BiliMergeOutcome.MERGED, null, null, null, true, System.currentTimeMillis())
        addTask(task)
        launchTask(taskId)
        return taskId
    }

    suspend fun setMaxConcurrentTasks(value: Int): Int {
        val target = value.coerceIn(MIN_CONCURRENT_TASKS, MAX_CONCURRENT_TASKS)
        concurrencyMutex.withLock {
            val current = currentMaxConcurrentTasks
            if (target == current) return target
            if (target > current) {
                repeat(target - current) { semaphore.release() }
            } else {
                repeat(current - target) { semaphore.acquire() }
            }
            currentMaxConcurrentTasks = target
        }
        return target
    }

    fun pause(taskId: String) { activeJobs[taskId]?.cancel(); scope.launch { updateTaskState(taskId, BiliTaskState.PAUSED) } }
    fun resume(taskId: String) { scope.launch { updateTaskState(taskId, BiliTaskState.QUEUED) }; launchTask(taskId) }
    fun cancel(taskId: String, deleteTemp: Boolean = false) { activeJobs[taskId]?.cancel(); activeJobs.remove(taskId); scope.launch { updateTaskState(taskId, BiliTaskState.CANCELLED); if (deleteTemp) cleanTemp(taskId) } }

    suspend fun restore() {
        for (s in snapshotStore.loadAll()) {
            val st = BiliTaskState.valueOf(s.state)
            if (st in ACTIVE_STATES) { val r = s.copy(state = BiliTaskState.PAUSED.name); snapshotStore.save(r); addTask(snap(r)) } else addTask(snap(s))
        }
    }

    private fun launchTask(taskId: String) {
        if (activeJobs.containsKey(taskId)) return
        activeJobs[taskId] = scope.launch { semaphore.acquire(); try { runTask(taskId) } finally { semaphore.release(); activeJobs.remove(taskId) } }
    }

    private suspend fun runTask(taskId: String) {
        try {
            val req = _tasks.value.firstOrNull { it.id == taskId }?.request ?: return
            updateTask(taskId) { it.copy(title = req.reference.bvid, state = BiliTaskState.DOWNLOADING) }
            val vp = File(tempDirectory, "${taskId}_v.m4s")
            val ap = File(tempDirectory, "${taskId}_a.m4s")
            val mp4 = File(tempDirectory, "${taskId}_output.mp4")

            val videoUrl: String
            val audioUrl: String?
            if (req.preResolvedVideoUrl != null) {
                videoUrl = req.preResolvedVideoUrl
                audioUrl = req.preResolvedAudioUrl
            } else {
                updateTaskState(taskId, BiliTaskState.RESOLVING)
                val info = apiService.resolveVideo(req.reference.bvid)
                val cid = info.pages.firstOrNull()?.cid ?: return
                updateTask(taskId) { it.copy(title = info.title ?: "") }
                val playUrl = apiService.fetchPlayUrl(req.reference.bvid, cid)
                val dash = playUrl.dash ?: throw BiliApiException(-1, "No DASH")
                val qp = req.qualityPriority.map { BiliQuality.fromCode(it) }
                val ap2 = req.audioPriority.map { BiliAudioQuality.fromCode(it) }
                val sel = streamSelector.select(dash, qp, ap2, req.preferAvc)
                updateTaskState(taskId, BiliTaskState.DOWNLOADING)
                videoUrl = sel.videoStream.resolvedUrl
                audioUrl = sel.audioStream?.resolvedUrl
            }

            val vTotal = apiService.probeContentLength(videoUrl)
            val aTotal = audioUrl?.let { apiService.probeContentLength(it) } ?: -1

            var vd = vp.length()
            var ad = ap.length()
            var lastUpdate = System.currentTimeMillis()

            val vDeferred = scope.async {
                chunkDownloader.downloadToFile(videoUrl, vp, vp.length(), vTotal, onProgress = { bytes ->
                    vd = bytes
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 500) { lastUpdate = now; scope.launch { progress(taskId, vd, vTotal, ad, aTotal) } }
                })
            }
            val aDeferred = audioUrl?.let { url ->
                scope.async { chunkDownloader.downloadToFile(url, ap, ap.length(), aTotal, onProgress = { }) }
            }
            try {
                vDeferred.await()
                aDeferred?.await()
            } catch (e: LinkExpiredException) {
                BiliLogger.w(TAG, "Link expired, re-fetching stream and resuming")
                val freshPlayUrl = apiService.fetchPlayUrl(req.reference.bvid, req.preResolvedCid)
                val freshDash = freshPlayUrl.dash ?: throw BiliApiException(-1, "No DASH after refresh")
                val freshSel = streamSelector.select(freshDash, listOf(BiliQuality.Q_1080P, BiliQuality.Q_720P, BiliQuality.Q_480P, BiliQuality.Q_360P), emptyList(), true)
                chunkDownloader.downloadToFile(freshSel.videoStream.resolvedUrl, vp, vp.length(), -1, onProgress = { })
            }

            updateTaskState(taskId, BiliTaskState.MERGING)
            val mergeResult = merger.merge(vp, if (ap.exists()) ap else null, mp4)

            updateTaskState(taskId, BiliTaskState.EXPORTING)
            val fileName = BiliVideoFileName.create(
                title = req.preResolvedTitle,
                bvid = req.reference.bvid,
                qualityLabel = req.preResolvedQualityLabel,
            )
            val uri = exporter.exportVideo(mp4, fileName)

            vp.delete()
            ap.delete()
            mp4.delete()

            updateTask(taskId) {
                it.copy(
                    state = BiliTaskState.COMPLETED,
                    mergeOutcome = mergeResult.outcome,
                    downgradeReason = mergeResult.reason,
                    outputUri = uri?.toString(),
                )
            }
        } catch (e: CancellationException) { updateTaskState(taskId, BiliTaskState.PAUSED) }
        catch (e: Exception) {
            val msg = e.javaClass.simpleName + ": " + e.message
            BiliLogger.e(TAG, "Task FAILED: $msg", e)
            updateTask(taskId) { it.copy(state = BiliTaskState.FAILED, errorMessage = msg, retryable = true) }
        }
    }

    private suspend fun progress(taskId: String, vd: Long, vt: Long, ad: Long, at: Long) {
        updateTask(taskId) { t -> t.copy(progress = BiliTaskProgress(vd, vt, ad, at, 0, -1)) }
    }
    private suspend fun addTask(t: BiliDownloadTask) { tasksMutex.withLock { _tasks.value = _tasks.value + t } }
    private suspend fun updateTask(id: String, f: (BiliDownloadTask) -> BiliDownloadTask) { tasksMutex.withLock { _tasks.value = _tasks.value.map { if (it.id == id) f(it) else it } } }
    private suspend fun updateTaskState(id: String, s: BiliTaskState) { updateTask(id) { it.copy(state = s) } }
    private fun snap(s: BiliTaskSnapshot) = BiliDownloadTask(s.id, s.request, s.title, BiliTaskState.valueOf(s.state), s.progress, BiliQuality.fromCode(s.selectedQualityCode), BiliMergeOutcome.valueOf(s.mergeOutcome), s.downgradeReason, null, s.errorMessage, s.state == BiliTaskState.FAILED.name, s.createdAt)
    private fun cleanTemp(id: String) { tempDirectory.listFiles()?.filter { it.name.startsWith(id) }?.forEach { it.delete() } }

    companion object {
        private const val TAG = "BiliDownloadEngine"
        const val DEFAULT_CONCURRENT_TASKS = 1
        const val MIN_CONCURRENT_TASKS = 1
        const val MAX_CONCURRENT_TASKS = 4
        private val ACTIVE_STATES = setOf(BiliTaskState.QUEUED, BiliTaskState.RESOLVING, BiliTaskState.DOWNLOADING, BiliTaskState.MERGING)
    }
}
