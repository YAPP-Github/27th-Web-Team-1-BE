package kr.co.lokit.api.common.permission

import kr.co.lokit.api.common.constant.UserRole
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.CommentRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.PhotoRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.domain.User
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PermissionService(
    private val coupleRepository: CoupleRepositoryPort,
    private val albumRepository: AlbumRepositoryPort,
    private val photoRepository: PhotoRepositoryPort,
    private val commentRepository: CommentRepositoryPort,
    private val userRepository: UserRepositoryPort,
) {
    fun isAdmin(userId: Long): Boolean = getUserRole(userId) == UserRole.ADMIN

    fun isCoupleMember(
        userId: Long,
        coupleId: Long,
    ): Boolean {
        if (isAdmin(userId)) return true
        val couple = coupleRepository.findByUserId(userId) ?: return false
        return couple.id == coupleId
    }

    @Cacheable(cacheNames = [CacheNames.ALBUM_COUPLE], key = "#albumId", sync = true)
    fun getAlbumCoupleId(albumId: Long): Long = getAlbumOrThrow(albumId).coupleId

    @Cacheable(cacheNames = [CacheNames.ALBUM], key = "#userId + ':' + #albumId", sync = true)
    fun canAccessAlbum(
        userId: Long,
        albumId: Long,
    ): Boolean {
        if (isAdmin(userId)) return true

        val coupleId = getAlbumCoupleId(albumId)
        return isCoupleMember(userId, coupleId)
    }

    fun canModifyAlbum(
        userId: Long,
        albumId: Long,
    ): Boolean {
        if (isAdmin(userId)) return true

        return getAlbumOrThrow(albumId).createdById == userId
    }

    fun canDeleteAlbum(
        userId: Long,
        albumId: Long,
    ): Boolean {
        if (isAdmin(userId)) return true

        return getAlbumOrThrow(albumId).createdById == userId
    }

    @Cacheable(cacheNames = [CacheNames.PHOTO], key = "#userId + ':' + #photoId", sync = true)
    fun canReadPhoto(
        userId: Long,
        photoId: Long,
    ): Boolean {
        if (isAdmin(userId)) return true

        val photo = photoRepository.findById(photoId)
        val album = getAlbumOrThrow(photo.albumId!!)

        return isCoupleMember(userId, album.coupleId)
    }

    fun canModifyPhoto(
        userId: Long,
        photoId: Long,
    ): Boolean {
        if (isAdmin(userId)) return true

        val photo = photoRepository.findById(photoId)
        return photo.uploadedById == userId
    }

    fun canDeletePhoto(
        userId: Long,
        photoId: Long,
    ): Boolean {
        if (isAdmin(userId)) return true

        val photo = photoRepository.findById(photoId)
        return photo.uploadedById == userId
    }

    fun canAccessComment(
        userId: Long,
        commentId: Long,
    ): Boolean {
        if (isAdmin(userId)) return true

        val comment = commentRepository.findById(commentId)
        val photo = photoRepository.findById(comment.photoId)
        val album = getAlbumOrThrow(photo.albumId!!)

        return isCoupleMember(userId, album.coupleId)
    }

    private fun getUserRole(userId: Long): UserRole =
        userRepository.findById(userId)?.role
            ?: throw entityNotFound<User>(userId)

    private fun getAlbumOrThrow(albumId: Long): Album =
        albumRepository.findById(albumId)
            ?: throw entityNotFound<Album>(albumId)
}
