package kr.co.lokit.api.domain.email.presentation

import jakarta.validation.Valid
import kr.co.lokit.api.domain.email.application.port.`in`.SaveNotificationEmailUseCase
import kr.co.lokit.api.domain.email.dto.SaveNotificationEmailRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("emails")
class NotificationEmailController(
    private val saveNotificationEmailUseCase: SaveNotificationEmailUseCase,
) : NotificationEmailApi {
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun saveNotificationEmail(
        @RequestBody @Valid request: SaveNotificationEmailRequest,
    ) {
        saveNotificationEmailUseCase.save(request.email)
    }
}
