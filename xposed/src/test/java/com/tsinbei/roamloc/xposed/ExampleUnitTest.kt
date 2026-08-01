package com.tsinbei.roamloc.xposed

import com.tsinbei.roamloc.xposed.utils.FakeLoc
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val originalJitter = FakeLoc.enableLocationJitter
    private val originalLatitude = FakeLoc.latitude
    private val originalLongitude = FakeLoc.longitude
    private val originalAltitude = FakeLoc.altitude
    private val originalSpeed = FakeLoc.speed
    private val originalSpeedAmplitude = FakeLoc.speedAmplitude

    @After
    fun restoreFakeLocationState() {
        FakeLoc.enableLocationJitter = originalJitter
        FakeLoc.latitude = originalLatitude
        FakeLoc.longitude = originalLongitude
        FakeLoc.altitude = originalAltitude
        FakeLoc.speed = originalSpeed
        FakeLoc.speedAmplitude = originalSpeedAmplitude
    }

    @Test
    fun disabledJitterUsesExactConfiguredValues() {
        FakeLoc.enableLocationJitter = false
        FakeLoc.latitude = 31.2304
        FakeLoc.longitude = 121.4737
        FakeLoc.altitude = 42.5
        FakeLoc.speed = 0.0
        FakeLoc.speedAmplitude = 10.0

        assertEquals(Pair(31.2304, 121.4737), FakeLoc.jitterLocation())
        assertEquals(42.5, FakeLoc.offset_altitude, 0.0)
        assertEquals(0.0f, FakeLoc.simulatedSpeed(), 0.0f)
    }

    @Test
    fun simulatedSpeedNeverBecomesNegative() {
        FakeLoc.enableLocationJitter = true
        FakeLoc.speed = -0.1
        FakeLoc.speedAmplitude = 10.0

        assertTrue(FakeLoc.simulatedSpeed() >= 0.0f)
    }
}
