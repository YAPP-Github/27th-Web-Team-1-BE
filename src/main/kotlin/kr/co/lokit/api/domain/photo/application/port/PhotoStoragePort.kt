package kr.co.lokit.api.domain.photo.application.port

import kr.co.lokit.api.domain.photo.dto.PresignedUrl

interface PhotoStoragePort {
    fun generatePresignedUrl(
        key: String,
        contentType: String,
    ): PresignedUrl

    fun verifyFileExists(objectUrl: String)

    fun verifyFileNotExists(key: String)

    fun deleteFileByKey(key: String)

    fun deleteFileByUrl(url: String)
}
