package kr.co.lokit.api.domain.photo.presentation.mapping

import kr.co.lokit.api.domain.photo.domain.PhotoDetailReadModel
import kr.co.lokit.api.fixture.createAlbum
import kr.co.lokit.api.fixture.createLocation
import kr.co.lokit.api.fixture.createPhoto
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PhotoMappingsTest {
    @Test
    fun `사진 응답 isEditable은 업로더와 조회자가 같을 때 true다`() {
        val ownerId = 1L
        val photo = createPhoto(id = 1L, uploadedById = ownerId)

        val ownResponse = photo.toResponse(ownerId)
        val otherResponse = photo.toResponse(2L)

        assertTrue(ownResponse.isEditable)
        assertFalse(otherResponse.isEditable)
    }

    @Test
    fun `사진 상세 응답 isEditable은 업로더와 조회자가 같을 때 true다`() {
        val readModel =
            PhotoDetailReadModel(
                id = 1L,
                url = "https://example.com/photo.jpg",
                takenAt = LocalDateTime.now(),
                albumName = "여행",
                uploadedById = 1L,
                uploaderName = "업로더",
                uploaderProfileImageUrl = null,
                address = "서울",
                description = null,
                longitude = 127.0,
                latitude = 37.5,
            )

        assertTrue(readModel.toResponse(1L).isEditable)
        assertFalse(readModel.toResponse(2L).isEditable)
    }

    @Test
    fun `기본 앨범 응답은 생성자 본인이어도 isEditable이 false다`() {
        val ownerId = 1L
        val album =
            createAlbum(id = 1L, createdById = ownerId, isDefault = true).also {
                it.photos = listOf(createPhoto(id = 10L, uploadedById = ownerId, location = createLocation()))
            }

        val response = album.toAlbumWithPhotosResponse(ownerId)

        assertFalse(response.isEditable)
    }
}
