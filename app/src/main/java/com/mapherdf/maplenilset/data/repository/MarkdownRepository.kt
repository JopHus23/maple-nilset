package com.mapherdf.maplenilset.data.repository

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mapherdf.maplenilset.const.MarkdownFiles

interface MarkdownRepository {
    suspend fun read(fileName: String): Result<String>

    suspend fun write(fileName: String, content: String): Result<Unit>

    suspend fun readDocument(uri: String): Result<String>

    suspend fun writeDocument(uri: String, content: String): Result<Unit>
}

@Singleton
class LocalMarkdownRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : MarkdownRepository {
    private val markdownDirectory = File(appContext.filesDir, MarkdownFiles.DIRECTORY)

    override suspend fun read(fileName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val file = resolveMarkdownFile(fileName)
            if (!file.exists()) {
                if (fileName == MarkdownFiles.HOME_PLACEHOLDER) {
                    copyPlaceholder(file)
                } else {
                    file.parentFile?.mkdirs()
                    file.createNewFile()
                }
            }
            file.readText()
        }
    }

    override suspend fun write(fileName: String, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = resolveMarkdownFile(fileName)
                file.parentFile?.mkdirs()
                replaceFile(file, content)
            }
        }

    override suspend fun readDocument(uri: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            appContext.contentResolver.openInputStream(uri.toUri())?.use { stream ->
                stream.bufferedReader().readText()
            } ?: throw IllegalStateException("Markdown document could not be opened: $uri")
        }
    }

    override suspend fun writeDocument(uri: String, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                appContext.contentResolver.openOutputStream(uri.toUri(), "wt")?.use { stream ->
                    stream.write(content.toByteArray())
                } ?: throw IllegalStateException("Markdown document could not be opened: $uri")
            }
        }

    private fun resolveMarkdownFile(fileName: String): File {
        require(fileName.isNotBlank()) { "Markdown file name must not be blank" }
        require(!fileName.contains('/') && !fileName.contains('\\')) {
            "Markdown file name must not contain a path separator"
        }
        require(fileName != "." && fileName != "..") {
            "Markdown file name must not reference a parent directory"
        }
        require(fileName.endsWith(MarkdownFiles.FILE_EXTENSION)) {
            "Markdown file name must use ${MarkdownFiles.FILE_EXTENSION}"
        }
        markdownDirectory.mkdirs()
        return File(markdownDirectory, fileName)
    }

    private fun copyPlaceholder(target: File) {
        target.parentFile?.mkdirs()
        appContext.assets.open(MarkdownFiles.PLACEHOLDER_ASSET).use { input ->
            target.outputStream().use(input::copyTo)
        }
    }

    private fun replaceFile(target: File, content: String) {
        val temporaryFile = File.createTempFile("markdown-", ".tmp", markdownDirectory)
        try {
            temporaryFile.writeText(content)
            if (!temporaryFile.renameTo(target)) {
                target.writeText(temporaryFile.readText())
                temporaryFile.delete()
            }
        } finally {
            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }
        }
    }
}
