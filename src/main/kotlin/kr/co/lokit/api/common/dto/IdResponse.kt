package kr.co.lokit.api.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.co.lokit.api.common.entity.BaseEntity

@Schema(description = "ID 응답")
data class IdResponse(
    @Schema(description = "생성된 리소스의 ID", example = "1")
    val id: Long,
) {
    companion object {
        fun from(entity: BaseEntity): IdResponse = IdResponse(entity.id)
    }
}
