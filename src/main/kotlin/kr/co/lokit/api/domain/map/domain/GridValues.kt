package kr.co.lokit.api.domain.map.domain

import kotlin.math.pow

object GridValues {
    const val HOME_ZOOM_LEVEL = 13
    const val CLUSTER_ZOOM_THRESHOLD = 15

    fun getGridSize(zoom: Int): Double {
        val effectiveZoom = zoom.coerceIn(0, 22)
        return 1.0 / 2.0.pow(effectiveZoom - 4.0)
    }

    fun getSupportedZoomLevels(): Set<Int> = (0..22).toSet()
}
