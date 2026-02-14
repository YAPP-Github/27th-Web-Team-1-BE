package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.common.util.orZero

object MapCacheKeyFactory {
    data class ParsedCellKey(
        val zoom: Int,
        val cellX: Long,
        val cellY: Long,
        val coupleId: Long,
        val albumId: Long,
        val version: Long,
    )

    data class ParsedIndividualKey(
        val west: Double,
        val south: Double,
        val east: Double,
        val north: Double,
        val coupleId: Long,
        val albumId: Long,
    )

    @JvmStatic
    fun buildCellKey(
        zoom: Int,
        cellX: Long,
        cellY: Long,
        coupleId: Long?,
        albumId: Long?,
        version: Long,
    ): String = "${buildCellBaseKey(zoom, cellX, cellY, coupleId, albumId)}_v$version"

    @JvmStatic
    fun buildCellBaseKey(
        zoom: Int,
        cellX: Long,
        cellY: Long,
        coupleId: Long?,
        albumId: Long?,
    ): String = "z${zoom}_x${cellX}_y${cellY}_c${normalizeId(coupleId)}_a${normalizeId(albumId)}"

    @JvmStatic
    fun buildIndividualKey(
        bbox: BBox,
        zoom: Int,
        coupleId: Long?,
        albumId: Long?,
        version: Long,
    ): String {
        val west = toScaledInt(bbox.west)
        val south = toScaledInt(bbox.south)
        val east = toScaledInt(bbox.east)
        val north = toScaledInt(bbox.north)
        return "ind_z${zoom}_w${west}_s${south}_e${east}_n${north}_c${normalizeId(
            coupleId,
        )}_a${normalizeId(albumId)}_v$version"
    }

    fun parseCellKey(key: String): ParsedCellKey? {
        val match = CELL_KEY_REGEX.matchEntire(key) ?: return null
        return ParsedCellKey(
            zoom = match.groupValues[1].toInt(),
            cellX = match.groupValues[2].toLong(),
            cellY = match.groupValues[3].toLong(),
            coupleId = match.groupValues[4].toLong(),
            albumId = match.groupValues[5].toLong(),
            version = match.groupValues[6].toLong(),
        )
    }

    fun parseIndividualKey(key: String): ParsedIndividualKey? {
        val match = INDIVIDUAL_KEY_REGEX.matchEntire(key) ?: return null
        return ParsedIndividualKey(
            west = match.groupValues[2].toLong() / SCALE_FACTOR,
            south = match.groupValues[3].toLong() / SCALE_FACTOR,
            east = match.groupValues[4].toLong() / SCALE_FACTOR,
            north = match.groupValues[5].toLong() / SCALE_FACTOR,
            coupleId = match.groupValues[6].toLong(),
            albumId = match.groupValues[7].toLong(),
        )
    }

    fun buildRequestStateKey(
        zoom: Int,
        coupleId: Long,
        albumId: Long?,
    ): String = "z${zoom}_c${coupleId}_a${normalizeId(albumId)}"

    private fun normalizeId(value: Long?): Long = value.orZero()

    private fun toScaledInt(value: Double): Long = (value * SCALE_FACTOR).toLong()

    private const val SCALE_FACTOR = 1_000_000.0
    private val CELL_KEY_REGEX = Regex("^z(-?\\d+)_x(-?\\d+)_y(-?\\d+)_c(-?\\d+)_a(-?\\d+)_v(-?\\d+)$")
    private val INDIVIDUAL_KEY_REGEX =
        Regex("^ind_z(-?\\d+)_w(-?\\d+)_s(-?\\d+)_e(-?\\d+)_n(-?\\d+)_c(-?\\d+)_a(-?\\d+)_v(-?\\d+)$")
}
