package sv.edu.unicaes.hospital.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion
import sv.edu.unicaes.hospital.ui.modelo.CitaUi
import sv.edu.unicaes.hospital.ui.modelo.OcupacionUi
import sv.edu.unicaes.hospital.ui.modelo.TurnoUi
import sv.edu.unicaes.hospital.ui.theme.BordeSuave
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalTheme
import sv.edu.unicaes.hospital.ui.theme.Primario100
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Primario800
import sv.edu.unicaes.hospital.ui.theme.Texto2

// Tarjetas y filas que se repiten entre Recepción y Doctor.

/** Texto gris a la derecha de una fila ("3 libres", "Terminado"). */
@Composable
fun TextoSecundario(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Text(text = texto, style = TextStyle(fontSize = 16.sp, lineHeight = 22.sp), color = Texto2, modifier = modifier)
}

/** "Mi turno" / "Turno en curso": etiqueta arriba, horario · área y tipo; borde teal 2,5 en curso. */
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            TextosFila(
                titulo = "${turno.horario} · ${turno.area}",
                soporte = titulo,
                etiquetaArriba = true,
                modifier = Modifier.weight(1f),
            )
            turno.tipo?.let { EtiquetaPersonal(it) }
        }
    }
}

/** Círculo de 40 con icono (`.avatar` de la bandeja): teal por defecto, rojo en lo urgente. */
@Composable
fun AvatarIcono(
    @DrawableRes icono: Int,
    modifier: Modifier = Modifier,
    fondo: Color = Primario100,
    tinte: Color = Primario800,
) {
    Box(
        modifier = modifier.size(40.dp).clip(CircleShape).background(fondo),
        contentAlignment = Alignment.Center,
    ) { Icon(painterResource(icono), contentDescription = null, tint = tinte, modifier = Modifier.size(22.dp)) }
}

/** Entrada de la bandeja de Inicio: icono, título, detalle, badge y chevron. */
@Composable
fun TarjetaBandeja(
    titulo: String,
    detalle: String,
    @DrawableRes icono: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prioritaria: Boolean = false,
    badge: @Composable () -> Unit,
) {
    val error = MaterialTheme.colorScheme.error
    TarjetaLista(
        elementos = listOf(Unit),
        modifier = modifier,
        colorBorde = if (prioritaria) error else BordeSuave,
        anchoBorde = if (prioritaria) 2.5.dp else 1.dp,
    ) { _, _ ->
        FilaLista(
            titulo = titulo,
            soporte = detalle,
            onClick = onClick,
            divisor = false,
            enTarjeta = true,
            inicio = {
                if (prioritaria) {
                    AvatarIcono(icono, fondo = HospitalTheme.estados.ambulanciaEnServicio.fondo, tinte = error)
                } else {
                    AvatarIcono(icono)
                }
            },
            final = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
                ) {
                    badge()
                    Chevron()
                }
            },
        )
    }
}

/** Ocupación de todas las plantas en una tarjeta: título, "9/12 camas ocupadas", libres y barra de 4. */
@Composable
fun TarjetaOcupacion(
    ocupacion: List<OcupacionUi>,
    modifier: Modifier = Modifier,
) {
    TarjetaHospital(modifier = modifier) {
        ocupacion.forEach { planta ->
            val nombre =
                stringResource(
                    if (planta.tipo == TipoHabitacion.VIP) R.string.ocupacion_vip else R.string.ocupacion_semiprivadas,
                )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                TextosFila(
                    titulo = stringResource(R.string.ocupacion_planta, planta.tipo.planta, nombre),
                    soporte = stringResource(R.string.ocupacion_ocupadas, planta.ocupadas, planta.total),
                    modifier = Modifier.weight(1f),
                )
                TextoSecundario(stringResource(R.string.ocupacion_libres, planta.libres))
            }
            LinearProgressIndicator(
                progress = { planta.ocupadas.toFloat() / planta.total },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                trackColor = BordeSuave,
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
        }
    }
}

/** Fila de agenda dentro de [TarjetaLista]: "10:30 Luis Martínez", motivo (o doctor) y estado. */
@Composable
fun FilaCita(
    cita: CitaUi,
    modifier: Modifier = Modifier,
    mostrarDoctor: Boolean = false,
    resaltada: Boolean = false,
    divisor: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    FilaLista(
        titulo = "${cita.hora} ${cita.paciente}",
        soporte = if (mostrarDoctor) cita.doctor else cita.motivo,
        modifier = modifier,
        onClick = onClick,
        resaltada = resaltada,
        divisor = divisor,
        enTarjeta = true,
        final = { BadgeCita(cita.estado) },
    )
}

/** Fila de turno de la semana dentro de [TarjetaLista]: Terminado en gris, Sin turno sin badge. */
@Composable
fun FilaTurno(
    turno: TurnoUi,
    modifier: Modifier = Modifier,
    divisor: Boolean = true,
) {
    if (turno.sinTurno) {
        FilaLista(
            titulo = turno.dia,
            soporte = stringResource(R.string.turno_sin_turno),
            modifier = modifier,
            divisor = divisor,
            enTarjeta = true,
        )
        return
    }
    FilaLista(
        titulo = "${turno.dia} · ${turno.horario}",
        soporte = turno.area,
        modifier = modifier,
        divisor = divisor,
        enTarjeta = true,
        final = {
            when {
                turno.terminado -> TextoSecundario(stringResource(R.string.turno_terminado))
                turno.tipo != null -> EtiquetaPersonal(turno.tipo)
            }
        },
    )
}
