package kr.co.lokit.api.domain.map.infrastructure

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
        val inverseGridSize = 1.0 / gridSize

        val photos = queryClusterPhotos(
            west = west,
            south = south,
            east = east,
            north = north,
            inverseGridSize = inverseGridSize,
            userId = userId,
            albumId = albumId,
        )

        photos.toClusterProjections()
    }

    private fun queryClusterPhotos(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        inverseGridSize: Double,
        userId: Long?,
        albumId: Long?,
    ): List<UniquePhotoRecord> {
        val sql = buildClusterQuery(userId, albumId)
        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(sql, false)

        var i = 1
        stmt.set(i++, inverseGridSize)
        stmt.set(i++, inverseGridSize)
        stmt.set(i++, west)
        stmt.set(i++, south)
        stmt.set(i++, east)
        stmt.set(i++, north)
        if (userId != null) stmt.set(i++, userId)
        if (albumId != null) stmt.set(i++, albumId)

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

    private fun buildClusterQuery(userId: Long?, albumId: Long?): String = buildString {
        append(
            """
            SELECT
                MAX(p.id) AS id,
                p.url,
                ST_X(p.location) AS longitude,
                ST_Y(p.location) AS latitude,
                FLOOR(ST_X(p.location) * ?) AS cell_x,
                FLOOR(ST_Y(p.location) * ?) AS cell_y,
                MAX(p.taken_at) AS taken_at
            FROM ${PhotoTable.tableName} p
            WHERE
                p.location && ST_MakeEnvelope(?, ?, ?, ?, 4326)
                AND p.is_deleted = false
            """.trimIndent()
        )

        if (userId != null) {
            append(
                """
                AND EXISTS (
                    SELECT 1
                    FROM album a
                    JOIN couple_user cu ON a.couple_id = cu.couple_id
                    WHERE a.id = p.album_id
                        AND cu.user_id = ?
                )
                """.trimIndent()
            )
        }

        if (albumId != null) {
            append(" AND p.album_id = ? ")
        }

        append(
            """
            GROUP BY
                cell_x, cell_y, p.url, p.location
            """.trimIndent()
        )
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

        var i = 1
        stmt.set(i++, west)
        stmt.set(i++, south)
        stmt.set(i++, east)
        stmt.set(i++, north)
        if (userId != null) stmt.set(i++, userId)
        if (albumId != null) stmt.set(i++, albumId)

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
        results
    }

    private fun buildPhotosQuery(userId: Long?, albumId: Long?): String = buildString {
        append(
            """
            SELECT
                p.id,
                p.url,
                p.taken_at,
                p.address,
                ST_X(p.location) AS longitude,
                ST_Y(p.location) AS latitude
            FROM ${PhotoTable.tableName} p
            WHERE
                p.location && ST_MakeEnvelope(?, ?, ?, ?, 4326)
                AND p.is_deleted = false
            """.trimIndent()
        )

        if (userId != null) {
            append(
                """
                AND EXISTS (
                    SELECT 1
                    FROM album a
                    JOIN couple_user cu ON a.couple_id = cu.couple_id
                    WHERE a.id = p.album_id
                        AND cu.user_id = ?
                )
                """.trimIndent()
            )
        }

        if (albumId != null) {
            append(" AND p.album_id = ? ")
        }

        append(" ORDER BY p.taken_at DESC ")
    }

    override fun findPhotosInGridCell(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        userId: Long?,
    ): List<ClusterPhotoProjection> = transaction(database) {
        val sql = buildGridCellQuery(userId)
        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(sql, false)

        var i = 1
        stmt.set(i++, west)
        stmt.set(i++, south)
        stmt.set(i++, east)
        stmt.set(i++, north)
        if (userId != null) stmt.set(i++, userId)

        val results = mutableListOf<ClusterPhotoProjection>()
        stmt.executeQuery().use { rs ->
            while (rs.next()) {
                results.add(
                    ClusterPhotoProjection(
                        id = rs.getLong("id"),
                        url = rs.getString("url"),
                        longitude = rs.getDouble("longitude"),
                        latitude = rs.getDouble("latitude"),
                        takenAt = rs.getTimestamp("taken_at").toLocalDateTime(),
                        address = rs.getString("address"),
                    )
                )
            }
        }
        results
    }

    private fun buildGridCellQuery(userId: Long?): String = buildString {
        append(
            """
            SELECT
                p.id,
                p.url,
                p.taken_at,
                p.address,
                ST_X(p.location) AS longitude,
                ST_Y(p.location) AS latitude
            FROM ${PhotoTable.tableName} p
            WHERE
                p.location && ST_MakeEnvelope(?, ?, ?, ?, 4326)
                AND p.is_deleted = false
            """.trimIndent()
        )

        if (userId != null) {
            append(
                """
                AND EXISTS (
                    SELECT 1
                    FROM album a
                    JOIN couple_user cu ON a.couple_id = cu.couple_id
                    WHERE a.id = p.album_id
                        AND cu.user_id = ?
                )
                """.trimIndent()
            )
        }

        append(" ORDER BY p.taken_at DESC ")
    }
}
