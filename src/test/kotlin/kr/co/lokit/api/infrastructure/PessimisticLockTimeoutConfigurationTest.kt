package kr.co.lokit.api.infrastructure

import kr.co.lokit.api.common.concurrency.LockPolicy
import kr.co.lokit.api.domain.couple.infrastructure.CoupleJpaRepository
import kr.co.lokit.api.domain.couple.infrastructure.InviteCodeJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.repository.QueryHints
import java.time.LocalDateTime

class PessimisticLockTimeoutConfigurationTest {
    @Test
    fun `all pessimistic lock queries define lock timeout`() {
        assertLockTimeout(InviteCodeJpaRepository::class.java, "findByCodeForUpdate", String::class.java)
        assertLockTimeout(
            InviteCodeJpaRepository::class.java,
            "findActiveUnusedByUserIdForUpdate",
            java.lang.Long::class.javaPrimitiveType ?: Long::class.java,
            LocalDateTime::class.java,
        )
        assertLockTimeout(CoupleJpaRepository::class.java, "findByIdForUpdate", java.lang.Long::class.javaPrimitiveType ?: Long::class.java)
        assertLockTimeout(
            UserJpaRepository::class.java,
            "findAllByIdInForUpdate",
            List::class.java,
        )
    }

    private fun assertLockTimeout(
        repositoryType: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>,
    ) {
        val method = repositoryType.getMethod(methodName, *parameterTypes)
        val hints = method.getAnnotation(QueryHints::class.java)
        assertNotNull(hints, "$methodName must define @QueryHints for lock timeout")

        val lockTimeout =
            hints!!
                .value
                .firstOrNull { it.name == LOCK_TIMEOUT_HINT_NAME }
                ?.value
        assertEquals(LockPolicy.DB_PESSIMISTIC_LOCK_TIMEOUT_MILLIS_TEXT, lockTimeout, "$methodName lock timeout hint mismatch")
    }

    companion object {
        private const val LOCK_TIMEOUT_HINT_NAME = "jakarta.persistence.lock.timeout"
    }
}
