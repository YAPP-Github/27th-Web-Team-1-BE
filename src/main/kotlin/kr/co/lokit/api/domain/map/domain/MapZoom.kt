package kr.co.lokit.api.domain.map.domain

import kotlin.math.floor

class MapZoom private constructor(
    val level: Double,
    val discrete: Int,
) {
    companion object {
        private const val MIN_ZOOM_LEVEL = 0
        private const val MAX_ZOOM_LEVEL = 22

        fun from(level: Double): MapZoom {
            val normalizedLevel = normalize(level)
            return MapZoom(
                level = normalizedLevel,
                discrete = floor(normalizedLevel).toInt(),
            )
        }

        private fun normalize(level: Double): Double {
            if (!level.isFinite()) {
                return MIN_ZOOM_LEVEL.toDouble()
            }
            return level.coerceIn(MIN_ZOOM_LEVEL.toDouble(), MAX_ZOOM_LEVEL.toDouble())
        }
    }
}
