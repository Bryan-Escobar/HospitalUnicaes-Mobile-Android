package sv.edu.unicaes.hospital.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.domain.model.EstadoAmbulancia
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.ui.theme.AmbarBorde
import sv.edu.unicaes.hospital.ui.theme.AmbarFondo
import sv.edu.unicaes.hospital.ui.theme.AmbarTexto
import sv.edu.unicaes.hospital.ui.theme.Borde
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalTheme
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.Neutro700
import sv.edu.unicaes.hospital.ui.theme.ParColor
import sv.edu.unicaes.hospital.ui.theme.Primario200
import sv.edu.unicaes.hospital.ui.theme.Primario50
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Primario800
import sv.edu.unicaes.hospital.ui.theme.Superficie2

// `.badge`: 13/16 600, borde 1, píldora; el glifo va en 700.
private val estiloBadge = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
private val estiloBadgeGrande = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)

/** Píldora de estado: el estado nunca se comunica solo con color, siempre texto + glifo. */
@Composable
fun BadgeEstado(
    texto: String,
    colores: ParColor,
    modifier: Modifier = Modifier,
    glifo: String? = null,
    grande: Boolean = false,
) {
    val estilo = if (grande) estiloBadgeGrande else estiloBadge
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(colores.fondo)
                .border(1.dp, colores.borde, CircleShape)
                .then(
                    if (grande) {
                        Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    } else {
                        Modifier.padding(start = 8.dp, end = 9.dp, top = 3.dp, bottom = 3.dp)
                    },
                ).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glifo != null) Text(text = glifo, style = estilo.copy(fontWeight = FontWeight.Bold), color = colores.texto)
        Text(text = texto, style = estilo, color = colores.texto, maxLines = 1)
    }
}

/** Etiqueta neutra: tipo de personal, área o habitación asignada. */
@Composable
fun Etiqueta(
    texto: String,
    modifier: Modifier = Modifier,
) {
    BadgeEstado(texto = texto, colores = HospitalTheme.estados.etiqueta, modifier = modifier)
}

/**
 * Tipo de personal o área: Emergencia en rojo; médicos (Doctor, Alergista…) en teal;
 * el resto (Recepcionista, Enfermería…) neutro.
 */
@Composable
fun EtiquetaPersonal(
    tipo: String,
    modifier: Modifier = Modifier,
) {
    val estados = HospitalTheme.estados
    val colores =
        when (tipo) {
            "Emergencia" -> estados.personalEmergencia
            "Doctor", "Alergista", "General", "Médico" -> estados.personalMedico
            else -> estados.etiqueta
        }
    BadgeEstado(texto = tipo, colores = colores, modifier = modifier)
}

@Composable
fun BadgeLaboratorio(
    estado: EstadoLaboratorio,
    modifier: Modifier = Modifier,
    conteo: Int? = null,
) {
    val estados = HospitalTheme.estados
    val (colores, glifo, texto) =
        when (estado) {
            EstadoLaboratorio.PENDIENTE -> Triple(estados.labPendiente, "○", R.string.lab_pendiente)
            EstadoLaboratorio.EN_PROCESO -> Triple(estados.labEnProceso, "◐", R.string.lab_en_proceso)
            EstadoLaboratorio.COMPLETADO -> Triple(estados.labCompletado, "✓", R.string.lab_completado)
        }
    val etiqueta = stringResource(texto)
    BadgeEstado(
        texto = if (conteo != null) "$conteo $etiqueta" else etiqueta,
        colores = colores,
        glifo = glifo,
        modifier = modifier,
    )
}

@Composable
fun BadgeAmbulancia(
    estado: EstadoAmbulancia,
    modifier: Modifier = Modifier,
    grande: Boolean = false,
) {
    val estados = HospitalTheme.estados
    val (colores, glifo, texto) =
        when (estado) {
            EstadoAmbulancia.DISPONIBLE -> {
                Triple(estados.ambulanciaDisponible, "✓", R.string.ambulancia_disponible)
            }

            EstadoAmbulancia.EN_SERVICIO -> {
                Triple(estados.ambulanciaEnServicio, "→", R.string.ambulancia_en_servicio)
            }

            EstadoAmbulancia.EN_MANTENIMIENTO -> {
                Triple(estados.ambulanciaEnMantenimiento, "‖", R.string.ambulancia_en_mantenimiento)
            }
        }
    BadgeEstado(texto = stringResource(texto), colores = colores, glifo = glifo, modifier = modifier, grande = grande)
}

@Composable
fun BadgeCita(
    estado: EstadoCita,
    modifier: Modifier = Modifier,
) {
    val estados = HospitalTheme.estados
    val (colores, glifo, texto) =
        when (estado) {
            EstadoCita.PROGRAMADA -> Triple(estados.citaProgramada, null, R.string.cita_programada)
            EstadoCita.ATENDIDA -> Triple(estados.citaAtendida, "✓", R.string.cita_atendida)
            EstadoCita.CANCELADA -> Triple(estados.citaCancelada, "✕", R.string.cita_cancelada)
        }
    BadgeEstado(texto = stringResource(texto), colores = colores, glifo = glifo, modifier = modifier)
}

enum class EstadoConexion { EN_VIVO, RECONECTANDO, SIN_CONEXION }

/** Indicador de tiempo real (componente clave de la demo sincrónica). Chip M3: radio 8. */
@Composable
fun ChipEnVivo(
    modifier: Modifier = Modifier,
    estado: EstadoConexion = EstadoConexion.EN_VIVO,
) {
    val forma = RoundedCornerShape(8.dp)
    val (colores, texto) =
        when (estado) {
            EstadoConexion.EN_VIVO -> {
                ParColor(Primario800, Primario50, Primario200) to R.string.conexion_en_vivo
            }

            EstadoConexion.RECONECTANDO -> {
                ParColor(AmbarTexto, AmbarFondo, AmbarBorde) to
                    R.string.conexion_reconectando
            }

            EstadoConexion.SIN_CONEXION -> {
                ParColor(Neutro700, Superficie2, Borde) to R.string.conexion_sin_conexion
            }
        }
    Row(
        modifier =
            modifier
                .clip(forma)
                .background(colores.fondo)
                .border(1.dp, colores.borde, forma)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (estado) {
            EstadoConexion.EN_VIVO -> {
                Box(
                    modifier = Modifier.size(14.dp).clip(CircleShape).background(Primario200),
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(8.dp).clip(CircleShape).background(Primario700)) }
            }

            EstadoConexion.RECONECTANDO -> {
                Box(Modifier.size(8.dp).border(2.dp, AmbarTexto, CircleShape))
            }

            EstadoConexion.SIN_CONEXION -> {
                Text(text = "✕", style = estiloBadge, color = colores.texto)
            }
        }
        Text(
            text = stringResource(texto),
            style = estiloBadge.copy(fontWeight = FontWeight.Medium),
            color = colores.texto,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BadgesPreview() {
    HospitalUnicaesTheme {
        Column(
            modifier = Modifier.padding(Espacio.md),
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            EstadoLaboratorio.entries.forEach { BadgeLaboratorio(it) }
            EstadoAmbulancia.entries.forEach { BadgeAmbulancia(it) }
            EstadoCita.entries.forEach { BadgeCita(it) }
            Etiqueta("Alergista")
            EstadoConexion.entries.forEach { ChipEnVivo(estado = it) }
        }
    }
}
