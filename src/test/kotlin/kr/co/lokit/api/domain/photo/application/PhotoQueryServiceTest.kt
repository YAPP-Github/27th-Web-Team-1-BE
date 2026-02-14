package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.map.application.port.MapClientPort
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.photo.application.port.PhotoRepositoryPort
import kr.co.lokit.api.domain.photo.domain.DeIdentifiedUserProfile
import kr.co.lokit.api.fixture.createCouple
import kr.co.lokit.api.fixture.createPhotoDetail
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @InjectMocks
    lateinit var photoQueryService: PhotoQueryService

    @Test
    fun `사진 상세 정보를 조회할 수 있다`() {
        val photoDetail = createPhotoDetail(description = "테스트 사진", uploadedById = 2L)
        `when`(photoRepository.findDetailById(1L)).thenReturn(photoDetail)
        `when`(mapClientPort.reverseGeocode(127.0, 37.5)).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = null, regionName = "강남구"),
        )
        `when`(coupleRepository.findByUserId(1L)).thenReturn(
            createCouple(id = 1L, userIds = listOf(1L, 2L), status = CoupleStatus.CONNECTED),
        )

        val result = photoQueryService.getPhotoDetail(1L, 1L)

        assertEquals(1L, result.id)
        assertEquals("여행", result.albumName)
        assertEquals("서울 강남구", result.address)
        assertEquals("테스트", result.uploaderName)
    }

    @Test
    fun `존재하지 않는 사진 조회 시 예외가 발생한다`() {
        `when`(photoRepository.findDetailById(999L)).thenThrow(
            BusinessException.ResourceNotFoundException(
                "Photo(id=999)을(를) 찾을 수 없습니다",
            ),
        )

        assertThrows<BusinessException.ResourceNotFoundException> {
            photoQueryService.getPhotoDetail(999L, 1L)
        }
    }

    @Test
    fun `커플 연결 해제 시 끊은 사용자의 프로필이 비식별 처리된다`() {
        val disconnectedByUserId = 2L
        val viewerUserId = 1L
        val photoDetail = createPhotoDetail(
            uploadedById = disconnectedByUserId,
            uploaderName = "탈퇴한유저",
            uploaderProfileImageUrl = "https://example.com/profile.jpg",
        )
        `when`(photoRepository.findDetailById(1L)).thenReturn(photoDetail)
        `when`(mapClientPort.reverseGeocode(127.0, 37.5)).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = null, regionName = "강남구"),
        )
        `when`(coupleRepository.findByUserId(viewerUserId)).thenReturn(
            createCouple(
                id = 1L,
                userIds = listOf(viewerUserId, disconnectedByUserId),
                status = CoupleStatus.DISCONNECTED,
                disconnectedByUserId = disconnectedByUserId,
            ),
        )

        val result = photoQueryService.getPhotoDetail(1L, viewerUserId)

        assertEquals(DeIdentifiedUserProfile.DISPLAY_NAME, result.uploaderName)
        assertNull(result.uploaderProfileImageUrl)
    }

    @Test
    fun `커플 연결 해제 시 본인의 프로필은 비식별 처리되지 않는다`() {
        val disconnectedByUserId = 2L
        val viewerUserId = 1L
        val photoDetail = createPhotoDetail(
            uploadedById = viewerUserId,
            uploaderName = "나",
            uploaderProfileImageUrl = "https://example.com/my-profile.jpg",
        )
        `when`(photoRepository.findDetailById(1L)).thenReturn(photoDetail)
        `when`(mapClientPort.reverseGeocode(127.0, 37.5)).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = null, regionName = "강남구"),
        )
        `when`(coupleRepository.findByUserId(viewerUserId)).thenReturn(
            createCouple(
                id = 1L,
                userIds = listOf(viewerUserId, disconnectedByUserId),
                status = CoupleStatus.DISCONNECTED,
                disconnectedByUserId = disconnectedByUserId,
            ),
        )

        val result = photoQueryService.getPhotoDetail(1L, viewerUserId)

        assertEquals("나", result.uploaderName)
        assertEquals("https://example.com/my-profile.jpg", result.uploaderProfileImageUrl)
    }
}
