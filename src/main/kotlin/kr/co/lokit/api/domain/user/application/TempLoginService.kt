package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.domain.album.application.AlbumService
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.map.application.AlbumBoundsService
import kr.co.lokit.api.domain.map.application.MapService
import kr.co.lokit.api.domain.photo.domain.Location
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.infrastructure.PhotoRepository
import kr.co.lokit.api.domain.photo.infrastructure.file.S3PresignedUrlGenerator
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.dto.LoginPhotoResponse
import kr.co.lokit.api.domain.user.dto.LoginResponse
import kr.co.lokit.api.domain.user.infrastructure.UserRepository
import kr.co.lokit.api.domain.workspace.application.WorkspaceService
import kr.co.lokit.api.domain.workspace.domain.Workspace
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.ThreadLocalRandom

@Service
class TempLoginService(
    private val photoRepository: PhotoRepository,
    private val albumService: AlbumService,
    private val workSpaceService: WorkspaceService,
    private val albumBoundsService: AlbumBoundsService,
    private val userRepository: UserRepository,
    private val mapService: MapService
) {
    @Value("\${aws.s3.bucket}")
    private lateinit var bucket: String

    // 임시 회원가입/로그인 기능
    @Transactional
    fun login(user: User): LoginResponse {
        val userId = (userRepository.findByEmail(user.email) ?: userRepository.save(user)).id

        val workspace =
            workSpaceService.create(Workspace(name = "ws" + ThreadLocalRandom.current().nextInt(10000, 100000)), userId)
        val album = albumService.create(
            Album(
                title = "ab" + ThreadLocalRandom.current().nextInt(100, 1000),
                workspaceId = workspace.id
            )
        )
        val photos = mutableListOf<Photo>()

        for (tempPhoto in TEMP_PHOTOS) {
            val longitude = GANGNAM_LONGITUDE - ThreadLocalRandom.current().nextDouble(-0.01, 0.01)
            val latitude = GANGNAM_LATITUDE + ThreadLocalRandom.current().nextDouble(-0.01, 0.01)
            val photo = photoRepository.save(
                Photo(
                    albumId = album.id,
                    location = Location(
                        longitude = longitude,
                        latitude = latitude,
                    ),
                    description = DESCRIPTIONS.get(ThreadLocalRandom.current().nextInt(DESCRIPTIONS.size)),
                ).apply {
                    url = S3PresignedUrlGenerator.OBJECT_URL_TEMPLATE.format(
                        bucket,
                        tempPhoto
                    )
                    uploadedById = userId
                    takenAt = LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(100).toLong())
                }
            )
            photos.add(photo)
            albumBoundsService.updateBoundsOnPhotoAdd(album.id, longitude, latitude)
        }

        val albumMapInfo = mapService.getAlbumMapInfo(album.id)

        return LoginResponse(
            userId = userId,
            workspaceId = workspace.id,
            albumId = album.id,
            photos = photos.map {
                LoginPhotoResponse(
                    photoId = it.id,
                    url = it.url!!,
                    longitude = it.location.longitude,
                    latitude = it.location.latitude,
                    description = it.description,
                )
            },
            albumLocation = albumMapInfo
        )
    }

    companion object {
        private val TEMP_PHOTOS =
            listOf("두.jpg", "몰.jpeg", "센.png", "네.jpg", "카.png", "라.png", "배.png", "당.png", "토.jpg")
        private val DESCRIPTIONS = listOf(
            "매일 꿈꾸는 바로 한가지, 퇴사",
            "세상에서 가장 건설적인 활동은 이직",
            "가장 멋진 서류는 사직서",
            "금요일 퇴근보다 금요일 퇴사가 멋진 법",
            "이직, 퇴사, 사퇴... 세상에서 가장 멋진 단어들",
            "옛말에 이직이 보약이다 라는 말이 있지..",
            "퇴사는 나의 힘",
            "퇴사 가능성, 내가 회사를 다니게 해주는 힘",
            "오늘도 퇴사를 꿈꾸며 출근한다",
            "후라이의 꿈, 네모의 꿈, 그리고 우리의 오랜 꿈, 퇴사",
            "사직서는 나의 로망",
            "언제 꺼낼거야.. 오래 숨겨온 사직서를 말이야",
            "퇴사를 향한 첫 걸음은 사직서 작성부터",
            "이직을 기다리게 하지마. 이직은 언제나 우릴 기다리고 있어",
            "나는 오늘 하루도 이직에 대한 꿈으로 버틴다",
            "ㅋ"
        )
        private const val GANGNAM_LONGITUDE = 127.02836155
        private const val GANGNAM_LATITUDE = 37.49648606
    }
}
