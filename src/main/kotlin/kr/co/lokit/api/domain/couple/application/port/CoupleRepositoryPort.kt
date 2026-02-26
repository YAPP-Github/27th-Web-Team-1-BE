package kr.co.lokit.api.domain.couple.application.port

import kr.co.lokit.api.domain.couple.domain.Couple
import java.time.LocalDate

interface CoupleRepositoryPort {
    fun save(couple: Couple): Couple

    fun findById(id: Long): Couple?

    fun saveWithUser(
        couple: Couple,
        userId: Long,
    ): Couple

    fun findByDisconnectedByUserId(userId: Long): Couple?

    fun addUser(
        coupleId: Long,
        userId: Long,
    ): Couple

    fun findByUserId(userId: Long): Couple?

    fun findByUserIdFresh(userId: Long): Couple?

    fun deleteById(id: Long)

    fun removeCoupleUser(userId: Long)

    fun disconnect(
        coupleId: Long,
        userId: Long,
    )

    fun reconnect(
        coupleId: Long,
        userId: Long,
    ): Couple

    fun updateFirstMetDate(coupleId: Long, firstMetDate: LocalDate)
}
