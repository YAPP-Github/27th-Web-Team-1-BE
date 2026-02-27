package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.PhotoRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.application.port.`in`.GetMyPageUseCase
import kr.co.lokit.api.domain.user.application.port.`in`.UpdateMyPageUseCase
import kr.co.lokit.api.domain.user.domain.MyPageReadModel
import kr.co.lokit.api.domain.user.domain.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ThreadLocalRandom

@Service
class MyPageService(
    private val userRepository: UserRepositoryPort,
    private val coupleRepository: CoupleRepositoryPort,
    private val photoRepository: PhotoRepositoryPort,
    private val albumRepository: AlbumRepositoryPort,
) : UpdateMyPageUseCase,
    GetMyPageUseCase {
    @Transactional(readOnly = true)
    override fun getMyPage(userId: Long): MyPageReadModel {
        val me = userRepository.findById(userId) ?: throw entityNotFound<User>(userId)
        val couple = coupleRepository.findByUserIdFresh(userId)
        val isCoupled = couple?.isConnectedAndFull() == true

        val partner =
            if (isCoupled) {
                couple.partnerIdFor(userId)?.let { partnerId ->
                    userRepository.findById(partnerId) ?: throw entityNotFound<User>(partnerId)
                }
            } else {
                null
            }

        val coupledDay =
            if (!isCoupled) {
                null
            } else {
                requireNotNull(couple).firstMetDate?.toDDay()
            }

        val couplePhotoCount = couple?.let { photoRepository.countByCoupleId(it.id) } ?: 0L
        val backgroundImageUrl =
            couple?.let {
                resolveRandomBackgroundImageUrl(
                    coupleId = it.id,
                    photoCount = couplePhotoCount,
                )
            }

        val firstMetDate = if (isCoupled) couple.firstMetDate else null
        val defaultAlbumId =
            couple
                ?.let {
                    albumRepository.findDefaultByCoupleId(it.id)?.id ?: throw entityNotFound<Album>(it.id)
                } ?: 0L

        return MyPageReadModel(
            myEmail = me.email,
            myName = me.name,
            myProfileImageUrl = me.profileImageUrl,
            partnerName = partner?.name,
            partnerProfileImageUrl = partner?.profileImageUrl,
            firstMetDate = firstMetDate,
            coupledDay = coupledDay,
            couplePhotoCount = couplePhotoCount,
            defaultAlbumId = defaultAlbumId,
            backgroundImageUrl = backgroundImageUrl,
        )
    }

    @Transactional
    override fun updateNickname(
        userId: Long,
        nickname: String,
    ): User {
        val user = userRepository.findById(userId) ?: throw entityNotFound<User>(userId)
        return userRepository.update(user.withNickname(nickname))
    }

    @Transactional
    override fun updateProfileImage(
        userId: Long,
        profileImageUrl: String,
    ): User {
        val user = userRepository.findById(userId) ?: throw entityNotFound<User>(userId)
        return userRepository.update(user.withProfileImage(profileImageUrl))
    }

    private fun LocalDate?.toDDay(): Long? {
        if (this == null) return null
        val days = ChronoUnit.DAYS.between(this, LocalDate.now()) + 1
        return days.takeIf { it >= 0 }
    }

    private fun resolveRandomBackgroundImageUrl(
        coupleId: Long,
        photoCount: Long,
    ): String? {
        if (photoCount <= 0L) return null
        val randomOffset = ThreadLocalRandom.current().nextLong(photoCount).toInt()
        return photoRepository.findPhotoUrlByCoupleIdWithOffset(coupleId, randomOffset)
    }
}
