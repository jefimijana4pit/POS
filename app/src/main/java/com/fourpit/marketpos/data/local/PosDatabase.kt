package com.fourpit.marketpos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocationEntity::class,
        ProductEntity::class,
        BarTableEntity::class,
        PosOrderEntity::class,
        PosOrderItemEntity::class,
        WorkerLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun catalogLocationDao(): CatalogLocationDao
    abstract fun tableOrderDao(): TableOrderDao
    abstract fun salesDao(): SalesDao

    companion object {
        @Volatile
        private var INSTANCE: PosDatabase? = null

        fun getInstance(context: Context): PosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PosDatabase::class.java,
                    "marketpos.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
