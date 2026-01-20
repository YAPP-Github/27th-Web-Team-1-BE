package kr.co.lokit.api.album.infrastructure

import jakarta.persistence.Entity
import kr.co.lokit.api.common.entity.BaseEntity

@Entity
class AlbumEntity(
    val title: String,
) : BaseEntity() {

    var inviteCode: String? = null
    var imageCount: Int = 0
}
