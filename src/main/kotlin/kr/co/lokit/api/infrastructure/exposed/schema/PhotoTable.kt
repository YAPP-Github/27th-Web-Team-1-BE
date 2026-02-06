package kr.co.lokit.api.infrastructure.exposed.schema

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object PhotoTable : LongIdTable("photo") {
    val url: Column<String> = varchar("url", 2100).uniqueIndex()
    val takenAt: Column<LocalDateTime> = datetime("taken_at")
    val albumId: Column<Long> = long("album_id").index()
    val uploadedById: Column<Long> = long("uploaded_by").index()
    val coupleId: Column<Long?> = long("couple_id").nullable()
    val location: Column<String> = text("location")
    val description: Column<String?> = varchar("description", 1000).nullable()
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
    val version: Column<Long> = long("version")
    val isDeleted: Column<Boolean> = bool("is_deleted").default(false)
}
