package com.docufind.app.support

import android.content.Context
import android.os.Build
import com.docufind.app.BuildConfig
import com.docufind.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceDiagnostics(
    val appName: String,
    val versionName: String,
    val versionCode: Int,
    val androidVersion: String,
    val androidSdk: Int,
    val manufacturer: String,
    val model: String,
    val timestamp: String
) {
    fun toReportBlock(): String = buildString {
        appendLine("--- Device Diagnostics ---")
        appendLine("App name: $appName")
        appendLine("App version name: $versionName")
        appendLine("App version code: $versionCode")
        appendLine("Android version: $androidVersion (API $androidSdk)")
        appendLine("Device manufacturer: $manufacturer")
        appendLine("Device model: $model")
        appendLine("Timestamp: $timestamp")
    }
}

@Singleton
class DeviceDiagnosticsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun collect(): DeviceDiagnostics {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val timestamp = Instant.now()
            .atZone(ZoneId.systemDefault())
            .format(formatter)
        return DeviceDiagnostics(
            appName = context.getString(R.string.app_name),
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            androidSdk = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER.orEmpty().ifBlank { "Unknown" },
            model = Build.MODEL.orEmpty().ifBlank { "Unknown" },
            timestamp = timestamp
        )
    }
}
