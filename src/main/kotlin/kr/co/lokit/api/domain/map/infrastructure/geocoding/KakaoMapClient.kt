package kr.co.lokit.api.domain.map.infrastructure.geocoding

import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.domain.map.application.AddressFormatter
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.PlaceResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
@ConditionalOnExpression("'\${kakao.api.key:}'.trim().length() > 0")
class KakaoMapClient(
    @Value("\${kakao.api.key}")
    private val apiKey: String,
) : MapClient {
    private val restClient =
        RestClient
            .builder()
            .baseUrl(BASE_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK $apiKey")
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(5_000)
                    setReadTimeout(10_000)
                },
            ).build()

    @Retryable(
        retryFor = [kr.co.lokit.api.common.exception.BusinessException.KakaoApiException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, multiplier = 2.0, random = true),
    )
    @Cacheable(
        cacheNames = [CacheNames.REVERSE_GEOCODE],
        key = "T(java.lang.Math).round(#longitude * 10000) + ',' + T(java.lang.Math).round(#latitude * 10000)",
        unless = "#result.address == null && #result.placeName == null",
    )
    override fun reverseGeocode(
        longitude: Double,
        latitude: Double,
    ): LocationInfoResponse {
        val response =
            try {
                restClient
                    .get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/v2/local/geo/coord2address.json")
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .build()
                    }.retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError) { _, res ->
                        throw kr.co.lokit.api.common.exception.BusinessException.InvalidInputException(
                            message = "카카오 좌표변환 요청이 올바르지 않습니다 (status: ${res.statusCode})",
                            errors = errorDetailsOf(ErrorField.STATUS_CODE to res.statusCode.value()),
                        )
                    }.onStatus(HttpStatusCode::is5xxServerError) { _, res ->
                        throw kr.co.lokit.api.common.exception.BusinessException.KakaoApiException(
                            message = "카카오 좌표변환 서버 오류가 발생했습니다 (status: ${res.statusCode})",
                            errors = errorDetailsOf(ErrorField.STATUS_CODE to res.statusCode.value()),
                        )
                    }.body(KakaoGeocodingResponse::class.java)
            } catch (e: kr.co.lokit.api.common.exception.BusinessException.InvalidInputException) {
                throw e
            } catch (e: RestClientException) {
                throw kr.co.lokit.api.common.exception.BusinessException.KakaoApiException(
                    message = "카카오 좌표변환 API 호출이 실패했습니다",
                    cause = e,
                )
            }

        val document = response?.documents?.firstOrNull()
        val roadAddress = document?.roadAddress
        val address = document?.address

        val formattedAddress =
            if (roadAddress != null) {
                AddressFormatter.buildAddressFromRegions(
                    region3depthName = roadAddress.region3depthName,
                    roadName = roadAddress.roadName,
                    buildingNo = roadAddress.mainBuildingNo,
                    subBuildingNo = roadAddress.subBuildingNo,
                )
            } else {
                address?.let {
                    AddressFormatter.buildAddressFromRegions(
                        region3depthName = it.region3depthName,
                        addressNo = it.mainAddressNo,
                        subAddressNo = it.subAddressNo,
                    )
                }
            }

        val regionName = address?.region2depthName ?: roadAddress?.region2depthName

        return LocationInfoResponse(
            address = formattedAddress,
            roadName = roadAddress?.roadName,
            placeName = roadAddress?.buildingName?.takeIf { it.isNotBlank() },
            regionName = AddressFormatter.removeProvinceAndCity(regionName),
        )
    }

    @Retryable(
        retryFor = [kr.co.lokit.api.common.exception.BusinessException.KakaoApiException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, multiplier = 2.0, random = true),
    )
    @Cacheable(cacheNames = [CacheNames.SEARCH_PLACES], key = "#query", unless = "#result.isEmpty()")
    override fun searchPlaces(query: String): List<PlaceResponse> {
        val response =
            try {
                restClient
                    .get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/v2/local/search/keyword.json")
                            .queryParam("query", query)
                            .queryParam("size", MAX_SEARCH_RESULTS)
                            .build()
                    }.retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError) { _, res ->
                        throw kr.co.lokit.api.common.exception.BusinessException.InvalidInputException(
                            message = "카카오 장소검색 요청이 올바르지 않습니다 (status: ${res.statusCode})",
                            errors = errorDetailsOf(ErrorField.STATUS_CODE to res.statusCode.value()),
                        )
                    }.onStatus(HttpStatusCode::is5xxServerError) { _, res ->
                        throw kr.co.lokit.api.common.exception.BusinessException.KakaoApiException(
                            message = "카카오 장소검색 서버 오류가 발생했습니다 (status: ${res.statusCode})",
                            errors = errorDetailsOf(ErrorField.STATUS_CODE to res.statusCode.value()),
                        )
                    }.body(KakaoPlaceSearchResponse::class.java)
            } catch (e: kr.co.lokit.api.common.exception.BusinessException.InvalidInputException) {
                throw e
            } catch (e: RestClientException) {
                throw kr.co.lokit.api.common.exception.BusinessException.KakaoApiException(
                    message = "카카오 장소검색 API 호출이 실패했습니다",
                    cause = e,
                )
            }

        return response?.documents?.map { doc ->
            PlaceResponse(
                placeName = doc.placeName,
                address = AddressFormatter.removeProvinceAndCity(doc.addressName),
                roadAddress =
                    doc.roadAddressName
                        .takeIf { it.isNotBlank() }
                        ?.let { AddressFormatter.removeProvinceAndCity(it) },
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
