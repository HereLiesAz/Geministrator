package com.hereliesaz.geministrator.android.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// Entities
@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val initialPrompt: String,
    val endTimestamp: Long,
    val status: String, // e.g., "SUCCESS", "FAILURE"
)

@Entity(
    tableName = "log_entry",
    foreignKeys = [ForeignKey(
        entity = SessionHistoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["sessionId"])]
)
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val agent: String,
    val message: String,
    val content: String?,
    val timestamp: Long = System.currentTimeMillis(),
)

// Relation (for querying a session with all its logs)
data class SessionWithLogs(
    @Embedded val session: SessionHistoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val logs: List<LogEntryEntity>,
)

// DAO
@Dao
interface HistoryDao {
    @Insert
    suspend fun insertSession(session: SessionHistoryEntity): Long

    @Insert
    suspend fun insertLogEntries(logEntries: List<LogEntryEntity>)

    @Query("SELECT * FROM session_history ORDER BY endTimestamp DESC")
    fun getAllSessions(): Flow<List<SessionHistoryEntity>>

    @Transaction
    @Query("SELECT * FROM session_history WHERE id = :sessionId")
    fun getSessionWithLogs(sessionId: Long): Flow<SessionWithLogs>
}


// Database
@Database(entities = [SessionHistoryEntity::class, LogEntryEntity::class], version = 1)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: HistoryDatabase? = null

        fun getDatabase(context: Context): HistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "geministrator_history_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}