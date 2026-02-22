package kr.co.lokit.api.domain.album.presentation.mapping

import kr.co.lokit.api.fixture.createAlbum
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AlbumDtoMappingsTest {
    @Test
    fun `기본 앨범은 생성자 본인에게도 isEditable이 false다`() {
        val ownerId = 1L
        val album = createAlbum(id = 10L, createdById = ownerId, isDefault = true)

        val response = listOf(album).toSelectableResponse(ownerId)

        assertFalse(response.albums.first().isEditable)
    }

    @Test
    fun `일반 앨범은 생성자 본인에게 isEditable이 true다`() {
        val ownerId = 1L
        val album = createAlbum(id = 10L, createdById = ownerId, isDefault = false)

        val response = listOf(album).toSelectableResponse(ownerId)

        assertTrue(response.albums.first().isEditable)
    }
}
