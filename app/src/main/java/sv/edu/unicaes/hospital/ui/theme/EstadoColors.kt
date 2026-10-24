package sv.edu.unicaes.hospital.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colores de un badge de estado: texto (e icono) sobre fondo, con su borde. */
@Immutable
data class ParColor(
    val texto: Color,
    val fondo: Color,
    val borde: Color = fondo,
)

/**
 * Colores semánticos de los estados del dominio. Material 3 no los tiene,
 * así que se exponen como extensión del tema: `HospitalTheme.estados.labPendiente`.
 */
@Immutable
data class EstadoColors(
    val labPendiente: ParColor = ParColor(AmbarTexto, AmbarFondo, AmbarBorde),
    val labEnProceso: ParColor = ParColor(AzulTexto, AzulFondo, AzulBorde),
    val labCompletado: ParColor = ParColor(VerdeTexto, VerdeFondo, VerdeBorde),
    val ambulanciaDisponible: ParColor = ParColor(VerdeTexto, VerdeFondo, VerdeBorde),
    val ambulanciaEnServicio: ParColor = ParColor(RojoTexto, RojoFondo, RojoBorde),
    val ambulanciaEnMantenimiento: ParColor = ParColor(GrisTexto, GrisFondo, GrisBorde),
    val camaLibre: ParColor = ParColor(VerdeTexto, VerdeFondo, VerdeBorde),
    val camaOcupada: ParColor = ParColor(Superficie, CamaOcupada),
    val camaElegida: ParColor = ParColor(Superficie, Primario700, Primario200),
    val citaProgramada: ParColor = ParColor(AzulTexto, AzulFondo, AzulBorde),
    val citaAtendida: ParColor = ParColor(VerdeTexto, VerdeFondo, VerdeBorde),
    val citaCancelada: ParColor = ParColor(GrisTexto, GrisFondo, GrisBorde),
    val solicitudNueva: ParColor = ParColor(AmbarTexto, AmbarFondo, AmbarBorde),
    val solicitudRechazada: ParColor = ParColor(GrisTexto, GrisFondo, GrisBorde),
    val turnoEnCurso: ParColor = ParColor(Primario800, Primario50, Primario200),
    // Etiquetas informativas: tipo de personal, habitación asignada, área.
    val etiqueta: ParColor = ParColor(Texto2, Superficie2, BordeSuave),
)

val LocalEstadoColors = staticCompositionLocalOf { EstadoColors() }
