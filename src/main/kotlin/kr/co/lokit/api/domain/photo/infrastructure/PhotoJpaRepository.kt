package kr.co.lokit.api.domain.photo.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface PhotoJpaRepository : JpaRepository<PhotoEntity, Long> {
    @Query("SELECT p.url FROM Photo p WHERE p.createdAt >= :since")
    fun findUrlsCreatedSince(since: LocalDateTime): List<String>

    @Query("SELECT p FROM Photo p JOIN FETCH p.album JOIN FETCH p.uploadedBy WHERE p.id = :id")
    fun findByIdWithRelations(id: Long): PhotoEntity?

    @Query(
        "SELECT p FROM Photo p JOIN FETCH p.album JOIN FETCH p.uploadedBy"
            + " WHERE p.uploadedBy.id = :userId ORDER BY p.takenAt DESC"
    )
    fun findAllByUploadedById(userId: Long): List<PhotoEntity>
}
