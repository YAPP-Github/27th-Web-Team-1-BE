package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.application.port.AlbumBoundsRepositoryPort
import kr.co.lokit.api.domain.map.domain.AlbumBounds
import kr.co.lokit.api.domain.map.domain.BoundsIdType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlbumBoundsService(
    private val albumBoundsRepository: AlbumBoundsRepositoryPort,
) {
    @Transactional
    fun updateBoundsOnPhotoAdd(
        albumId: Long,
        coupleId: Long,
        longitude: Double,
        latitude: Double,
    ) {
        updateAlbumBounds(albumId, BoundsIdType.ALBUM, longitude, latitude)
        updateAlbumBounds(coupleId, BoundsIdType.COUPLE, longitude, latitude)
    }

    private fun updateAlbumBounds(
        id: Long,
        idType: BoundsIdType,
        longitude: Double,
        latitude: Double,
    ) {
        val existingBounds = albumBoundsRepository.findByStandardIdAndIdType(id, idType)
        if (existingBounds != null) {
            val expanded = existingBounds.expandedWith(longitude, latitude)
            albumBoundsRepository.update(expanded)
        } else {
            albumBoundsRepository.save(
                AlbumBounds.createInitial(id, idType, longitude, latitude),
            )
        }
    }
}
