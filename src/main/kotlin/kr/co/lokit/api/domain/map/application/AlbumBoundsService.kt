package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.application.port.AlbumBoundsRepositoryPort
import kr.co.lokit.api.domain.map.domain.AlbumBounds
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlbumBoundsService(
    private val albumBoundsRepository: AlbumBoundsRepositoryPort,
) {
    @Transactional
    fun updateBoundsOnPhotoAdd(
        albumId: Long,
        longitude: Double,
        latitude: Double,
    ) {
        val existingBounds = albumBoundsRepository.findByAlbumIdOrNull(albumId)

        existingBounds?.apply {
            val expanded = existingBounds.expandedWith(longitude, latitude)
            albumBoundsRepository.apply(expanded)
        } ?: run {
            albumBoundsRepository.save(
                AlbumBounds.createInitial(albumId, longitude, latitude),
            )
        }
    }

    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 50, multiplier = 2.0),
    )
    @Transactional
    fun updateBoundsFromLocations(
        albumId: Long,
        locations: List<Pair<Double, Double>>,
    ) {
        if (locations.isEmpty()) return

        val minLon = locations.minOf { it.first }
        val maxLon = locations.maxOf { it.first }
        val minLat = locations.minOf { it.second }
        val maxLat = locations.maxOf { it.second }

        val existingBounds = albumBoundsRepository.findByAlbumIdOrNull(albumId)

        if (existingBounds != null) {
            val expanded = existingBounds.copy(
                minLongitude = minOf(existingBounds.minLongitude, minLon),
                maxLongitude = maxOf(existingBounds.maxLongitude, maxLon),
                minLatitude = minOf(existingBounds.minLatitude, minLat),
                maxLatitude = maxOf(existingBounds.maxLatitude, maxLat),
            )
            albumBoundsRepository.apply(expanded)
        } else {
            albumBoundsRepository.save(
                AlbumBounds(
                    albumId = albumId,
                    minLongitude = minLon,
                    maxLongitude = maxLon,
                    minLatitude = minLat,
                    maxLatitude = maxLat,
                ),
            )
        }
    }
}
