package kr.co.lokit.api.domain.map.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.mapping.toResponse
import java.time.LocalDateTime

@Schema(description = "클러스터 응답")
data class ClusterResponse(
    @Schema(description = "클러스터 ID (줌 레벨 + 그리드 셀 인덱스)", example = "z14_130234_38456")
    val clusterId: String,
    @Schema(description = "클러스터 내 사진 개수", example = "42")
    val count: Int,
    @Schema(description = "대표 썸네일 URL (가장 최근 생성된 사진)", example = "https://example.com/photo.jpg")
    val thumbnailUrl: String,
    @field:NotNull
    @field:DecimalMin(value = "124.0", inclusive = true)
    @field:DecimalMax(value = "132.0", inclusive = true)
    @Schema(
        description = "클러스터 중심 경도",
        example = "127.0276",
        minimum = "124.0",
        maximum = "132.0",
        required = true
    )
    var longitude: Double,
    @field:NotNull
    @field:DecimalMin(value = "33.0", inclusive = true)
    @field:DecimalMax(value = "39.0", inclusive = true)
    @Schema(
        description = "클러스터 중심 위도",
        example = "37.4979",
        minimum = "33.0",
        maximum = "39.0",
        required = true
    )
    var latitude: Double,
)

@Schema(description = "개별 사진 응답 (줌 >= 15)")
data class MapPhotoResponse(
    @Schema(description = "사진 ID", example = "1")
    val id: Long,
    @Schema(description = "썸네일 URL", example = "https://example.com/photo.jpg")
    val thumbnailUrl: String,
    @field:NotNull
    @field:DecimalMin(value = "124.0", inclusive = true)
    @field:DecimalMax(value = "132.0", inclusive = true)
    @Schema(
        description = "사진 경도",
        example = "127.0276",
        minimum = "124.0",
        maximum = "132.0",
        required = true
    )
    var longitude: Double,
    @field:NotNull
    @field:DecimalMin(value = "33.0", inclusive = true)
    @field:DecimalMax(value = "39.0", inclusive = true)
    @Schema(
        description = "사진 위도",
        example = "37.4979",
        minimum = "33.0",
        maximum = "39.0",
        required = true
    )
    var latitude: Double,
    @Schema(description = "촬영일시 (ISO 8601 형식)", example = "2026-01-06T14:30:00")
    val takenAt: LocalDateTime,
)

@Schema(description = "지도 사진 조회 응답")
data class MapPhotosResponse(
    @Schema(description = "클러스터 목록 (줌 < 15일 때)")
    val clusters: List<ClusterResponse>? = null,
    @Schema(description = "개별 사진 목록 (줌 >= 15일 때)")
    val photos: List<MapPhotoResponse>? = null,
)

@Schema(description = "클러스터 내 사진 상세 응답")
data class ClusterPhotoResponse(
    @Schema(description = "사진 ID", example = "1")
    val id: Long,
    @Schema(description = "사진 URL", example = "https://example.com/photo.jpg")
    val url: String,
    @field:NotNull
    @field:DecimalMin(value = "124.0", inclusive = true)
    @field:DecimalMax(value = "132.0", inclusive = true)
    @Schema(
        description = "사진 경도",
        example = "127.0276",
        minimum = "124.0",
        maximum = "132.0",
        required = true
    )
    var longitude: Double,
    @field:NotNull
    @field:DecimalMin(value = "33.0", inclusive = true)
    @field:DecimalMax(value = "39.0", inclusive = true)
    @Schema(
        description = "사진 위도",
        example = "37.4979",
        minimum = "33.0",
        maximum = "39.0",
        required = true
    )
    var latitude: Double,
    @Schema(description = "촬영일시 (ISO 8601 형식)", example = "2026-01-06T14:30:00")
    val takenAt: LocalDateTime,
    @Schema(description = "주소", example = "역삼동 858")
    var address: String
)

@Schema(description = "바운딩 박스 응답")
data class BoundingBoxResponse(
    @Schema(description = "서쪽 경도 (최소 경도)", example = "126.9")
    val west: Double,
    @Schema(description = "남쪽 위도 (최소 위도)", example = "37.4")
    val south: Double,
    @Schema(description = "동쪽 경도 (최대 경도)", example = "127.1")
    val east: Double,
    @Schema(description = "북쪽 위도 (최대 위도)", example = "37.6")
    val north: Double,
)

@Schema(description = "앨범 지도 정보 응답")
data class AlbumMapInfoResponse(
    @Schema(description = "앨범 ID", example = "1")
    val albumId: Long,
    @Schema(description = "센터 경도 (사진이 없으면 null)", example = "127.0276")
    val centerLongitude: Double?,
    @Schema(description = "센터 위도 (사진이 없으면 null)", example = "37.4979")
    val centerLatitude: Double?,
    @Schema(description = "바운딩 박스 (사진이 없으면 null)")
    val boundingBox: BoundingBoxResponse?,
)

@Schema(description = "위치 정보 응답")
data class LocationInfoResponse(
    @Schema(description = "주소", example = "테헤란로 123-45")
    val address: String?,
    @Schema(description = "도로명", example = "테헤란로")
    val roadName: String? = null,
    @Schema(description = "장소명", example = "역삼역 3번출구")
    val placeName: String?,
    @Schema(description = "지역명", example = "강남구")
    val regionName: String?,
)

@Schema(description = "장소 정보")
data class PlaceResponse(
    @Schema(description = "장소명", example = "스타벅스 강남역점")
    val placeName: String,
    @Schema(description = "주소", example = "역삼동 858")
    val address: String?,
    @Schema(description = "도로명 주소", example = "서울 강남구 강남대로 396")
    val roadAddress: String?,
    @field:NotNull
    @field:DecimalMin(value = "124.0", inclusive = true)
    @field:DecimalMax(value = "132.0", inclusive = true)
    @Schema(
        description = "클러스터 중심 경도",
        example = "126.9780",
        minimum = "124.0",
        maximum = "132.0",
        required = true
    )
    var longitude: Double,
    @field:NotNull
    @field:DecimalMin(value = "33.0", inclusive = true)
    @field:DecimalMax(value = "39.0", inclusive = true)
    @Schema(
        description = "클러스터 중심 위도",
        example = "37.5665",
        minimum = "33.0",
        maximum = "39.0",
        required = true
    )
    var latitude: Double,
    @Schema(description = "카테고리", example = "카페")
    val category: String?,
)

@Schema(description = "장소 검색 응답")
data class PlaceSearchResponse(
    @Schema(description = "검색된 장소 목록")
    val places: List<PlaceResponse>,
)

@Schema(description = "홈 응답")
data class HomeResponse(
    @Schema(description = "위치 정보")
    val location: LocationInfoResponse,
    @Schema(description = "바운딩 박스 (사진이 없으면 null)")
    val boundingBox: BoundingBoxResponse,
    @Schema(description = "앨범 하이라이트 사진들 (최대 4장)")
    val albums: List<AlbumThumbnails>,
) {
    companion object {
        @Schema(description = "앨범 썸네일 정보")
        data class AlbumThumbnails(
            @Schema(description = "앨범 ID", example = "1")
            val id: Long,
            @Schema(description = "앨범 ID", example = "1")
            val title: String,
            @Schema(description = "앨범 ID", example = "1")
            val photoCount: Int,
            @Schema(description = "앨범 썸네일 사진들 (최대 4장)")
            val thumbnailUrls: List<String>,
        )

        fun of(location: LocationInfoResponse, albums: List<Album>, bBox: BBox): HomeResponse = HomeResponse(
            location = location,
            albums = albums.toAlbumThumbnails(),
            boundingBox = bBox.toResponse(),
        )

        fun List<Album>.toAlbumThumbnails(): List<AlbumThumbnails> =
            this.map {
                val actualPhotoCount = if (it.isDefault) {
                    it.photos.size
                } else {
                    it.photoCount
                }

                AlbumThumbnails(
                    id = it.id,
                    title = it.title,
                    photoCount = actualPhotoCount,
                    thumbnailUrls = it.thumbnails.map { thumbnail -> thumbnail.url }
                )
            }
    }
}

typealias ClusterPhotosPageResponse = List<ClusterPhotoResponse>

@Schema(description = "지도 ME 응답 (홈 + 사진 조회 통합)")
data class MapMeResponse(
    @Schema(description = "위치 정보")
    val location: LocationInfoResponse,
    @Schema(description = "바운딩 박스")
    val boundingBox: BoundingBoxResponse,
    @Schema(description = "기록 수(전체 보기 사진 개수)")
    val totalHistoryCount: Int,
    @Schema(description = "앨범 하이라이트 사진들 (최대 4장)")
    val albums: List<HomeResponse.Companion.AlbumThumbnails>,
    @Schema(description = "데이터 버전 (사진 변경 시 증가, 프론트 캐싱에 사용)")
    val dataVersion: Long = 0,
    @Schema(description = "클러스터 목록 (줌 < 15일 때, dataVersion 미변경 시 null)")
    val clusters: List<ClusterResponse>? = null,
    @Schema(description = "개별 사진 목록 (줌 >= 15일 때, dataVersion 미변경 시 null)")
    val photos: List<MapPhotoResponse>? = null,
)
