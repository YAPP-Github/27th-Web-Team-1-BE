package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.domain.user.domain.AuthTokens

interface LoginService {
    fun login(code: String): AuthTokens
}
