package kr.co.lokit.api.domain.couple.infrastructure

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.entityNotFound
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
    private val userJpaRepository: UserJpaRepository,
    private val albumJpaRepository: AlbumJpaRepository,
) : CoupleRepositoryPort {
    override fun save(couple: Couple): Couple {
        val entity = couple.toEntity()
        return coupleJpaRepository.save(entity).toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Couple? =
        coupleJpaRepository.findByIdFetchUsers(id)?.toDomain()

    @Transactional
    override fun saveWithUser(couple: Couple, userId: Long): Couple {
        val userEntity = userJpaRepository.findByIdOrNull(userId)
            ?: throw entityNotFound<User>(userId)

        val coupleEntity = couple.toEntity()
        val savedCouple = coupleJpaRepository.save(coupleEntity)

        val coupleUser = CoupleUserEntity(
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

    @Transactional
    override fun addUser(coupleId: Long, userId: Long): Couple {
        val coupleEntity = coupleJpaRepository.findByIdFetchUsers(coupleId)
            ?: throw entityNotFound<Couple>(coupleId)
        val userEntity = userJpaRepository.findByIdOrNull(userId)
            ?: throw entityNotFound<User>(userId)

        if (coupleEntity.coupleUsers.size >= MAX_COUPLE_MEMBERS) {
            throw BusinessException.CoupleMaxMembersExceededException(
                errors = mapOf(
                    "coupleId" to coupleId.toString(),
                    "maxMembers" to MAX_COUPLE_MEMBERS.toString(),
                ),
            )
        }

        val coupleUser = CoupleUserEntity(
            couple = coupleEntity,
            user = userEntity,
        )
        coupleEntity.addUser(coupleUser)

        return coupleEntity.toDomain()
    }

    @Cacheable(cacheNames = ["userCouple"], key = "#userId", sync = true)
    @Transactional(readOnly = true)
    override fun findByUserId(userId: Long): Couple? =
        coupleJpaRepository.findByUserId(userId)?.toDomain()

    companion object {
        private const val MAX_COUPLE_MEMBERS = 2
    }
}
