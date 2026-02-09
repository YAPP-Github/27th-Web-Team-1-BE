package kr.co.lokit.api.domain.user.infrastructure.oauth

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.user.dto.KakaoTokenResponse
import kr.co.lokit.api.domain.user.dto.KakaoUserInfoResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@Component
@EnableConfigurationProperties(KakaoOAuthProperties::class)
class KakaoOAuthClient(
    private val properties: KakaoOAuthProperties,
    private val restClient: RestClient = RestClient.builder()
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(5_000)
            setReadTimeout(10_000)
        })
        .build(),
) : OAuthClient {

    override val provider: OAuthProvider = OAuthProvider.KAKAO

    override fun getAccessToken(code: String): String {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", properties.clientId)
            add("client_secret", properties.clientSecret)
            add("redirect_uri", properties.redirectUri)
            add("code", code)
        }

        val response = restClient.post()
            .uri(KakaoOAuthProperties.TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError) { _, res ->
                throw BusinessException.InvalidKakaoTokenException(
                    message = "카카오 인가 코드가 유효하지 않습니다 (status: ${res.statusCode})",
                    errors = mapOf("statusCode" to res.statusCode.value().toString()),
                )
            }
            .onStatus(HttpStatusCode::is5xxServerError) { _, res ->
                throw BusinessException.KakaoApiException(
                    message = "카카오 토큰 발급 중 서버 오류가 발생했습니다 (status: ${res.statusCode})",
                    errors = mapOf("statusCode" to res.statusCode.value().toString()),
                )
            }
            .body(KakaoTokenResponse::class.java)
            ?: throw BusinessException.KakaoApiException(
                message = "카카오 토큰 응답을 파싱할 수 없습니다",
            )

        return response.accessToken
    }

    override fun getUserInfo(accessToken: String): OAuthUserInfo {
        val response = restClient.get()
            .uri(KakaoOAuthProperties.USER_INFO_URL)
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError) { _, res ->
                throw BusinessException.InvalidKakaoTokenException(
                    message = "카카오 토큰이 유효하지 않습니다 (status: ${res.statusCode})",
                    errors = mapOf("statusCode" to res.statusCode.value().toString()),
                )
            }
            .onStatus(HttpStatusCode::is5xxServerError) { _, res ->
                throw BusinessException.KakaoApiException(
                    message = "카카오 서버 오류가 발생했습니다 (status: ${res.statusCode})",
                    errors = mapOf("statusCode" to res.statusCode.value().toString()),
                )
            }
            .body(KakaoUserInfoResponse::class.java)
            ?: throw BusinessException.KakaoApiException(
                message = "카카오 API 응답을 파싱할 수 없습니다",
            )

        return KakaoOAuthUserInfo(response)
    }
}