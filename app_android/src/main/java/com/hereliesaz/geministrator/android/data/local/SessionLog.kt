package com.hereliesaz.geministrator.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_logs")
data class SessionLog(
    @PrimaryKey val id: String,
    val name: String,
    val timestamp: Long,
    val logContent: String
)
