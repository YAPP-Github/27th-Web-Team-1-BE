package kr.co.lokit.api.domain.map.infrastructure.geocoding

import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.PlaceResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
@ConditionalOnProperty(name = ["kakao.api.key"])
class KakaoMapClient(
    @Value("\${kakao.api.key}")
    private val apiKey: String,
) : MapClient {

    private val restClient = RestClient.builder()
        .baseUrl(BASE_URL)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK $apiKey")
        .build()

    override fun reverseGeocode(longitude: Double, latitude: Double): LocationInfoResponse {
        val response = restClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/v2/local/geo/coord2address.json")
                    .queryParam("x", longitude)
                    .queryParam("y", latitude)
                    .build()
            }
            .retrieve()
            .body(KakaoGeocodingResponse::class.java)

        val document = response?.documents?.firstOrNull()
        val roadAddress = document?.roadAddress
        val address = document?.address

        return LocationInfoResponse(
            address = roadAddress?.addressName ?: address?.addressName,
            placeName = roadAddress?.buildingName?.takeIf { it.isNotBlank() },
            regionName = address?.region2depthName ?: roadAddress?.region2depthName,
        )
    }

    override fun searchPlaces(query: String): List<PlaceResponse> {
        val response = restClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/v2/local/search/keyword.json")
                    .queryParam("query", query)
                    .queryParam("size", MAX_SEARCH_RESULTS)
                    .build()
            }
            .retrieve()
            .body(KakaoPlaceSearchResponse::class.java)

        return response?.documents?.map { doc ->
            PlaceResponse(
                placeName = doc.placeName,
                address = doc.addressName,
                roadAddress = doc.roadAddressName.takeIf { it.isNotBlank() },
                longitude = doc.x.toDoubleOrNull() ?: 0.0,
                latitude = doc.y.toDoubleOrNull() ?: 0.0,
                category = extractCategory(doc.categoryName),
            )
        } ?: emptyList()
    }

    private fun extractCategory(categoryName: String): String? {
        val parts = categoryName.split(" > ")
        return parts.lastOrNull()?.takeIf { it.isNotBlank() }
    }

    companion object {
        private const val BASE_URL = "https://dapi.kakao.com"
        private const val MAX_SEARCH_RESULTS = 15
    }
}
