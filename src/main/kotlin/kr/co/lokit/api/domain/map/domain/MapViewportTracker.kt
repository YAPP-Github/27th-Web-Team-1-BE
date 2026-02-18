package kr.co.lokit.api.domain.map.domain

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max

class MapViewportTracker {
    private data class ViewportState(
        val centerX: Long,
        val centerY: Long,
        val requestedAtMillis: Long,
    )

    private val requestStates = ConcurrentHashMap<String, ViewportState>()

    fun clearForCouple(coupleId: Long) {
        requestStates.keys.removeIf { it.contains("_c${coupleId}_") }
    }

    fun directionalPrefetchCells(
        zoom: Int,
        gridSize: Double,
        requestedWindow: CellWindow,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
    ): Set<Pair<Long, Long>> {
        if (coupleId == null) {
            return emptySet()
        }

        val centerLon = (bbox.west + bbox.east) / 2.0
        val centerLat = (bbox.south + bbox.north) / 2.0
        val (centerX, centerY) = MapGridIndex.toCell(centerLon, centerLat, gridSize)
        val now = System.currentTimeMillis()
        val stateKey = "z${zoom}_c${coupleId}_a${albumId ?: 0L}"
        val previous = requestStates.put(stateKey, ViewportState(centerX, centerY, now)) ?: return emptySet()

        val deltaX = centerX - previous.centerX
        val deltaY = centerY - previous.centerY
        if (deltaX == 0L && deltaY == 0L) {
            return emptySet()
        }

        val elapsedMillis = max(1L, now - previous.requestedAtMillis)
        val cellSpeedPerSec = (max(abs(deltaX), abs(deltaY)).toDouble() * 1000.0) / elapsedMillis
        val margin = MapPrefetchPolicy.marginBy(zoom, cellSpeedPerSec)
        if (margin <= 0) {
            return emptySet()
        }

        val koreaWindow = MapGridIndex.toCellWindow(BBox.KOREA_BOUNDS, gridSize)
        val dirX = deltaX.compareTo(0L)
        val dirY = deltaY.compareTo(0L)

        return MapPrefetchPolicy
            .directionalCandidates(
                requestedXMin = requestedWindow.xMin,
                requestedXMax = requestedWindow.xMax,
                requestedYMin = requestedWindow.yMin,
                requestedYMax = requestedWindow.yMax,
                margin = margin,
                dirX = dirX,
                dirY = dirY,
            ).asSequence()
            .filter { (x, y) ->
                x in koreaWindow.xMin..koreaWindow.xMax &&
                    y in koreaWindow.yMin..koreaWindow.yMax &&
                    (x !in requestedWindow.xMin..requestedWindow.xMax || y !in requestedWindow.yMin..requestedWindow.yMax)
            }.take(MapPrefetchPolicy.MAX_PREFETCH_CELLS)
            .toSet()
    }
}
