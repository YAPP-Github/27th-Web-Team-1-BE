package kr.co.lokit.api.domain.map.domain

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

object MercatorProjection {
    const val EARTH_RADIUS_METERS = 6378137.0

    fun longitudeToMeters(longitude: Double): Double = longitude * (PI * EARTH_RADIUS_METERS / 180.0)

    fun latitudeToMeters(latitude: Double): Double = ln(tan((90.0 + latitude) * PI / 360.0)) * EARTH_RADIUS_METERS

    fun metersToLongitude(meters: Double): Double = meters / (PI * EARTH_RADIUS_METERS / 180.0)

    fun metersToLatitude(meters: Double): Double = atan(exp(meters / EARTH_RADIUS_METERS)) * 360.0 / PI - 90.0
}
