package sv.edu.unicaes.hospital.ui.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sv.edu.unicaes.hospital.ui.components.EstadoCargando
import sv.edu.unicaes.hospital.ui.components.EstadoError

@Composable
fun SplashScreen(
    onDestino: (Any) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val estado = uiState) {
        SplashUiState.Cargando -> {
            EstadoCargando()
        }

        is SplashUiState.Error -> {
            EstadoError(error = estado.error, onReintentar = viewModel::resolverDestino)
        }

        is SplashUiState.Listo -> {
            EstadoCargando()
            LaunchedEffect(estado.destino) { onDestino(estado.destino) }
        }
    }
}
