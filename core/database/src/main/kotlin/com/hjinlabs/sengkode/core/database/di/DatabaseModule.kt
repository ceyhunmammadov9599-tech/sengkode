package com.hjinlabs.sengkode.core.database.di

import android.content.Context
import androidx.room.Room
import com.hjinlabs.sengkode.core.database.AppDatabase
import com.hjinlabs.sengkode.core.database.HistoryDao
import com.hjinlabs.sengkode.core.database.HistoryRepositoryImpl
import com.hjinlabs.sengkode.core.database.TemplateDao
import com.hjinlabs.sengkode.core.database.TemplateRepositoryImpl
import com.hjinlabs.sengkode.core.model.repository.HistoryRepository
import com.hjinlabs.sengkode.core.model.repository.TemplateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseProvider {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideTemplateDao(db: AppDatabase): TemplateDao = db.templateDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(impl: TemplateRepositoryImpl): TemplateRepository
}
