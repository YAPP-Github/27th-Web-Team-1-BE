package kr.co.lokit.api.domain.user.application.port

import kr.co.lokit.api.domain.user.domain.User

interface UserRepositoryPort {
    fun save(user: User): User

    fun findById(id: Long): User?

    fun lockWithEmail(email: String)

    fun findByEmail(
        email: String,
        name: String,
    ): User

    fun apply(
        user: User,
        name: String,
        profileImageUrl: String?,
    )
}
