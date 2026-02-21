package kr.co.lokit.api.domain.user.domain

data class MyPageReadModel(
    val myName: String,
    val myProfileImageUrl: String?,
    val partnerName: String?,
    val partnerProfileImageUrl: String?,
    val coupledDay: Long?,
    val couplePhotoCount: Long,
)
