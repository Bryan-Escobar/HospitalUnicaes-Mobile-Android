package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.ui.components.Chevron
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

/** Pestaña "Más" de Recepción: Laboratorio, Ambulancia, Turnos y Perfil. */
@Composable
fun MasScreen(
    onLaboratorio: () -> Unit,
    onAmbulancia: () -> Unit,
    onTurnos: () -> Unit,
    onPerfil: () -> Unit,
) {
    Scaffold(topBar = { TopBarHospital(titulo = stringResource(R.string.tab_mas)) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            FilaLista(
                titulo = stringResource(R.string.mas_laboratorio),
                soporte = stringResource(R.string.mas_laboratorio_detalle),
                onClick = onLaboratorio,
                final = { Chevron() },
            )
            FilaLista(
                titulo = stringResource(R.string.mas_ambulancia),
                soporte = stringResource(R.string.mas_ambulancia_detalle),
                onClick = onAmbulancia,
                final = { Chevron() },
            )
            FilaLista(
                titulo = stringResource(R.string.tab_turnos),
                soporte = stringResource(R.string.mas_turnos_detalle),
                onClick = onTurnos,
                final = { Chevron() },
            )
            FilaLista(
                titulo = stringResource(R.string.tab_perfil),
                soporte = stringResource(R.string.mas_perfil_detalle),
                onClick = onPerfil,
                final = { Chevron() },
            )
        }
    }
}

@PreviewPantalla
@Composable
private fun MasPreview() {
    HospitalUnicaesTheme { MasScreen(onLaboratorio = {}, onAmbulancia = {}, onTurnos = {}, onPerfil = {}) }
}
