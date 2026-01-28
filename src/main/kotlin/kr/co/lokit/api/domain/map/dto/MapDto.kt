package kr.co.lokit.api.domain.map.dto

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "클러스터 응답")
data class ClusterResponse(
    @Schema(description = "클러스터 ID (줌 레벨 + 그리드 셀 인덱스)", example = "z14_130234_38456")
    val clusterId: String,
    @Schema(description = "클러스터 내 사진 개수", example = "42")
    val count: Int,
    @Schema(description = "대표 썸네일 URL (가장 최근 생성된 사진)", example = "https://example.com/photo.jpg")
    val thumbnailUrl: String,
    @Schema(description = "클러스터 중심 경도", example = "127.0276")
    val longitude: Double,
    @Schema(description = "클러스터 중심 위도", example = "37.4979")
    val latitude: Double,
)

@Schema(description = "개별 사진 응답 (줌 >= 15)")
data class MapPhotoResponse(
    @Schema(description = "사진 ID", example = "1")
    val id: Long,
    @Schema(description = "썸네일 URL", example = "https://example.com/photo.jpg")
    val thumbnailUrl: String,
    @Schema(description = "경도", example = "127.0276")
    val longitude: Double,
    @Schema(description = "위도", example = "37.4979")
    val latitude: Double,
    @Schema(description = "촬영일 (yyyy.MM.dd 형식)", example = "2026.01.06")
    val date: String,
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
    @Schema(description = "경도", example = "127.0276")
    val longitude: Double,
    @Schema(description = "위도", example = "37.4979")
    val latitude: Double,
    @Schema(description = "촬영일 (yyyy.MM.dd 형식)", example = "2026.01.06")
    val date: String,
)

@Schema(description = "클러스터 사진 목록 페이지네이션 응답")
data class ClusterPhotosPageResponse(
    @Schema(description = "사진 목록")
    val photos: List<ClusterPhotoResponse>,
    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    val page: Int,
    @Schema(description = "페이지 크기", example = "20")
    val size: Int,
    @Schema(description = "전체 사진 개수", example = "42")
    val totalElements: Long,
    @Schema(description = "전체 페이지 수", example = "3")
    val totalPages: Int,
    @Schema(description = "마지막 페이지 여부", example = "false")
    val last: Boolean,
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
    @Schema(description = "주소", example = "서울특별시 강남구 역삼동 123-45")
    val address: String?,
    @Schema(description = "장소명", example = "역삼역 3번출구")
    val placeName: String?,
    @Schema(description = "지역명", example = "강남구")
    val regionName: String?,
)

@Schema(description = "장소 정보")
data class PlaceResponse(
    @Schema(description = "장소명", example = "스타벅스 강남역점")
    val placeName: String,
    @Schema(description = "지번 주소", example = "서울 강남구 역삼동 858")
    val address: String,
    @Schema(description = "도로명 주소", example = "서울 강남구 강남대로 396")
    val roadAddress: String?,
    @Schema(description = "경도", example = "127.0276")
    val longitude: Double,
    @Schema(description = "위도", example = "37.4979")
    val latitude: Double,
    @Schema(description = "카테고리", example = "카페")
    val category: String?,
)

@Schema(description = "장소 검색 응답")
data class PlaceSearchResponse(
    @Schema(description = "검색된 장소 목록")
    val places: List<PlaceResponse>,
)