package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.AlbumBounds
import kr.co.lokit.api.domain.map.infrastructure.AlbumBoundsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlbumBoundsService(
    private val albumBoundsRepository: AlbumBoundsRepository,
) {
    @Transactional
    fun updateBoundsOnPhotoAdd(
        albumId: Long,
        longitude: Double,
        latitude: Double,
    ) {
        val existingBounds = albumBoundsRepository.findByAlbumId(albumId)

        if (existingBounds == null) {
            albumBoundsRepository.save(
                AlbumBounds.createInitial(albumId, longitude, latitude),
            )
        } else {
            val expanded = existingBounds.expandedWith(longitude, latitude)
            albumBoundsRepository.updateBounds(expanded)
        }
    }
}
