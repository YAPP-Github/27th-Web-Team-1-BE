package kr.co.lokit.api.domain.map.application

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun `주소가 공백이면 toRoadHeader는 입력을 그대로 반환한다`() {
        assertEquals("   ", AddressFormatter.toRoadHeader("   ", "테헤란로"))
        assertEquals("", AddressFormatter.toRoadHeader(""))
    }

    @Test
    fun `지정한 도로명이 주소 토큰에서 발견되지 않으면 일반 추출 로직을 사용한다`() {
        val result = AddressFormatter.toRoadHeader("서울 강남구 테헤란로 123", "없는도로")

        assertEquals("테헤란로", result)
    }

    @Test
    fun `도로명이 없고 번길이나 길만 있으면 해당 토큰을 반환한다`() {
        val result = AddressFormatter.toRoadHeader("서울 강남구 14번길 100")

        assertEquals("14번길", result)
    }

    @Test
    fun `도로명과 길이 모두 없으면 숫자 토큰을 제외한 주소를 반환한다`() {
        val result = AddressFormatter.toRoadHeader("서울 강남구 역삼동 123-45")

        assertEquals("서울 강남구 역삼동", result)
    }

    @Test
    fun `removeProvinceAndCity는 도 시 토큰을 제거한다`() {
        val result = AddressFormatter.removeProvinceAndCity("경기도 성남시 분당구 수내동")

        assertEquals("분당구 수내동", result)
    }

    @Test
    fun `removeProvinceAndCity는 제거 결과가 비면 원본을 반환한다`() {
        val result = AddressFormatter.removeProvinceAndCity("서울특별시")

        assertEquals("서울특별시", result)
    }

    @Test
    fun `removeProvinceAndCity는 null 입력을 그대로 반환한다`() {
        assertNull(AddressFormatter.removeProvinceAndCity(null))
    }

    @Test
    fun `buildAddressFromRegions는 도로명과 본번 부번을 조합한다`() {
        val result =
            AddressFormatter.buildAddressFromRegions(
                region3depthName = "역삼동",
                roadName = "테헤란로",
                buildingNo = "123",
                subBuildingNo = "45",
            )

        assertEquals("역삼동 테헤란로 123-45", result)
    }

    @Test
    fun `buildAddressFromRegions는 도로명이 없으면 지번을 조합한다`() {
        val result =
            AddressFormatter.buildAddressFromRegions(
                region3depthName = "역삼동",
                addressNo = "858",
                subAddressNo = "1",
            )

        assertEquals("역삼동 858-1", result)
    }

    @Test
    fun `buildAddressFromRegions는 유효한 값이 없으면 null을 반환한다`() {
        val result =
            AddressFormatter.buildAddressFromRegions(
                region3depthName = " ",
                roadName = " ",
                buildingNo = " ",
                addressNo = " ",
            )

        assertNull(result)
    }
}
