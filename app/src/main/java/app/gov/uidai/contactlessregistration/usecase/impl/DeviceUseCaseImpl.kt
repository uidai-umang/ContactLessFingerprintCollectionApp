package app.gov.uidai.contactlessregistration.usecase.impl

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import app.gov.uidai.contactlessregistration.utils.camera.CameraSpecManager
import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult
import app.gov.uidai.contactlessregistration.model.device.DeviceRegistrationRequest
import app.gov.uidai.contactlessregistration.model.device.DeviceRegistrationResponse
import app.gov.uidai.contactlessregistration.repository.ClfRepository
import app.gov.uidai.contactlessregistration.usecase.DeviceUseCase
import javax.inject.Inject

class DeviceUseCaseImpl @Inject constructor(
    private val clfRepository: ClfRepository
) : DeviceUseCase {

    override suspend fun registerDeviceIfNeeded(
        context: Context,
        operatorId: String,
        androidId: String
    ): ApiResult<DeviceRegistrationResponse> {
        val cameraSpec = CameraSpecManager.fetch(context)
            ?: return ApiResult.Error("Unable to read camera characteristics", -1)

        val request = DeviceRegistrationRequest(
            operatorId = operatorId,
            androidId = androidId,
            deviceFingerprint = Build.FINGERPRINT,
            deviceModel = Build.MODEL,
            deviceManufacturer = Build.MANUFACTURER,
            osVersion = Build.VERSION.RELEASE,
            androidSdkVersion = Build.VERSION.SDK_INT,
            androidSecurityPatch = Build.VERSION.SECURITY_PATCH,
            socModel = Build.SOC_MODEL.takeIf { Build.VERSION.SDK_INT >= 31 } ?: Build.HARDWARE,
            ramTotalMb = getTotalRamMb(context),
            cameraFingerprintHash = cameraSpec.fingerprintHash,
            cameraId = cameraSpec.cameraId,
            lensFacing = cameraSpec.lensFacing,
            hardwareLevel = cameraSpec.hardwareLevel,
            sensorPhysicalSizeMm = cameraSpec.sensorPhysicalSizeMm,
            sensorActiveArraySize = cameraSpec.sensorActiveArraySize,
            pixelArraySize = cameraSpec.pixelArraySize,
            focalLengthMm = cameraSpec.focalLengthMm,
            aperture = cameraSpec.aperture,
            minFocusDistanceDiopters = cameraSpec.minFocusDistanceDiopters,
            hyperfocalDistanceDiopters = cameraSpec.hyperfocalDistanceDiopters,
            hasFlash = cameraSpec.hasFlash,
            hasOis = cameraSpec.hasOis,
            maxDigitalZoom = cameraSpec.maxDigitalZoom,
            sensorOrientation = cameraSpec.sensorOrientation,
            supportsRaw = cameraSpec.supportsRaw,
            afModes = cameraSpec.afModes,
            aeModes = cameraSpec.aeModes,
            awbModes = cameraSpec.awbModes
        )

        return clfRepository.registerDevice(request)
    }

    private fun getTotalRamMb(context: Context): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem / (1024 * 1024)).toInt()
    }
}