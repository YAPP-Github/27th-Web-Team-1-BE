package kr.co.lokit.api.domain.map.infrastructure

import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MapJpaRepository : JpaRepository<PhotoEntity, Long>
