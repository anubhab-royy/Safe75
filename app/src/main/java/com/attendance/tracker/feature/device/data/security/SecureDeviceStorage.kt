package com.attendance.tracker.feature.device.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.attendance.tracker.feature.device.domain.model.EnrolledDevice
import javax.inject.Inject

class SecureDeviceStorage @Inject constructor(
    private val context: Context
) {

    private val sharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveDevice(deviceId: String, deviceSecret: String, createdAt: Long) {
        sharedPreferences.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_DEVICE_SECRET, deviceSecret)
            .putLong(KEY_CREATED_AT, createdAt)
            .apply()
    }

    fun getDevice(): EnrolledDevice? {
        val deviceId = sharedPreferences.getString(KEY_DEVICE_ID, null)
        val deviceSecret = sharedPreferences.getString(KEY_DEVICE_SECRET, null)
        val createdAt = sharedPreferences.getLong(KEY_CREATED_AT, 0L)

        if (deviceId.isNullOrBlank() || deviceSecret.isNullOrBlank()) {
            return null
        }

        return EnrolledDevice(deviceId, deviceSecret, createdAt)
    }

    fun clearDevice() {
        sharedPreferences.edit()
            .remove(KEY_DEVICE_ID)
            .remove(KEY_DEVICE_SECRET)
            .remove(KEY_CREATED_AT)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "safe75_secure_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_SECRET = "device_secret"
        private const val KEY_CREATED_AT = "created_at"
    }
}
