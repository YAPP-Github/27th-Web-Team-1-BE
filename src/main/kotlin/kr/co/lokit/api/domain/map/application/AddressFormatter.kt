package kr.co.lokit.api.domain.map.application

object AddressFormatter {

    private val buildingNumberRegex = Regex("^\\d+(-\\d+)?$")
    private val roadRegex = Regex(".*(대로|로)$")
    private val branchRoadRegex = Regex(".*(번길|길)$")

    /**
     * 지도 헤더 표시 정책: [주 도로명 + ○길/○번길]
     * 건물번호는 생략한다.
     * 예: "부산광역시 강서구 녹산산단382로 14번길 214-31" -> "녹산산단382로 14번길"
     * 예: "경기도 안성시 죽산면 죽산초교길 69-4" -> "죽산초교길"
     */
    fun toRoadHeader(addressName: String?, roadName: String?): String? {
        if (addressName.isNullOrBlank()) return addressName
        if (roadName.isNullOrBlank()) return toRoadHeader(addressName)

        val tokens = addressName.split(Regex("\\s+"))
        val roadIndex = tokens.indexOfFirst { it.contains(roadName) }

        if (roadIndex == -1) return toRoadHeader(addressName)

        val roadToken = tokens[roadIndex]
        // roadName이 포함된 토큰 이후에 분기 도로명(길/번길)이 있는지 확인
        val nextToken = tokens.getOrNull(roadIndex + 1)
        val hasBranch = nextToken?.matches(branchRoadRegex) ?: false

        return if (hasBranch) {
            "$roadToken $nextToken"
        } else {
            roadToken
        }
    }

    fun toRoadHeader(address: String?): String? {
        if (address.isNullOrBlank()) return address

        // 공백 기준 토큰화
        val tokens = address.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return address

        // 1) 주 도로명(…로/…대로) 탐색
        val mainIndex = tokens.indexOfFirst { it.matches(roadRegex) }
        if (mainIndex >= 0) {
            val main = tokens[mainIndex]
            // 2) 바로 다음 토큰이 분기 도로명(…길/…번길)이면 함께 포함
            val branch = tokens.getOrNull(mainIndex + 1)
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
}
