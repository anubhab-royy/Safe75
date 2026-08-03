package com.attendance.tracker.feature.ocr.structure

import org.junit.Assert.assertEquals
import org.junit.Test

class TableStructureDetectorTest {

    private val detector = OpenCVGridDetector()

    @Test
    fun testClusterCoords_groupsCloseValuesAndAverages() {
        val coords = listOf(10, 12, 11, 45, 47, 100, 101, 102)
        // Grouping 10,11,12 -> avg = 11
        // Grouping 45,47 -> avg = 46
        // Grouping 100,101,102 -> avg = 101
        
        // Let's invoke clusterCoords via reflection or duplicate helper inside the test
        // since it is private inside OpenCVGridDetector.
        val clusterCoordsMethod = OpenCVGridDetector::class.java.getDeclaredMethod(
            "clusterCoords",
            List::class.java,
            Int::class.java
        )
        clusterCoordsMethod.isAccessible = true
        
        @Suppress("UNCHECKED_CAST")
        val result = clusterCoordsMethod.invoke(detector, coords, 5) as List<Int>
        
        assertEquals(3, result.size)
        assertEquals(11, result[0])
        assertEquals(46, result[1])
        assertEquals(101, result[2])
    }

    @Test
    fun testFindClosestBoundaryIndex_resolvesIndicesCorrectly() {
        val findClosestBoundaryIndexMethod = OpenCVGridDetector::class.java.getDeclaredMethod(
            "findClosestBoundaryIndex",
            Int::class.java,
            List::class.java
        )
        findClosestBoundaryIndexMethod.isAccessible = true

        val boundaries = listOf(0, 100, 200, 300)

        // y = 10 is closest to index 0 (value 0)
        val idx1 = findClosestBoundaryIndexMethod.invoke(detector, 10, boundaries) as Int
        assertEquals(0, idx1)

        // y = 92 is closest to index 1 (value 100)
        val idx2 = findClosestBoundaryIndexMethod.invoke(detector, 92, boundaries) as Int
        assertEquals(1, idx2)

        // y = 155 is closest to index 2 (value 200)
        val idx3 = findClosestBoundaryIndexMethod.invoke(detector, 155, boundaries) as Int
        assertEquals(2, idx3)
    }
}
