package kr.co.lokit.api.domain.map.domain

import kotlin.math.floor
import kotlin.math.ceil

/**
 * 공간 영역을 나타내는 Bounding Box.
 */
data class BBox(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
) {
    companion object {
        fun fromCenter(zoom: Int, longitude: Double, latitude: Double): BBox {
            val gridSize = GridValues.getGridSize(zoom - 1)
            val inverseGridSize = 1.0 / gridSize
            val halfGridSize = gridSize / 2.0

             return BBox(
                 west = floor((longitude - halfGridSize) * inverseGridSize) * gridSize,
                 south = floor((latitude - halfGridSize) * inverseGridSize) * gridSize,
                 east = ceil((longitude + halfGridSize) * inverseGridSize) * gridSize,
                 north = ceil((latitude + halfGridSize) * inverseGridSize) * gridSize,
             )
        }

        fun fromString(bbox: String): BBox {
            val parts = bbox.split(",")
            require(parts.size == 4) { "bbox는 ,로 구분된 네 가지 실수 값을 가져아한다: west,south,east,north" }
            return BBox(
                west = parts[0].toDouble(),
                south = parts[1].toDouble(),
                east = parts[2].toDouble(),
                north = parts[3].toDouble(),
            )
        }

        fun fromStringCenter(bbox: String, zoom: Int): BBox {
            val parsed = fromString(bbox)
            val gridSize = GridValues.getGridSize(zoom - 1)
            val inverseGridSize = 1.0 / gridSize
            return BBox(
                west = floor(parsed.west * inverseGridSize) * gridSize,
                south = floor(parsed.south * inverseGridSize) * gridSize,
                east = ceil(parsed.east * inverseGridSize) * gridSize,
                north = ceil(parsed.north * inverseGridSize) * gridSize,
            )
        }
    }
}

/**
 * 클러스터 ID 포맷: z{zoom}_{cellX}_{cellY}
 * 예시: z14_130234_38456
 */
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

/**
 * 그리드 셀 정보 (디코딩된 clusterId).
 */
data class GridCell(
    val zoom: Int,
    val cellX: Long,
    val cellY: Long,
) {
    fun toBBox(): BBox {
        val gridSize = GridValues.getGridSize(zoom - 1)
        val west = cellX * gridSize
        val south = cellY * gridSize
        return BBox(
            west = west,
            south = south,
            east = west + gridSize,
            north = south + gridSize,
        )
    }

    fun toClusterId(): String = ClusterId.format(zoom, cellX, cellY)
}
