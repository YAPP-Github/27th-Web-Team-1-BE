package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.domain.user.domain.LoginResult

interface LoginService {
    fun login(code: String): LoginResult
}
