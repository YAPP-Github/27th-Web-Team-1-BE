package kr.co.lokit.api.domain.couple.domain

import kr.co.lokit.api.common.util.InviteCodeGenerator

data class Couple(
    val id: Long = 0,
    val name: String,
    var inviteCode: String = InviteCodeGenerator.generate(),
    val userIds: List<Long> = emptyList(),
) {
    init {
        require(userIds.size <= 2)
        require(inviteCode.length == INVITE_CODE_LENGTH)
    }

    companion object {
        const val INVITE_CODE_LENGTH = 8
    }
}
