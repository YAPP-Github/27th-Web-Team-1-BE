package kr.co.lokit.api.domain.photo.domain

data class CommentWithEmoticons(
    val comment: Comment,
    val userName: String,
    val userProfileImageUrl: String?,
    val emoticons: List<EmoticonSummary>,
) {
    fun deIdentified(): CommentWithEmoticons =
        copy(
            userName = DeIdentifiedUserProfile.DISPLAY_NAME,
            userProfileImageUrl = DeIdentifiedUserProfile.hiddenProfileImageUrl(),
        )
}

data class EmoticonSummary(
    val emoji: String,
    val count: Int,
    val reacted: Boolean,
)
