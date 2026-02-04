package kr.co.lokit.api.domain.map.infrastructure

import kr.co.lokit.api.common.dto.PageResult
import kr.co.lokit.api.domain.map.application.port.ClusterPhotoProjection
import kr.co.lokit.api.domain.map.application.port.ClusterProjection
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.application.port.PhotoProjection
import kr.co.lokit.api.domain.map.application.port.UniquePhotoRecord
import kr.co.lokit.api.infrastructure.exposed.PostGisExtensions
import kr.co.lokit.api.infrastructure.exposed.PostGisExtensions.makeEnvelope
import kr.co.lokit.api.infrastructure.exposed.intersects
import kr.co.lokit.api.infrastructure.exposed.schema.PhotoTable
import kr.co.lokit.api.infrastructure.exposed.toClusterProjections
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Repository

@Repository
@ConditionalOnBean(Database::class)
class ExposedMapQueryAdapter(
    private val database: Database,
) : MapQueryPort {

    override fun findClustersWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        gridSize: Double,
        albumId: Long?,
    ): List<ClusterProjection> = transaction(database) {
        val uniquePhotos = queryUniquePhotosWithDistinctOn(west, south, east, north, gridSize, albumId)
        uniquePhotos.toClusterProjections()
    }

    private fun queryUniquePhotosWithDistinctOn(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        gridSize: Double,
        albumId: Long?,
    ): List<UniquePhotoRecord> {
        val sql = buildDistinctOnQuery(albumId)

        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(sql, false)

        var paramIndex = 1
        stmt.set(paramIndex++, west)
        stmt.set(paramIndex++, south)
        stmt.set(paramIndex++, east)
        stmt.set(paramIndex++, north)
        if (albumId != null) stmt.set(paramIndex++, albumId)
        stmt.set(paramIndex++, gridSize)
        stmt.set(paramIndex, gridSize)

        val results = mutableListOf<UniquePhotoRecord>()
        stmt.executeQuery().use { rs ->
            while (rs.next()) {
                results.add(
                    UniquePhotoRecord(
                        id = rs.getLong("id"),
                        url = rs.getString("url"),
                        longitude = rs.getDouble("longitude"),
                        latitude = rs.getDouble("latitude"),
                        cellX = rs.getLong("cell_x"),
                        cellY = rs.getLong("cell_y"),
                        createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                    )
                )
            }
        }
        return results
    }

    private fun buildDistinctOnQuery(albumId: Long?): String = buildString {
        append("SELECT DISTINCT ON (url) ")
        append("id, url, ")
        append("ST_X(location) as longitude, ")
        append("ST_Y(location) as latitude, ")
        append("FLOOR(ST_X(location) / ?) as cell_x, ")
        append("FLOOR(ST_Y(location) / ?) as cell_y, ")
        append("created_at ")
        append("FROM ${PhotoTable.tableName} ")
        append("WHERE location && ST_MakeEnvelope(?, ?, ?, ?, 4326) ")
        append("AND ${PhotoTable.isDeleted.name} = false ")
        if (albumId != null) append("AND ${PhotoTable.albumId.name} = ? ")
        append("ORDER BY url, created_at DESC")
    }

    override fun findPhotosWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        albumId: Long?,
    ): List<PhotoProjection> = transaction(database) {
        val envelope = makeEnvelope(west, south, east, north)
        val longitudeExpr = PostGisExtensions.extractX(PhotoTable.location)
        val latitudeExpr = PostGisExtensions.extractY(PhotoTable.location)

        PhotoTable
            .select(PhotoTable.id, PhotoTable.url, PhotoTable.takenAt, longitudeExpr, latitudeExpr)
            .where {
                val baseCondition = PhotoTable.location.intersects(envelope) and (PhotoTable.isDeleted eq false)
                albumId?.let { baseCondition and (PhotoTable.albumId eq it) } ?: baseCondition
            }
            .orderBy(PhotoTable.takenAt, SortOrder.DESC)
            .map { row ->
                PhotoProjection(
                    id = row[PhotoTable.id].value,
                    url = row[PhotoTable.url],
                    longitude = row[longitudeExpr],
                    latitude = row[latitudeExpr],
                    takenAt = row[PhotoTable.takenAt],
                )
            }
    }

    override fun findPhotosInGridCell(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        page: Int,
        size: Int,
    ): PageResult<ClusterPhotoProjection> = transaction(database) {
        val envelope = makeEnvelope(west, south, east, north)
        val offset = PageResult.calculateOffset(page, size)

        val totalElements = PhotoTable
            .selectAll()
            .where { PhotoTable.location.intersects(envelope) and (PhotoTable.isDeleted eq false) }
            .count()

        val longitudeExpr = PostGisExtensions.extractX(PhotoTable.location)
        val latitudeExpr = PostGisExtensions.extractY(PhotoTable.location)

        val content = PhotoTable
            .select(PhotoTable.id, PhotoTable.url, PhotoTable.takenAt, longitudeExpr, latitudeExpr)
            .where { PhotoTable.location.intersects(envelope) and (PhotoTable.isDeleted eq false) }
            .orderBy(PhotoTable.takenAt, SortOrder.DESC)
            .limit(size).offset(offset)
            .map { row ->
                ClusterPhotoProjection(
                    id = row[PhotoTable.id].value,
                    url = row[PhotoTable.url],
                    longitude = row[longitudeExpr],
                    latitude = row[latitudeExpr],
                    takenAt = row[PhotoTable.takenAt],
                )
            }

        PageResult(
            content = content,
            page = page,
            size = size,
            totalElements = totalElements,
        )
    }
}
