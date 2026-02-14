package kr.co.lokit.api.domain.photo.domain

object PhotoStorageKey {
    private const val PREFIX = "photos"

    fun fromUniqueToken(token: String): String = "$PREFIX/$token"
}
