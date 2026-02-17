package kr.co.lokit.api.domain.map.domain

import kotlin.math.floor
import kotlin.math.pow

object GridValues {
    const val HOME_ZOOM_LEVEL = 13
    const val CLUSTER_ZOOM_THRESHOLD = 15

    fun getGridSize(
        zoom: Int,
        gridPx: Int = 60,
    ): Double {
        val worldSizeMeters = 40075016.68557849
        val totalPxAtZoom = 256.0 * 2.0.pow(zoom.toDouble())
        return (worldSizeMeters / totalPxAtZoom) * gridPx
    }

    fun getGridSize(
        zoomLevel: Double,
        gridPx: Int = 60,
    ): Double {
        val normalized = normalizeZoomLevel(zoomLevel)
        val lowerZoom = floor(normalized).toInt()
        val upperZoom = (lowerZoom + 1).coerceAtMost(MAX_ZOOM_LEVEL)
        if (lowerZoom == upperZoom) {
            return getGridSize(lowerZoom, gridPx)
        }
        val progress = normalized - lowerZoom
        val lower = getGridSize(lowerZoom, gridPx)
        val upper = getGridSize(upperZoom, gridPx)
        return lower * (upper / lower).pow(progress)
    }

    fun getSupportedZoomLevels(): Set<Int> = (0..22).toSet()

    private fun normalizeZoomLevel(level: Double): Double {
        if (!level.isFinite()) return 0.0
        return level.coerceIn(0.0, MAX_ZOOM_LEVEL.toDouble())
    }

    private const val MAX_ZOOM_LEVEL = 22
}
