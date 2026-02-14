package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.common.util.orFalse

object AddressFormatter {
    private val buildingNumberRegex = Regex("^\\d+(-\\d+)?$")
    private val roadRegex = Regex(".*(대로|로)$")
    private val branchRoadRegex = Regex(".*(번길|길)$")

    fun toRoadHeader(
        addressName: String,
        roadName: String,
    ): String {
        if (addressName.isBlank()) return addressName
        if (roadName.isBlank()) return toRoadHeader(addressName)

        val tokens = addressName.split(Regex("\\s+"))
        val roadIndex = tokens.indexOfFirst { it.contains(roadName) }

        if (roadIndex == -1) return toRoadHeader(addressName)

        val roadToken = tokens[roadIndex]
        val nextToken = tokens.getOrNull(roadIndex + 1)
        val hasBranch = nextToken?.matches(branchRoadRegex).orFalse()

        return if (hasBranch) {
            "$roadToken $nextToken"
        } else {
            roadToken
        }
    }

    fun toRoadHeader(address: String): String {
        if (address.isBlank()) return address

        val tokens = address.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return address

        val mainIndex = tokens.indexOfFirst { it.matches(roadRegex) }
        if (mainIndex >= 0) {
            val main = tokens[mainIndex]
            val branch =
                tokens
                    .getOrNull(mainIndex + 1)
                    ?.takeIf { it.matches(branchRoadRegex) }

            return buildString {
                append(main)
                if (branch != null) {
                    append(' ')
                    append(branch)
                }
            }
        }

        val branchOnly = tokens.firstOrNull { it.matches(branchRoadRegex) }
        if (branchOnly != null) return branchOnly

        val withoutNumbers = tokens.filterNot { it.matches(buildingNumberRegex) }
        return if (withoutNumbers.isNotEmpty()) withoutNumbers.joinToString(" ") else address
    }

    fun removeProvinceAndCity(address: String?): String? {
        if (address.isNullOrBlank()) return address

        val tokens = address.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val filtered =
            tokens.filterNot { token ->
                token.endsWith("도") || token.endsWith("시") ||
                    token.endsWith("특별시") || token.endsWith("광역시")
            }

        return if (filtered.isNotEmpty()) filtered.joinToString(" ") else address
    }

    fun buildAddressFromRegions(
        region3depthName: String?,
        roadName: String? = null,
        buildingNo: String? = null,
        subBuildingNo: String? = null,
        addressNo: String? = null,
        subAddressNo: String? = null,
    ): String? {
        val parts = mutableListOf<String>()

        region3depthName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }

        if (roadName != null && roadName.isNotBlank()) {
            parts.add(roadName)
            when {
                buildingNo != null && subBuildingNo != null && subBuildingNo.isNotBlank() -> {
                    parts.add("$buildingNo-$subBuildingNo")
                }

                buildingNo != null && buildingNo.isNotBlank() -> {
                    parts.add(buildingNo)
                }
            }
        } else if (addressNo != null) {
            when {
                addressNo.isNotBlank() && subAddressNo != null && subAddressNo.isNotBlank() -> {
                    parts.add("$addressNo-$subAddressNo")
                }

                addressNo.isNotBlank() -> {
                    parts.add(addressNo)
                }
            }
        }

        return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }
}
