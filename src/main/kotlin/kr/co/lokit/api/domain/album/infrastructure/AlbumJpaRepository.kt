package kr.co.lokit.api.domain.album.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AlbumJpaRepository : JpaRepository<AlbumEntity, Long> {
    @Query(
        """
        select a from Album a
        join a.workspace w
        join w.workspaceUsers wu
        where wu.user._id = :userId
        order by a.updatedAt desc, a.createdAt desc
        """
    )
    fun findAllByUserId(userId: Long): List<AlbumEntity>

    @Query("select distinct a from Album a left join fetch a.photos order by a.photoAddedAt desc, a.createdAt desc")
    fun findAllWithPhotos(): List<AlbumEntity>

    @Query("select a from Album a left join fetch a.photos where a._id = :id ")
    fun findByIdWithPhotos(id: Long): List<AlbumEntity>
}
