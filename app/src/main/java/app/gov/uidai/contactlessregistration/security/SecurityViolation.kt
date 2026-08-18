package app.gov.uidai.contactlessregistration.security

sealed class SecurityViolation(
    val userMessage: String,
    val logDetails: String,
    val openSettings: Boolean = false
) {
    object AppTampered : SecurityViolation(
        userMessage = "This app installation appears to be tampered with. Please reinstall from a trusted source.",
        logDetails = "App tampering detected"
    )
    object DebuggingDetected : SecurityViolation(
        userMessage = "Debugging tools detected. Please close them and restart the app.",
        logDetails = "Debugger/tracer attached"
    )
    object HookingFrameworkDetected : SecurityViolation(
        userMessage = "This device appears to have hooking/instrumentation frameworks installed.",
        logDetails = "Hooking framework detected"
    )
    object DeviceRooted : SecurityViolation(
        userMessage = "This app cannot run on a rooted device for security reasons.",
        logDetails = "Root detected"
    )
    object DeveloperModeEnabled : SecurityViolation(
        userMessage = "Please disable Developer Options / USB debugging to continue.",
        logDetails = "Developer mode enabled",
        openSettings = true
    )
    object RunningOnEmulator : SecurityViolation(
        userMessage = "This app cannot run on an emulator.",
        logDetails = "Emulator detected"
    )
    object RuntimeSelfProtection : SecurityViolation(
        userMessage = "A security threat was detected at runtime. The app will now close.",
        logDetails = "RASP violation"
    )
    data class Unknown(val detail: String) : SecurityViolation(
        userMessage = "A security check failed. The app will now close.",
        logDetails = "Unknown violation: $detail"
    )
}