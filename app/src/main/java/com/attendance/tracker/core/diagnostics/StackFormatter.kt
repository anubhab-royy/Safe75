package com.attendance.tracker.core.diagnostics

/**
 * Pure helper that formats a [Throwable] into capped, line-based output.
 *
 * Frame count and total length are bounded so crash reports stay small even
 * for deeply nested or recursive failures.
 */
object StackFormatter {

    /**
     * Formats [throwable] and its cause chain as a list of lines.
     * Each throwable contributes one header line plus its frames, up to
     * [maxFrames] total frame lines.
     */
    fun frames(throwable: Throwable, maxFrames: Int = MAX_FRAMES): List<String> {
        val lines = mutableListOf<String>()
        var current: Throwable? = throwable
        var total = 0
        while (current != null && total < maxFrames) {
            lines += "${current.javaClass.name}: ${current.message.orEmpty()}"
            for (frame in current.stackTrace) {
                if (total >= maxFrames) break
                lines += "\tat $frame"
                total++
            }
            current = current.cause
        }
        return lines
    }

    /**
     * Formats [throwable] as a single string capped to [maxChars].
     */
    fun asString(throwable: Throwable, maxChars: Int = MAX_STRING_CHARS): String {
        val text = frames(throwable).joinToString("\n")
        return if (text.length <= maxChars) text else text.take(maxChars)
    }

    const val MAX_FRAMES = 40
    const val MAX_STRING_CHARS = 8000
}
