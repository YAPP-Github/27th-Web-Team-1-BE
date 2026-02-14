package kr.co.lokit.api.common.exception

object ErrorField {
    const val ID = "id"
    const val ENTITY_NAME = "entityName"
    const val USER_ID = "userId"
    const val COUPLE_ID = "coupleId"
    const val COUPLE_STATUS = "coupleStatus"
    const val ALBUM_ID = "albumId"
    const val PHOTO_ID = "photoId"
    const val TITLE = "title"
    const val COMMENT_ID = "commentId"
    const val EMOJI = "emoji"
    const val STATUS = "status"
    const val STATUS_CODE = "statusCode"
    const val REASON = "reason"
    const val MAX_MEMBERS = "maxMembers"
    const val WITHDRAWN_AT = "withdrawnAt"
    const val PROVIDER_ID = "providerId"
    const val UPLOADED_BY_ID = "uploadedById"
}

fun errorDetailsOf(vararg pairs: Pair<String, Any?>): Map<String, String> =
    pairs.associate { (key, value) -> key to value.toString() }
