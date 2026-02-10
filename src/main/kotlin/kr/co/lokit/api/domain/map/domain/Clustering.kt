package kr.co.lokit.api.domain.map.domain

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

data class BBox(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
) {
    companion object {
        private const val HORIZONTAL_MULTIPLIER = 2.5
        private const val VERTICAL_MULTIPLIER = 5.0
        private const val EARTH_RADIUS_METERS = 6378137.0

        fun fromCenter(
            zoom: Int,
            longitude: Double,
            latitude: Double,
        ): BBox {
            val mx = longitude * (PI * EARTH_RADIUS_METERS / 180.0)
            val my = ln(tan((90.0 + latitude) * PI / 360.0)) * EARTH_RADIUS_METERS

            val worldSize = 2 * PI * EARTH_RADIUS_METERS
            val tileSizeAtZoom = worldSize / 2.0.pow(zoom.toDouble())
            val hHalf = tileSizeAtZoom * HORIZONTAL_MULTIPLIER
            val vHalf = tileSizeAtZoom * VERTICAL_MULTIPLIER

            val gridSize = GridValues.getGridSize(zoom)
            val westM = floor((mx - hHalf) / gridSize) * gridSize
            val southM = floor((my - vHalf) / gridSize) * gridSize
            val eastM = ceil((mx + hHalf) / gridSize) * gridSize
            val northM = ceil((my + vHalf) / gridSize) * gridSize

            return BBox(
                west = westM / (PI * EARTH_RADIUS_METERS / 180.0),
                south = atan(exp(southM / EARTH_RADIUS_METERS)) * 360.0 / PI - 90.0,
                east = eastM / (PI * EARTH_RADIUS_METERS / 180.0),
                north = atan(exp(northM / EARTH_RADIUS_METERS)) * 360.0 / PI - 90.0,
            )
        }

        fun parseToBBox(bbox: String): BBox {
            val parts = bbox.split(",")
            require(parts.size == 4)
            return BBox(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
        }
    }
}

data class GridCell(
    val zoom: Int,
    val cellX: Long,
    val cellY: Long,
) {
    fun toBBox(): BBox {
        val gridSize = GridValues.getGridSize(zoom)
        val earthRadius = 6378137.0

        val westM = cellX * gridSize
        val southM = cellY * gridSize
        val eastM = westM + gridSize
        val northM = southM + gridSize

        return BBox(
            west = westM / (PI * earthRadius / 180.0),
            south = atan(exp(southM / earthRadius)) * 360.0 / PI - 90.0,
            east = eastM / (PI * earthRadius / 180.0),
            north = atan(exp(northM / earthRadius)) * 360.0 / PI - 90.0,
        )
    }

    fun toClusterId(): String = ClusterId.format(zoom, cellX, cellY)
}

object ClusterId {
    private val PATTERN = Regex("""^z(\d+)_(-?\d+)_(-?\d+)$""")

    fun format(
        zoom: Int,
        cellX: Long,
        cellY: Long,
    ): String = "z${zoom}_${cellX}_$cellY"

    fun parse(clusterId: String): GridCell {
        val match =

            PATTERN.matchEntire(clusterId)

                ?: throw IllegalArgumentException("Invalid clusterId format: $clusterId")

        return GridCell(
            zoom = match.groupValues[1].toInt(),
            cellX = match.groupValues[2].toLong(),
            cellY = match.groupValues[3].toLong(),
        )
    }

    fun isValid(clusterId: String): Boolean = PATTERN.matches(clusterId)
}
