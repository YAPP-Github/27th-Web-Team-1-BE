package kr.co.lokit.api.domain.couple.infrastructure

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.album.infrastructure.AlbumJpaRepository
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.mapping.toDomain
import kr.co.lokit.api.domain.couple.mapping.toEntity
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaCoupleRepository(
    private val coupleJpaRepository: CoupleJpaRepository,
    private val coupleUserJpaRepository: CoupleUserJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val albumJpaRepository: AlbumJpaRepository,
) : CoupleRepositoryPort {
    override fun save(couple: Couple): Couple {
        val entity = couple.toEntity()
        return coupleJpaRepository.save(entity).toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Couple? = coupleJpaRepository.findByIdFetchUsers(id)?.toDomain()

    @Transactional
    override fun saveWithUser(
        couple: Couple,
        userId: Long,
    ): Couple {
        val userEntity =
            userJpaRepository.findByIdOrNull(userId)
                ?: throw entityNotFound<User>(userId)

        val coupleEntity = couple.toEntity()
        val savedCouple = coupleJpaRepository.save(coupleEntity)

        val coupleUser =
            CoupleUserEntity(
                couple = savedCouple,
                user = userEntity,
            )
        savedCouple.addUser(coupleUser)

        val defaultAlbum = AlbumEntity(title = "전체보기", couple = savedCouple, createdBy = userEntity, isDefault = true)
        albumJpaRepository.save(defaultAlbum)

        return savedCouple.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findByInviteCode(inviteCode: String): Couple? =
        coupleJpaRepository.findByInviteCode(inviteCode)?.toDomain()

    @Transactional(readOnly = true)
    override fun findByDisconnectedByUserId(userId: Long): Couple? =
        coupleJpaRepository.findByDisconnectedByUserId(userId)?.toDomain()

    @Transactional
    override fun addUser(
        coupleId: Long,
        userId: Long,
    ): Couple {
        val coupleEntity =
            coupleJpaRepository.findByIdFetchUsers(coupleId)
                ?: throw entityNotFound<Couple>(coupleId)
        val userEntity =
            userJpaRepository.findByIdOrNull(userId)
                ?: throw entityNotFound<User>(userId)

        if (coupleEntity.coupleUsers.size >= Couple.MAX_MEMBERS) {
            throw BusinessException.CoupleMaxMembersExceededException(
                errors =
                    errorDetailsOf(
                        ErrorField.COUPLE_ID to coupleId,
                        ErrorField.MAX_MEMBERS to Couple.MAX_MEMBERS,
                    ),
            )
        }

        val coupleUser =
            CoupleUserEntity(
                couple = coupleEntity,
                user = userEntity,
            )
        coupleEntity.addUser(coupleUser)

        return coupleEntity.toDomain()
    }

    @Cacheable(cacheNames = [CacheNames.USER_COUPLE], key = "#userId", sync = true)
    @Transactional(readOnly = true)
    override fun findByUserId(userId: Long): Couple? = coupleJpaRepository.findByUserId(userId)?.toDomain()

    @Transactional
    override fun deleteById(id: Long) {
        coupleJpaRepository.deleteById(id)
    }

    @Transactional
    override fun removeCoupleUser(userId: Long) {
        coupleUserJpaRepository.deleteByUserId(userId)
    }

    @Transactional
    override fun disconnect(
        coupleId: Long,
        userId: Long,
    ) {
        val entity =
            coupleJpaRepository.findByIdOrNull(coupleId)
                ?: throw entityNotFound<Couple>(coupleId)
        entity.disconnect(userId)
    }

    @Transactional
    override fun reconnect(
        coupleId: Long,
        userId: Long,
    ): Couple {
        val entity =
            coupleJpaRepository.findByIdFetchUsers(coupleId)
                ?: throw entityNotFound<Couple>(coupleId)
        val userEntity =
            userJpaRepository.findByIdOrNull(userId)
                ?: throw entityNotFound<User>(userId)
        entity.reconnect()
        entity.addUser(CoupleUserEntity(couple = entity, user = userEntity))
        return entity.toDomain()
    }
}
