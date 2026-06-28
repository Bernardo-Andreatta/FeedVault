package com.bernardo.feedvault.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE media_items ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE media_items ADD COLUMN aspectRatio REAL NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE media_items ADD COLUMN mimeType TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS video_clips (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "mediaItemId INTEGER NOT NULL, " +
            "uri TEXT NOT NULL, " +
            "startMs INTEGER NOT NULL, " +
            "endMs INTEGER NOT NULL, " +
            "label TEXT NOT NULL DEFAULT '', " +
            "dateCreated INTEGER NOT NULL)"
        )
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE video_clips ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE video_clips ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE media_items ADD COLUMN thumbnailFrameMs INTEGER NOT NULL DEFAULT -1")
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS vault_items (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "storedFileName TEXT NOT NULL, " +
            "displayName TEXT NOT NULL, " +
            "mediaType TEXT NOT NULL, " +
            "mimeType TEXT NOT NULL, " +
            "sizeBytes INTEGER NOT NULL DEFAULT 0, " +
            "aspectRatio REAL NOT NULL DEFAULT 0, " +
            "dateAdded INTEGER NOT NULL)"
        )
    }
}

private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE media_items ADD COLUMN encrypted INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(entities = [MediaItem::class, VideoClip::class, VaultItem::class], version = 10)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun videoClipDao(): VideoClipDao
    abstract fun vaultItemDao(): VaultItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "secure_gallery_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
