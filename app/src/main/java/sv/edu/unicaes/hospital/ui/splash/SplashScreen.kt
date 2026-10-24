package sv.edu.unicaes.hospital.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.ui.components.EstadoCargando
import sv.edu.unicaes.hospital.ui.components.EstadoError
import sv.edu.unicaes.hospital.ui.navigation.DoctorRuta
import sv.edu.unicaes.hospital.ui.navigation.RecepcionRuta
import sv.edu.unicaes.hospital.ui.theme.Espacio

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

        SplashUiState.ElegirRol -> {
            ElegirRolDemo(onDestino = onDestino)
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

@Composable
private fun ElegirRolDemo(onDestino: (Any) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Espacio.lg),
        verticalArrangement = Arrangement.spacedBy(Espacio.xs, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.demo_titulo),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { onDestino(RecepcionRuta) }) { Text(stringResource(R.string.demo_recepcion)) }
        OutlinedButton(onClick = { onDestino(DoctorRuta) }) { Text(stringResource(R.string.demo_doctor)) }
    }
}
