package kr.co.lokit.api.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import kotlin.reflect.KProperty1

@Schema(description = "ID 응답")
data class IdResponse(
    @Schema(description = "생성된 리소스의 ID", example = "1")
    val id: Long,
) {
}

fun <T> T.toIdResponse(idProp: KProperty1<T, Long>): IdResponse =
    IdResponse(idProp.get(this))

fun isValidId(id: Long?): Boolean = id != null && id > 0
