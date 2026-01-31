package kr.co.lokit.api.domain.map.domain

/**
 * 지도 줌 레벨에 따른 클러스터링 그리드 크기 설정.
 * - z10: ~1.7km
 * - z11: ~850m
 * - z12: ~425m
 * - z13: ~210m
 * - z14: ~105m
 */
object GridValues {
    private val GRID_SIZES: Map<Int, Double> =
        mapOf(
            10 to 0.015625,          // 1/64 degrees
            11 to 0.0078125,         // 1/128 degrees
            12 to 0.00390625,        // 1/256 degrees
            13 to 0.001953125,       // 1/512 degrees
            14 to 0.0009765625,      // 1/1024 degrees
            15 to 0.00048828125,     // 1/2048 degrees
            16 to 0.000244140625,    // 1/4096 degrees
            17 to 0.0001220703125,   // 1/8192 degrees
        )

    const val HOME_ZOOM_LEVEL = 13
    const val CLUSTER_ZOOM_THRESHOLD = 18
    private val DEFAULT_GRID_SIZE: Double = GRID_SIZES[HOME_ZOOM_LEVEL]!!

    fun getGridSize(zoom: Int): Double = GRID_SIZES[zoom] ?: DEFAULT_GRID_SIZE

    fun getSupportedZoomLevels(): Set<Int> = GRID_SIZES.keys
}
