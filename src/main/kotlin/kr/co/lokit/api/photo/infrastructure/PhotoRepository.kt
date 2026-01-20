package kr.co.lokit.api.photo.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface PhotoRepository : JpaRepository<PhotoEntity, Long> {
}
