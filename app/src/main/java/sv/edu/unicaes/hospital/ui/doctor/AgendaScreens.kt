@file:OptIn(ExperimentalMaterial3Api::class)

package sv.edu.unicaes.hospital.ui.doctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.ui.components.BadgeCita
import sv.edu.unicaes.hospital.ui.components.BadgeEstado
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.BotonSecundario
import sv.edu.unicaes.hospital.ui.components.CabeceraPersona
import sv.edu.unicaes.hospital.ui.components.ChipsFiltro
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.EstiloFilaSoporte
import sv.edu.unicaes.hospital.ui.components.FilaCita
import sv.edu.unicaes.hospital.ui.components.FilaDato
import sv.edu.unicaes.hospital.ui.components.FilaTurno
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.PestanasHospital
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TarjetaLista
import sv.edu.unicaes.hospital.ui.components.TarjetaTurno
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.AgendaUi
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.DetalleCitaUi
import sv.edu.unicaes.hospital.ui.modelo.TurnoUi
import sv.edu.unicaes.hospital.ui.recepcion.RellenoCuerpo
import sv.edu.unicaes.hospital.ui.theme.AmbarTexto
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalTheme
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.Neutro700
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Texto2
import sv.edu.unicaes.hospital.ui.theme.VerdeTexto

/** AND-D1 · Agenda (destino inicial del Doctor): turno en curso y citas del día elegido. */
@Composable
fun AgendaScreen(
    estado: UiState<AgendaUi>,
    onCita: (String) -> Unit,
    onReintentar: () -> Unit = {},
) {
    var dia by rememberSaveable { mutableIntStateOf(2) }
    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.tab_agenda),
                enVivo = true,
                acciones = {
                    IconoAccion(
                        R.drawable.ic_calendar,
                        stringResource(R.string.accion_calendario),
                        {},
                        tinte = Neutro700,
                    )
                },
            )
        },
    ) { padding ->
        ContenidoConEstado(
            estado = estado,
            mensajeError = stringResource(R.string.citas_error),
            onReintentar = onReintentar,
            modifier = Modifier.padding(padding),
            vacio = {
                EstadoVacio(
                    titulo = stringResource(R.string.agenda_vacio_titulo),
                    detalle = stringResource(R.string.agenda_vacio_detalle),
                    icono = R.drawable.ic_calendar,
                    accion = stringResource(R.string.agenda_ir_hoy),
                    onAccion = { dia = 2 },
                )
            },
        ) { agenda ->
            LazyColumn(contentPadding = RellenoCuerpo, verticalArrangement = Arrangement.spacedBy(Espacio.sm)) {
                agenda.turno?.let { turno ->
                    item { TarjetaTurno(titulo = stringResource(R.string.agenda_turno_en_curso), turno = turno) }
                }
                item { ChipsFiltro(opciones = agenda.dias, seleccionada = dia, onSeleccion = { dia = it }) }
                // Se resalta la próxima cita programada.
                val proxima = agenda.citas.firstOrNull { it.estado == EstadoCita.PROGRAMADA }?.id
                item {
                    TarjetaLista(agenda.citas) { cita, ultimo ->
                        FilaCita(
                            cita = cita,
                            resaltada = cita.id == proxima,
                            divisor = !ultimo,
                            onClick = { onCita(cita.id) },
                        )
                    }
                }
            }
        }
    }
}

/** AND-D2 · Detalle de cita: datos básicos, alergias y acceso a la consulta. */
@Composable
fun DetalleCitaScreen(
    estado: UiState<DetalleCitaUi>,
    onVolver: () -> Unit,
    onVerExpediente: () -> Unit,
    onIniciarConsulta: () -> Unit,
    onReintentar: () -> Unit = {},
) {
    val hora = (estado as? UiState.Exito)?.datos?.cita?.hora
    Scaffold(
        topBar = {
            TopBarHospital(
                titulo =
                    hora?.let { stringResource(R.string.cita_titulo, it) }
                        ?: stringResource(R.string.cita_titulo_simple),
                onVolver = onVolver,
            )
        },
    ) { padding ->
        ContenidoConEstado(
            estado = estado,
            mensajeError = stringResource(R.string.cita_error),
            onReintentar = onReintentar,
            modifier = Modifier.padding(padding),
            vacio = {
                EstadoVacio(
                    titulo = stringResource(R.string.cita_no_disponible_titulo),
                    detalle = stringResource(R.string.cita_no_disponible_detalle),
                    accion = stringResource(R.string.cita_volver_agenda),
                    onAccion = onVolver,
                )
            },
        ) { detalle ->
            Column(
                modifier = Modifier.fillMaxSize().padding(RellenoCuerpo),
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                CabeceraPersona(
                    nombre = detalle.cita.paciente,
                    soporte = stringResource(R.string.cita_edad_dui, detalle.edad, detalle.dui),
                )
                TarjetaHospital {
                    FilaDato(stringResource(R.string.campo_motivo), detalle.cita.motivo)
                    FilaDato(stringResource(R.string.campo_fecha_simple), detalle.cita.fecha)
                    FilaDatoConBadge(stringResource(R.string.citas_filtro_estado)) { BadgeCita(detalle.cita.estado) }
                }
                TarjetaHospital {
                    val alergias = detalle.alergias
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    if (alergias ==
                                        null
                                    ) {
                                        R.drawable.ic_check
                                    } else {
                                        R.drawable.ic_alerta
                                    },
                                ),
                            contentDescription = null,
                            tint = if (alergias == null) VerdeTexto else AmbarTexto,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text =
                                alergias?.let { stringResource(R.string.expediente_alergias_valor, it) }
                                    ?: stringResource(R.string.cita_sin_alergias),
                            style = EstiloFilaSoporte,
                        )
                    }
                }
                BotonSecundario(
                    texto = stringResource(R.string.cita_ver_expediente),
                    onClick = onVerExpediente,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.weight(1f))
                if (detalle.cita.estado == EstadoCita.PROGRAMADA) {
                    BotonPrimario(texto = stringResource(R.string.cita_iniciar_consulta), onClick = onIniciarConsulta)
                }
            }
        }
    }
}

/** Como [FilaDato] (`.kv`), pero con un badge como valor. */
@Composable
private fun FilaDatoConBadge(
    etiqueta: String,
    badge: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = etiqueta,
            style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
            color = Texto2,
            modifier = Modifier.weight(1f),
        )
        badge()
    }
}

/** AND-D7 · Mis turnos (solo lectura): esta semana y la siguiente; el turno en curso va aparte. */
@Composable
fun MisTurnosScreen(
    estado: UiState<Pair<List<TurnoUi>, List<TurnoUi>>>,
    onReintentar: () -> Unit = {},
) {
    var semana by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(topBar = { TopBarHospital(titulo = stringResource(R.string.turnos_mis_turnos)) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(RellenoCuerpo),
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            PestanasHospital(
                titulos =
                    listOf(
                        stringResource(R.string.turnos_esta_semana),
                        stringResource(R.string.turnos_siguiente),
                    ),
                seleccionada = semana,
                onSeleccion = { semana = it },
            )
            ContenidoConEstado(
                estado = estado,
                mensajeError = stringResource(R.string.turnos_error),
                onReintentar = onReintentar,
                vacio = {
                    EstadoVacio(
                        titulo = stringResource(R.string.turnos_vacio_semana_titulo),
                        detalle = stringResource(R.string.turnos_vacio_semana_detalle),
                        icono = R.drawable.ic_schedule,
                        accion = stringResource(R.string.turnos_semana_siguiente),
                        onAccion = { semana = 1 },
                    )
                },
            ) { (esta, siguiente) ->
                val turnos = if (semana == 0) esta else siguiente
                val enCurso = turnos.firstOrNull { it.enCurso }
                val resto = turnos.filterNot { it.enCurso }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Espacio.sm)) {
                    enCurso?.let { turno -> item { TarjetaTurnoEnCurso(turno) } }
                    if (resto.isNotEmpty()) {
                        item {
                            TarjetaLista(resto) { turno, ultimo -> FilaTurno(turno, divisor = !ultimo) }
                        }
                    }
                }
            }
        }
    }
}

/** Turno en curso de "Mis turnos": "Mié 21 · 06:00–18:00" 16/24 500, badge "● En curso" y área. */
@Composable
private fun TarjetaTurnoEnCurso(turno: TurnoUi) {
    TarjetaHospital(colorBorde = Primario700, anchoBorde = 2.5.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${turno.dia} · ${turno.horario}",
                style = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
            )
            BadgeEstado(
                texto = stringResource(R.string.turno_en_curso),
                colores = HospitalTheme.estados.turnoEnCurso,
                glifo = "●",
            )
        }
        Text(text = turno.area, style = EstiloFilaSoporte, color = Texto2)
    }
}

@PreviewPantalla
@Composable
private fun AgendaPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme { AgendaScreen(estado = estado.con(DatosMuestra.agendaDoctor), onCita = {}) }
}

@PreviewPantalla
@Composable
private fun DetalleCitaPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        DetalleCitaScreen(
            estado = estado.con(DatosMuestra.detalleCita),
            onVolver = {},
            onVerExpediente = {},
            onIniciarConsulta = {},
        )
    }
}

@PreviewPantalla
@Composable
private fun MisTurnosPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        MisTurnosScreen(estado = estado.con(DatosMuestra.turnosDoctorSemana to DatosMuestra.turnosDoctorSiguiente))
    }
}
