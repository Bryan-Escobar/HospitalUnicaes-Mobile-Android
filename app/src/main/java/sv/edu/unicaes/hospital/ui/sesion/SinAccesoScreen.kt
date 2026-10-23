package sv.edu.unicaes.hospital.ui.sesion

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.ui.components.EstadoMensaje
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

/** Paciente y Administrador no usan la app Android (iOS y Web respectivamente). */
@Composable
fun SinAccesoScreen(onCerrarSesion: () -> Unit) {
    EstadoMensaje(
        titulo = stringResource(R.string.sin_acceso_titulo),
        detalle = stringResource(R.string.sin_acceso_detalle),
        accion = stringResource(R.string.sesion_cerrar),
        onAccion = onCerrarSesion,
    )
}

@Preview(showBackground = true)
@Composable
private fun SinAccesoPreview() {
    HospitalUnicaesTheme { SinAccesoScreen(onCerrarSesion = {}) }
}
