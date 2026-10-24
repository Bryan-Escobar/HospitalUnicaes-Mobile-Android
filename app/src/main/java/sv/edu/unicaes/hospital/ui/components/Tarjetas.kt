package sv.edu.unicaes.hospital.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion
import sv.edu.unicaes.hospital.ui.modelo.CitaUi
import sv.edu.unicaes.hospital.ui.modelo.OcupacionUi
import sv.edu.unicaes.hospital.ui.modelo.TurnoUi
import sv.edu.unicaes.hospital.ui.theme.BordeSuave
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalTheme
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Primario800

// Tarjetas y filas que se repiten entre Recepción y Doctor.

/** "Mi turno" / "Turno en curso": borde teal de 2,5 cuando el turno está en curso. */
@Composable
fun TarjetaTurno(
    titulo: String,
    turno: TurnoUi,
    modifier: Modifier = Modifier,
) {
    TarjetaHospital(
        modifier = modifier,
        colorBorde = if (turno.enCurso) Primario700 else BordeSuave,
        anchoBorde = if (turno.enCurso) 2.5.dp else 1.dp,
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            Text(
                text = "${turno.horario} · ${turno.area}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            turno.tipo?.let { Etiqueta(it) }
        }
    }
}

/** Entrada de la bandeja de Inicio: título, detalle y badge con el conteo. */
@Composable
fun TarjetaBandeja(
    titulo: String,
    detalle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prioritaria: Boolean = false,
    badge: @Composable () -> Unit,
) {
    TarjetaHospital(
        modifier = modifier,
        onClick = onClick,
        colorBorde = if (prioritaria) MaterialTheme.colorScheme.error else BordeSuave,
        anchoBorde = if (prioritaria) 2.5.dp else 1.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            badge()
            Chevron()
        }
    }
}

/** Ocupación agregada por planta: "Planta 2 · semiprivadas 9/12 — 3 libres" con barra. */
@Composable
fun TarjetaOcupacion(
    ocupacion: OcupacionUi,
    modifier: Modifier = Modifier,
) {
    val nombre =
        stringResource(
            if (ocupacion.tipo == TipoHabitacion.VIP) R.string.ocupacion_vip else R.string.ocupacion_semiprivadas,
        )
    TarjetaHospital(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.ocupacion_planta, ocupacion.tipo.planta, nombre),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${ocupacion.ocupadas}/${ocupacion.total}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        LinearProgressIndicator(
            progress = { ocupacion.ocupadas.toFloat() / ocupacion.total },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Espacio.xxs)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            trackColor = BordeSuave,
            drawStopIndicator = {},
            gapSize = 0.dp,
        )
        Text(
            text = stringResource(R.string.ocupacion_libres, ocupacion.libres),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Fila de agenda: hora a la izquierda, paciente, motivo (o doctor) y estado. */
@Composable
fun FilaCita(
    cita: CitaUi,
    modifier: Modifier = Modifier,
    mostrarDoctor: Boolean = false,
    resaltada: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    FilaLista(
        titulo = cita.paciente,
        soporte = if (mostrarDoctor) "${cita.doctor} · ${cita.motivo}" else cita.motivo,
        modifier = modifier,
        onClick = onClick,
        resaltada = resaltada,
        inicio = {
            Text(
                text = cita.hora,
                style = MaterialTheme.typography.titleMedium,
                color = Primario800,
                modifier = Modifier.width(52.dp),
            )
        },
        final = { BadgeCita(cita.estado) },
    )
}

/** Fila de turno de la semana (Doctor) con estado En curso / Terminado / Sin turno. */
@Composable
fun FilaTurno(
    turno: TurnoUi,
    modifier: Modifier = Modifier,
) {
    if (turno.sinTurno) {
        FilaLista(titulo = turno.dia, soporte = stringResource(R.string.turno_sin_turno), modifier = modifier)
        return
    }
    FilaLista(
        titulo = "${turno.dia} · ${turno.horario}",
        soporte = if (turno.terminado) "${turno.area} — ${stringResource(R.string.turno_terminado)}" else turno.area,
        modifier = modifier,
        resaltada = turno.enCurso,
        final = {
            when {
                turno.enCurso -> {
                    BadgeEstado(
                        texto = stringResource(R.string.turno_en_curso),
                        colores = HospitalTheme.estados.turnoEnCurso,
                        glifo = "●",
                    )
                }

                turno.tipo != null -> {
                    Etiqueta(turno.tipo)
                }
            }
        },
    )
}
