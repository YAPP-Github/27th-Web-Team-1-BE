package kr.co.lokit.api.domain.photo.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PhotoJpaRepository : JpaRepository<PhotoEntity, Long> {
    @Query("SELECT p FROM Photo p JOIN FETCH p.album JOIN FETCH p.uploadedBy WHERE p.id = :id")
    fun findByIdWithRelations(id: Long): PhotoEntity?

    @Query("SELECT p FROM Photo p JOIN FETCH p.album JOIN FETCH p.uploadedBy WHERE p.uploadedBy.id = :userId")
    fun findAllByUploadedById(userId: Long): List<PhotoEntity>
}
