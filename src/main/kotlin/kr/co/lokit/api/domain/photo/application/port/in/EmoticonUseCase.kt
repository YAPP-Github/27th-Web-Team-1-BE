package kr.co.lokit.api.domain.photo.application.port.`in`

import kr.co.lokit.api.domain.photo.domain.Emoticon

interface EmoticonUseCase {
    fun addEmoticon(
        commentId: Long,
        userId: Long,
        emoji: String,
    ): Emoticon

    fun removeEmoticon(
        commentId: Long,
        userId: Long,
        emoji: String,
    )
}
