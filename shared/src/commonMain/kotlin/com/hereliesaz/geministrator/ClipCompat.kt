package com.hereliesaz.geministrator

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

internal fun Modifier.clip(shape: Shape): Modifier = graphicsLayer {
    this.shape = shape
    clip = true
}
