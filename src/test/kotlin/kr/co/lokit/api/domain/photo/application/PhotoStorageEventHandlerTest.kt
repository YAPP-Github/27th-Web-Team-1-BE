package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.domain.photo.application.port.PhotoStoragePort
import kr.co.lokit.api.domain.photo.domain.PhotoDeletedEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PhotoStorageEventHandlerTest {

    @Mock
    lateinit var photoStoragePort: PhotoStoragePort

    @InjectMocks
    lateinit var handler: PhotoStorageEventHandler

    @Test
    fun `S3 활성화 시 deleteFileByUrl 호출`() {
        val event = PhotoDeletedEvent(photoUrl = "https://bucket.s3.region.amazonaws.com/photos/test.jpg")

        handler.handlePhotoDeleted(event)

        verify(photoStoragePort).deleteFileByUrl(event.photoUrl)
    }

    @Test
    fun `S3 비활성화 시 정상 스킵`() {
        val handler = PhotoStorageEventHandler(photoStoragePort = null)
        val event = PhotoDeletedEvent(photoUrl = "https://bucket.s3.region.amazonaws.com/photos/test.jpg")

        handler.handlePhotoDeleted(event)
    }

    @Test
    fun `S3 삭제 실패 시 예외 미전파`() {
        val event = PhotoDeletedEvent(photoUrl = "https://bucket.s3.region.amazonaws.com/photos/test.jpg")
        doThrow(RuntimeException("S3 error")).`when`(photoStoragePort).deleteFileByUrl(event.photoUrl)

        handler.handlePhotoDeleted(event)
    }
}
