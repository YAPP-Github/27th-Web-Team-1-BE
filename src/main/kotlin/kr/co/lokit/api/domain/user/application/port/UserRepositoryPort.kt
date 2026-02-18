package kr.co.lokit.api.domain.user.application.port

import kr.co.lokit.api.domain.user.domain.User

interface UserRepositoryPort {
    fun save(user: User): User

    fun findById(id: Long): User?

    fun findByEmail(email: String): User

    fun update(user: User): User

    fun lockByIds(ids: List<Long>)

    fun withdraw(userId: Long)

    fun reactivate(userId: Long)
}
