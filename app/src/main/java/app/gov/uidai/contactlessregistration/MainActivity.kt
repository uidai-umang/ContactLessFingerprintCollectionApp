package app.gov.uidai.contactlessregistration

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import app.gov.uidai.contactlessregistration.ui.theme.md_theme_scrim
import app.gov.uidai.contactlessregistration.ui.theme.md_theme_surface
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val sharedViewModel: SharedViewModel by viewModels()

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
        sharedViewModel.initialize(this)
    }
}