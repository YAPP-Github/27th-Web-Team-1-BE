package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.common.util.DateTimeUtils.toDateString
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.map.application.port.MapClientPort
import kr.co.lokit.api.domain.photo.application.port.PhotoRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.`in`.GetPhotoDetailUseCase
import kr.co.lokit.api.domain.photo.dto.PhotoDetailResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PhotoQueryService(
    private val photoRepository: PhotoRepositoryPort,
    private val albumRepository: AlbumRepositoryPort,
    private val mapClientPort: MapClientPort,
) : GetPhotoDetailUseCase {

    @Transactional(readOnly = true)
    override fun getPhotosByAlbum(albumId: Long, userId: Long): List<Album> {
        val album = albumRepository.findById(albumId)
            ?: throw entityNotFound<Album>(albumId)

        return if (album.isDefault) {
            val albumIds =
                photoRepository.findAllByUserId(userId)
                    .mapNotNull { it.albumId }
                    .distinct()

            albumRepository.findAllByIds(albumIds)
        } else {
            albumRepository.findByIdWithPhotos(albumId)
        }
    }

    @Transactional(readOnly = true)
    override fun getPhotoDetail(photoId: Long): PhotoDetailResponse {
        val photoDetail = photoRepository.findDetailById(photoId)

        val locationInfo = mapClientPort.reverseGeocode(
            photoDetail.location.longitude,
            photoDetail.location.latitude,
        )

        return PhotoDetailResponse(
            id = photoDetail.id,
            url = photoDetail.url,
            takenAt = photoDetail.takenAt?.toDateString()!!,
            albumName = photoDetail.albumName,
            uploaderName = photoDetail.uploaderName,
            address = locationInfo.address,
            description = photoDetail.description,
        )
    }
}
