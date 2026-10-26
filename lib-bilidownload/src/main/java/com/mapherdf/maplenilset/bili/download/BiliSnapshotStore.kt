package com.mapherdf.maplenilset.bili.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

class BiliSnapshotStore(
    private val snapshotFile: File,
) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun save(snapshot: BiliTaskSnapshot) = withContext(Dispatchers.IO) {
        val all = loadAll().filter { it.id != snapshot.id } + snapshot
        snapshotFile.parentFile?.mkdirs()
        snapshotFile.writeText(json.encodeToString(ListSerializer(BiliTaskSnapshot.serializer()), all))
    }

    suspend fun loadAll(): List<BiliTaskSnapshot> = withContext(Dispatchers.IO) {
        if (!snapshotFile.exists()) return@withContext emptyList()
        runCatching {
            json.decodeFromString(
                ListSerializer(BiliTaskSnapshot.serializer()),
                snapshotFile.readText(),
            )
        }.getOrDefault(emptyList())
    }

    suspend fun findById(taskId: String): BiliTaskSnapshot? = withContext(Dispatchers.IO) {
        loadAll().firstOrNull { it.id == taskId }
    }

    suspend fun updateProgress(
        taskId: String,
        videoDone: Long,
        videoTotal: Long,
        audioDone: Long,
        audioTotal: Long,
    ) = withContext(Dispatchers.IO) {
        val updated = loadAll().map { snapshot ->
            if (snapshot.id == taskId) {
                snapshot.copy(
                    progress = snapshot.progress.copy(
                        videoBytesDownloaded = videoDone,
                        videoBytesTotal = videoTotal,
                        audioBytesDownloaded = audioDone,
                        audioBytesTotal = audioTotal,
                    ),
                )
            } else snapshot
        }
        snapshotFile.writeText(json.encodeToString(ListSerializer(BiliTaskSnapshot.serializer()), updated))
    }

    suspend fun updateState(taskId: String, state: BiliTaskState) = withContext(Dispatchers.IO) {
        val updated = loadAll().map { snapshot ->
            if (snapshot.id == taskId) snapshot.copy(state = state.name) else snapshot
        }
        snapshotFile.writeText(json.encodeToString(ListSerializer(BiliTaskSnapshot.serializer()), updated))
    }

    suspend fun remove(taskId: String) = withContext(Dispatchers.IO) {
        val updated = loadAll().filter { it.id != taskId }
        snapshotFile.writeText(json.encodeToString(ListSerializer(BiliTaskSnapshot.serializer()), updated))
    }
}