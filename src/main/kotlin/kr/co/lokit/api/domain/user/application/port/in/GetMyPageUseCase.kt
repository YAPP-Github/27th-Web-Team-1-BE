package kr.co.lokit.api.domain.user.application.port.`in`

import kr.co.lokit.api.domain.user.domain.MyPageReadModel

interface GetMyPageUseCase {
    fun getMyPage(userId: Long): MyPageReadModel
}
