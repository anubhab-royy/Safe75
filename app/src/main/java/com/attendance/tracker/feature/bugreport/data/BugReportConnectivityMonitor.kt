package com.attendance.tracker.feature.bugreport.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.attendance.tracker.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class BugReportConnectivityMonitor @Inject constructor(
    @ApplicationContext context: Context,
    private val scheduler: BugReportWorkScheduler,
    dispatcherProvider: DispatcherProvider
) {
    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
    private val started = AtomicBoolean(false)

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scope.launch { scheduler.enqueuePendingUploads() }
        }
    }

    fun start() {
        if (!started.compareAndSet(false, true)) return
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)
        }.onFailure {
            started.set(false)
        }
    }
}
