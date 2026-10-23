package sv.edu.unicaes.hospital.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import sv.edu.unicaes.hospital.data.repository.EstadoSesion
import sv.edu.unicaes.hospital.ui.auth.LoginScreen
import sv.edu.unicaes.hospital.ui.doctor.DoctorHomeScreen
import sv.edu.unicaes.hospital.ui.recepcion.RecepcionHomeScreen
import sv.edu.unicaes.hospital.ui.sesion.SesionViewModel
import sv.edu.unicaes.hospital.ui.sesion.SinAccesoScreen
import sv.edu.unicaes.hospital.ui.splash.SplashScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    sesionViewModel: SesionViewModel = hiltViewModel(),
) {
    val estadoSesion by sesionViewModel.estadoSesion.collectAsStateWithLifecycle()

    // Si la sesión se cierra o no se puede refrescar estando dentro, volver al login.
    LaunchedEffect(estadoSesion) {
        val enLogin = navController.currentDestination?.hasRoute(LoginRuta::class) == true
        val enSplash = navController.currentDestination?.hasRoute(SplashRuta::class) == true
        if (estadoSesion == EstadoSesion.INACTIVA && !enLogin && !enSplash) {
            navController.irA(LoginRuta)
        }
    }

    NavHost(navController = navController, startDestination = SplashRuta) {
        composable<SplashRuta> {
            SplashScreen(onDestino = { navController.irA(it) })
        }
        composable<LoginRuta> {
            // Tras ingresar, el Splash resuelve el destino según el rol.
            LoginScreen(onIngresado = { navController.irA(SplashRuta) })
        }
        composable<RecepcionRuta> {
            RecepcionHomeScreen(onCerrarSesion = sesionViewModel::cerrarSesion)
        }
        composable<DoctorRuta> {
            DoctorHomeScreen(onCerrarSesion = sesionViewModel::cerrarSesion)
        }
        composable<SinAccesoRuta> {
            SinAccesoScreen(onCerrarSesion = sesionViewModel::cerrarSesion)
        }
    }
}

/** Navega limpiando la pila: no se vuelve atrás al login ni al splash. */
private fun NavHostController.irA(ruta: Any) {
    navigate(ruta) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
