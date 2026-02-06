package kr.co.lokit.api.domain.map.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotoResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotosPageResponse
import kr.co.lokit.api.domain.map.dto.HomeResponse
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapMeResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.dto.PlaceSearchResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

@SecurityRequirement(name = "Authorization")
@Tag(name = "Map", description = "지도 API")
interface MapApi {
    @Operation(
        summary = "홈 조회(홈 화면 초기 진입 시 1회 호출)",
        description = "",
        hidden = true,
    )
    fun home(
        @Parameter(hidden = true) userId: Long,
        @RequestParam longitude: Double,
        @RequestParam latitude: Double,
    ): HomeResponse

    @Operation(
        summary = "지도 ME 조회 (홈 + 사진 조회 통합)",
        description = """
            홈 정보와 지도 사진을 한 번에 조회합니다.

            - 위치 정보, 앨범 목록, 바운딩 박스 (map/home 응답)
            - 줌 레벨과 바운딩 박스 기반 사진/클러스터 (map/photos 응답)
            - 두 API를 하나로 통합하여 네트워크 요청을 줄입니다.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = MapMeResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 파라미터",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content()],
            ),
        ],
    )
    fun getMe(
        @Parameter(hidden = true) userId: Long,
        @Parameter(
            description = "경도",
            example = "127.0276",
            required = true,
        )
        @RequestParam longitude: Double,
        @Parameter(
            description = "위도",
            example = "37.4979",
            required = true,
        )
        @RequestParam latitude: Double,
        @Parameter(
            description = "줌 레벨. 15 미만이면 클러스터링, 15 이상이면 개별 사진 반환",
            example = "12",
            required = true,
        )
        @RequestParam zoom: Int,
        @Parameter(
            description = "바운딩 박스 (west,south,east,north 형식의 경도/위도)",
            example = "126.9,37.4,127.1,37.6",
            required = true,
        )
        @RequestParam albumId: Long?,
        @Parameter(
            description = "이전 응답의 dataVersion. 일치하면 사진 데이터를 생략하여 응답 최적화",
            example = "3",
            required = false,
        )
        @RequestParam lastDataVersion: Long?,
    ): MapMeResponse

    @Operation(
        summary = "지도 사진 조회",
        description = """
            줌 레벨과 바운딩 박스를 기반으로 지도에 표시할 사진 또는 클러스터를 조회합니다.

            - **줌 레벨 < 15**: ST_SnapToGrid를 사용하여 사진을 클러스터링하여 반환
            - clusterId: 줌 레벨 + 그리드 셀 인덱스 (예: z14_130234_38456)
            - count: 클러스터 내 사진 개수
            - thumbnailUrl: 클러스터 내 가장 최근 생성된 사진의 URL

            - **줌 레벨 >= 15**: 개별 사진 썸네일을 반환
        """,
        hidden = true,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = MapPhotosResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 파라미터",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "403",
                description = "접근 권한 없음",
                content = [Content()],
            ),
        ],
    )
    fun getPhotos(
        @Parameter(hidden = true) userId: Long,
        @Parameter(
            description = "줌 레벨 (0-20). 15 미만이면 클러스터링, 16 이상이면 개별 사진 반환",
            example = "12",
            required = true,
        )
        zoom: Int,
        @Parameter(
            description = "바운딩 박스 (west,south,east,north 형식의 경도/위도)",
            example = "126.9,37.4,127.1,37.6",
            required = true,
        )
        bbox: String,
        @Parameter(
            description = "앨범 ID (선택). 지정 시 해당 앨범의 사진만 조회",
            example = "1",
            required = false,
        )
        @RequestParam albumId: Long?,
    ): MapPhotosResponse

    @Operation(
        summary = "클러스터 내 사진 목록 조회",
        description = """
            클러스터 ID를 기반으로 해당 그리드 셀 영역 내의 모든 사진을 조회합니다.

            - clusterId는 GET /map/photos 응답에서 반환된 값을 사용
            - clusterId 형식: z{zoom}_{cellX}_{cellY} (예: z14_130234_38456)
            - 사진은 생성일시 기준 내림차순 정렬
            - 페이지네이션 지원 (page, size 파라미터)
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = ClusterPhotoResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 clusterId 형식",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "403",
                description = "접근 권한 없음",
                content = [Content()],
            ),
        ],
    )
    fun getClusterPhotos(
        @Parameter(hidden = true) userId: Long,
        @Parameter(
            description = "클러스터 ID (z{zoom}_{cellX}_{cellY} 형식)",
            example = "z14_130234_38456",
            required = true,
        )
        @RequestParam clusterId: String,
    ): ClusterPhotosPageResponse

    @Operation(
        summary = "앨범 지도 정보 조회",
        description = """
            앨범 ID를 기반으로 해당 앨범 사진들의 중심 좌표와 바운딩 박스를 조회합니다.

            - 앨범에 사진이 없는 경우 centerLongitude, centerLatitude, boundingBox가 null
            - 앨범 선택 시 지도를 해당 위치로 이동하는 데 사용
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = AlbumMapInfoResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "403",
                description = "접근 권한 없음",
                content = [Content()],
            ),
        ],
    )
    fun getAlbumMapInfo(
        @Parameter(hidden = true) userId: Long,
        @Parameter(
            description = "앨범 ID",
            example = "1",
            required = true,
        )
        @PathVariable albumId: Long,
    ): AlbumMapInfoResponse

    @Operation(
        summary = "위치 정보 조회",
        description = """
            좌표를 기반으로 해당 위치의 주소 정보를 조회합니다.

            - Kakao 역지오코딩 API를 사용하여 좌표 → 주소 변환
            - 도로명 주소 우선, 없으면 지번 주소 반환
            - 건물명이 있으면 placeName에 포함
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = LocationInfoResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content()],
            ),
        ],
    )
    fun getLocationInfo(
        @Parameter(
            description = "경도",
            example = "127.0276",
            required = true,
        )
        @RequestParam longitude: Double,
        @Parameter(
            description = "위도",
            example = "37.4979",
            required = true,
        )
        @RequestParam latitude: Double,
    ): LocationInfoResponse

    @Operation(
        summary = "장소 검색",
        description = """
            키워드로 장소를 검색합니다.

            - Kakao 키워드 검색 API를 사용하여 장소 정보 조회
            - 최대 15개의 검색 결과 반환
            - 장소명, 주소, 좌표, 카테고리 정보 포함
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "검색 성공",
                content = [Content(schema = Schema(implementation = PlaceSearchResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content()],
            ),
        ],
    )
    fun searchPlaces(
        @Parameter(
            description = "검색 키워드",
            example = "스타벅스 강남",
            required = true,
        )
        @RequestParam query: String,
    ): PlaceSearchResponse
}
