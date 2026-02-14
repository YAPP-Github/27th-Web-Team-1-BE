package kr.co.lokit.api.domain.map.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapDtoSerializationTest {
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())

    @Test
    fun `ClusterResponse 직렬화 결과에는 내부 takenAt 필드가 포함되지 않는다`() {
        val response =
            ClusterResponse(
                clusterId = "z11_10_10",
                count = 2,
                thumbnailUrl = "https://example.com/a.jpg",
                longitude = 127.0,
                latitude = 37.3,
                takenAt = LocalDateTime.of(2025, 1, 1, 10, 0),
            )

        val json = objectMapper.writeValueAsString(response)

        assertTrue(json.contains("\"clusterId\""))
        assertFalse(json.contains("takenAt"))
    }
}
