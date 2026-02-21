package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import org.springframework.stereotype.Component

@Component
class CurrentCoupleAlbumResolver(
    private val coupleRepository: CoupleRepositoryPort,
    private val albumRepository: AlbumRepositoryPort,
) {
    fun requireCurrentCoupleId(userId: Long): Long =
        coupleRepository.findByUserId(userId)?.id
            ?: throw BusinessException.DefaultAlbumNotFoundForUserException(
                errors = errorDetailsOf(ErrorField.USER_ID to userId),
            )

    fun requireDefaultAlbum(
        userId: Long,
        errorField: String = ErrorField.USER_ID,
    ): Album {
        val coupleId = requireCurrentCoupleId(userId)
        return albumRepository.findDefaultByCoupleId(coupleId)
            ?: throw BusinessException.DefaultAlbumNotFoundForUserException(
                errors = errorDetailsOf(errorField to userId),
            )
    }

    fun validateAlbumBelongsToCurrentCouple(
        userId: Long,
        albumId: Long,
        errorField: String = ErrorField.USER_ID,
    ) {
        val coupleId = requireCurrentCoupleId(userId)
        val targetAlbum =
            albumRepository.findById(albumId)
                ?: throw BusinessException.ResourceNotFoundException(
                    errors = errorDetailsOf(ErrorField.ALBUM_ID to albumId),
                )
        if (targetAlbum.coupleId != coupleId) {
            throw BusinessException.ForbiddenException(
                errors =
                    errorDetailsOf(
                        errorField to userId,
                        ErrorField.ALBUM_ID to albumId,
                    ),
            )
        }
    }
}
