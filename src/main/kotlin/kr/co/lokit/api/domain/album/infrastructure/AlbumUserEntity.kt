package kr.co.lokit.api.domain.album.infrastructure

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity

@Entity
class AlbumUserEntity(
    @JoinColumn(nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val album: AlbumEntity,
    @JoinColumn(nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val user: UserEntity
) : BaseEntity() {
}
