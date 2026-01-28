package kr.co.lokit.api.domain.map.infrastructure

import kr.co.lokit.api.common.dto.PageResult
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class MapRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
) : MapRepository {
    override fun findClustersWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        gridSize: Double,
        albumId: Long?,
    ): List<ClusterProjection> {
        val albumFilter = if (albumId != null) "AND album_id = ?" else ""
        val sql =
            """
            WITH clustered AS (
                SELECT
                    floor(ST_X(location) / ?) AS cell_x,
                    floor(ST_Y(location) / ?) AS cell_y,
                    id,
                    url,
                    ST_X(location) as longitude,
                    ST_Y(location) as latitude,
                    created_at
                FROM photo
                WHERE location && ST_MakeEnvelope(?, ?, ?, ?, 4326)
                  AND is_deleted = false
                  $albumFilter
            ),
            ranked AS (
                SELECT
                    cell_x,
                    cell_y,
                    id,
                    url,
                    longitude,
                    latitude,
                    ROW_NUMBER() OVER (PARTITION BY cell_x, cell_y ORDER BY created_at DESC) as rn,
                    COUNT(*) OVER (PARTITION BY cell_x, cell_y) as cluster_count,
                    AVG(longitude) OVER (PARTITION BY cell_x, cell_y) as center_longitude,
                    AVG(latitude) OVER (PARTITION BY cell_x, cell_y) as center_latitude
                FROM clustered
            )
            SELECT
                cell_x,
                cell_y,
                cluster_count as count,
                url as thumbnail_url,
                center_longitude,
                center_latitude
            FROM ranked
            WHERE rn = 1
            ORDER BY cluster_count DESC
            """.trimIndent()

        val params = mutableListOf<Any>(gridSize, gridSize, west, south, east, north)
        if (albumId != null) params.add(albumId)

        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                ClusterProjection(
                    cellX = rs.getLong("cell_x"),
                    cellY = rs.getLong("cell_y"),
                    count = rs.getInt("count"),
                    thumbnailUrl = rs.getString("thumbnail_url"),
                    centerLongitude = rs.getDouble("center_longitude"),
                    centerLatitude = rs.getDouble("center_latitude"),
                )
            },
            *params.toTypedArray(),
        )
    }

    override fun findPhotosWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        albumId: Long?,
    ): List<PhotoProjection> {
        val albumFilter = if (albumId != null) "AND album_id = ?" else ""
        val sql =
            """
            SELECT id, url, ST_X(location) as longitude, ST_Y(location) as latitude, taken_at
            FROM photo
            WHERE location && ST_MakeEnvelope(?, ?, ?, ?, 4326)
              AND is_deleted = false
              $albumFilter
            ORDER BY taken_at DESC
            """.trimIndent()

        val params = mutableListOf<Any>(west, south, east, north)
        if (albumId != null) params.add(albumId)

        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                PhotoProjection(
                    id = rs.getLong("id"),
                    url = rs.getString("url"),
                    longitude = rs.getDouble("longitude"),
                    latitude = rs.getDouble("latitude"),
                    takenAt = rs.getTimestamp("taken_at").toLocalDateTime(),
                )
            },
            *params.toTypedArray(),
        )
    }

    /**
     * Finds photos within a grid cell with pagination.
     */
    override fun findPhotosInGridCell(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        page: Int,
        size: Int,
    ): PageResult<ClusterPhotoProjection> {
        val totalElements = countPhotosInGridCell(west, south, east, north)
        val offset = PageResult.calculateOffset(page, size)

        val sql =
            """
            SELECT id, url, ST_X(location) as longitude, ST_Y(location) as latitude, taken_at
            FROM photo
            WHERE location && ST_MakeEnvelope(?, ?, ?, ?, 4326)
              AND is_deleted = false
            ORDER BY taken_at DESC
            LIMIT ? OFFSET ?
            """.trimIndent()

        val content =
            jdbcTemplate.query(
                sql,
                { rs, _ ->
                    ClusterPhotoProjection(
                        id = rs.getLong("id"),
                        url = rs.getString("url"),
                        longitude = rs.getDouble("longitude"),
                        latitude = rs.getDouble("latitude"),
                        takenAt = rs.getTimestamp("taken_at").toLocalDateTime(),
                    )
                },
                west,
                south,
                east,
                north,
                size,
                offset,
            )

        return PageResult(
            content = content,
            page = page,
            size = size,
            totalElements = totalElements,
        )
    }

    private fun countPhotosInGridCell(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
    ): Long {
        val sql =
            """
            SELECT COUNT(*)
            FROM photo
            WHERE location && ST_MakeEnvelope(?, ?, ?, ?, 4326)
              AND is_deleted = false
            """.trimIndent()

        return jdbcTemplate.queryForObject(sql, Long::class.java, west, south, east, north) ?: 0L
    }
}
