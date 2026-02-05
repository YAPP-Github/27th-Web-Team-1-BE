package kr.co.lokit.api.domain.map.infrastructure

import kr.co.lokit.api.common.dto.PageResult
import kr.co.lokit.api.domain.map.application.port.ClusterPhotoProjection
import kr.co.lokit.api.domain.map.application.port.ClusterProjection
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.application.port.PhotoProjection
import kr.co.lokit.api.domain.map.application.port.UniquePhotoRecord
import kr.co.lokit.api.infrastructure.exposed.schema.PhotoTable
import kr.co.lokit.api.infrastructure.exposed.toClusterProjections
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedMapQueryAdapter(
    private val database: Database,
) : MapQueryPort {

    override fun findClustersWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        gridSize: Double,
        userId: Long?,
        albumId: Long?,
    ): List<ClusterProjection> = transaction(database) {
        val uniquePhotos = queryUniquePhotosWithDistinctOn(west, south, east, north, gridSize, userId, albumId)
        uniquePhotos.toClusterProjections()
    }

    private fun queryUniquePhotosWithDistinctOn(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        gridSize: Double,
        userId: Long?,
        albumId: Long?,
    ): List<UniquePhotoRecord> {
        val sql = buildDistinctOnQuery(userId, albumId)

        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(sql, false)

        var paramIndex = 1
        stmt.set(paramIndex++, gridSize)
        stmt.set(paramIndex++, gridSize)
        stmt.set(paramIndex++, west)
        stmt.set(paramIndex++, south)
        stmt.set(paramIndex++, east)
        stmt.set(paramIndex++, north)
        if (userId != null) stmt.set(paramIndex++, userId)
        if (albumId != null) stmt.set(paramIndex++, albumId)

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
                        takenAt = rs.getTimestamp("taken_at").toLocalDateTime(),
                    )
                )
            }
        }
        return results
    }

    private fun buildDistinctOnQuery(userId: Long?, albumId: Long?): String = buildString {
        append("SELECT id, url, longitude, latitude, cell_x, cell_y, taken_at FROM ( ")
        append("  SELECT p.id, p.url, p.location, p.taken_at, ")
        append("         ST_X(p.location) as longitude, ST_Y(p.location) as latitude, ")
        append("         FLOOR(ST_X(p.location) / ?) as cell_x, ")
        append("         FLOOR(ST_Y(p.location) / ?) as cell_y, ")
        append("         ROW_NUMBER() OVER (PARTITION BY p.url ORDER BY p.taken_at DESC) as rn ")
        append("  FROM ${PhotoTable.tableName} p ")
        if (userId != null) {
            append("  JOIN album a ON p.album_id = a.id ")
            append("  JOIN couple_user cu ON a.couple_id = cu.couple_id ")
        }
        append("  WHERE p.location && ST_MakeEnvelope(?, ?, ?, ?, 4326) ")
        append("    AND p.is_deleted = false ")
        if (userId != null) append("    AND cu.user_id = ? ")
        if (albumId != null) append("    AND p.album_id = ? ")
        append(") ranked ")
        append("WHERE rn = 1 ")
        append("ORDER BY taken_at DESC")
    }

    override fun findPhotosWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        userId: Long?,
        albumId: Long?,
    ): List<PhotoProjection> = transaction(database) {
        val sql = buildPhotosQuery(userId, albumId)
        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(sql, false)

        var paramIndex = 1
        stmt.set(paramIndex++, west)
        stmt.set(paramIndex++, south)
        stmt.set(paramIndex++, east)
        stmt.set(paramIndex++, north)
        if (userId != null) stmt.set(paramIndex++, userId)
        if (albumId != null) stmt.set(paramIndex++, albumId)

        val results = mutableListOf<PhotoProjection>()
        stmt.executeQuery().use { rs ->
            while (rs.next()) {
                results.add(
                    PhotoProjection(
                        id = rs.getLong("id"),
                        url = rs.getString("url"),
                        longitude = rs.getDouble("longitude"),
                        latitude = rs.getDouble("latitude"),
                        takenAt = rs.getTimestamp("taken_at").toLocalDateTime(),
                    )
                )
            }
        }
        return@transaction results
    }

    private fun buildPhotosQuery(userId: Long?, albumId: Long?): String = buildString {
        append("SELECT p.id, p.url, p.taken_at, ")
        append("       ST_X(p.location) as longitude, ST_Y(p.location) as latitude ")
        append("FROM ${PhotoTable.tableName} p ")
        if (userId != null) {
            append("JOIN album a ON p.album_id = a.id ")
            append("JOIN couple_user cu ON a.couple_id = cu.couple_id ")
        }
        append("WHERE p.location && ST_MakeEnvelope(?, ?, ?, ?, 4326) ")
        append("  AND p.is_deleted = false ")
        if (userId != null) append("  AND cu.user_id = ? ")
        if (albumId != null) append("  AND p.album_id = ? ")
        append("ORDER BY p.taken_at DESC")
    }

    override fun findPhotosInGridCell(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        userId: Long?,
        page: Int,
        size: Int,
    ): PageResult<ClusterPhotoProjection> = transaction(database) {
        val sql = buildGridCellQuery(userId)
        val countSql = buildGridCellCountQuery(userId)
        val offset = PageResult.calculateOffset(page, size)
        val conn = TransactionManager.current().connection

        // Count query
        val countStmt = conn.prepareStatement(countSql, false)
        var paramIndex = 1
        countStmt.set(paramIndex++, west)
        countStmt.set(paramIndex++, south)
        countStmt.set(paramIndex++, east)
        countStmt.set(paramIndex++, north)
        if (userId != null) countStmt.set(paramIndex++, userId)

        val totalElements = countStmt.executeQuery().use { rs ->
            if (rs.next()) rs.getInt(1) else 0
        }

        // Content query
        val stmt = conn.prepareStatement(sql, false)
        paramIndex = 1
        stmt.set(paramIndex++, west)
        stmt.set(paramIndex++, south)
        stmt.set(paramIndex++, east)
        stmt.set(paramIndex++, north)
        if (userId != null) stmt.set(paramIndex++, userId)
        stmt.set(paramIndex++, size)
        stmt.set(paramIndex, offset)

        val content = mutableListOf<ClusterPhotoProjection>()
        stmt.executeQuery().use { rs ->
            while (rs.next()) {
                content.add(
                    ClusterPhotoProjection(
                        id = rs.getLong("id"),
                        url = rs.getString("url"),
                        longitude = rs.getDouble("longitude"),
                        latitude = rs.getDouble("latitude"),
                        takenAt = rs.getTimestamp("taken_at").toLocalDateTime(),
                    )
                )
            }
        }

        PageResult(
            content = content,
            page = page,
            size = size,
            totalElements = totalElements.toLong(),
        )
    }

    private fun buildGridCellQuery(userId: Long?): String = buildString {
        append("SELECT p.id, p.url, p.taken_at, ")
        append("       ST_X(p.location) as longitude, ST_Y(p.location) as latitude ")
        append("FROM ${PhotoTable.tableName} p ")
        if (userId != null) {
            append("JOIN album a ON p.album_id = a.id ")
            append("JOIN couple_user cu ON a.couple_id = cu.couple_id ")
        }
        append("WHERE p.location && ST_MakeEnvelope(?, ?, ?, ?, 4326) ")
        append("  AND p.is_deleted = false ")
        if (userId != null) append("  AND cu.user_id = ? ")
        append("ORDER BY p.taken_at DESC ")
        append("LIMIT ? OFFSET ?")
    }

    private fun buildGridCellCountQuery(userId: Long?): String = buildString {
        append("SELECT COUNT(*) ")
        append("FROM ${PhotoTable.tableName} p ")
        if (userId != null) {
            append("JOIN album a ON p.album_id = a.id ")
            append("JOIN couple_user cu ON a.couple_id = cu.couple_id ")
        }
        append("WHERE p.location && ST_MakeEnvelope(?, ?, ?, ?, 4326) ")
        append("  AND p.is_deleted = false ")
        if (userId != null) append("  AND cu.user_id = ? ")
    }
}
