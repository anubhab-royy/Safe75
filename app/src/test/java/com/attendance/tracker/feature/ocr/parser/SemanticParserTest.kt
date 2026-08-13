package com.attendance.tracker.feature.ocr.parser

import org.opencv.core.Rect
import com.attendance.tracker.feature.ocr.recognition.RecognizedCell
import com.attendance.tracker.feature.ocr.validation.ValidationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticParserTest {

    init {
        com.attendance.tracker.core.logger.Logger.setEngine(com.attendance.tracker.core.logger.NoOpLogEngine())
    }

    private val validationEngine = ValidationEngine()
    private val headerInterpreter = HeaderInterpreter()
    private val parser = SemanticParser(
        validationEngine,
        headerInterpreter,
        kotlinx.serialization.json.Json { prettyPrint = true }
    )

    @Test
    fun testParseTimetable_extractsMergedAndIndividualClassesCorrectly() {
        // Construct mock recognized cells matching the supplied sample timetable routine grid
        val mockCells = mutableListOf<RecognizedCell>()

        // 1. Row 0: Time slot headers
        mockCells.add(createCell(0, 0, "DAY"))
        mockCells.add(createCell(0, 1, "12.00pm - 12.50pm"))
        mockCells.add(createCell(0, 2, "12.50pm - 1:40pm"))
        mockCells.add(createCell(0, 3, "1:40pm - 2:30pm"))
        mockCells.add(createCell(0, 4, "2:30pm - 3:00pm")) // Recess Slot
        mockCells.add(createCell(0, 5, "3:00pm - 3:50pm"))
        mockCells.add(createCell(0, 6, "3:50pm - 4:40pm"))
        mockCells.add(createCell(0, 7, "4:40pm - 5:30pm"))
        mockCells.add(createCell(0, 8, "5:30pm - 6:00pm")) // Library Slot

        // 2. Col 0: Weekdays
        mockCells.add(createCell(1, 0, "Monday"))
        mockCells.add(createCell(2, 0, "Tuesday"))
        mockCells.add(createCell(3, 0, "Wednesday"))
        mockCells.add(createCell(4, 0, "Thursday"))
        mockCells.add(createCell(5, 0, "Friday"))

        // 3. Monday Entries
        // AI Lab spans 12.00pm - 2:30pm (cols 1, 2, 3)
        mockCells.add(createCell(1, 1, "AI Lab (SB)", colSpan = 3))
        mockCells.add(createCell(1, 4, "RECESS"))
        mockCells.add(createCell(1, 5, "Theory of Computation (AS)"))
        mockCells.add(createCell(1, 7, "Aptitude and Reasoning Ability (RK)"))
        mockCells.add(createCell(1, 8, "LIBRARY HOURS"))

        // 4. Tuesday Entries
        mockCells.add(createCell(2, 1, "OOP using JAVA (ANM)"))
        mockCells.add(createCell(2, 2, "Software Engineering (TB)"))
        mockCells.add(createCell(2, 4, "RECESS"))
        mockCells.add(createCell(2, 5, "Computer Networks (ABG)"))
        mockCells.add(createCell(2, 6, "Introduction to AI (SB)", colSpan = 2)) // Spans cols 6, 7
        mockCells.add(createCell(2, 8, "LIBRARY HOURS"))

        // Execute parsing
        val result = parser.parse(mockCells)

        // Assertions
        assertNotNull(result)
        val subjects = result.subjects
        
        // 5. Verify the number of parsed legitimate classes
        // Monday: AI Lab, Theory of Computation, Aptitude & Reasoning = 3
        // Tuesday: OOP using Java, Software Engineering, Computer Networks, Introduction to AI = 4
        // (excluding RECESS and LIBRARY HOURS)
        val mondayClasses = subjects.filter { it.day == "Monday" }
        assertEquals(3, mondayClasses.size)

        // Verify Monday AI Lab merged span
        val aiLab = mondayClasses.find { it.subject.contains("AI Lab") }
        assertNotNull(aiLab)
        assertEquals("12:00", aiLab?.start)
        assertEquals("14:30", aiLab?.end)
        assertEquals("SB", aiLab?.faculty)
        assertTrue(aiLab?.lab == true)

        // Verify Tuesday OOP using Java
        val oop = subjects.find { it.day == "Tuesday" && it.subject.contains("OOP") }
        assertNotNull(oop)
        assertEquals("12:00", oop?.start)
        assertEquals("12:50", oop?.end)
        assertEquals("ANM", oop?.faculty)
        assertTrue(oop?.lab == false)

        // Verify Tuesday merged Introduction to AI
        val introAi = subjects.find { it.day == "Tuesday" && it.subject.contains("Introduction") }
        assertNotNull(introAi)
        assertEquals("15:50", introAi?.start) // col 6 start
        assertEquals("17:30", introAi?.end)   // col 7 end
        assertEquals("SB", introAi?.faculty)

        // Verify correct JSON serialization output
        val jsonStr = parser.toJson(result)
        assertTrue(jsonStr.contains("AI Lab"))
        assertTrue(jsonStr.contains("\"day\": \"Monday\""))
    }

    private fun createCell(
        row: Int,
        col: Int,
        text: String,
        rowSpan: Int = 1,
        colSpan: Int = 1
    ): RecognizedCell {
        return RecognizedCell(
            id = java.util.UUID.randomUUID().toString(),
            startRow = row,
            startCol = col,
            rowSpan = rowSpan,
            colSpan = colSpan,
            rect = Rect(col * 100, row * 50, colSpan * 100, rowSpan * 50),
            text = text,
            confidence = 0.95f
        )
    }
}
