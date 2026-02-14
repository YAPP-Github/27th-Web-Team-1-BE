package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.map.application.AddressFormatter
import kr.co.lokit.api.domain.map.application.port.MapClientPort
import kr.co.lokit.api.domain.photo.application.port.PhotoRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.`in`.GetPhotoDetailUseCase
import kr.co.lokit.api.domain.photo.domain.DeIdentifiedUserProfile
import kr.co.lokit.api.domain.photo.dto.PhotoDetailResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PhotoQueryService(
    private val photoRepository: PhotoRepositoryPort,
    private val albumRepository: AlbumRepositoryPort,
    private val mapClientPort: MapClientPort,
    private val coupleRepository: CoupleRepositoryPort,
) : GetPhotoDetailUseCase {
    @Transactional(readOnly = true)
    override fun getPhotosByAlbum(
        albumId: Long,
        userId: Long,
    ): List<Album> = albumRepository.findByIdWithPhotos(albumId, userId)

    @Transactional(readOnly = true)
    override fun getPhotoDetail(
        photoId: Long,
        userId: Long,
    ): PhotoDetailResponse {
        val photoDetail = photoRepository.findDetailById(photoId)

        val locationInfo =
            mapClientPort.reverseGeocode(
                photoDetail.location.longitude,
                photoDetail.location.latitude,
            )

        val deIdentifiedUserId = coupleRepository.findByUserId(userId)?.deIdentifiedUserId()
        val shouldDeIdentify = deIdentifiedUserId == photoDetail.uploadedById
        val uploaderName = if (shouldDeIdentify) DeIdentifiedUserProfile.DISPLAY_NAME else photoDetail.uploaderName
        val uploaderProfileImageUrl =
            if (shouldDeIdentify) DeIdentifiedUserProfile.hiddenProfileImageUrl() else photoDetail.uploaderProfileImageUrl

        return PhotoDetailResponse(
            id = photoDetail.id,
            url = photoDetail.url,
            takenAt = photoDetail.takenAt,
            albumName = photoDetail.albumName,
            uploaderName = uploaderName,
            uploaderProfileImageUrl = uploaderProfileImageUrl,
            address = AddressFormatter.removeProvinceAndCity(locationInfo.address),
            description = photoDetail.description,
            longitude = photoDetail.location.longitude,
            latitude = photoDetail.location.latitude,
        )
    }
}
