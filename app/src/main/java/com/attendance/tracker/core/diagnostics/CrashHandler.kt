package com.attendance.tracker.core.diagnostics

/**
 * [Thread.UncaughtExceptionHandler] that persists a [CrashReport] before
 * delegating to the previously installed handler.
 *
 * The report is written synchronously: it is tiny and this is the only point
 * where a guaranteed, durable capture matters (the process is about to die).
 * The previous handler, when present, is always invoked so default Android
 * behavior (and any chain) is preserved — crashes are never suppressed.
 */
class CrashHandler(
    private val previous: Thread.UncaughtExceptionHandler?,
    private val storage: DiagnosticsStorage,
    private val logBuffer: LogBuffer,
    private val deviceInfoProvider: DeviceInfoProvider
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            val device = runCatching { deviceInfoProvider.deviceInfo() }
                .getOrElse { DeviceInfoBuilder().unknown() }
            val report = CrashReport(
                id = "${System.currentTimeMillis()}_${System.nanoTime()}",
                timestamp = System.currentTimeMillis(),
                threadName = thread.name,
                exceptionClass = throwable.javaClass.name,
                message = throwable.message.orEmpty(),
                stackTrace = StackFormatter.frames(throwable),
                causeClass = throwable.cause?.javaClass?.name,
                causeMessage = throwable.cause?.message,
                appVersion = device.appVersion,
                versionCode = device.versionCode,
                buildType = device.buildType,
                deviceInfo = device,
                recentLogs = logBuffer.snapshot()
            )
            storage.saveCrashReport(report)
        }
        previous?.uncaughtException(thread, throwable)
    }
}
