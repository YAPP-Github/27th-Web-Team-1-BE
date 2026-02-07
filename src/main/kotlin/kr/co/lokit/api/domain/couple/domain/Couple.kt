package kr.co.lokit.api.domain.couple.domain

data class Couple(
    val id: Long = 0,
    val name: String,
    val inviteCode: String? = null,
    val userIds: List<Long> = emptyList(),
) {
    init {
        require(userIds.size <= 2)
    }
}
