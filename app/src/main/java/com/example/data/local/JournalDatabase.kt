package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.JournalInteraction
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString("|||") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("|||").filter { it.isNotBlank() }
    }
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_interactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getInteractionsForUser(userId: String): Flow<List<JournalInteraction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: JournalInteraction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractions(interactions: List<JournalInteraction>)

    @Query("DELETE FROM journal_interactions WHERE id = :id AND userId = :userId")
    suspend fun deleteInteraction(id: String, userId: String)

    @Query("DELETE FROM journal_interactions WHERE userId = :userId")
    suspend fun clearUserInteractions(userId: String)
}

@Database(entities = [JournalInteraction::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: JournalDatabase? = null

        fun getInstance(context: android.content.Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    "reflect_ai_local.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
