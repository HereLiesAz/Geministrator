package com.hereliesaz.geministrator.ui.ide

import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistory(): Flow<List<String>>
}
