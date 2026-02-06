package kr.co.lokit.api.domain.map.domain

import kotlin.math.cos

object GridValues {
    const val HOME_ZOOM_LEVEL = 13
    const val CLUSTER_ZOOM_THRESHOLD = 15

    fun getGridSize(
        zoom: Int,
        latitude: Double,
        gridPx: Int = 60,
    ): Double {
        val effectiveZoom = zoom.coerceIn(0, 22)
        val tileSize = 256.0
        val earthCircumference = 40075017.0

        val metersPerPx =
            (earthCircumference * cos(Math.toRadians(latitude))) / (tileSize * Math.pow(2.0, effectiveZoom.toDouble()))
        val gridMeters = metersPerPx * gridPx

        return gridMeters / 111000.0
    }

    fun getGridSize(
        zoom: Int,
        gridPx: Int = 60,
    ): Double {
        val tileSize = 256.0
        return (tileSize * Math.pow(2.0, zoom.toDouble())) / gridPx
    }

    fun getSupportedZoomLevels(): Set<Int> = (0..22).toSet()
}
