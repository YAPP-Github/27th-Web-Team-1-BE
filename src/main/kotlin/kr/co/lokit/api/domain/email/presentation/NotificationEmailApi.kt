package kr.co.lokit.api.domain.email.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.domain.email.dto.SaveNotificationEmailRequest

@Tag(name = "NotificationEmail", description = "알림 이메일 수집 API")
interface NotificationEmailApi {
    @Operation(
        summary = "알림 이메일 저장",
        description = "인증 없이 이메일을 저장합니다. 이미 존재하는 이메일이면 조용히 무시합니다.",
        responses = [
            ApiResponse(responseCode = "204", description = "처리 완료"),
            ApiResponse(responseCode = "400", description = "요청 형식 오류"),
        ],
    )
    @SecurityRequirements
    fun saveNotificationEmail(request: SaveNotificationEmailRequest)
}
