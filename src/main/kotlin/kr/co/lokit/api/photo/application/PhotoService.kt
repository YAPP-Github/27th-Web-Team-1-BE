package kr.co.lokit.api.photo.application

import kr.co.lokit.api.photo.infrastructure.PhotoRepository
import org.springframework.stereotype.Service

@Service
class PhotoService(
    private val photoRepository: PhotoRepository
) {
}
