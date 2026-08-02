package com.attendance.tracker.core.logger

import android.util.Log
import com.attendance.tracker.BuildConfig

/**
 * Interface defining standard logging operations.
 */
interface LogEngine {
    fun d(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * Core application logger delegating to a configurable [LogEngine].
 *
 * In release builds logging is a no-op so no PII or debug output leaks into
 * production (see Quality: no debug logging in Release).
 */
object Logger {
    private var engine: LogEngine = createDefaultEngine()

    /**
     * Replaces the current logging engine.
     */
    fun setEngine(logEngine: LogEngine) {
        engine = logEngine
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) = engine.d(tag, message, throwable)
    fun i(tag: String, message: String, throwable: Throwable? = null) = engine.i(tag, message, throwable)
    fun w(tag: String, message: String, throwable: Throwable? = null) = engine.w(tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = engine.e(tag, message, throwable)

    private fun createDefaultEngine(): LogEngine =
        if (BuildConfig.DEBUG) DefaultLogEngine() else NoOpLogEngine()
}

/**
 * Standard log engine utilizing system android.util.Log.
 */
class DefaultLogEngine : LogEngine {
    override fun d(tag: String, message: String, throwable: Throwable?) {
        Log.d(tag, message, throwable)
    }
    override fun i(tag: String, message: String, throwable: Throwable?) {
        Log.i(tag, message, throwable)
    }
    override fun w(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
    }
    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}

/**
 * Discards every log call; used in release builds.
 */
class NoOpLogEngine : LogEngine {
    override fun d(tag: String, message: String, throwable: Throwable?) = Unit
    override fun i(tag: String, message: String, throwable: Throwable?) = Unit
    override fun w(tag: String, message: String, throwable: Throwable?) = Unit
    override fun e(tag: String, message: String, throwable: Throwable?) = Unit
}
