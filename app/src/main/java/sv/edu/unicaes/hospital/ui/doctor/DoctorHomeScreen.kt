package sv.edu.unicaes.hospital.ui.doctor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.ui.components.EstadoMensaje
import sv.edu.unicaes.hospital.ui.components.HomeConPestanas
import sv.edu.unicaes.hospital.ui.components.PantallaEnConstruccion
import sv.edu.unicaes.hospital.ui.components.Pestana
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

// Doctor · Alergista · Médico de emergencia (AND-D1 a AND-D7)
private val pestanasDoctor =
    listOf(
        Pestana(R.string.tab_agenda, R.drawable.ic_calendar),
        Pestana(R.string.tab_pacientes, R.drawable.ic_group),
        Pestana(R.string.tab_turnos, R.drawable.ic_schedule),
        Pestana(R.string.tab_perfil, R.drawable.ic_account_box),
    )

@Composable
fun DoctorHomeScreen(onCerrarSesion: () -> Unit) {
    HomeConPestanas(pestanas = pestanasDoctor) { pestana, modifier ->
        val titulo = stringResource(pestana.titulo)
        if (pestana.titulo == R.string.tab_perfil) {
            EstadoMensaje(
                titulo = titulo,
                detalle = stringResource(R.string.estado_en_construccion),
                accion = stringResource(R.string.sesion_cerrar),
                onAccion = onCerrarSesion,
                modifier = modifier,
            )
        } else {
            PantallaEnConstruccion(titulo = titulo, modifier = modifier)
        }
    }
}

@PreviewScreenSizes
@Composable
private fun DoctorHomePreview() {
    HospitalUnicaesTheme { DoctorHomeScreen(onCerrarSesion = {}) }
}
