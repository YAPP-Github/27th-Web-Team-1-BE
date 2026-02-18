package kr.co.lokit.api.domain.map.domain

import kotlin.math.floor

data class CellWindow(
    val xMin: Long,
    val xMax: Long,
    val yMin: Long,
    val yMax: Long,
)

data class MeterBounds(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
)

object MapGridIndex {
    fun toCell(
        longitude: Double,
        latitude: Double,
        gridSize: Double,
    ): Pair<Long, Long> =
        floor(MercatorProjection.longitudeToMeters(longitude) / gridSize).toLong() to
            floor(MercatorProjection.latitudeToMeters(latitude) / gridSize).toLong()

    fun toCellWindow(
        bbox: BBox,
        gridSize: Double,
    ): CellWindow {
        val (xMin, yMin) = toCell(bbox.west, bbox.south, gridSize)
        val (xMax, yMax) = toCell(bbox.east, bbox.north, gridSize)
        return CellWindow(
            xMin = xMin,
            xMax = xMax,
            yMin = yMin,
            yMax = yMax,
        )
    }

    fun toMeterBounds(
        coords: Set<Pair<Long, Long>>,
        gridSize: Double,
    ): MeterBounds {
        require(coords.isNotEmpty()) { "coords must not be empty" }
        return MeterBounds(
            west = coords.minOf { it.first } * gridSize,
            south = coords.minOf { it.second } * gridSize,
            east = (coords.maxOf { it.first } + 1) * gridSize,
            north = (coords.maxOf { it.second } + 1) * gridSize,
        )
    }
}
