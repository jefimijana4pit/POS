package com.example.marketpos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VenueLocationEntity::class,
        ProductEntity::class,
        SaleTransactionEntity::class,
        SaleLineItemEntity::class,
        BarTableEntity::class,
        TableOrderItemEntity::class,
        OrderEntity::class,
        OrderItemEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun catalogLocationDao(): CatalogLocationDao
    abstract fun salesDao(): SalesDao
    abstract fun tableOrderDao(): TableOrderDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN stockQuantity INTEGER NOT NULL DEFAULT 25")
                db.execSQL("ALTER TABLE products ADD COLUMN lowStockThreshold INTEGER NOT NULL DEFAULT 5")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN venueMode TEXT NOT NULL DEFAULT 'MARKET'")
                db.execSQL("ALTER TABLE sale_transactions ADD COLUMN venueMode TEXT NOT NULL DEFAULT 'MARKET'")
                db.execSQL("ALTER TABLE sale_transactions ADD COLUMN tableId TEXT")
                db.execSQL("ALTER TABLE sale_transactions ADD COLUMN tableName TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bar_tables (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        zone TEXT NOT NULL,
                        capacity INTEGER NOT NULL,
                        lockedByWorkerId TEXT,
                        lockedByWorkerName TEXT,
                        lockedAtMillis INTEGER,
                        awaitingCleanup INTEGER NOT NULL DEFAULT 0,
                        paidAndPrintedAtMillis INTEGER,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS table_order_items (
                        rowId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tableId TEXT NOT NULL,
                        productId TEXT NOT NULL,
                        productNameSr TEXT NOT NULL,
                        productNameEn TEXT NOT NULL,
                        category TEXT NOT NULL,
                        unitPriceRsd DOUBLE NOT NULL,
                        quantity INTEGER NOT NULL,
                        FOREIGN KEY(tableId) REFERENCES bar_tables(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_table_order_items_tableId ON table_order_items(tableId)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS orders (
                        id TEXT NOT NULL PRIMARY KEY,
                        orderNumber TEXT NOT NULL,
                        venueMode TEXT NOT NULL,
                        tableId TEXT,
                        tableName TEXT,
                        workerId TEXT NOT NULL,
                        workerName TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        completedAtMillis INTEGER,
                        transactionId TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS order_items (
                        rowId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        orderId TEXT NOT NULL,
                        productId TEXT NOT NULL,
                        productNameSr TEXT NOT NULL,
                        productNameEn TEXT NOT NULL,
                        category TEXT NOT NULL,
                        unitPriceRsd DOUBLE NOT NULL,
                        quantity INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        FOREIGN KEY(orderId) REFERENCES orders(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_order_items_orderId ON order_items(orderId)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS venue_locations (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        address TEXT NOT NULL,
                        venueMode TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE products ADD COLUMN locationId TEXT NOT NULL DEFAULT 'loc_mkt_central'")
                db.execSQL("UPDATE products SET locationId = 'loc_bar_center' WHERE venueMode = 'BAR_RESTAURANT'")
                db.execSQL("ALTER TABLE sale_transactions ADD COLUMN locationId TEXT NOT NULL DEFAULT 'loc_mkt_central'")
                db.execSQL("UPDATE sale_transactions SET locationId = 'loc_bar_center' WHERE venueMode = 'BAR_RESTAURANT'")
                db.execSQL("ALTER TABLE bar_tables ADD COLUMN locationId TEXT NOT NULL DEFAULT 'loc_bar_center'")
                db.execSQL("ALTER TABLE orders ADD COLUMN locationId TEXT NOT NULL DEFAULT 'loc_mkt_central'")
                db.execSQL("UPDATE orders SET locationId = 'loc_bar_center' WHERE venueMode = 'BAR_RESTAURANT'")
            }
        }

        @Volatile
        private var instance: PosDatabase? = null

        fun getInstance(context: Context): PosDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PosDatabase::class.java,
                    "market_pos.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
