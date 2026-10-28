package sv.edu.unicaes.hospital

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import sv.edu.unicaes.hospital.ui.navigation.AppNavHost
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash del sistema con el logo; al primer frame sigue la splash de Compose (misma marca).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HospitalUnicaesTheme {
                AppNavHost()
            }
        }
    }
}
