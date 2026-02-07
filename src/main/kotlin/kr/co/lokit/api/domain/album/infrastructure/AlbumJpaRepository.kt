package kr.co.lokit.api.domain.album.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AlbumJpaRepository : JpaRepository<AlbumEntity, Long> {
    @Query(
        """
        select a.id from Album a
        join a.couple c
        join c.coupleUsers cu
        where cu.user.id = :userId
        order by a.updatedAt desc, a.createdAt desc
        """,
    )
    fun findAlbumIdsByUserId(userId: Long): List<Long>

    @Query(
        """
        select a.id from Album a
        where a.couple.id = :coupleId
        order by a.updatedAt desc, a.createdAt desc
        """,
    )
    fun findAlbumIdsByCoupleId(coupleId: Long): List<Long>

    @Query(
        """
        select distinct a from Album a
        left join fetch a.photos p
        left join fetch p.uploadedBy
        where a.id in :ids
        """,
    )
    fun findAllWithPhotosByIds(ids: List<Long>): List<AlbumEntity>

    @Query(
        """
        select a.id from Album a
        order by a.photoAddedAt desc, a.createdAt desc
        """,
    )
    fun findAllAlbumIds(): List<Long>

    @Query(
        """
        select distinct a from Album a
        left join fetch a.photos p
        left join fetch p.uploadedBy
        where a.id = :id
        """,
    )
    fun findByIdWithPhotos(id: Long): List<AlbumEntity>

    @Query(
        """
            select a
            from Album a
            join a.couple c
            join c.coupleUsers cu
            where cu.user.id = :userId
                and a.isDefault = true
        """,
    )
    fun findByUserIdAndIsDefaultTrue(userId: Long): AlbumEntity?

    fun existsByCoupleIdAndTitle(
        coupleId: Long,
        title: String,
    ): Boolean

    @Query(
        """
        select sum(a.photoCount)
        from Album a
        join a.couple c
        join c.coupleUsers cu
        where cu.user.id = :userId
    """,
    )
    fun sumPhotoCountByUserId(userId: Long): Int?
}
