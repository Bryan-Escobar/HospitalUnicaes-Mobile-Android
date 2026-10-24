@file:OptIn(ExperimentalMaterial3Api::class)

package sv.edu.unicaes.hospital.ui.doctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.ui.components.Avatar
import sv.edu.unicaes.hospital.ui.components.BadgeCita
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.BotonSecundario
import sv.edu.unicaes.hospital.ui.components.ChipsFiltro
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.FilaCita
import sv.edu.unicaes.hospital.ui.components.FilaTurno
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TarjetaTurno
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.components.iniciales
import sv.edu.unicaes.hospital.ui.modelo.AgendaUi
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.DetalleCitaUi
import sv.edu.unicaes.hospital.ui.modelo.TurnoUi
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

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
                acciones = { IconoAccion(R.drawable.ic_calendar, stringResource(R.string.accion_calendario), {}) },
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
            LazyColumn(contentPadding = PaddingValues(vertical = Espacio.md)) {
                agenda.turno?.let { turno ->
                    item {
                        TarjetaTurno(
                            titulo = stringResource(R.string.agenda_turno_en_curso),
                            turno = turno,
                            modifier = Modifier.padding(horizontal = Espacio.md),
                        )
                    }
                }
                item {
                    ChipsFiltro(
                        opciones = agenda.dias,
                        seleccionada = dia,
                        onSeleccion = { dia = it },
                        modifier = Modifier.padding(vertical = Espacio.sm),
                    )
                }
                // Se resalta la próxima cita programada.
                val proxima = agenda.citas.firstOrNull { it.estado == EstadoCita.PROGRAMADA }?.id
                items(agenda.citas, key = { it.id }) { cita ->
                    FilaCita(cita = cita, resaltada = cita.id == proxima, onClick = { onCita(cita.id) })
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
        bottomBar = {
            if (estado is UiState.Exito && estado.datos.cita.estado == EstadoCita.PROGRAMADA) {
                BotonPrimario(
                    texto = stringResource(R.string.cita_iniciar_consulta),
                    onClick = onIniciarConsulta,
                    modifier = Modifier.navigationBarsPadding().padding(Espacio.md),
                )
            }
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
                modifier = Modifier.fillMaxSize().padding(Espacio.md),
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Espacio.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(iniciales(detalle.cita.paciente), tamano = 56.dp)
                    Column {
                        Text(detalle.cita.paciente, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = stringResource(R.string.cita_edad_dui, detalle.edad, detalle.dui),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TarjetaHospital {
                    DatoCita(stringResource(R.string.campo_motivo), detalle.cita.motivo)
                    DatoCita(stringResource(R.string.campo_fecha), detalle.cita.fecha)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.citas_filtro_estado),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        BadgeCita(detalle.cita.estado)
                    }
                }
                Text(
                    text =
                        detalle.alergias?.let { stringResource(R.string.expediente_alergias_valor, it) }
                            ?: stringResource(R.string.cita_sin_alergias),
                    style = MaterialTheme.typography.bodyLarge,
                )
                BotonSecundario(
                    texto = stringResource(R.string.cita_ver_expediente),
                    onClick = onVerExpediente,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DatoCita(
    etiqueta: String,
    valor: String,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.xxs)) {
        Text(etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.titleMedium)
    }
}

/** AND-D7 · Mis turnos (solo lectura): esta semana y la siguiente. */
@Composable
fun MisTurnosScreen(
    estado: UiState<Pair<List<TurnoUi>, List<TurnoUi>>>,
    onReintentar: () -> Unit = {},
) {
    var semana by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(topBar = { TopBarHospital(titulo = stringResource(R.string.turnos_mis_turnos)) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = semana) {
                Tab(
                    selected = semana == 0,
                    onClick = { semana = 0 },
                    text = { Text(stringResource(R.string.turnos_esta_semana)) },
                )
                Tab(
                    selected = semana == 1,
                    onClick = { semana = 1 },
                    text = { Text(stringResource(R.string.turnos_siguiente)) },
                )
            }
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
                LazyColumn {
                    items(if (semana == 0) esta else siguiente) { FilaTurno(it) }
                }
            }
        }
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
