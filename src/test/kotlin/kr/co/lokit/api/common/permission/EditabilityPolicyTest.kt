package kr.co.lokit.api.common.permission

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EditabilityPolicyTest {
    @Test
    fun `소유 리소스는 생성자와 조회자가 같으면 수정 가능하다`() {
        assertTrue(EditabilityPolicy.canEditOwnedResource(viewerUserId = 1L, createdByUserId = 1L))
        assertFalse(EditabilityPolicy.canEditOwnedResource(viewerUserId = 1L, createdByUserId = 2L))
    }

    @Test
    fun `기본 앨범은 생성자 본인이어도 수정 불가능하다`() {
        assertFalse(EditabilityPolicy.canEditAlbum(viewerUserId = 1L, createdByUserId = 1L, isDefault = true))
        assertTrue(EditabilityPolicy.canEditAlbum(viewerUserId = 1L, createdByUserId = 1L, isDefault = false))
    }

    @Test
    fun `이모티콘은 reacted 여부로 수정 가능 여부를 판단한다`() {
        assertTrue(EditabilityPolicy.canEditEmoticon(reacted = true))
        assertFalse(EditabilityPolicy.canEditEmoticon(reacted = false))
    }
}
