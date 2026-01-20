package kr.co.lokit.api.common.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "ID 응답")
data class IdResponse(
    @Schema(description = "생성된 리소스의 ID", example = "1")
    val id: Long,
)

fun Long.toIdResponse(): IdResponse = IdResponse(this)
