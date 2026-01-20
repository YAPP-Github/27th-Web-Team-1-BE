package kr.co.lokit.api.domain.photo.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface PhotoRepository : JpaRepository<PhotoEntity, Long> {
}
