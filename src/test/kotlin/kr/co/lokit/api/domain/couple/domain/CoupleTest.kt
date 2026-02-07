package kr.co.lokit.api.domain.couple.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CoupleTest {

    @Test
    fun `정상적으로 커플을 생성할 수 있다`() {
        val couple = Couple(name = "우리 커플")

        assertEquals("우리 커플", couple.name)
        assertEquals(0L, couple.id)
    }

    @Test
    fun `기본값이 올바르게 설정된다`() {
        val couple = Couple(name = "테스트")

        assertEquals(0L, couple.id)
        assertNull(couple.inviteCode)
        assertEquals(emptyList(), couple.userIds)
    }

    @Test
    fun `모든 필드를 지정하여 커플을 생성할 수 있다`() {
        val couple = Couple(
            id = 1L,
            name = "우리 커플",
            inviteCode = "12345678",
            userIds = listOf(1L, 2L),
        )

        assertEquals(1L, couple.id)
        assertEquals("우리 커플", couple.name)
        assertEquals("12345678", couple.inviteCode)
        assertEquals(listOf(1L, 2L), couple.userIds)
    }
}
