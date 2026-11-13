package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [UniversalGroupEntity::class, ProductEntity::class],
  version = 2,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun universalGroupDao(): UniversalGroupDao
  abstract fun productDao(): ProductDao

  companion object {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "ALTER TABLE universal_groups ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_universal_groups_cloudId ON universal_groups (cloudId)",
        )
        db.execSQL(
          "ALTER TABLE products ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''",
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS index_products_cloudId ON products (cloudId)",
        )
      }
    }

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase =
      INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "pooja_electronics_parts.db",
        )
          .addMigrations(MIGRATION_1_2)
          .build()
          .also { INSTANCE = it }
      }
  }
}
