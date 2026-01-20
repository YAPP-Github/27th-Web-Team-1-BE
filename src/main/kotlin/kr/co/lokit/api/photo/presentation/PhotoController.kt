package kr.co.lokit.api.photo.presentation

import kr.co.lokit.api.photo.application.PhotoService
import org.springframework.web.bind.annotation.RestController

@RestController
class PhotoController(
    private val photoService: PhotoService
) {
}
