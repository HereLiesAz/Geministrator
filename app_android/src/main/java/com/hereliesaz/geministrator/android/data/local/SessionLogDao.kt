package com.hereliesaz.geministrator.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionLogDao {
    @Query("SELECT * FROM session_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SessionLog>>

    @Query("SELECT * FROM session_logs WHERE id = :id")
    fun getById(id: String): Flow<SessionLog>

    @Insert
    suspend fun insert(sessionLog: SessionLog)
}
