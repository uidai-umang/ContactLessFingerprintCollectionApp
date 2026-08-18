package app.gov.uidai.contactlessregistration.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import java.io.File

/**
 * Standalone security validator, ported from OperatorMitra's SplashActivity
 * -- extracted into its own class (not tied to an Activity) so it can be
 * called once at launch, and also periodically via [performRuntimeCheck]
 * if the Host App keeps a foreground presence.
 *
 * All checks are best-effort heuristics (root/emulator/hook detection is
 * inherently a cat-and-mouse game, none of this is airtight) -- same
 * caveat as the original implementation this was ported from.
 */
class SecurityValidator(private val context: Context) {

    private val packageManager get() = context.packageManager

    /** Full validation, run once at app start. Returns null if clean. */
    fun performComprehensiveValidation(isDebugBuild: Boolean): SecurityViolation? {
        if (isDebugBuild) return null

        val checks: List<Pair<() -> Boolean, SecurityViolation>> = listOf(
            ::isAppTampered to SecurityViolation.AppTampered,
            ::isDebuggingDetected to SecurityViolation.DebuggingDetected,
            ::isHookingFrameworkDetected to SecurityViolation.HookingFrameworkDetected,
            ::isDeviceRooted to SecurityViolation.DeviceRooted,
            ::isDeveloperModeEnabled to SecurityViolation.DeveloperModeEnabled,
            ::isRunningOnEmulator to SecurityViolation.RunningOnEmulator,
            ::isRuntimeApplicationSelfProtection to SecurityViolation.RuntimeSelfProtection
        )

        checks.shuffled().forEach { (check, violation) ->
            try {
                if (check()) return violation
            } catch (e: Exception) {
                return SecurityViolation.Unknown("Exception in check: ${e.message}")
            }
        }
        return null
    }

    /** Lighter, periodic check -- for use in onResume if the app stays foregrounded. */
    fun performRuntimeCheck(): SecurityViolation? {
        if (isDebuggingDetected()) return SecurityViolation.DebuggingDetected
        if (isHookingFrameworkDetected()) return SecurityViolation.HookingFrameworkDetected
        if (isRuntimeApplicationSelfProtection()) return SecurityViolation.RuntimeSelfProtection
        return null
    }

    // ---- Root detection ----

    private fun isDeviceRooted(): Boolean = checkRootApps()

    private fun checkRootApps(): Boolean {
        val rootApps = arrayOf(
            "com.noshufou.android.su", "com.noshufou.android.su.elite", "eu.chainfire.supersu",
            "com.koushikdutta.superuser", "com.thirdparty.superuser", "com.yellowes.su",
            "com.koushikdutta.rommanager", "com.dimonvideo.luckypatcher", "com.chelpus.lackypatch",
            "com.ramdroid.appquarantine", "com.topjohnwu.magisk", "com.kingroot.kinguser",
            "com.kingo.root", "com.smedialink.oneclickroot", "com.zhiqupk.root.global",
            "com.alephzain.framaroot", "com.android.vending.billing.InAppBillingService.COIN",
            "com.android.vending.billing.InAppBillingService.LUCK", "com.chelpus.luckypatcher",
            "com.koushikdutta.rommanager.license", "com.formyhm.hiderootPremium"
        )
        return rootApps.any { pkg ->
            try {
                packageManager.getPackageInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    // ---- Debugging detection ----

    private fun isDebuggingDetected(): Boolean =
        Debug.isDebuggerConnected() || isTracerPidDetected()

    private fun isTracerPidDetected(): Boolean {
        return try {
            val file = File("/proc/self/status")
            if (file.exists()) {
                val status = file.readText()
                val tracerPid = status.substringAfter("TracerPid:").substringBefore("\n").trim()
                tracerPid != "0"
            } else false
        } catch (e: Exception) {
            false
        }
    }

    // ---- Hooking framework detection ----

    private fun isHookingFrameworkDetected(): Boolean =
        isXposedDetected() || isSubstrateDetected() || isCydiaDetected()

    private fun isXposedDetected(): Boolean {
        return try {
            Class.forName("de.robv.android.xposed.XposedHelpers") != null ||
                    File("/system/framework/XposedBridge.jar").exists() ||
                    packageManager.getPackageInfo("de.robv.android.xposed.installer", 0) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun isSubstrateDetected(): Boolean {
        return try {
            File("/system/lib/libsubstrate.so").exists() ||
                    File("/system/lib64/libsubstrate.so").exists()
        } catch (e: Exception) {
            false
        }
    }

    private fun isCydiaDetected(): Boolean {
        return try {
            packageManager.getPackageInfo("com.saurik.substrate", 0) != null
        } catch (e: Exception) {
            false
        }
    }

    // ---- Emulator detection ----

    private fun isRunningOnEmulator(): Boolean =
        isGenericEmulator() || isGenymotion() || isBlueStacks() || isNoxPlayer() || isLdPlayer()

    private fun isGenericEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == Build.PRODUCT ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu")
    }

    private fun isGenymotion(): Boolean =
        Build.MANUFACTURER.contains("Genymotion") ||
                Build.PRODUCT.contains("vbox86p") ||
                Build.FINGERPRINT.startsWith("generic/vbox86p")

    private fun isBlueStacks(): Boolean =
        Build.MANUFACTURER == "BlueStacks" || Build.PRODUCT.contains("BlueStacks")

    private fun isNoxPlayer(): Boolean =
        Build.BOARD.lowercase().contains("nox") ||
                Build.BOOTLOADER.lowercase().contains("nox") ||
                Build.HARDWARE.lowercase().contains("nox") ||
                Build.PRODUCT.lowercase().contains("nox") ||
                Build.SERIAL.lowercase().contains("nox")

    private fun isLdPlayer(): Boolean =
        Build.MANUFACTURER.contains("LDPlayer") || Build.MODEL.contains("LDPlayer")

    // ---- Developer mode ----

    private fun isDeveloperModeEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1 ||
                    Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    // ---- Tampering ----

    private fun isAppTampered(): Boolean = isInstallSourceSuspicious()

    private fun isInstallSourceSuspicious(): Boolean {
        return try {
            val installer = packageManager.getInstallerPackageName(context.packageName)
            installer != null && (installer.contains("blackmarket") || installer.contains("aptoide"))
        } catch (e: Exception) {
            false
        }
    }

    // ---- RASP ----

    private fun isRuntimeApplicationSelfProtection(): Boolean =
        isMemoryDumpDetected() || isDynamicAnalysisDetected() || isCodeInjectionDetected()

    private fun isMemoryDumpDetected(): Boolean {
        return try {
            val file = File("/proc/self/maps")
            if (file.exists()) {
                val maps = file.readText()
                maps.contains("frida") || maps.contains("xposed")
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun isDynamicAnalysisDetected(): Boolean {
        return try {
            File("/data/local/tmp/").listFiles()?.any { file ->
                file.name.contains("frida") || file.name.contains("gdb") || file.name.contains("strace")
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun isCodeInjectionDetected(): Boolean {
        return try {
            Thread.currentThread().stackTrace.any { element ->
                element.className.contains("xposed") ||
                        element.className.contains("substrate") ||
                        element.className.contains("frida")
            }
        } catch (e: Exception) {
            false
        }
    }
}