package kr.co.lokit.api.domain.map.infrastructure

import kr.co.lokit.api.common.concurrency.withPermit
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
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.Semaphore

class ExposedMapQueryAdapter(
    private val database: Database,
) : MapQueryPort {
    private val dbSemaphore = Semaphore(4)

    override fun findClustersWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        gridSize: Double,
        coupleId: Long?,
        albumId: Long?,
    ): List<ClusterProjection> =
        dbSemaphore.withPermit {
            transaction(database) {
                val margin = gridSize * 0.5
                executeQuery(MapSqlTemplates.clusterQuery(coupleId, albumId), {
                    setDouble(1, west - margin)
                    setDouble(2, south - margin)
                    setDouble(3, east + margin)
                    setDouble(4, north + margin)
                    var idx = 5
                    coupleId?.let { setLong(idx++, it) }
                    albumId?.let { setLong(idx++, it) }
                    for (i in idx..idx + 5) setDouble(i, gridSize)
                }) { it.toUniquePhotoRecord() }.toClusterProjections()
            }
        }

    override fun findPhotosWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        coupleId: Long?,
        albumId: Long?,
    ): List<PhotoProjection> =
        dbSemaphore.withPermit {
            transaction(database) {
                executeQuery(MapSqlTemplates.photosQuery(coupleId, albumId), {
                    setDouble(1, west)
                    setDouble(2, south)
                    setDouble(3, east)
                    setDouble(4, north)
                    bindCommonParams(5, coupleId, albumId)
                }) { it.toPhotoProjection() }
            }
        }

    override fun findPhotosInGridCell(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        coupleId: Long?,
    ): List<ClusterPhotoProjection> =
        dbSemaphore.withPermit {
            transaction(database) {
                executeQuery(MapSqlTemplates.gridCellQuery(coupleId), {
                    setDouble(1, west)
                    setDouble(2, south)
                    setDouble(3, east)
                    setDouble(4, north)
                    coupleId?.let { setLong(5, it) }
                }) { it.toClusterPhotoProjection() }
            }
        }

    private fun <T> executeQuery(
        sql: String,
        setup: PreparedStatement.() -> Unit,
        mapper: (ResultSet) -> T,
    ): List<T> {
        val conn = TransactionManager.current().connection.connection as Connection

        return conn.prepareStatement(sql).use { stmt ->
            stmt.setup()
            stmt.executeQuery().use { rs ->
                val results = mutableListOf<T>()
                while (rs.next()) {
                    results.add(mapper(rs))
                }
                results
            }
        }
    }

    private fun PreparedStatement.bindCommonParams(
        startIndex: Int,
        coupleId: Long?,
        albumId: Long?,
    ) {
        var idx = startIndex
        coupleId?.let { setLong(idx++, it) }
        albumId?.let { setLong(idx++, it) }
    }

    private fun ResultSet.toUniquePhotoRecord() =
        UniquePhotoRecord(
            id = getLong("id"),
            url = getString("url"),
            longitude = getDouble("longitude"),
            latitude = getDouble("latitude"),
            cellX = getLong("cell_x"),
            cellY = getLong("cell_y"),
            takenAt = getTimestamp("taken_at").toLocalDateTime(),
            count = getInt("photo_count"),
        )

    private fun ResultSet.toPhotoProjection() =
        PhotoProjection(
            id = getLong("id"),
            url = getString("url"),
            longitude = getDouble("longitude"),
            latitude = getDouble("latitude"),
            takenAt = getTimestamp("taken_at").toLocalDateTime(),
        )

    private fun ResultSet.toClusterPhotoProjection() =
        ClusterPhotoProjection(
            id = getLong("id"),
            url = getString("url"),
            longitude = getDouble("longitude"),
            latitude = getDouble("latitude"),
            takenAt = getTimestamp("taken_at").toLocalDateTime(),
            address = getString("address"),
        )
}

private object MapSqlTemplates {
    fun clusterQuery(
        coupleId: Long?,
        albumId: Long?,
    ) = """
        WITH projected_photos AS (
            SELECT p.id, p.url, p.taken_at, ST_X(p.location) AS longitude, ST_Y(p.location) AS latitude,
                    ST_Transform(p.location, 3857) as geom_3857
            FROM ${PhotoTable.tableName} p
            WHERE p.location && ST_Transform(ST_MakeEnvelope(?, ?, ?, ?, 3857), 4326) AND p.is_deleted = false
            ${if (coupleId != null) "AND p.couple_id = ?" else ""}
            ${if (albumId != null) "AND p.album_id = ?" else ""}
        ),
        ranked AS (
            SELECT *,
                FLOOR(ST_X(geom_3857) / ?) AS cell_x, FLOOR(ST_Y(geom_3857) / ?) AS cell_y,
                COUNT(*) OVER (PARTITION BY FLOOR(ST_X(geom_3857) / ?), FLOOR(ST_Y(geom_3857) / ?)) AS photo_count,
                ROW_NUMBER() OVER (PARTITION BY FLOOR(ST_X(geom_3857) / ?), FLOOR(ST_Y(geom_3857) / ?) ORDER BY taken_at DESC) AS rn
            FROM projected_photos
        )
        SELECT * FROM ranked WHERE rn = 1
        """.trimIndent()

    fun photosQuery(
        coupleId: Long?,
        albumId: Long?,
    ) = """
        SELECT p.id, p.url, p.taken_at, p.address, ST_X(p.location) AS longitude, ST_Y(p.location) AS latitude
        FROM ${PhotoTable.tableName} p
        WHERE p.location && ST_MakeEnvelope(?, ?, ?, ?, 4326) AND p.is_deleted = false
        ${if (coupleId != null) "AND p.couple_id = ?" else ""}
        ${if (albumId != null) "AND p.album_id = ?" else ""}
        ORDER BY p.taken_at DESC
        """.trimIndent()

    fun gridCellQuery(coupleId: Long?) =
        """
        SELECT p.id, p.url, p.taken_at, p.address, ST_X(p.location) AS longitude, ST_Y(p.location) AS latitude
        FROM ${PhotoTable.tableName} p
        WHERE p.location && ST_MakeEnvelope(?, ?, ?, ?, 4326) AND p.is_deleted = false
        ${if (coupleId != null) "AND p.couple_id = ?" else ""}
        ORDER BY p.taken_at DESC
        """.trimIndent()
}
