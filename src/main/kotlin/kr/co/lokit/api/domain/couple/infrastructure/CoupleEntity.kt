package kr.co.lokit.api.domain.couple.infrastructure

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import kr.co.lokit.api.common.constants.CoupleStatus
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(name = "Couple")
class CoupleEntity(
    @Column(nullable = false, length = 20)
    val name: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CoupleStatus = CoupleStatus.CONNECTED,
    var disconnectedAt: LocalDateTime? = null,
    var disconnectedByUserId: Long? = null,
    var firstMetDate: LocalDate? = null,
) : BaseEntity() {
    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
        mappedBy = "couple",
    )
    var albums: MutableList<AlbumEntity> = mutableListOf()
        protected set

    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
        mappedBy = "couple",
    )
    var coupleUsers: MutableList<CoupleUserEntity> = mutableListOf()
        protected set

    fun addUser(coupleUser: CoupleUserEntity) {
        require(coupleUsers.size < 2)
        coupleUsers.add(coupleUser)
    }

    fun addAlbum(album: AlbumEntity) {
        albums.add(album)
    }

    fun disconnect(userId: Long) {
        status = CoupleStatus.DISCONNECTED
        disconnectedAt = LocalDateTime.now()
        disconnectedByUserId = userId
    }

    fun reconnect() {
        status = CoupleStatus.CONNECTED
        disconnectedAt = null
        disconnectedByUserId = null
    }
}
