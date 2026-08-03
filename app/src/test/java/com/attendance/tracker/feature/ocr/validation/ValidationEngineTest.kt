package com.attendance.tracker.feature.ocr.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidationEngineTest {

    private val engine = ValidationEngine()

    @Test
    fun testCleanDay_correctsCommonMispellings() {
        assertEquals("Monday", engine.cleanDay("Mondav"))
        assertEquals("Tuesday", engine.cleanDay("Tuesdav"))
        assertEquals("Wednesday", engine.cleanDay("wednesdav"))
        assertEquals("Friday", engine.cleanDay("Fri"))
        assertEquals("Thursday", engine.cleanDay("thursdav"))
    }

    @Test
    fun testCleanSubjectText_fixesUnmatchedBrackets() {
        assertEquals("OOP using JAVA (ANM)", engine.cleanSubjectText("OOP using JAVA ANM)"))
        assertEquals("AI Lab (SB)", engine.cleanSubjectText("AI Lab SB)"))
        assertEquals("Computer Networks (ABG)", engine.cleanSubjectText("Computer Networks (ABG"))
    }

    @Test
    fun testCleanSubjectText_normalizesSubjectSpelling() {
        assertEquals("Computer Networks (ABG)", engine.cleanSubjectText("computor networks (ABG)"))
        assertEquals("OOP using JAVA (ANM)", engine.cleanSubjectText("oop using javaa (ANM)"))
    }
}
