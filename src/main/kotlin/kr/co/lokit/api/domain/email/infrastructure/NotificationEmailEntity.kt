package kr.co.lokit.api.domain.email.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import kr.co.lokit.api.common.entity.BaseEntity

@Entity(name = "NotificationEmail")
class NotificationEmailEntity(
    @Column(nullable = false, length = 320)
    val email: String,
) : BaseEntity()
