package com.hereliesaz.geministrator.ui.ide

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class HistoryRepositoryImpl : HistoryRepository {
    override fun getHistory(): Flow<List<String>> {
        // For now, return a static list. In a real app, this would be a database or file.
        return flowOf(listOf("Initial history item 1", "Initial history item 2"))
    }
}
