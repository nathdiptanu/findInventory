package com.docufind.app.security.detection

import android.content.Context
import android.os.Build
import com.docufind.app.security.logging.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class SecurityWarning(
    val type: WarningType,
    val message: String
)

enum class WarningType {
    ROOT,
    EMULATOR,
    DEVELOPER_MODE,
    HOOKING
}

@Singleton
class RootDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureLogger: SecureLogger
) {
    fun detect(): List<SecurityWarning> = buildList {
        if (checkRootPaths()) {
            add(SecurityWarning(WarningType.ROOT, "Root access detected. Your documents may be at increased risk."))
        }
        if (checkMagisk()) {
            add(SecurityWarning(WarningType.ROOT, "Magisk detected. Proceed with caution."))
        }
        if (checkBusybox()) {
            add(SecurityWarning(WarningType.ROOT, "Busybox detected on device."))
        }
        if (checkHookingFrameworks(context)) {
            add(SecurityWarning(WarningType.HOOKING, "Hooking framework detected. App integrity may be compromised."))
        }
        if (checkDeveloperMode()) {
            add(SecurityWarning(WarningType.DEVELOPER_MODE, "Developer options are enabled."))
        }
    }.also { warnings ->
        if (warnings.isNotEmpty()) {
            secureLogger.warn("Security warnings detected: ${warnings.size}")
        }
    }

    private fun checkRootPaths(): Boolean {
        val paths = listOf(
            "/system/app/Superuser.apk",
            "/system/xbin/su",
            "/system/bin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkMagisk(): Boolean {
        return File("/sbin/.magisk").exists() ||
            File("/data/adb/magisk").exists()
    }

    private fun checkBusybox(): Boolean {
        return File("/system/xbin/busybox").exists() ||
            File("/system/bin/busybox").exists()
    }

    private fun checkHookingFrameworks(context: Context): Boolean {
        val packages = listOf(
            "de.robv.android.xposed.installer",
            "io.va.exposed",
            "com.saurik.substrate"
        )
        return packages.any { pkg ->
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun checkDeveloperMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) == 1
        } else {
            false
        }
    }
}

@Singleton
class EmulatorDetector @Inject constructor() {
    fun detect(): SecurityWarning? {
        val isEmulator = (Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
            "google_sdk" == Build.PRODUCT)

        return if (isEmulator) {
            SecurityWarning(
                WarningType.EMULATOR,
                "Running on an emulator. For testing only — do not store real documents."
            )
        } else {
            null
        }
    }
}
