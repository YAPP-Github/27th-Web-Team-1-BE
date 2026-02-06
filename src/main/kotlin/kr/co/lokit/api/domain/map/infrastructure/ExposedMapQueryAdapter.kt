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
        coupleId: Long?,
        albumId: Long?,
    ): List<ClusterProjection> = transaction(database) {
        val inverseGridSize = 1.0 / gridSize

        val margin = gridSize * 0.5
        val expandedWest = west - margin
        val expandedSouth = south - margin
        val expandedEast = east + margin
        val expandedNorth = north + margin

        val photos = queryClusterPhotos(
            west = expandedWest,
            south = expandedSouth,
            east = expandedEast,
            north = expandedNorth,
            inverseGridSize = inverseGridSize,
            coupleId = coupleId,
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
        coupleId: Long?,
        albumId: Long?,
    ): List<UniquePhotoRecord> {
        val sql = buildClusterQuery(coupleId, albumId)
        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(sql, true)

        var i = 1
        stmt.set(i++, inverseGridSize)
        stmt.set(i++, inverseGridSize)
        stmt.set(i++, west)
        stmt.set(i++, south)
        stmt.set(i++, east)
        stmt.set(i++, north)
        if (coupleId != null) stmt.set(i++, coupleId)
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
                        count = rs.getInt("photo_count"),
                    )
                )
            }
        }
        return results
    }

    private fun buildClusterQuery(coupleId: Long?, albumId: Long?): String = buildString {
        append(
            """
        WITH photo_cells AS (
            SELECT
                p.id,
                p.url,
                ST_X(p.location) AS longitude,
                ST_Y(p.location) AS latitude,
                FLOOR(ST_X(p.location) * ?) AS cell_x,
                FLOOR(ST_Y(p.location) * ?) AS cell_y,
                p.taken_at
            FROM ${PhotoTable.tableName} p
            WHERE p.location && ST_MakeEnvelope(?, ?, ?, ?, 4326)
                AND p.is_deleted = false
        """.trimIndent()
        )

        if (coupleId != null) {
            append(" AND p.couple_id = ? ")
        }

        if (albumId != null) {
            append(" AND p.album_id = ? ")
        }

        append(
            """

        ),
        ranked AS (
            SELECT *,
                COUNT(*) OVER (PARTITION BY cell_x, cell_y) AS photo_count,
                ROW_NUMBER() OVER (PARTITION BY cell_x, cell_y ORDER BY taken_at DESC) AS rn
            FROM photo_cells
        )
        SELECT id, url, longitude, latitude, cell_x, cell_y, taken_at, photo_count
        FROM ranked
        WHERE rn = 1
        """.trimIndent()
        )
    }


    override fun findPhotosWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        coupleId: Long?,
        albumId: Long?,
    ): List<PhotoProjection> = transaction(database) {
        val sql = buildPhotosQuery(coupleId, albumId)
        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(sql, true)

        var i = 1
        stmt.set(i++, west)
        stmt.set(i++, south)
        stmt.set(i++, east)
        stmt.set(i++, north)
        if (coupleId != null) stmt.set(i++, coupleId)
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

    private fun buildPhotosQuery(coupleId: Long?, albumId: Long?): String = buildString {
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

        if (coupleId != null) {
            append(" AND p.couple_id = ? ")
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
        coupleId: Long?,
    ): List<ClusterPhotoProjection> = transaction(database) {
        val sql = buildGridCellQuery(coupleId)
        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(sql, true)

        var i = 1
        stmt.set(i++, west)
        stmt.set(i++, south)
        stmt.set(i++, east)
        stmt.set(i++, north)
        if (coupleId != null) stmt.set(i++, coupleId)

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

    private fun buildGridCellQuery(coupleId: Long?): String = buildString {
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

        if (coupleId != null) {
            append(" AND p.couple_id = ? ")
        }

        append(" ORDER BY p.taken_at DESC ")
    }
}
