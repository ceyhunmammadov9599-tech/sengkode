package com.hjinlabs.sengkode.feature.generator.di

import com.hjinlabs.sengkode.core.export.QrBitmapRenderer
import com.hjinlabs.sengkode.core.qr.QrEngine
import com.hjinlabs.sengkode.core.qr.ZxingQrEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Engine bindings live at the consumer boundary so :core:qr stays a
 * pure JVM module with zero dependency-injection awareness.
 */
@Module
@InstallIn(SingletonComponent::class)
object GeneratorModule {

    @Provides
    @Singleton
    fun provideQrEngine(): QrEngine = ZxingQrEngine()

    @Provides
    @Singleton
    fun provideQrBitmapRenderer(): QrBitmapRenderer = QrBitmapRenderer()

    @Provides
    @Singleton
    fun provideDrawListBitmapRenderer(): com.hjinlabs.sengkode.core.export.DrawListBitmapRenderer =
        com.hjinlabs.sengkode.core.export.DrawListBitmapRenderer()

    @Provides
    fun provideGenerationDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

/**
 * Session restore handoff binding (history/templates -> studio).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RestoreStoreModule {

    @Binds
    @Singleton
    abstract fun bindRestoreStore(
        impl: com.hjinlabs.sengkode.feature.generator.InMemoryStudioRestoreStore,
    ): com.hjinlabs.sengkode.core.model.repository.StudioRestoreStore
}
