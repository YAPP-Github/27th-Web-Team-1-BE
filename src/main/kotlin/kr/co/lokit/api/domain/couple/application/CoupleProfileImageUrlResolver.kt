package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.domain.couple.domain.CoupleProfileImage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CoupleProfileImageUrlResolver(
    @Value("\${aws.s3.public-base-url:}") private val publicBaseUrl: String,
) {
    fun resolve(image: CoupleProfileImage): String {
        val normalizedBaseUrl = publicBaseUrl.trim().trimEnd('/')
        return if (normalizedBaseUrl.isEmpty()) {
            "/${image.objectKey}"
        } else {
            "$normalizedBaseUrl/${image.objectKey}"
        }
    }
}
