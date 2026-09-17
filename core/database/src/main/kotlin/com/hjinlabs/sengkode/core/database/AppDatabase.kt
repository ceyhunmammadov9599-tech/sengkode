package com.hjinlabs.sengkode.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema v1: history + templates. exportSchema = true - the schema
 * JSON is committed (schemas/), so every future version change gets a
 * real migration test against the committed snapshot. No destructive
 * migration policy anywhere: schema changes require migrations.
 */
@Database(
    entities = [HistoryEntity::class, TemplateEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun templateDao(): TemplateDao

    companion object {
        const val NAME = "sengkode.db"
    }
}
