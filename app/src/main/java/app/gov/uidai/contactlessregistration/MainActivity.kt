package app.gov.uidai.contactlessregistration

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.gov.uidai.contactlessregistration.data.remote.network.ApiResult
import app.gov.uidai.contactlessregistration.ui.registration.RegistrationRoute
import app.gov.uidai.contactlessregistration.ui.theme.AttendanceAppTheme
import app.gov.uidai.contactlessregistration.ui.theme.md_theme_scrim
import app.gov.uidai.contactlessregistration.ui.theme.md_theme_surface
import app.gov.uidai.contactlessregistration.ui.uidentry.UidEntryRoute
import app.gov.uidai.contactlessregistration.usecase.DeviceUseCase
import app.gov.uidai.contactlessregistration.utils.device.DeviceRegistrationGate
import app.gov.uidai.contactlessregistration.utils.worker.CaptureWorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch

// Route constants — replaces the XML nav graph's IDs/Safe Args
object Routes {
    const val UID_ENTRY = "uid_entry"
    const val REGISTRATION = "registration/{uidHash}"
    fun registration(uidHash: String) = "registration/$uidHash"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val sharedViewModel: SharedViewModel by viewModels()

    @Inject
    lateinit var deviceUseCase: DeviceUseCase

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        setContent {
            AttendanceAppTheme {
                val navController = rememberNavController()
                val sharedUiState by sharedViewModel.uiState.collectAsStateWithLifecycle()

                NavHost(navController = navController, startDestination = Routes.UID_ENTRY) {
                    composable(Routes.UID_ENTRY) {
                        UidEntryRoute(
                            sharedUiState = sharedUiState,
                            onClearSharedMessage = sharedViewModel::clearError,
                            onNavigateToRegistration = { uidHash ->
                                navController.navigate(Routes.registration(uidHash))
                            }
                        )
                    }
                    composable(
                        route = Routes.REGISTRATION,
                        arguments = listOf(navArgument("uidHash") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val uidHash = backStackEntry.arguments?.getString("uidHash").orEmpty()
                        RegistrationRoute(
                            uidHash = uidHash,
                            sharedUiState = sharedUiState,
                            onNavigateUp = { navController.navigateUp() }
                        )
                    }
                }
            }
        }
    }
}