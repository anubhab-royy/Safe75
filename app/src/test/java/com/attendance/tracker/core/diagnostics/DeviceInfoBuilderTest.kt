package com.attendance.tracker.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [DeviceInfoBuilder] null/blank normalisation.
 */
class DeviceInfoBuilderTest {

    private val builder = DeviceInfoBuilder()

    @Test
    fun testBuild_populatesAllFields() {
        val info = builder.build(
            appVersion = "1.0.0",
            versionCode = 7,
            buildType = "release",
            androidVersion = "15",
            sdkInt = 35,
            manufacturer = "Google",
            model = "Pixel",
            brand = "google",
            abi = "arm64-v8a",
            locale = "en-US",
            densityDpi = 420,
            memoryClassMb = 256
        )

        assertEquals("1.0.0", info.appVersion)
        assertEquals(7, info.versionCode)
        assertEquals("release", info.buildType)
        assertEquals("15", info.androidVersion)
        assertEquals(35, info.sdkInt)
        assertEquals("Pixel", info.model)
        assertEquals("arm64-v8a", info.abi)
        assertEquals("en-US", info.locale)
        assertEquals(420, info.densityDpi)
        assertEquals(256, info.memoryClassMb)
    }

    @Test
    fun testUnknown_usesUnknownDefaults() {
        val info = builder.unknown()

        assertEquals(DeviceInfoBuilder.UNKNOWN, info.appVersion)
        assertEquals(DeviceInfoBuilder.UNKNOWN, info.model)
        assertEquals(DeviceInfoBuilder.UNKNOWN, info.abi)
        assertEquals(0, info.sdkInt)
        assertEquals(0, info.memoryClassMb)
    }

    @Test
    fun testBuild_blankStringsUseUnknown() {
        val info = builder.build(
            appVersion = "  ",
            versionCode = 0,
            buildType = "",
            androidVersion = null,
            sdkInt = 0,
            manufacturer = null,
            model = "Model",
            brand = null,
            abi = null,
            locale = null,
            densityDpi = 0,
            memoryClassMb = 0
        )

        assertEquals(DeviceInfoBuilder.UNKNOWN, info.appVersion)
        assertEquals(DeviceInfoBuilder.UNKNOWN, info.buildType)
        assertEquals("Model", info.model)
    }
}
