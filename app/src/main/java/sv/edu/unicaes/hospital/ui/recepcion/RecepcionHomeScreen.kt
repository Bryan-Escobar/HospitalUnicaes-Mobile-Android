package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.ui.components.EstadoMensaje
import sv.edu.unicaes.hospital.ui.components.HomeConPestanas
import sv.edu.unicaes.hospital.ui.components.PantallaEnConstruccion
import sv.edu.unicaes.hospital.ui.components.Pestana
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

// Recepcionista · Asistente · Enfermero/a (AND-R1 a AND-R17)
private val pestanasRecepcion =
    listOf(
        Pestana(R.string.tab_inicio, R.drawable.ic_home),
        Pestana(R.string.tab_pacientes, R.drawable.ic_group),
        Pestana(R.string.tab_citas, R.drawable.ic_calendar),
        Pestana(R.string.tab_camas, R.drawable.ic_bed),
        Pestana(R.string.tab_mas, R.drawable.ic_menu),
    )

@Composable
fun RecepcionHomeScreen(onCerrarSesion: () -> Unit) {
    HomeConPestanas(pestanas = pestanasRecepcion) { pestana, modifier ->
        val titulo = stringResource(pestana.titulo)
        if (pestana.titulo == R.string.tab_mas) {
            // Laboratorio, Ambulancia, Turnos y Perfil vivirán aquí.
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
private fun RecepcionHomePreview() {
    HospitalUnicaesTheme { RecepcionHomeScreen(onCerrarSesion = {}) }
}
