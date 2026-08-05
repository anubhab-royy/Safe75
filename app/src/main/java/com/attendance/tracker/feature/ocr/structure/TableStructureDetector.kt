package com.attendance.tracker.feature.ocr.structure

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Domain model representing a single cell inside a detected table grid.
 */
data class TableGridCell(
    val id: String = UUID.randomUUID().toString(),
    val startRow: Int,
    val startCol: Int,
    val rowSpan: Int,
    val colSpan: Int,
    /** Bounding rectangle of the cell in absolute image coordinates. */
    val rect: Rect
)

/**
 * Domain model representing the full grid layout structure of a table.
 */
data class TableGridModel(
    val rows: Int,
    val cols: Int,
    val cells: List<TableGridCell>
)

/**
 * Interface defining table cell grid extraction.
 * Supports modularity allowing alternative future implementations (like layout analysis or ML-based detectors).
 */
interface TableStructureDetector {
    /**
     * Determines cell rows, columns, and spans for a given table rect using horizontal and vertical masks.
     */
    fun detectStructure(
        tableRect: Rect,
        horizontalLines: Mat,
        verticalLines: Mat,
        gridMask: Mat
    ): TableGridModel
}

/**
 * OpenCV morphological line-projection grid structural detector.
 */
@Singleton
class OpenCVGridDetector @Inject constructor() : TableStructureDetector {

    override fun detectStructure(
        tableRect: Rect,
        horizontalLines: Mat,
        verticalLines: Mat,
        gridMask: Mat
    ): TableGridModel {
        // Crop horizontal, vertical, and combined grid masks to the table's region
        val subHoriz = Mat(horizontalLines, tableRect)
        val subVert = Mat(verticalLines, tableRect)
        val subGrid = Mat(gridMask, tableRect)

        val hierarchy = Mat()
        val hContours = ArrayList<MatOfPoint>()
        val vContours = ArrayList<MatOfPoint>()
        val cellContours = ArrayList<MatOfPoint>()
        val rowBoundaries: List<Int>
        val colBoundaries: List<Int>
        val cells = ArrayList<TableGridCell>()
        val subGridInverted = Mat()

        try {
            // 1. Extract Horizontal Line Y coordinates
            Imgproc.findContours(subHoriz, hContours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            val yCoords = ArrayList<Int>()
            yCoords.add(0)
            yCoords.add(tableRect.height)
            for (contour in hContours) {
                val r = Imgproc.boundingRect(contour)
                yCoords.add(r.y + r.height / 2)
            }
            rowBoundaries = clusterCoords(yCoords, tolerance = 12).sorted()

            // 2. Extract Vertical Line X coordinates
            Imgproc.findContours(subVert, vContours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            val xCoords = ArrayList<Int>()
            xCoords.add(0)
            xCoords.add(tableRect.width)
            for (contour in vContours) {
                val r = Imgproc.boundingRect(contour)
                xCoords.add(r.x + r.width / 2)
            }
            colBoundaries = clusterCoords(xCoords, tolerance = 12).sorted()

            // 3. Find cell contours using negative space (invert the grid)
            Core.bitwise_not(subGrid, subGridInverted)

            Imgproc.findContours(subGridInverted, cellContours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

            val minCellArea = (tableRect.width * tableRect.height) * 0.0001 // Filter tiny noise cells

            for (contour in cellContours) {
                val cellRect = Imgproc.boundingRect(contour)
                val area = cellRect.width * cellRect.height
                if (area < minCellArea || cellRect.width >= tableRect.width * 0.98 || cellRect.height >= tableRect.height * 0.98) {
                    // Ignore noise or full table bounding boxes
                    continue
                }

                // Map pixel coordinates to row/col indices using closest boundaries
                val startRow = findClosestBoundaryIndex(cellRect.y, rowBoundaries)
                val endRow = findClosestBoundaryIndex(cellRect.y + cellRect.height, rowBoundaries)
                val rowSpan = maxOf(1, endRow - startRow)

                val startCol = findClosestBoundaryIndex(cellRect.x, colBoundaries)
                val endCol = findClosestBoundaryIndex(cellRect.x + cellRect.width, colBoundaries)
                val colSpan = maxOf(1, endCol - startCol)

                // Convert cell coordinates back to absolute image coordinates
                val absoluteRect = Rect(
                    tableRect.x + cellRect.x,
                    tableRect.y + cellRect.y,
                    cellRect.width,
                    cellRect.height
                )

                cells.add(
                    TableGridCell(
                        startRow = startRow,
                        startCol = startCol,
                        rowSpan = rowSpan,
                        colSpan = colSpan,
                        rect = absoluteRect
                    )
                )
            }
        } finally {
            // Release sub-mats and hierarchy
            subHoriz.release()
            subVert.release()
            subGrid.release()
            subGridInverted.release()
            hierarchy.release()
            hContours.forEach { it.release() }
            vContours.forEach { it.release() }
            cellContours.forEach { it.release() }
        }

        val rowsCount = maxOf(1, rowBoundaries.size - 1)
        val colsCount = maxOf(1, colBoundaries.size - 1)

        return TableGridModel(rows = rowsCount, cols = colsCount, cells = cells)
    }

    /**
     * Group coordinate projection lines within [tolerance] and calculate the average cluster coordinates.
     */
    private fun clusterCoords(coords: List<Int>, tolerance: Int): List<Int> {
        if (coords.isEmpty()) return emptyList()
        val sorted = coords.sorted()
        val clusters = ArrayList<Int>()
        var currentSum = sorted[0]
        var currentCount = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] - sorted[i - 1] <= tolerance) {
                currentSum += sorted[i]
                currentCount++
            } else {
                clusters.add(currentSum / currentCount)
                currentSum = sorted[i]
                currentCount = 1
            }
        }
        clusters.add(currentSum / currentCount)
        return clusters
    }

    /**
     * Finds the index of the boundary line closest to a given coordinate value.
     */
    private fun findClosestBoundaryIndex(value: Int, boundaries: List<Int>): Int {
        var closestIndex = 0
        var minDiff = Int.MAX_VALUE
        for (i in boundaries.indices) {
            val diff = abs(boundaries[i] - value)
            if (diff < minDiff) {
                minDiff = diff
                closestIndex = i
            }
        }
        return closestIndex
    }
}
