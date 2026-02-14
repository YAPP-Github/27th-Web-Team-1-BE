package kr.co.lokit.api.config.docs

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.lang.reflect.Method

@Configuration
class OpenApiConfig(
    @Value("\${spring.profiles.active:local}")
    private val activeProfile: String,
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
                    Tag().name("Couple").description("커플 API"),
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
    fun apiV10Group(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("api-v1.0")
            .packagesToScan("kr.co.lokit.api")
            .addOpenApiMethodFilter { supportsVersion(it, "1.0") }
            .addOperationCustomizer { operation, _ -> addVersionHeader(operation, "1.0") }
            .build()

    @Bean
    fun apiV11Group(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("api-v1.1")
            .packagesToScan("kr.co.lokit.api")
            .addOpenApiMethodFilter { supportsVersion(it, "1.1") }
            .addOperationCustomizer { operation, _ -> addVersionHeader(operation, "1.1") }
            .build()

    private fun supportsVersion(method: Method, targetVersion: String): Boolean {
        val versions = findVersions(method)
        return versions.isEmpty() || versions.contains(targetVersion)
    }

    private fun findVersions(method: Method): Set<String> {
        val methodVersions =
            (
                versionAsList(method.getAnnotation(GetMapping::class.java)?.version) +
                    versionAsList(method.getAnnotation(PostMapping::class.java)?.version) +
                    versionAsList(method.getAnnotation(PutMapping::class.java)?.version) +
                    versionAsList(method.getAnnotation(DeleteMapping::class.java)?.version) +
                    versionAsList(method.getAnnotation(PatchMapping::class.java)?.version) +
                    versionAsList(method.getAnnotation(RequestMapping::class.java)?.version)
            ).toSet()
        if (methodVersions.isNotEmpty()) {
            return methodVersions
        }

        val classVersions =
            (
                versionAsList(method.declaringClass.getAnnotation(RequestMapping::class.java)?.version)
            ).toSet()

        return classVersions
    }

    private fun versionAsList(version: String?): List<String> =
        if (version.isNullOrBlank()) {
            emptyList()
        } else {
            listOf(version)
        }

    private fun addVersionHeader(
        operation: io.swagger.v3.oas.models.Operation,
        version: String,
    ): io.swagger.v3.oas.models.Operation {
        val hasVersionHeader =
            operation.parameters
                ?.any { it.`in` == "header" && it.name.equals("X-API-VERSION", ignoreCase = true) }
                ?: false
        if (!hasVersionHeader) {
            operation.addParametersItem(
                Parameter()
                    .name("X-API-VERSION")
                    .`in`("header")
                    .required(true)
                    .description("API 버전 헤더")
                    .schema(
                        StringSchema()
                            ._default(version),
                    ),
            )
        }
        return operation
    }
}
