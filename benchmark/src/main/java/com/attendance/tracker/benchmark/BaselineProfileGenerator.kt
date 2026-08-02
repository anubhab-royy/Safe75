package com.attendance.tracker.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a baseline profile for the release build of the app.
 *
 * Run on a connected device/emulator with:
 *
 *     ./gradlew :benchmark:connectedCheck
 *
 * The generated profile is written to
 * `benchmark/build/outputs/connected_android_test_additional_output/benchmark/...`,
 * and should be copied to `app/src/main/baseline-prof.txt` and shipped with the
 * release build so ART can pre-compile hot startup paths ahead of time.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = "com.attendance.tracker",
        profileBlock = {
            startActivityAndWait()
        }
    )
}
