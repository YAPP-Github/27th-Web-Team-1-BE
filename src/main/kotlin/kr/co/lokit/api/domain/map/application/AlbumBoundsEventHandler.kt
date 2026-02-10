package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.photo.domain.PhotoCreatedEvent
import kr.co.lokit.api.domain.photo.domain.PhotoLocationUpdatedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AlbumBoundsEventHandler(
    private val albumBoundsService: AlbumBoundsService,
) {
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handlePhotoCreated(event: PhotoCreatedEvent) {
        albumBoundsService.updateBoundsOnPhotoAdd(
            albumId = event.albumId,
            coupleId = event.coupleId,
            longitude = event.longitude,
            latitude = event.latitude,
        )
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handlePhotoLocationUpdated(event: PhotoLocationUpdatedEvent) {
        albumBoundsService.updateBoundsOnPhotoAdd(
            albumId = event.albumId,
            coupleId = event.coupleId,
            longitude = event.longitude,
            latitude = event.latitude,
        )
    }
}
