package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.map.application.port.MapClientPort
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.photo.application.port.PhotoRepositoryPort
import kr.co.lokit.api.fixture.createPhotoDetail
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PhotoQueryServiceTest {

    @Mock
    lateinit var photoRepository: PhotoRepositoryPort

    @Mock
    lateinit var albumRepository: AlbumRepositoryPort

    @Mock
    lateinit var mapClientPort: MapClientPort

    @InjectMocks
    lateinit var photoQueryService: PhotoQueryService

    @Test
    fun `사진 상세 정보를 조회할 수 있다`() {
        val photoDetail = createPhotoDetail(description = "테스트 사진")
        `when`(photoRepository.findDetailById(1L)).thenReturn(photoDetail)
        `when`(mapClientPort.reverseGeocode(127.0, 37.5)).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = null, regionName = "강남구"),
        )

        val result = photoQueryService.getPhotoDetail(1L)

        assertEquals(1L, result.id)
        assertEquals("여행", result.albumName)
        assertEquals("서울 강남구", result.address)
    }

    @Test
    fun `존재하지 않는 사진 조회 시 예외가 발생한다`() {
        `when`(photoRepository.findDetailById(999L)).thenThrow(
            BusinessException.ResourceNotFoundException(
                "Photo(id=999)을(를) 찾을 수 없습니다",
            ),
        )

        assertThrows<BusinessException.ResourceNotFoundException> {
            photoQueryService.getPhotoDetail(999L)
        }
    }
}
