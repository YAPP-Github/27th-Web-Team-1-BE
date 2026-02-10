package kr.co.lokit.api.domain.map.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AlbumBoundsTest {

    @Test
    fun `초기 바운드를 생성할 수 있다`() {
        val bounds = AlbumBounds.createInitial(1L, BoundsIdType.ALBUM, 127.0, 37.5)

        assertEquals(1L, bounds.standardId)
        assertEquals(BoundsIdType.ALBUM, bounds.idType)
        assertEquals(127.0, bounds.minLongitude)
        assertEquals(127.0, bounds.maxLongitude)
        assertEquals(37.5, bounds.minLatitude)
        assertEquals(37.5, bounds.maxLatitude)
    }

    @Test
    fun `중심 좌표를 올바르게 계산한다`() {
        val bounds = AlbumBounds(
            standardId = 1L,
            idType = BoundsIdType.ALBUM,
            minLongitude = 126.0,
            maxLongitude = 128.0,
            minLatitude = 37.0,
            maxLatitude = 38.0,
        )

        assertEquals(127.0, bounds.centerLongitude)
        assertEquals(37.5, bounds.centerLatitude)
    }

    @Test
    fun `새로운 좌표로 바운드를 확장할 수 있다`() {
        val bounds = AlbumBounds.createInitial(1L, BoundsIdType.ALBUM, 127.0, 37.5)

        val expanded = bounds.expandedWith(126.0, 38.0)

        assertEquals(126.0, expanded.minLongitude)
        assertEquals(127.0, expanded.maxLongitude)
        assertEquals(37.5, expanded.minLatitude)
        assertEquals(38.0, expanded.maxLatitude)
    }

    @Test
    fun `바운드 범위 안의 좌표로는 확장되지 않는다`() {
        val bounds = AlbumBounds(
            standardId = 1L,
            idType = BoundsIdType.ALBUM,
            minLongitude = 126.0,
            maxLongitude = 128.0,
            minLatitude = 37.0,
            maxLatitude = 38.0,
        )

        val expanded = bounds.expandedWith(127.0, 37.5)

        assertEquals(126.0, expanded.minLongitude)
        assertEquals(128.0, expanded.maxLongitude)
        assertEquals(37.0, expanded.minLatitude)
        assertEquals(38.0, expanded.maxLatitude)
    }
}
