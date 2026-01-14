package kr.co.lokit.api.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import kr.co.lokit.api.common.exception.BusinessException
import org.hibernate.annotations.SoftDelete
import org.hibernate.proxy.HibernateProxy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@SoftDelete(columnName = "is_deleted")
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var _id: Long? = null

    protected val persistedId: Long?
        get() = _id
    val id: Long
        get() = _id ?: throw BusinessException.NotInitializedException.entityId()

    @CreatedDate
    @Column(updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }

        val otherId: Long? =
            when (other) {
                is HibernateProxy -> other.hibernateLazyInitializer.identifier as? Long
                is BaseEntity -> other.persistedId
                else -> null
            }

        if (persistedId == null || otherId == null) {
            return false
        }
        return persistedId == otherId
    }

    override fun hashCode(): Int = persistedId?.hashCode() ?: 0
}
