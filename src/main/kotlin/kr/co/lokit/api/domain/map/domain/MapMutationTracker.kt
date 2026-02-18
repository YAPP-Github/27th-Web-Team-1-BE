package kr.co.lokit.api.domain.map.domain

import kr.co.lokit.api.common.util.orZero
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong

class MapMutationTracker(
    private val maxMutationsPerCouple: Int,
) {
    private data class PhotoMutation(
        val sequence: Long,
        val longitude: Double,
        val latitude: Double,
        val albumId: Long?,
    )

    private val coupleVersions = ConcurrentHashMap<Long, AtomicLong>()
    private val coupleMutations = ConcurrentHashMap<Long, ConcurrentLinkedDeque<PhotoMutation>>()

    fun evictForCouple(coupleId: Long) {
        coupleVersions.computeIfAbsent(coupleId) { AtomicLong(0) }.incrementAndGet()
        coupleMutations.remove(coupleId)
    }

    fun currentSequence(coupleId: Long?): Long = coupleId?.let { coupleVersions[it]?.get() }.orZero()

    fun recordMutation(
        coupleId: Long,
        albumId: Long?,
        longitude: Double,
        latitude: Double,
    ): Long {
        val sequence = coupleVersions.computeIfAbsent(coupleId) { AtomicLong(0) }.incrementAndGet()
        val deque = coupleMutations.computeIfAbsent(coupleId) { ConcurrentLinkedDeque() }
        deque.addLast(
            PhotoMutation(
                sequence = sequence,
                longitude = longitude,
                latitude = latitude,
                albumId = albumId,
            ),
        )
        while (deque.size > maxMutationsPerCouple) {
            deque.pollFirst()
        }
        return sequence
    }

    fun viewportSequence(
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
    ): Long =
        coupleId
            ?.let { id ->
                coupleMutations[id]
                    ?.asSequence()
                    ?.filter { mutation -> mutation.longitude in bbox.west..bbox.east }
                    ?.filter { mutation -> mutation.latitude in bbox.south..bbox.north }
                    ?.filter { mutation -> albumId == null || mutation.albumId == albumId }
                    ?.maxOfOrNull { mutation -> mutation.sequence }
            }.orZero()

    fun dataVersion(
        coupleId: Long?,
        albumId: Long?,
    ): Long =
        coupleId
            ?.let { id ->
                val albumScoped =
                    if (albumId == null) {
                        currentSequence(id)
                    } else {
                        coupleMutations[id]
                            ?.asSequence()
                            ?.filter { mutation -> mutation.albumId == albumId }
                            ?.maxOfOrNull { mutation -> mutation.sequence }
                            .orZero()
                    }
                MapDataVersion.of(id, albumId, albumScoped)
            }.orZero()
}
