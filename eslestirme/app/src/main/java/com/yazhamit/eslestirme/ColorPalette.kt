package com.yazhamit.eslestirme

object ColorPalette {
    val colors = listOf(
        floatArrayOf(1.0f, 0.0f, 0.0f), // Red
        floatArrayOf(0.0f, 1.0f, 0.0f), // Green
        floatArrayOf(0.0f, 0.0f, 1.0f), // Blue
        floatArrayOf(1.0f, 1.0f, 0.0f), // Yellow
        floatArrayOf(1.0f, 0.0f, 1.0f), // Magenta
        floatArrayOf(0.0f, 1.0f, 1.0f), // Cyan
        floatArrayOf(1.0f, 0.5f, 0.0f), // Orange
        floatArrayOf(0.5f, 0.0f, 1.0f), // Purple
        floatArrayOf(0.0f, 0.5f, 1.0f), // Light Blue
        floatArrayOf(0.5f, 1.0f, 0.0f), // Lime
        floatArrayOf(1.0f, 0.0f, 0.5f), // Pink
        floatArrayOf(0.5f, 0.5f, 0.5f), // Gray
        floatArrayOf(0.0f, 0.5f, 0.0f), // Dark Green
        floatArrayOf(0.5f, 0.0f, 0.0f), // Dark Red
        floatArrayOf(0.0f, 0.0f, 0.5f)  // Dark Blue
    )

    fun getColorForIndex(index: Int): FloatArray {
        return colors[index % colors.size]
    }
}
