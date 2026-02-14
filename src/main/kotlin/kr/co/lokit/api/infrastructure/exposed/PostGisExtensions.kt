package kr.co.lokit.api.infrastructure.exposed

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.and

object PostGisExtensions {
    fun makeEnvelope(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        srid: Int = 4326,
    ): Expression<String> =
        object : Expression<String>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                queryBuilder.append("ST_MakeEnvelope(")
                queryBuilder.append(west.toString())
                queryBuilder.append(", ")
                queryBuilder.append(south.toString())
                queryBuilder.append(", ")
                queryBuilder.append(east.toString())
                queryBuilder.append(", ")
                queryBuilder.append(north.toString())
                queryBuilder.append(", ")
                queryBuilder.append(srid.toString())
                queryBuilder.append(")")
            }
        }

    fun extractX(location: Column<String>): Expression<Double> =
        object : Expression<Double>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                queryBuilder.append("ST_X(")
                queryBuilder.append(location)
                queryBuilder.append(")")
            }
        }

    fun extractY(location: Column<String>): Expression<Double> =
        object : Expression<Double>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                queryBuilder.append("ST_Y(")
                queryBuilder.append(location)
                queryBuilder.append(")")
            }
        }

    fun intersects(
        location: Column<String>,
        envelope: Expression<String>,
    ): Expression<Boolean> =
        object : Expression<Boolean>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                queryBuilder.append(location)
                queryBuilder.append(" && ")
                envelope.toQueryBuilder(queryBuilder)
            }
        }

    fun stWithin(
        location: Column<String>,
        envelope: Expression<String>,
    ): Expression<Boolean> =
        object : Expression<Boolean>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                queryBuilder.append("ST_Within(")
                queryBuilder.append(location)
                queryBuilder.append(", ")
                envelope.toQueryBuilder(queryBuilder)
                queryBuilder.append(")")
            }
        }

    fun floor(expr: Expression<Double>): Expression<Long> =
        object : Expression<Long>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                queryBuilder.append("FLOOR(")
                expr.toQueryBuilder(queryBuilder)
                queryBuilder.append(")")
            }
        }

    fun divide(
        expr: Expression<Double>,
        divisor: Double,
    ): Expression<Double> =
        object : Expression<Double>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                queryBuilder.append("(")
                expr.toQueryBuilder(queryBuilder)
                queryBuilder.append(" / ")
                queryBuilder.append(divisor.toString())
                queryBuilder.append(")")
            }
        }

    fun gridCellX(
        location: Column<String>,
        gridSize: Double,
    ): Expression<Long> = floor(divide(extractX(location), gridSize))

    fun gridCellY(
        location: Column<String>,
        gridSize: Double,
    ): Expression<Long> = floor(divide(extractY(location), gridSize))
}

fun Column<String>.intersects(envelope: Expression<String>): Expression<Boolean> =
    PostGisExtensions.intersects(this, envelope)

fun Column<String>.within(envelope: Expression<String>): Expression<Boolean> =
    PostGisExtensions.stWithin(this, envelope)

infix fun Op<Boolean>.andOptional(other: Op<Boolean>?): Op<Boolean> = other?.let { this and it } ?: this
