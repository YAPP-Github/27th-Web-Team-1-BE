package kr.co.lokit.api.domain.map.domain

/**
 * 지도 줌 레벨에 따른 클러스터링 그리드 크기 설정.
 * - z10: ~1.7km
 * - z11: ~850m
 * - z12: ~425m
 * - z13: ~210m
 * - z14: ~105m
 */
object GridConfig {
    private val GRID_SIZES: Map<Int, Double> =
        mapOf(
            10 to 0.015625,      // 1/64 degrees
            11 to 0.0078125,     // 1/128 degrees
            12 to 0.00390625,    // 1/256 degrees
            13 to 0.001953125,   // 1/512 degrees
            14 to 0.0009765625,  // 1/1024 degrees
        )

    private const val DEFAULT_GRID_SIZE = 0.001953125 // ~210m

    fun getGridSize(zoom: Int): Double = GRID_SIZES[zoom] ?: DEFAULT_GRID_SIZE

    fun getSupportedZoomLevels(): Set<Int> = GRID_SIZES.keys
}