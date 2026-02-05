package kr.co.lokit.api.domain.map.application

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AddressFormatterTest {

    @Test
    fun `카카오 응답 형식을 참고하여 도로명을 추출한다 - 안성시 죽산초교길`() {
        val addressName = "경기도 안성시 죽산면 죽산초교길 69-4"
        val roadName = "죽산초교길"

        val result = AddressFormatter.toRoadHeader(addressName, roadName)

        assertEquals("죽산초교길", result)
    }

    @Test
    fun `카카오 응답 형식을 참고하여 도로명을 추출한다 - 녹산산단382로`() {
        val addressName = "부산광역시 강서구 녹산산단382로 14번길 214-31"
        val roadName = "녹산산단382로"

        val result = AddressFormatter.toRoadHeader(addressName, roadName)

        assertEquals("녹산산단382로 14번길", result)
    }

    @Test
    fun `도로명 정보가 없으면 기존 로직을 사용한다`() {
        val addressName = "부산광역시 강서구 녹산산단382로 14번길 214-31"

        val result = AddressFormatter.toRoadHeader(addressName, "")

        assertEquals("녹산산단382로 14번길", result)
    }

    @Test
    fun `도로명 토큰 뒤에 길이나 번길이 없으면 도로명만 반환한다`() {
        val addressName = "서울특별시 강남구 테헤란로 123"
        val roadName = "테헤란로"

        val result = AddressFormatter.toRoadHeader(addressName, roadName)

        assertEquals("테헤란로", result)
    }
}
