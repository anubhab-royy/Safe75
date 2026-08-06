package com.attendance.tracker.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [StackFormatter] frame and length capping.
 */
class StackFormatterTest {

    @Test
    fun testFrames_includesClassMessageAndFrames() {
        val exception = IllegalStateException("bad state")

        val frames = StackFormatter.frames(exception)

        assertTrue(frames.first().contains("IllegalStateException"))
        assertTrue(frames.first().contains("bad state"))
        assertTrue(frames.any { it.startsWith("\tat ") })
    }

    @Test
    fun testFrames_respectsMaxFrames() {
        val exception = Exception("deep")
        exception.stackTrace = Array(100) { StackTraceElement("com.example.C", "m$it", "C.kt", it) }

        val frames = StackFormatter.frames(exception, maxFrames = 10)

        assertEquals(11, frames.size)
    }

    @Test
    fun testAsString_capsLength() {
        val exception = Exception("x")
        exception.stackTrace = Array(200) { StackTraceElement("com.example.D", "m", "D.kt", 1) }

        val text = StackFormatter.asString(exception, maxChars = 500)

        assertEquals(500, text.length)
    }

    @Test
    fun testAsString_shortTextUntouched() {
        val text = StackFormatter.asString(IllegalArgumentException("short"), maxChars = 500)

        assertTrue(text.contains("short"))
        assertTrue(text.length <= 500)
    }
}
