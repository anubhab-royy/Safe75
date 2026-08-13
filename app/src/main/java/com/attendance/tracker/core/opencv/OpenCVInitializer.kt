package com.attendance.tracker.core.opencv

import com.attendance.tracker.core.logger.Logger
import org.opencv.android.OpenCVLoader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lazy, thread-safe OpenCV native library loader.
 *
 * The native library is no longer preloaded during app startup; it is loaded
 * exactly once per process, on the first call to [ensureLoaded] (which happens
 * only when an OCR operation actually begins). Concurrent callers block until
 * the single load attempt completes and then share its result, so subsequent
 * OCR requests are never delayed by re-initialization.
 */
@Singleton
class OpenCVInitializer @Inject constructor() {

    private val lock = Any()
    private var state: Boolean? = null

    /**
     * Loads OpenCV on first use and returns whether it is now usable.
     * Thread-safe; the underlying native load runs at most once per process.
     */
    fun ensureLoaded(): Boolean {
        synchronized(lock) {
            state?.let { return it }
            val loaded = runCatching {
                OpenCVLoader.initLocal()
            }.onFailure {
                Logger.e(TAG, "OpenCV initialization failed", it)
            }.getOrDefault(false)
            if (loaded) {
                Logger.i(TAG, "OpenCV loaded lazily on first OCR use")
            }
            state = loaded
            return loaded
        }
    }

    private companion object {
        const val TAG = "OpenCVInitializer"
    }
}
