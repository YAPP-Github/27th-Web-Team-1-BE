package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.domain.photo.infrastructure.PhotoRepository
import org.springframework.stereotype.Service

@Service
class PhotoService(
    private val photoRepository: PhotoRepository
) {
}
