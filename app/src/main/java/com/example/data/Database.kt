package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Entities
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "favorite_thikrs")
data class FavoriteThikrEntity(
    @PrimaryKey val id: Int
)

@Entity(tableName = "custom_category_orders")
data class CustomCategoryOrderEntity(
    @PrimaryKey val categoryName: String,
    val displayOrder: Int
)

// 2. DAOs
@Dao
interface SettingDao {
    @Query("SELECT * FROM settings WHERE `key` = :key")
    fun getSetting(key: String): Flow<SettingEntity?>

    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getSettingSync(key: String): SettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: SettingEntity)
}

@Dao
interface FavoriteThikrDao {
    @Query("SELECT * FROM favorite_thikrs")
    fun getAllFavorites(): Flow<List<FavoriteThikrEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteThikrEntity)

    @Query("DELETE FROM favorite_thikrs WHERE id = :id")
    suspend fun removeFavorite(id: Int)
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_thikrs WHERE id = :id)")
    suspend fun isFavorite(id: Int): Boolean
}

@Dao
interface CustomCategoryOrderDao {
    @Query("SELECT * FROM custom_category_orders ORDER BY displayOrder ASC")
    fun getAllCategoryOrders(): Flow<List<CustomCategoryOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCategoryOrders(orders: List<CustomCategoryOrderEntity>)

    @Query("DELETE FROM custom_category_orders")
    suspend fun clearCategoryOrders()
}

// 3. Database
@Database(
    entities = [SettingEntity::class, FavoriteThikrEntity::class, CustomCategoryOrderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AtharDatabase : RoomDatabase() {
    abstract fun settingDao(): SettingDao
    abstract fun favoriteThikrDao(): FavoriteThikrDao
    abstract fun customCategoryOrderDao(): CustomCategoryOrderDao

    companion object {
        @Volatile
        private var INSTANCE: AtharDatabase? = null

        fun getDatabase(context: Context): AtharDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AtharDatabase::class.java,
                    "athar_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
