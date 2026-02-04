package kr.co.lokit.api.infrastructure.exposed

import kr.co.lokit.api.domain.map.application.port.ClusterCandidate
import kr.co.lokit.api.domain.map.application.port.ClusterData
import kr.co.lokit.api.domain.map.application.port.ClusterProjection
import kr.co.lokit.api.domain.map.application.port.GridKey
import kr.co.lokit.api.domain.map.application.port.RankedPhoto
import kr.co.lokit.api.domain.map.application.port.UniquePhotoRecord

object ClusteringPipeline {

    fun groupByGridCell(photos: List<UniquePhotoRecord>): Map<GridKey, List<UniquePhotoRecord>> =
        photos.groupBy { GridKey(it.cellX, it.cellY) }

    fun calculateClusterStats(grouped: Map<GridKey, List<UniquePhotoRecord>>): List<ClusterData> =
        grouped.map { (key, photos) ->
            val sortedByCreatedAt = photos.sortedByDescending { it.createdAt }
            ClusterData(
                gridKey = key,
                count = photos.size,
                centerLongitude = photos.map { it.longitude }.average(),
                centerLatitude = photos.map { it.latitude }.average(),
                photosByRank = sortedByCreatedAt.mapIndexed { index, photo ->
                    RankedPhoto(url = photo.url, rank = index + 1)
                },
            )
        }

    fun deduplicateThumbnails(clusters: List<ClusterData>): List<ClusterProjection> {
        val sortedClusters = clusters.sortedByDescending { it.count }

        return sortedClusters.fold(
            initial = DeduplicationState(usedUrls = emptySet(), results = emptyList()),
        ) { state, cluster ->
            val selectedUrl = cluster.photosByRank
                .sortedBy { it.rank }
                .firstOrNull { it.url !in state.usedUrls }
                ?.url
                ?: cluster.photosByRank.firstOrNull()?.url
                ?: ""

            DeduplicationState(
                usedUrls = state.usedUrls + selectedUrl,
                results = state.results + ClusterProjection(
                    cellX = cluster.gridKey.cellX,
                    cellY = cluster.gridKey.cellY,
                    count = cluster.count,
                    thumbnailUrl = selectedUrl,
                    centerLongitude = cluster.centerLongitude,
                    centerLatitude = cluster.centerLatitude,
                ),
            )
        }.results
    }

    fun deduplicateThumbnailsFromCandidates(candidates: List<ClusterCandidate>): List<ClusterProjection> {
        val grouped = candidates.groupBy { GridKey(it.cellX, it.cellY) }

        val clusterData = grouped.map { (key, clusterCandidates) ->
            val first = clusterCandidates.first()
            ClusterData(
                gridKey = key,
                count = first.count,
                centerLongitude = first.centerLongitude,
                centerLatitude = first.centerLatitude,
                photosByRank = clusterCandidates.map { RankedPhoto(it.thumbnailUrl, it.rank) },
            )
        }

        return deduplicateThumbnails(clusterData)
    }

    private data class DeduplicationState(
        val usedUrls: Set<String>,
        val results: List<ClusterProjection>,
    )
}

fun List<UniquePhotoRecord>.toClusterProjections(): List<ClusterProjection> =
    this
        .let(ClusteringPipeline::groupByGridCell)
        .let(ClusteringPipeline::calculateClusterStats)
        .let(ClusteringPipeline::deduplicateThumbnails)
