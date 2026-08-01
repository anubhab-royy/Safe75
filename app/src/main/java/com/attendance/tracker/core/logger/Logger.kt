package com.attendance.tracker.core.logger

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
 */
object Logger {
    private var engine: LogEngine = DefaultLogEngine()

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
}

/**
 * Standard log engine utilizing system android.util.Log.
 */
class DefaultLogEngine : LogEngine {
    override fun d(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.d(tag, message, throwable)
    }
    override fun i(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.i(tag, message, throwable)
    }
    override fun w(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.w(tag, message, throwable)
    }
    override fun e(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.e(tag, message, throwable)
    }
}
