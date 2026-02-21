package kr.co.lokit.api.domain.photo.presentation.mapping

import kr.co.lokit.api.domain.photo.domain.CommentWithEmoticons
import kr.co.lokit.api.domain.photo.domain.EmoticonSummary
import kr.co.lokit.api.fixture.createComment
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommentDtoMappingsTest {
    @Test
    fun `댓글 응답 isEditable은 작성자와 조회자가 같을 때 true다`() {
        val comment =
            CommentWithEmoticons(
                comment = createComment(id = 1L, userId = 1L),
                userName = "user",
                userProfileImageUrl = null,
                emoticons = emptyList(),
            )

        assertTrue(comment.toResponse(1L).isEditable)
        assertFalse(comment.toResponse(2L).isEditable)
    }

    @Test
    fun `이모티콘 응답 isEditable은 reacted가 true일 때 true다`() {
        val comment =
            CommentWithEmoticons(
                comment = createComment(id = 1L, userId = 1L),
                userName = "user",
                userProfileImageUrl = null,
                emoticons =
                    listOf(
                        EmoticonSummary(emoji = "❤️", count = 1, reacted = true),
                        EmoticonSummary(emoji = "🔥", count = 2, reacted = false),
                    ),
            )

        val response = comment.toResponse(1L)

        assertTrue(response.emoticons.first { it.emoji == "❤️" }.isEditable)
        assertFalse(response.emoticons.first { it.emoji == "🔥" }.isEditable)
    }
}
