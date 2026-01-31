package kr.co.lokit.api.config.security

import kr.co.lokit.api.common.annotation.CurrentUser
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import kr.co.lokit.api.domain.user.mapping.toDomain
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUser::class.java) &&
            parameter.parameterType == User::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): User {
        val authentication =
            SecurityContextHolder.getContext().authentication
                ?: throw BusinessException.UnauthorizedException()

        val userEntity =
            authentication.principal as? UserEntity
                ?: throw BusinessException.UnauthorizedException("Invalid authentication principal")

        return userEntity.toDomain()
    }
}
