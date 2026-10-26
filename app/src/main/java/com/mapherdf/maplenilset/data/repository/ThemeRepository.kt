package com.mapherdf.maplenilset.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import kotlin.math.roundToInt
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import dagger.hilt.android.qualifiers.ApplicationContext
import com.mapherdf.maplenilset.const.ThemeStoreKeys
import com.mapherdf.maplenilset.core.theme.ThemeColors
import com.mapherdf.maplenilset.core.theme.ThemeColorParser
import com.mapherdf.maplenilset.core.theme.ThemeMode
import com.mapherdf.maplenilset.core.theme.ThemePreset
import com.mapherdf.maplenilset.core.theme.UserThemeSettings
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = ThemeStoreKeys.STORE_NAME,
)

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) {
    private val dataStore = context.themeDataStore
    private val appContext = context.applicationContext
    private val backgroundDirectory = File(appContext.filesDir, "background")
    private val fontDirectory = File(appContext.filesDir, "font")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val defaultSettings = UserThemeSettings(
        mode = ThemeMode.DARK,
        paletteId = ThemePreset.MAPLE.id,
    )

    val settings: StateFlow<UserThemeSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val serialized = preferences[SETTINGS_KEY]
            if (serialized.isNullOrBlank()) {
                defaultSettings
            } else {
                try {
                    json.decodeFromString(UserThemeSettings.serializer(), serialized)
                } catch (exception: SerializationException) {
                    defaultSettings
                } catch (exception: IllegalArgumentException) {
                    defaultSettings
                }
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = defaultSettings,
        )

    val isReady: StateFlow<Boolean> = settings
        .map { true }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    suspend fun selectPalette(paletteId: String) {
        update { current ->
            current.copy(paletteId = paletteId)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        update { it.copy(mode = mode) }
    }

    suspend fun setCardBorders(enabled: Boolean) {
        update { it.copy(showCardBorders = enabled) }
    }

    suspend fun setCardMaskOpacity(opacity: Float) {
        update {
            it.copy(
                cardMaskOpacity = opacity.coerceIn(
                    UserThemeSettings.MIN_CARD_MASK_OPACITY,
                    UserThemeSettings.MAX_CARD_MASK_OPACITY,
                ),
            )
        }
    }

    suspend fun setTextScaleEnabled(enabled: Boolean) {
        update { it.copy(textScaleEnabled = enabled) }
    }

    suspend fun setTextScale(scale: Float) {
        update {
            it.copy(
                textScale = scale.coerceIn(
                    UserThemeSettings.MIN_SCALE,
                    UserThemeSettings.MAX_SCALE,
                ),
            )
        }
    }

    suspend fun setUiScaleEnabled(enabled: Boolean) {
        update { it.copy(uiScaleEnabled = enabled) }
    }

    suspend fun setUiScale(scale: Float) {
        update {
            it.copy(
                uiScale = scale.coerceIn(
                    UserThemeSettings.MIN_SCALE,
                    UserThemeSettings.MAX_SCALE,
                ),
            )
        }
    }

    suspend fun setCustomColors(colors: ThemeColors, useDark: Boolean) {
        update { current ->
            current.copy(
                paletteId = ThemePreset.CUSTOM.id,
                customLightColors = if (useDark) current.customLightColors else colors,
                customDarkColors = if (useDark) colors else current.customDarkColors,
            )
        }
    }

    suspend fun applyCroppedBackgroundImage(
        sourceUri: String,
        cropLeft: Float,
        cropTop: Float,
        cropRight: Float,
        cropBottom: Float,
    ): String {
        return withContext(Dispatchers.IO) {
            val source = decodeBitmap(sourceUri)
            requireNotNull(source)
            require(cropLeft in 0f..1f && cropTop in 0f..1f)
            require(cropRight in 0f..1f && cropBottom in 0f..1f)

            val left = (source.width * cropLeft).roundToInt()
            val top = (source.height * cropTop).roundToInt()
            val right = (source.width * cropRight).roundToInt()
            val bottom = (source.height * cropBottom).roundToInt()
            val width = (right - left).coerceAtLeast(1)
            val height = (bottom - top).coerceAtLeast(1)
            val cropped = Bitmap.createBitmap(source, left, top, width, height)

            backgroundDirectory.mkdirs()
            val output = File(backgroundDirectory, "background-${System.currentTimeMillis()}.jpg")
            output.outputStream().use { stream ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            }

            deleteCurrentBackgroundFile()
            releaseCurrentContentPermission()
            val backgroundUri = Uri.fromFile(output).toString()
            update { current ->
                current.copy(
                    backgroundImageUri = backgroundUri,
                    backgroundOpacity = UserThemeSettings.DEFAULT_BACKGROUND_OPACITY,
                )
            }
            backgroundUri
        }
    }

    suspend fun applyCustomFont(sourceUri: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(sourceUri)
            val fontName = resolveFontDisplayName(uri)
            val extension = resolveFontExtension(uri) ?: return@runCatching false
            fontDirectory.mkdirs()
            val temporaryFile = File.createTempFile("custom-font-", ".$extension", fontDirectory)
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                temporaryFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                temporaryFile.delete()
                return@runCatching false
            }

            if (!isValidFontFile(temporaryFile)) {
                temporaryFile.delete()
                return@runCatching false
            }

            deleteCurrentFontFile()
            val fontFile = File(fontDirectory, "custom-font-${System.currentTimeMillis()}.$extension")
            require(temporaryFile.renameTo(fontFile))
            update { current ->
                current.copy(
                    customFontPath = Uri.fromFile(fontFile).toString(),
                    customFontName = fontName,
                )
            }
            true
        }.getOrDefault(false)
    }

    private fun decodeBitmap(sourceUri: String): Bitmap? {
        val uri = Uri.parse(sourceUri)
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val metrics = appContext.resources.displayMetrics
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                requestedWidth = metrics.widthPixels,
                requestedHeight = metrics.heightPixels,
            )
        }
        return appContext.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        requestedWidth: Int,
        requestedHeight: Int,
    ): Int {
        var inSampleSize = 1
        val longestSourceSide = maxOf(width, height)
        val longestRequestedSide = maxOf(requestedWidth, requestedHeight)
        var sampleLongestSide = longestSourceSide
        while (sampleLongestSide / 2 >= longestRequestedSide) {
            sampleLongestSide /= 2
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun deleteCurrentBackgroundFile() {
        val currentUri = settings.value.backgroundImageUri ?: return
        val parsedUri = Uri.parse(currentUri)
        if (parsedUri.scheme != "file") return
        val currentFile = File(parsedUri.path ?: return)
        val directoryPath = backgroundDirectory.canonicalPath
        if (runCatching { currentFile.canonicalPath }.getOrNull()?.startsWith(directoryPath) == true) {
            currentFile.delete()
        }
    }

    private fun releaseCurrentContentPermission() {
        val currentUri = settings.value.backgroundImageUri ?: return
        val parsedUri = Uri.parse(currentUri)
        if (parsedUri.scheme != "content") return
        runCatching {
            appContext.contentResolver.releasePersistableUriPermission(
                parsedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    private fun resolveFontExtension(uri: Uri): String? {
        val mimeType = appContext.contentResolver.getType(uri)?.lowercase()
        val extension = when (mimeType) {
            "application/x-font-ttf", "font/ttf", "application/font-sfnt" -> "ttf"
            "application/x-font-otf", "font/otf", "application/vnd.ms-opentype" -> "otf"
            else -> null
        }
        if (extension != null) return extension

        val displayName = resolveFontDisplayName(uri)
        if (displayName != null) {
            val resolved = displayName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            if (resolved == "ttf" || resolved == "otf") return resolved
        }
        return null
    }

    private fun resolveFontDisplayName(uri: Uri): String? {
        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                return name?.trim()?.takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    private fun isValidFontFile(file: File): Boolean = runCatching {
        Typeface.createFromFile(file)
        true
    }.getOrDefault(false)

    private fun deleteCurrentFontFile() {
        val currentPath = settings.value.customFontPath ?: return
        val parsedPath = Uri.parse(currentPath)
        if (parsedPath.scheme != "file") return
        val currentFile = File(parsedPath.path ?: return)
        val directoryPath = fontDirectory.canonicalPath
        if (runCatching { currentFile.canonicalPath }.getOrNull()?.startsWith(directoryPath) == true) {
            currentFile.delete()
        }
    }

    suspend fun resetCustomColors(useDark: Boolean) {
        update { current ->
            current.copy(
                paletteId = ThemePreset.CUSTOM.id,
                customLightColors = if (useDark) current.customLightColors else UserThemeSettings.FALLBACK_LIGHT_COLORS,
                customDarkColors = if (useDark) UserThemeSettings.FALLBACK_DARK_COLORS else current.customDarkColors,
            )
        }
    }

    suspend fun resetCustomBackgroundImage() {
        deleteCurrentBackgroundFile()
        releaseCurrentContentPermission()
        update { current ->
            current.copy(backgroundImageUri = null)
        }
    }

    suspend fun resetCustomFont() {
        deleteCurrentFontFile()
        update { current ->
            current.copy(
                customFontPath = null,
                customFontName = null,
            )
        }
    }

    suspend fun setBackgroundOpacity(opacity: Float) {
        update { current ->
            current.copy(
                backgroundOpacity = opacity.coerceIn(
                    UserThemeSettings.MIN_BACKGROUND_OPACITY,
                    UserThemeSettings.MAX_BACKGROUND_OPACITY,
                ),
            )
        }
    }

    suspend fun setConsoleBackground(enabled: Boolean) {
        update { current ->
            current.copy(showConsoleBackground = enabled)
        }
    }

    suspend fun setConsoleOutputFontSize(fontSize: Float) {
        update { current ->
            current.copy(
                consoleOutputFontSize = fontSize.coerceIn(
                    UserThemeSettings.MIN_CONSOLE_OUTPUT_FONT_SIZE,
                    UserThemeSettings.MAX_CONSOLE_OUTPUT_FONT_SIZE,
                ),
            )
        }
    }

    private suspend fun update(transform: (UserThemeSettings) -> UserThemeSettings) {
        val next = transform(settings.value)
        dataStore.edit { preferences ->
            preferences[SETTINGS_KEY] = json.encodeToString(
                UserThemeSettings.serializer(),
                next,
            )
        }
    }

    private companion object {
        val SETTINGS_KEY = stringPreferencesKey(ThemeStoreKeys.SETTINGS)
    }
}
