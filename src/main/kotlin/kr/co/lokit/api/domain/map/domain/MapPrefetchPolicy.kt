package kr.co.lokit.api.domain.map.domain

object MapPrefetchPolicy {
    const val MAX_PREFETCH_CELLS = 120
    private const val MAX_PREFETCH_MARGIN_CELLS = 6
    private const val NORMAL_PAN_CELLS_PER_SEC = 1.5
    private const val FAST_PAN_CELLS_PER_SEC = 3.0

    fun marginBy(
        zoom: Int,
        cellSpeedPerSec: Double,
    ): Int {
        val baseMargin =
            when {
                zoom <= 10 -> 4
                zoom <= 12 -> 3
                zoom <= 14 -> 2
                zoom <= 16 -> 1
                else -> 0
            }
        val speedBoost =
            when {
                cellSpeedPerSec >= FAST_PAN_CELLS_PER_SEC -> 2
                cellSpeedPerSec >= NORMAL_PAN_CELLS_PER_SEC -> 1
                else -> 0
            }
        return (baseMargin + speedBoost).coerceAtMost(MAX_PREFETCH_MARGIN_CELLS)
    }

    fun directionalCandidates(
        requestedXMin: Long,
        requestedXMax: Long,
        requestedYMin: Long,
        requestedYMax: Long,
        margin: Int,
        dirX: Int,
        dirY: Int,
    ): Set<Pair<Long, Long>> {
        if (margin <= 0) return emptySet()

        val coords = LinkedHashSet<Pair<Long, Long>>()

        if (dirX > 0) {
            for (x in (requestedXMax + 1)..(requestedXMax + margin)) {
                for (y in (requestedYMin - margin)..(requestedYMax + margin)) {
                    coords.add(x to y)
                }
            }
        } else if (dirX < 0) {
            for (x in (requestedXMin - margin)..(requestedXMin - 1)) {
                for (y in (requestedYMin - margin)..(requestedYMax + margin)) {
                    coords.add(x to y)
                }
            }
        }

        if (dirY > 0) {
            for (y in (requestedYMax + 1)..(requestedYMax + margin)) {
                for (x in (requestedXMin - margin)..(requestedXMax + margin)) {
                    coords.add(x to y)
                }
            }
        } else if (dirY < 0) {
            for (y in (requestedYMin - margin)..(requestedYMin - 1)) {
                for (x in (requestedXMin - margin)..(requestedXMax + margin)) {
                    coords.add(x to y)
                }
            }
        }

        return coords
    }
}
