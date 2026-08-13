package com.attendance.tracker.feature.bugreport.data

import com.attendance.tracker.feature.device.data.remote.NetworkError
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BugReportRetryPolicyTest {
    @Test
    fun transientFailuresAreRetryable() {
        assertTrue(BugReportRetryPolicy.isTransient(NetworkError.Timeout()))
        assertTrue(BugReportRetryPolicy.isTransient(NetworkError.UnknownHost()))
        assertTrue(BugReportRetryPolicy.isTransient(NetworkError.ServerError()))
        assertTrue(BugReportRetryPolicy.isTransient(NetworkError.Io()))
    }

    @Test
    fun clientAndAuthFailuresArePermanent() {
        assertFalse(BugReportRetryPolicy.isTransient(NetworkError.Validation()))
        assertFalse(BugReportRetryPolicy.isTransient(NetworkError.Unauthorized()))
        assertFalse(BugReportRetryPolicy.isTransient(NetworkError.Forbidden()))
        assertFalse(BugReportRetryPolicy.isTransient(NetworkError.Conflict()))
        assertFalse(BugReportRetryPolicy.isTransient(NetworkError.NotFound()))
    }
}
