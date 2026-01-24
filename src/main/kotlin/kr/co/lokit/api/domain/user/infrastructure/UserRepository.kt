package kr.co.lokit.api.domain.user.infrastructure

import kr.co.lokit.api.domain.user.domain.User

interface UserRepository {
    fun save(user: User): User

    fun findById(id: Long): User?

    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean
}
