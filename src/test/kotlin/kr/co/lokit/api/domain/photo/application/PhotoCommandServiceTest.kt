package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.map.application.MapPhotosCacheService
import kr.co.lokit.api.domain.map.application.port.`in`.SearchLocationUseCase
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.photo.application.port.PhotoRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.PhotoStoragePort
import kr.co.lokit.api.domain.photo.domain.PhotoCreatedEvent
import kr.co.lokit.api.domain.photo.domain.PhotoDeletedEvent
import kr.co.lokit.api.domain.photo.domain.PhotoLocationUpdatedEvent
import kr.co.lokit.api.fixture.createLocation
import kr.co.lokit.api.fixture.createPhoto
import kr.co.lokit.api.fixture.createUpdatePhotoRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationEventPublisher

@ExtendWith(MockitoExtension::class)
class PhotoCommandServiceTest {

    @Mock
    lateinit var photoRepository: PhotoRepositoryPort

    @Mock
    lateinit var albumRepository: AlbumRepositoryPort

    @Mock
    lateinit var photoStoragePort: PhotoStoragePort

    @Mock
    lateinit var eventPublisher: ApplicationEventPublisher

    @Mock
    lateinit var mapQueryService: SearchLocationUseCase

    @Mock
    lateinit var mapPhotosCacheService: MapPhotosCacheService

    @Mock
    lateinit var cacheManager: CacheManager

    @InjectMocks
    lateinit var photoCommandService: PhotoCommandService

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = any<T>() as T

    @Test
    fun `사진을 생성할 수 있다`() {
        val photo = createPhoto(albumId = 1L, location = createLocation(127.0, 37.5))
        val savedPhoto = createPhoto(
            id = 1L,
            albumId = 1L,
            coupleId = 1L,
            url = "https://example.com/picture.png",
            location = createLocation(127.0, 37.5)
        )
        doNothing().`when`(photoStoragePort).verifyFileExists(photo.url)
        `when`(mapQueryService.getLocationInfo(anyDouble(), anyDouble())).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = null, regionName = "강남구"),
        )
        `when`(photoRepository.save(anyObject())).thenReturn(savedPhoto)

        val result = photoCommandService.create(photo)

        assertEquals(1L, result.id)
        verify(eventPublisher).publishEvent(any(PhotoCreatedEvent::class.java))
    }

    @Test
    fun `사진을 삭제할 수 있다`() {
        val photo = createPhoto(id = 1L, url = "https://example.com/photo.jpg", uploadedById = 1L)
        `when`(photoRepository.findById(1L)).thenReturn(photo)

        photoCommandService.delete(1L, 1L)

        verify(photoRepository).deleteById(1L)
        verify(eventPublisher).publishEvent(PhotoDeletedEvent(photoUrl = photo.url))
    }

    @Test
    fun `사진을 수정할 수 있다`() {
        val request = createUpdatePhotoRequest(1L, 0.0, 0.0, description = "수정된 설명")
        val originalPhoto =
            createPhoto(id = 1L, albumId = 1L, coupleId = 1L, uploadedById = 1L, location = createLocation(127.0, 37.5))
        val updatedPhoto =
            createPhoto(
                id = 1L,
                albumId = 1L,
                coupleId = 1L,
                description = "수정된 설명",
                location = createLocation(0.0, 0.0),
                uploadedById = 1L
            )
        `when`(photoRepository.findById(1L)).thenReturn(originalPhoto)
        `when`(photoRepository.apply(anyObject())).thenReturn(updatedPhoto)

        val result = photoCommandService.update(1L, 1L, request.description, request.longitude, request.latitude, 1L)

        assertEquals("수정된 설명", result.description)
    }

    @Test
    fun `사진 위치 수정 시 이벤트가 발행된다`() {
        val request = createUpdatePhotoRequest(1L, longitude = 128.0, latitude = 38.0)
        val originalPhoto =
            createPhoto(id = 1L, albumId = 1L, coupleId = 1L, uploadedById = 1L, location = createLocation(127.0, 37.5))
        val updatedPhoto =
            createPhoto(
                id = 1L,
                albumId = 1L,
                coupleId = 1L,
                location = createLocation(longitude = 128.0, latitude = 38.0),
                uploadedById = 1L
            )
        `when`(photoRepository.findById(1L)).thenReturn(originalPhoto)
        `when`(photoRepository.apply(anyObject())).thenReturn(updatedPhoto)

        photoCommandService.update(1L, 1L, request.description, request.longitude, request.latitude, 1L)

        verify(eventPublisher).publishEvent(anyObject<PhotoLocationUpdatedEvent>())
    }
}
