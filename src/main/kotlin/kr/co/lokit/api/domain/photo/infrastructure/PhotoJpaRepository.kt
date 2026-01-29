package kr.co.lokit.api.domain.photo.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface PhotoJpaRepository : JpaRepository<PhotoEntity, Long> {
    @Query("SELECT p FROM Photo p JOIN FETCH p.album JOIN FETCH p.uploadedBy WHERE p._id = :id")
    fun findByIdWithRelations(id: Long): Optional<PhotoEntity>
}
