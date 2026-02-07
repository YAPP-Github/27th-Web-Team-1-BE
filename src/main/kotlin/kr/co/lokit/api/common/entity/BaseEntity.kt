package kr.co.lokit.api.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import kr.co.lokit.api.common.exception.entityIdNotInitialized
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
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    fun nonNullId(): Long = id ?: throw entityIdNotInitialized(this::class.simpleName ?: "Unknown")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        val thisClass = if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        val otherClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass

        if (thisClass != otherClass) return false

        val otherId: Long? =
            when (other) {
                is HibernateProxy -> other.hibernateLazyInitializer.identifier as? Long
                is BaseEntity -> other.id
                else -> null
            }

        if (id == null || otherId == null) return false
        return id == otherId
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
