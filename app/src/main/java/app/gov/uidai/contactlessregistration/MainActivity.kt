package app.gov.uidai.contactlessregistration

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult
import app.gov.uidai.contactlessregistration.ui.theme.md_theme_scrim
import app.gov.uidai.contactlessregistration.ui.theme.md_theme_surface
import app.gov.uidai.contactlessregistration.usecase.DeviceUseCase
import app.gov.uidai.contactlessregistration.utils.device.DeviceRegistrationGate
import app.gov.uidai.contactlessregistration.utils.worker.CaptureWorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val sharedViewModel: SharedViewModel by viewModels()
    @Inject
    lateinit var deviceUseCase: DeviceUseCase

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = Color.Transparent.toArgb(),
                darkScrim = md_theme_scrim.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = md_theme_surface.toArgb(),
                darkScrim = md_theme_scrim.toArgb()
            )
        )
        // Request notification permission for Chucker (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        sharedViewModel.initialize(this)
        CaptureWorkScheduler.schedule(this)

        if (!DeviceRegistrationGate.isRegistered(this)) {
            lifecycleScope.launch {
                val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val result = deviceUseCase.registerDeviceIfNeeded(
                    context = this@MainActivity,
                    operatorId = "00000000-0000-0000-0000-000000000001",
                    androidId = androidId
                )
                if (result is ApiResult.Success) {
                    DeviceRegistrationGate.markRegistered(this@MainActivity)
                }
            }
        }
    }

}