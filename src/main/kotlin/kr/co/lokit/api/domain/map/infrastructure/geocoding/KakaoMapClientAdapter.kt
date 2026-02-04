package kr.co.lokit.api.domain.map.infrastructure.geocoding

import kr.co.lokit.api.domain.map.application.port.MapClientPort
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.PlaceResponse
import org.springframework.stereotype.Component

@Component
class KakaoMapClientAdapter(
    private val kakaoMapClient: KakaoMapClient,
) : MapClientPort {
    override fun reverseGeocode(longitude: Double, latitude: Double): LocationInfoResponse =
        kakaoMapClient.reverseGeocode(longitude, latitude)

    override fun searchPlaces(query: String): List<PlaceResponse> =
        kakaoMapClient.searchPlaces(query)
}
