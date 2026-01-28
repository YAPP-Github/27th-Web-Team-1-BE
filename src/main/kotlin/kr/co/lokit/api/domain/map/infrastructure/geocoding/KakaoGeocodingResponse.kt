package kr.co.lokit.api.domain.map.infrastructure.geocoding

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoGeocodingResponse(
    val meta: Meta,
    val documents: List<Document>,
) {
    data class Meta(
        @JsonProperty("total_count")
        val totalCount: Int,
    )

    data class Document(
        @JsonProperty("road_address")
        val roadAddress: RoadAddress?,
        val address: Address?,
    )

    data class RoadAddress(
        @JsonProperty("address_name")
        val addressName: String,
        @JsonProperty("region_1depth_name")
        val region1depthName: String,
        @JsonProperty("region_2depth_name")
        val region2depthName: String,
        @JsonProperty("region_3depth_name")
        val region3depthName: String,
        @JsonProperty("road_name")
        val roadName: String,
        @JsonProperty("underground_yn")
        val undergroundYn: String,
        @JsonProperty("main_building_no")
        val mainBuildingNo: String,
        @JsonProperty("sub_building_no")
        val subBuildingNo: String,
        @JsonProperty("building_name")
        val buildingName: String,
        @JsonProperty("zone_no")
        val zoneNo: String,
    )

    data class Address(
        @JsonProperty("address_name")
        val addressName: String,
        @JsonProperty("region_1depth_name")
        val region1depthName: String,
        @JsonProperty("region_2depth_name")
        val region2depthName: String,
        @JsonProperty("region_3depth_name")
        val region3depthName: String,
        @JsonProperty("mountain_yn")
        val mountainYn: String,
        @JsonProperty("main_address_no")
        val mainAddressNo: String,
        @JsonProperty("sub_address_no")
        val subAddressNo: String,
    )
}
