package com.attendance.tracker.core.diagnostics

import android.util.Log
import com.attendance.tracker.core.logger.LogEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [LogEngine] that records every call into the shared [LogBuffer] and,
 * asynchronously, into [DiagnosticsStorage].
 *
 * In release builds [logcatEnabled] is false so nothing is printed to logcat;
 * the entry is still retained locally for crash reports and exports. Message
 * length is capped so storage stays bounded under heavy logging.
 */
@Singleton
class SafeLogEngine @Inject constructor(
    private val storage: DiagnosticsStorage,
    private val logBuffer: LogBuffer,
    private val scope: CoroutineScope,
    private val logcatEnabled: Boolean
) : LogEngine {

    override fun d(tag: String, message: String, throwable: Throwable?) =
        record(LogLevel.DEBUG, tag, message, throwable)

    override fun i(tag: String, message: String, throwable: Throwable?) =
        record(LogLevel.INFO, tag, message, throwable)

    override fun w(tag: String, message: String, throwable: Throwable?) =
        record(LogLevel.WARNING, tag, message, throwable)

    override fun e(tag: String, message: String, throwable: Throwable?) =
        record(LogLevel.ERROR, tag, message, throwable)

    private fun record(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag.take(MAX_TAG_LENGTH),
            message = message.take(MAX_MESSAGE_LENGTH),
            throwable = throwable?.let { StackFormatter.asString(it) }
        )
        logBuffer.add(entry)
        scope.launch {
            runCatching { storage.appendLog(entry) }
        }
        if (logcatEnabled) {
            androidLog(level, entry.tag, entry.message, throwable)
        }
    }

    private fun androidLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARNING -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }

    companion object {
        private const val MAX_TAG_LENGTH = 64
        private const val MAX_MESSAGE_LENGTH = 2000
    }
}
