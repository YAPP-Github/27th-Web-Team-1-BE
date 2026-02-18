package kr.co.lokit.api.domain.map.domain

import kr.co.lokit.api.common.util.orZero

object MapDataVersion {
    fun of(
        coupleId: Long,
        albumId: Long?,
        mutationVersion: Long,
    ): Long = (fnv64(coupleId, albumId.orZero(), mutationVersion) and Long.MAX_VALUE)

    private fun fnv64(vararg values: Long): Long {
        var hash = FNV64_OFFSET_BASIS
        values.forEach { value ->
            hash = hash xor value
            hash *= FNV64_PRIME
        }
        return hash
    }

    private const val FNV64_OFFSET_BASIS = -3750763034362895579L
    private const val FNV64_PRIME = 1099511628211L
}
