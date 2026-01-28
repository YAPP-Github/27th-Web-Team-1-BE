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
class OpenApiConfig {
    @Value("\${server.servlet.context-path:/}")
    private lateinit var contextPath: String

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
                    Tag().name("Workspace").description("워크스페이스 API"),
                    Tag().name("Album").description("앨범 API"),
                    Tag().name("Photo").description("사진 API"),
                    Tag().name("Map").description("지도 API"),
                ),
            ).components(
                Components()
                    .addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name("Authorization")
                            .description("JWT 토큰 또는 사용자 ID (dev/local 환경)"),
                    ),
            ).addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME_NAME))

    companion object {
        const val SECURITY_SCHEME_NAME = "Authorization"
    }

    @Bean
    fun apiGroup(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("api")
            .packagesToScan("kr.co.lokit.api")
            .build()
}
