package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.constant.DeIdentification
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.CommentRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.EmoticonRepositoryPort
import kr.co.lokit.api.domain.photo.domain.CommentWithEmoticons
import kr.co.lokit.api.fixture.createComment
import kr.co.lokit.api.fixture.createCouple
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class CommentServiceTest {

    @Mock
    lateinit var commentRepository: CommentRepositoryPort

    @Mock
    lateinit var emoticonRepository: EmoticonRepositoryPort

    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @InjectMocks
    lateinit var commentService: CommentService

    @Test
    fun `커플 연결 해제 시 끊은 사용자의 댓글이 비식별 처리된다`() {
        val disconnectedByUserId = 2L
        val viewerUserId = 1L
        val photoId = 10L

        val comments = listOf(
            CommentWithEmoticons(
                comment = createComment(id = 1L, userId = disconnectedByUserId, photoId = photoId),
                userName = "탈퇴한유저",
                userProfileImageUrl = "https://example.com/profile.jpg",
                emoticons = emptyList(),
            ),
            CommentWithEmoticons(
                comment = createComment(id = 2L, userId = viewerUserId, photoId = photoId),
                userName = "나",
                userProfileImageUrl = "https://example.com/my-profile.jpg",
                emoticons = emptyList(),
            ),
        )

        `when`(commentRepository.findAllByPhotoIdWithEmoticons(photoId, viewerUserId)).thenReturn(comments)
        `when`(coupleRepository.findByUserId(viewerUserId)).thenReturn(
            createCouple(
                id = 1L,
                userIds = listOf(viewerUserId, disconnectedByUserId),
                status = CoupleStatus.DISCONNECTED,
                disconnectedByUserId = disconnectedByUserId,
            ),
        )

        val result = commentService.getComments(photoId, viewerUserId)

        assertEquals(2, result.size)
        // 끊은 사용자의 댓글은 비식별 처리
        assertEquals(DeIdentification.DEFAULT_NAME, result[0].userName)
        assertNull(result[0].userProfileImageUrl)
        // 본인 댓글은 그대로
        assertEquals("나", result[1].userName)
        assertEquals("https://example.com/my-profile.jpg", result[1].userProfileImageUrl)
    }

    @Test
    fun `커플 연결 상태면 댓글이 비식별 처리되지 않는다`() {
        val viewerUserId = 1L
        val partnerUserId = 2L
        val photoId = 10L

        val comments = listOf(
            CommentWithEmoticons(
                comment = createComment(id = 1L, userId = partnerUserId, photoId = photoId),
                userName = "파트너",
                userProfileImageUrl = "https://example.com/partner.jpg",
                emoticons = emptyList(),
            ),
        )

        `when`(commentRepository.findAllByPhotoIdWithEmoticons(photoId, viewerUserId)).thenReturn(comments)
        `when`(coupleRepository.findByUserId(viewerUserId)).thenReturn(
            createCouple(
                id = 1L,
                userIds = listOf(viewerUserId, partnerUserId),
                status = CoupleStatus.CONNECTED,
            ),
        )

        val result = commentService.getComments(photoId, viewerUserId)

        assertEquals(1, result.size)
        assertEquals("파트너", result[0].userName)
        assertEquals("https://example.com/partner.jpg", result[0].userProfileImageUrl)
    }
}
