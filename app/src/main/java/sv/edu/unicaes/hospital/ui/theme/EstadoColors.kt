package sv.edu.unicaes.hospital.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Par de colores para un badge de estado: texto (y borde/icono) sobre fondo. */
@Immutable
data class ParColor(
    val texto: Color,
    val fondo: Color,
)

/**
 * Colores semánticos de los estados del dominio. Material 3 no los tiene,
 * así que se exponen como extensión del tema: `HospitalTheme.estados.labPendiente`.
 */
@Immutable
data class EstadoColors(
    val labPendiente: ParColor = ParColor(AmbarTexto, AmbarFondo),
    val labEnProceso: ParColor = ParColor(AzulTexto, AzulFondo),
    val labCompletado: ParColor = ParColor(VerdeTexto, VerdeFondo),
    val ambulanciaDisponible: ParColor = ParColor(VerdeTexto, VerdeFondo),
    val ambulanciaEnServicio: ParColor = ParColor(RojoTexto, RojoFondo),
    val ambulanciaEnMantenimiento: ParColor = ParColor(GrisTexto, GrisFondo),
    val camaLibre: ParColor = ParColor(VerdeTexto, VerdeFondo),
    val camaOcupada: ParColor = ParColor(Superficie, CamaOcupada),
    val citaProgramada: ParColor = ParColor(AzulTexto, AzulFondo),
    val citaAtendida: ParColor = ParColor(VerdeTexto, VerdeFondo),
    val citaCancelada: ParColor = ParColor(GrisTexto, GrisFondo),
)

val LocalEstadoColors = staticCompositionLocalOf { EstadoColors() }
