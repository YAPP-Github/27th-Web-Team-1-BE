package kr.co.lokit.api.domain.map.domain

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

    fun getSupportedZoomLevels(): Set<Int> = (0..22).toSet()
}
