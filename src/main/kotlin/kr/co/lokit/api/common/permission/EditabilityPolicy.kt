package kr.co.lokit.api.common.permission

object EditabilityPolicy {
    fun canEditOwnedResource(
        viewerUserId: Long,
        createdByUserId: Long,
    ): Boolean = viewerUserId == createdByUserId

    fun canEditAlbum(
        viewerUserId: Long,
        createdByUserId: Long,
        isDefault: Boolean,
    ): Boolean = !isDefault && canEditOwnedResource(viewerUserId, createdByUserId)

    fun canEditEmoticon(reacted: Boolean): Boolean = reacted
}
