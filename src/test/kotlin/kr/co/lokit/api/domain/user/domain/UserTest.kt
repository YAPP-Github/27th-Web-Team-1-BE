package kr.co.lokit.api.domain.user.domain

import kr.co.lokit.api.common.constant.UserRole
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UserTest {

    @Test
    fun `정상적으로 유저를 생성할 수 있다`() {
        val user = User(email = "test@test.com", name = "테스트")

        assertEquals(0L, user.id)
        assertEquals("test@test.com", user.email)
        assertEquals("테스트", user.name)
        assertEquals(UserRole.USER, user.role)
    }

    @Test
    fun `관리자 역할로 유저를 생성할 수 있다`() {
        val user = User(email = "admin@test.com", name = "관리자", role = UserRole.ADMIN)

        assertEquals(UserRole.ADMIN, user.role)
    }

    @Test
    fun `모든 필드를 지정하여 유저를 생성할 수 있다`() {
        val user = User(id = 1L, email = "test@test.com", name = "테스트", role = UserRole.USER)

        assertEquals(1L, user.id)
        assertEquals("test@test.com", user.email)
        assertEquals("테스트", user.name)
    }
}
