package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.album.infrastructure.AlbumRepository
import kr.co.lokit.api.domain.map.application.AlbumBoundsService
import kr.co.lokit.api.domain.map.infrastructure.geocoding.MapClient
import kr.co.lokit.api.domain.photo.domain.Location
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.domain.PhotoDetail
import kr.co.lokit.api.domain.photo.dto.UpdatePhotoRequest
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.photo.infrastructure.PhotoRepository
import kr.co.lokit.api.domain.photo.infrastructure.file.S3PresignedUrlGenerator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class PhotoServiceTest {

    @Mock
    lateinit var photoRepository: PhotoRepository

    @Mock
    lateinit var albumRepository: AlbumRepository

    @Mock
    lateinit var albumBoundsService: AlbumBoundsService

    @Mock
    lateinit var s3PresignedUrlGenerator: S3PresignedUrlGenerator

    @Mock
    lateinit var mapClient: MapClient

    @InjectMocks
    lateinit var photoService: PhotoService

    @Test
    fun `사진을 생성할 수 있다`() {
        val photo = Photo(albumId = 1L, location = Location(127.0, 37.5)).apply {
            url = "https://example.com/photo.jpg"
            uploadedById = 1L
        }
        val savedPhoto = Photo(id = 1L, albumId = 1L, location = Location(127.0, 37.5))
        `when`(photoRepository.save(photo)).thenReturn(savedPhoto)
        doNothing().`when`(albumBoundsService).updateBoundsOnPhotoAdd(1L, 127.0, 37.5)

        val result = photoService.create(photo)

        assertEquals(1L, result.id)
        verify(albumBoundsService).updateBoundsOnPhotoAdd(1L, 127.0, 37.5)
    }

    @Test
    fun `사진 상세 정보를 조회할 수 있다`() {
        val takenAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        val photoDetail = PhotoDetail(
            id = 1L,
            url = "https://example.com/photo.jpg",
            takenAt = takenAt,
            albumName = "여행",
            uploaderName = "테스트",
            location = Location(127.0, 37.5),
            description = "테스트 사진",
        )
        `when`(photoRepository.findDetailById(1L)).thenReturn(photoDetail)
        `when`(mapClient.reverseGeocode(127.0, 37.5)).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = null, regionName = "강남구"),
        )

        val result = photoService.getPhotoDetail(1L)

        assertEquals(1L, result.id)
        assertEquals("여행", result.albumName)
        assertEquals("서울 강남구", result.address)
    }

    @Test
    fun `존재하지 않는 사진 조회 시 예외가 발생한다`() {
        `when`(photoRepository.findDetailById(999L)).thenReturn(null)

        assertThrows<BusinessException.ResourceNotFoundException> {
            photoService.getPhotoDetail(999L)
        }
    }

    @Test
    fun `사진을 삭제할 수 있다`() {
        photoService.delete(1L)

        verify(photoRepository).deleteById(1L)
    }

    @Test
    fun `사진을 수정할 수 있다`() {
        val request = UpdatePhotoRequest(description = "수정된 설명")
        val updatedPhoto = Photo(id = 1L, albumId = 1L, location = Location(127.0, 37.5), description = "수정된 설명")
        `when`(photoRepository.update(1L, request)).thenReturn(updatedPhoto)

        val result = photoService.update(1L, request)

        assertEquals("수정된 설명", result.description)
    }

    @Test
    fun `사진 위치 수정 시 앨범 바운드도 업데이트된다`() {
        val request = UpdatePhotoRequest(longitude = 128.0, latitude = 38.0)
        val updatedPhoto = Photo(id = 1L, albumId = 1L, location = Location(128.0, 38.0))
        `when`(photoRepository.update(1L, request)).thenReturn(updatedPhoto)
        doNothing().`when`(albumBoundsService).updateBoundsOnPhotoAdd(1L, 128.0, 38.0)

        photoService.update(1L, request)

        verify(albumBoundsService).updateBoundsOnPhotoAdd(1L, 128.0, 38.0)
    }
}
