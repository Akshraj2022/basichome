package com.minimal.tvlauncher.util

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

/**
 * Calculates local sunrise and sunset purely on-device without internet permissions.
 * Uses astronomical solar declination and equation of time based on device timezone and day of year.
 */
object SunTimeHelper {

    data class SunTimes(val sunriseHour: Double, val sunsetHour: Double)

    /**
     * Approximates local sunrise and sunset hours in decimal local time (e.g., 6.25 = 6:15 AM).
     */
    fun calculateSunTimes(): SunTimes {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        // Approximate longitude from device timezone (15 degrees per timezone hour)
        val tz = TimeZone.getDefault()
        val tzOffsetHours = tz.getOffset(System.currentTimeMillis()) / 3600000.0
        val longitude = tzOffsetHours * 15.0
        val latitude = 23.5 // Moderate tropical/subtropical default latitude

        // Fractional year in radians
        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1)

        // Equation of time in minutes
        val eqtime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma)
                - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))

        // Solar declination angle in radians
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
                0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
                0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

        val zenith = Math.toRadians(90.833) // Official civil zenith
        val latRad = Math.toRadians(latitude)

        val cosHourAngle = (cos(zenith) - sin(latRad) * sin(decl)) / (cos(latRad) * cos(decl))
        val clampedCos = cosHourAngle.coerceIn(-1.0, 1.0)
        val hourAngleDeg = Math.toDegrees(acos(clampedCos))

        // Solar noon in minutes from midnight UTC
        val solarNoonUtcMinutes = 720.0 - 4.0 * longitude - eqtime

        val sunriseUtcMinutes = solarNoonUtcMinutes - hourAngleDeg * 4.0
        val sunsetUtcMinutes = solarNoonUtcMinutes + hourAngleDeg * 4.0

        val sunriseLocalHour = (sunriseUtcMinutes / 60.0 + tzOffsetHours + 24.0) % 24.0
        val sunsetLocalHour = (sunsetUtcMinutes / 60.0 + tzOffsetHours + 24.0) % 24.0

        return SunTimes(
            sunriseHour = sunriseLocalHour.coerceIn(5.5, 7.0),
            sunsetHour = sunsetLocalHour.coerceIn(17.5, 19.5)
        )
    }

    /**
     * Determines whether it is currently daytime (between sunrise and sunset).
     */
    fun isDaytime(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0
        val times = calculateSunTimes()
        return currentHour >= times.sunriseHour && currentHour < times.sunsetHour
    }
}
