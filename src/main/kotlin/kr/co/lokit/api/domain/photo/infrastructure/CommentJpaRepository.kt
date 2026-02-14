package kr.co.lokit.api.domain.photo.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CommentJpaRepository : JpaRepository<CommentEntity, Long> {
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.photo.id = :photoId ORDER BY c.createdAt ASC")
    fun findAllByPhotoId(photoId: Long): List<CommentEntity>
}
