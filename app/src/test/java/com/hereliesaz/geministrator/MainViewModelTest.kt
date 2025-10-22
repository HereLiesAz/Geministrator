package com.hereliesaz.geministrator

import com.hereliesaz.geministrator.ui.core.MainViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        viewModel = MainViewModel()
    }

    @Test
    fun `setLoading updates isLoading state`() {
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.setLoading(true)
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.setLoading(false)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
