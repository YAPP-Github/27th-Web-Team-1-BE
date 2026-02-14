package kr.co.lokit.api.domain.map.application

enum class ClusterBoundaryMergeType(
    val propertyValue: String,
) {
    DISTANCE("distance"),
    LEGACY("legacy"),
    ;

    companion object {
        fun from(raw: String?): ClusterBoundaryMergeType =
            entries.firstOrNull { it.propertyValue.equals(raw, ignoreCase = true) } ?: DISTANCE
    }
}
