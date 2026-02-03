package kr.co.lokit.api.domain.album.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AlbumJpaRepository : JpaRepository<AlbumEntity, Long> {
    @Query(
        """
        select a.id from Album a
        join a.workspace w
        join w.workspaceUsers wu
        where wu.user.id = :userId
        order by a.updatedAt desc, a.createdAt desc
        """
    )
    fun findAlbumIdsByUserId(userId: Long): List<Long>

    @Query(
        """
        select distinct a from Album a
        left join fetch a.photos p
        left join fetch p.uploadedBy
        where a.id in :ids
        """
    )
    fun findAllWithPhotosByIds(ids: List<Long>): List<AlbumEntity>

    @Query(
        """
        select a.id from Album a
        order by a.photoAddedAt desc, a.createdAt desc
        """
    )
    fun findAllAlbumIds(): List<Long>

    @Query(
        """
        select distinct a from Album a
        left join fetch a.photos p
        left join fetch p.uploadedBy
        where a.id = :id
        """
    )
    fun findByIdWithPhotos(id: Long): List<AlbumEntity>

    fun findByWorkspaceIdAndIsDefaultTrue(workspaceId: Long): AlbumEntity?
}
