package com.hereliesaz.geministrator.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * A [ViewModelProvider.Factory] that can be used to create any type of [ViewModel].
 * This is useful for testing, as it allows us to inject mock dependencies into our ViewModels.
 *
 * @param T The type of ViewModel to create.
 * @property create A lambda that creates and returns an instance of the ViewModel.
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory<T : ViewModel>(private val create: () -> T) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return create() as T
    }
}
