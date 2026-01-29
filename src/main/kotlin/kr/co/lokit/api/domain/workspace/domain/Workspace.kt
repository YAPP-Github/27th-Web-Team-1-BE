package kr.co.lokit.api.domain.workspace.domain

data class Workspace(
    val id: Long = 0,
    val name: String,
    val inviteCode: String? = null,
    val userIds: List<Long> = emptyList(),
)
