package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.domain.photo.application.port.PhotoStoragePort
import kr.co.lokit.api.domain.photo.domain.PhotoDeletedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PhotoStorageEventHandler(
    private val photoStoragePort: PhotoStoragePort?,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener
    fun handlePhotoDeleted(event: PhotoDeletedEvent) {
        val port = photoStoragePort ?: return
        try {
            port.deleteFileByUrl(event.photoUrl)
        } catch (e: Exception) {
            log.warn("S3 파일 삭제 실패 (고아 스케줄러에 위임): url={}", event.photoUrl, e)
        }
    }
}
