package kr.co.lokit.api.domain.map.application.port

import kr.co.lokit.api.domain.map.domain.AlbumBounds
import kr.co.lokit.api.domain.map.domain.BoundsIdType

interface AlbumBoundsRepositoryPort {
    fun save(bounds: AlbumBounds): AlbumBounds
    fun findByStandardIdAndIdType(standardId: Long, idType: BoundsIdType): AlbumBounds?
    fun apply(bounds: AlbumBounds): AlbumBounds
}
