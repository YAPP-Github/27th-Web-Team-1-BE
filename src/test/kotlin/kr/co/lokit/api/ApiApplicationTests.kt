package kr.co.lokit.api

import kr.co.lokit.api.config.TestMapConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestMapConfig::class)
class ApiApplicationTests {
    @Test
    fun contextLoads() {
    }
}
