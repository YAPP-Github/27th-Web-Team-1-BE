package kr.co.lokit.api.domain.user.presentation.mapping

import kr.co.lokit.api.domain.user.domain.MyPageReadModel
import kr.co.lokit.api.domain.user.dto.MyPageResponse

fun MyPageReadModel.toResponse(): MyPageResponse =
    MyPageResponse(
        myEmail = myEmail,
        myName = myName,
        myProfileImageUrl = myProfileImageUrl,
        partnerName = partnerName,
        partnerProfileImageUrl = partnerProfileImageUrl,
        firstMetDate = firstMetDate,
        coupledDay = coupledDay,
        couplePhotoCount = couplePhotoCount,
    )
