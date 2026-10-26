package com.mapherdf.maplenilset.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.mapherdf.maplenilset.BuildConfig
import com.mapherdf.maplenilset.const.LocalStoreKeys
import com.mapherdf.maplenilset.core.command.CommandRegistry
import com.mapherdf.maplenilset.core.command.NilSetCommandCenter
import com.mapherdf.maplenilset.core.command.commands.ClearConsoleCommand
import com.mapherdf.maplenilset.core.command.commands.ClearDataCommand
import com.mapherdf.maplenilset.data.repository.CalendarRepository
import com.mapherdf.maplenilset.data.repository.LocalCalendarRepository
import com.mapherdf.maplenilset.data.repository.LocalMarkdownRepository
import com.mapherdf.maplenilset.data.repository.MarkdownRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

private val Context.localDataStore: DataStore<Preferences> by preferencesDataStore(
    name = LocalStoreKeys.STORE_NAME,
)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideLocalDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.localDataStore

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    @BiliCacheDirectory
    fun provideBiliCacheDirectory(@ApplicationContext context: Context): File =
        File(context.cacheDir, "bili")

    @Provides
    @Singleton
    fun provideCommandRegistry(): CommandRegistry = CommandRegistry(
        commands = listOf(
            ClearDataCommand(),
            ClearConsoleCommand(),
        ),
        isDebug = BuildConfig.DEBUG,
    )

    @Provides
    @Singleton
    fun provideCommandCenter(registry: CommandRegistry): NilSetCommandCenter =
        NilSetCommandCenter(registry)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(
        impl: LocalCalendarRepository,
    ): CalendarRepository

    @Binds
    @Singleton
    abstract fun bindMarkdownRepository(
        impl: LocalMarkdownRepository,
    ): MarkdownRepository
}
