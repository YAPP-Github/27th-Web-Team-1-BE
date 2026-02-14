package kr.co.lokit.api.domain.map.domain

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class BBox(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
) {
    fun isWithin(other: BBox): Boolean =
        west >= other.west &&
            south >= other.south &&
            east <= other.east &&
            north <= other.north

    fun intersects(other: BBox): Boolean =
        west < other.east && east > other.west && south < other.north && north > other.south

    fun clampTo(other: BBox): BBox? {
        val clampedWest = max(west, other.west)
        val clampedSouth = max(south, other.south)
        val clampedEast = min(east, other.east)
        val clampedNorth = min(north, other.north)
        return if (clampedWest < clampedEast && clampedSouth < clampedNorth) {
            BBox(
                west = clampedWest,
                south = clampedSouth,
                east = clampedEast,
                north = clampedNorth,
            )
        } else {
            null
        }
    }

    fun clampToKorea(): BBox? = clampTo(KOREA_BOUNDS)

    companion object {
        val KOREA_BOUNDS = BBox(west = 124.0, south = 33.0, east = 132.0, north = 39.5)
        private const val HORIZONTAL_MULTIPLIER = 2.5
        private const val VERTICAL_MULTIPLIER = 5.0

        fun fromRequestedBoundsInKorea(
            west: Double,
            south: Double,
            east: Double,
            north: Double,
        ): BBox {
            require(west < east) { "west must be less than east" }
            require(south < north) { "south must be less than north" }

            val bbox = BBox(west = west, south = south, east = east, north = north)
            require(bbox.isWithin(KOREA_BOUNDS)) { "bbox must be within KOREA_BOUNDS" }
            return bbox
        }

        fun fromCenter(
            zoom: Int,
            longitude: Double,
            latitude: Double,
        ): BBox {
            val mx = MercatorProjection.longitudeToMeters(longitude)
            val my = MercatorProjection.latitudeToMeters(latitude)

            val worldSize = 2 * PI * MercatorProjection.EARTH_RADIUS_METERS
            val tileSizeAtZoom = worldSize / 2.0.pow(zoom.toDouble())
            val hHalf = tileSizeAtZoom * HORIZONTAL_MULTIPLIER
            val vHalf = tileSizeAtZoom * VERTICAL_MULTIPLIER

            val gridSize = GridValues.getGridSize(zoom)
            val westM = floor((mx - hHalf) / gridSize) * gridSize
            val southM = floor((my - vHalf) / gridSize) * gridSize
            val eastM = ceil((mx + hHalf) / gridSize) * gridSize
            val northM = ceil((my + vHalf) / gridSize) * gridSize

            return BBox(
                west = MercatorProjection.metersToLongitude(westM),
                south = MercatorProjection.metersToLatitude(southM),
                east = MercatorProjection.metersToLongitude(eastM),
                north = MercatorProjection.metersToLatitude(northM),
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

        val westM = cellX * gridSize
        val southM = cellY * gridSize
        val eastM = westM + gridSize
        val northM = southM + gridSize

        return BBox(
            west = MercatorProjection.metersToLongitude(westM),
            south = MercatorProjection.metersToLatitude(southM),
            east = MercatorProjection.metersToLongitude(eastM),
            north = MercatorProjection.metersToLatitude(northM),
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
            PATTERN.matchEntire(clusterId) ?: throw IllegalArgumentException("Invalid clusterId format: $clusterId")

        return GridCell(
            zoom = match.groupValues[1].toInt(),
            cellX = match.groupValues[2].toLong(),
            cellY = match.groupValues[3].toLong(),
        )
    }

    fun isValid(clusterId: String): Boolean = PATTERN.matches(clusterId)
}
