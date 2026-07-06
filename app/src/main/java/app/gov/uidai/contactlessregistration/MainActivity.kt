package app.gov.uidai.contactlessregistration

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.gov.uidai.contactlessregistration.ui.theme.md_theme_scrim
import app.gov.uidai.contactlessregistration.ui.theme.md_theme_surface
import app.gov.uidai.contactlessregistration.utils.worker.CaptureWorkScheduler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val sharedViewModel: SharedViewModel by viewModels()

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
    }

}