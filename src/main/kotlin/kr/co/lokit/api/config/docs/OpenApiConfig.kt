package kr.co.lokit.api.config.docs

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    @Value("\${server.servlet.context-path:/}")
    private val contextPath: String,
) {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Lokit API")
                    .version("1.0.0")
                    .description("Lokit API 문서"),
            ).servers(
                listOf(
                    Server().url(contextPath).description("API Server"),
                ),
            ).tags(
                listOf(
                    Tag().name("Auth").description("인증 API"),
                    Tag().name("User").description("사용자 API"),
                    Tag().name("MyPage").description("마이페이지 API"),
                    Tag().name("Couple").description("커플 API"),
                    Tag().name("Album").description("앨범 API"),
                    Tag().name("Photo").description("사진 API"),
                    Tag().name("Comment").description("댓글/이모지 API"),
                    Tag().name("Map").description("지도 API"),
                    Tag().name("NotificationEmail").description("알림 이메일 수집 API"),
                    Tag().name("Admin").description("개발/운영 지원 API"),
                ),
            ).components(
                Components()
                    .addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.COOKIE)
                            .name("accessToken")
                            .description(securityDescription()),
                    ),
            ).addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME_NAME))

    private fun securityDescription(): String =
        """
        쿠키 기반 인증:
        - 카카오 로그인 후 accessToken 쿠키가 자동 설정됩니다
        """.trimIndent()

    companion object {
        const val SECURITY_SCHEME_NAME = "Authorization"
    }

    @Bean
    fun apiGroup(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("api-v1.0")
            .packagesToScan("kr.co.lokit.api")
            .build()
}
