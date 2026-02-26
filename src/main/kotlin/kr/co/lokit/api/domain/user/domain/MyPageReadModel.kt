package kr.co.lokit.api.domain.user.domain

import java.time.LocalDate

data class MyPageReadModel(
    val myEmail: String,
    val myName: String,
    val myProfileImageUrl: String?,
    val partnerName: String?,
    val partnerProfileImageUrl: String?,
    val firstMetDate: LocalDate?,
    val coupledDay: Long?,
    val couplePhotoCount: Long,
)
