package com.hereliesaz.geministrator.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SessionLog::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionLogDao(): SessionLogDao
}
