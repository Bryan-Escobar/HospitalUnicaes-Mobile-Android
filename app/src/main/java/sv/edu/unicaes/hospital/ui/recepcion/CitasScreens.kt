@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.ui.components.Banner
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.BotonSecundario
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.ChipDesplegable
import sv.edu.unicaes.hospital.ui.components.ChipsFiltro
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.DialogoConfirmacion
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.Etiqueta
import sv.edu.unicaes.hospital.ui.components.FilaCita
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TipoBanner
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.AgendaDoctorUi
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.SolicitudCitaUi
import sv.edu.unicaes.hospital.ui.theme.BordeCampo
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.Primario100
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Primario900

/** AND-R5 · Citas del día agrupadas por doctor de turno. */
@Composable
fun CitasScreen(
    estado: UiState<List<AgendaDoctorUi>>,
    onNuevaCita: () -> Unit,
    onCita: () -> Unit,
    onReintentar: () -> Unit = {},
) {
    var dia by rememberSaveable { mutableIntStateOf(2) }
    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.tab_citas),
                acciones = { IconoAccion(R.drawable.ic_calendar, stringResource(R.string.accion_calendario), {}) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevaCita,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.citas_nueva)) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ChipsFiltro(opciones = DatosMuestra.diasSemana, seleccionada = dia, onSeleccion = { dia = it })
            Row(
                modifier =
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Espacio.md, vertical = Espacio.xxs),
                horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
            ) {
                ChipDesplegable(stringResource(R.string.citas_filtro_doctor))
                ChipDesplegable(stringResource(R.string.citas_filtro_especialidad))
                ChipDesplegable(stringResource(R.string.citas_filtro_estado))
            }
            ContenidoConEstado(
                estado = estado,
                mensajeError = stringResource(R.string.citas_error),
                onReintentar = onReintentar,
                vacio = {
                    EstadoVacio(
                        titulo = stringResource(R.string.citas_vacio_titulo),
                        detalle = stringResource(R.string.citas_vacio_detalle),
                        icono = R.drawable.ic_calendar,
                        accion = stringResource(R.string.citas_nueva),
                        onAccion = onNuevaCita,
                    )
                },
            ) { agendas ->
                LazyColumn(contentPadding = PaddingValues(bottom = Espacio.xxl * 2)) {
                    agendas.forEach { agenda ->
                        item {
                            TituloSeccion(
                                texto = "${agenda.doctor} · ${agenda.especialidad} · ${agenda.horario}",
                                modifier = Modifier.padding(horizontal = Espacio.md),
                            )
                        }
                        items(agenda.citas, key = { it.id }) { FilaCita(cita = it, onClick = onCita) }
                        items(agenda.horasLibres) { hora ->
                            FilaLista(
                                titulo = stringResource(R.string.citas_libre),
                                inicio = { Text(hora, style = MaterialTheme.typography.titleMedium) },
                                final = {
                                    TextButton(
                                        onClick = onNuevaCita,
                                    ) { Text(stringResource(R.string.citas_agendar)) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * AND-R6 · Crear / reprogramar cita. Solo doctores con turno en esa fecha y hora; el choque
 * de horario llega como error de la API y se muestra legible.
 */
@Composable
fun NuevaCitaScreen(
    onCerrar: () -> Unit,
    onGuardar: () -> Unit,
    solicitud: SolicitudCitaUi? = null,
    reprogramar: Boolean = false,
    errorChoque: Boolean = false,
) {
    var paciente by rememberSaveable { mutableStateOf(solicitud?.paciente?.let { "$it · 01234567-8" } ?: "") }
    var fecha by rememberSaveable { mutableStateOf("Jue 22 oct 2026") }
    var doctor by rememberSaveable { mutableStateOf("Dra. Ana Rivas · Alergista · 08:00–14:00") }
    var hora by rememberSaveable { mutableStateOf("10:30") }
    var motivo by rememberSaveable { mutableStateOf(solicitud?.motivo ?: "Control de alergia") }
    var confirmarCancelacion by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(if (reprogramar) R.string.citas_reprogramar else R.string.citas_nueva),
                onVolver = onCerrar,
                cerrar = true,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Espacio.md),
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            if (solicitud != null) {
                Banner(
                    tipo = TipoBanner.INFO,
                    texto = stringResource(R.string.citas_desde_solicitud, solicitud.paciente),
                )
            }
            CampoTexto(stringResource(R.string.campo_paciente), paciente, { paciente = it })
            CampoTexto(stringResource(R.string.campo_fecha), fecha, { fecha = it })
            CampoTexto(
                etiqueta = stringResource(R.string.campo_doctor_turno),
                valor = doctor,
                onValor = { doctor = it },
                ayuda = stringResource(R.string.citas_ayuda_doctor),
            )
            TituloSeccion(stringResource(R.string.campo_hora))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                DatosMuestra.horasCita.forEach { opcion ->
                    val ocupada = opcion in DatosMuestra.horasOcupadas
                    val elegida = opcion == hora
                    FilterChip(
                        selected = elegida,
                        enabled = !ocupada,
                        onClick = { hora = opcion },
                        label = {
                            Text(
                                when {
                                    elegida -> "✓ $opcion"
                                    ocupada -> stringResource(R.string.citas_hora_ocupada, opcion)
                                    else -> opcion
                                },
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primario100,
                                selectedLabelColor = Primario900,
                            ),
                        border =
                            FilterChipDefaults.filterChipBorder(
                                enabled = !ocupada,
                                selected = elegida,
                                borderColor = BordeCampo,
                                selectedBorderColor = Primario700,
                            ),
                    )
                }
            }
            CampoTexto(stringResource(R.string.campo_motivo), motivo, { motivo = it })
            if (errorChoque) Banner(tipo = TipoBanner.AVISO, texto = stringResource(R.string.citas_error_choque))
            BotonPrimario(
                texto = stringResource(R.string.citas_guardar),
                onClick = onGuardar,
                modifier = Modifier.padding(top = Espacio.sm),
            )
            if (reprogramar) {
                BotonSecundario(
                    texto = stringResource(R.string.citas_cancelar_cita),
                    onClick = { confirmarCancelacion = true },
                    destructivo = true,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }

    if (confirmarCancelacion) {
        DialogoConfirmacion(
            titulo = stringResource(R.string.citas_cancelar_titulo),
            texto = stringResource(R.string.citas_cancelar_texto),
            confirmar = stringResource(R.string.citas_cancelar_cita),
            onConfirmar = {
                confirmarCancelacion = false
                onCerrar()
            },
            onCancelar = { confirmarCancelacion = false },
            destructivo = true,
        )
    }
}

/** AND-R7 · Bandeja de solicitudes de citas que llegan desde la app iOS. */
@Composable
fun SolicitudesCitasScreen(
    estado: UiState<List<SolicitudCitaUi>>,
    onVolver: () -> Unit,
    onConvertir: (SolicitudCitaUi) -> Unit,
    onReintentar: () -> Unit = {},
) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var rechazando by rememberSaveable { mutableStateOf<String?>(null) }
    var motivoRechazo by rememberSaveable { mutableStateOf("") }
    val nuevas = (estado as? UiState.Exito)?.datos?.size ?: 0

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.inicio_solicitudes_citas),
                onVolver = onVolver,
                enVivo = true,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = pestana) {
                listOf(
                    stringResource(R.string.solicitudes_nuevas, nuevas),
                    stringResource(R.string.solicitudes_convertidas),
                    stringResource(R.string.solicitudes_rechazadas),
                ).forEachIndexed { indice, titulo ->
                    Tab(selected = pestana == indice, onClick = { pestana = indice }, text = { Text(titulo) })
                }
            }
            ContenidoConEstado(
                estado = if (pestana == 0) estado else UiState.Vacio,
                mensajeError = stringResource(R.string.solicitudes_error),
                onReintentar = onReintentar,
                vacio = {
                    EstadoVacio(
                        titulo = stringResource(R.string.solicitudes_vacio_titulo),
                        detalle = stringResource(R.string.solicitudes_vacio_detalle),
                        icono = R.drawable.ic_calendar,
                    )
                },
            ) { solicitudes ->
                LazyColumn(
                    contentPadding = PaddingValues(Espacio.md),
                    verticalArrangement = Arrangement.spacedBy(Espacio.sm),
                ) {
                    items(solicitudes, key = { it.id }) { solicitud ->
                        TarjetaSolicitudCita(
                            solicitud = solicitud,
                            onRechazar = { rechazando = solicitud.id },
                            onConvertir = { onConvertir(solicitud) },
                        )
                    }
                }
            }
        }
    }

    if (rechazando != null) {
        // El motivo es obligatorio: el paciente lo ve en su app.
        AlertDialog(
            onDismissRequest = { rechazando = null },
            title = { Text(stringResource(R.string.solicitudes_rechazar_titulo)) },
            text = {
                CampoTexto(
                    etiqueta = stringResource(R.string.solicitudes_motivo_rechazo),
                    valor = motivoRechazo,
                    onValor = { motivoRechazo = it },
                    ayuda = stringResource(R.string.solicitudes_motivo_ayuda),
                    singleLine = false,
                    minLines = 2,
                )
            },
            confirmButton = {
                TextButton(onClick = { rechazando = null }, enabled = motivoRechazo.isNotBlank()) {
                    Text(stringResource(R.string.accion_rechazar))
                }
            },
            dismissButton = {
                TextButton(onClick = { rechazando = null }) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}

@Composable
private fun TarjetaSolicitudCita(
    solicitud: SolicitudCitaUi,
    onRechazar: () -> Unit,
    onConvertir: () -> Unit,
) {
    TarjetaHospital {
        Text("${solicitud.paciente} · ${solicitud.hace}", style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.solicitudes_motivo, solicitud.motivo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
            modifier = Modifier.padding(vertical = Espacio.xxs),
        ) {
            Etiqueta(solicitud.especialidad)
            Etiqueta(solicitud.preferencia)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Espacio.xs, Alignment.End),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BotonSecundario(texto = stringResource(R.string.accion_rechazar), onClick = onRechazar)
            Button(onClick = onConvertir, modifier = Modifier.padding(start = Espacio.xxs)) {
                Text(stringResource(R.string.solicitudes_convertir))
            }
        }
    }
}

@PreviewPantalla
@Composable
private fun CitasPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        CitasScreen(estado = estado.con(DatosMuestra.agendasDelDia), onNuevaCita = {}, onCita = {})
    }
}

@PreviewPantalla
@Composable
private fun NuevaCitaPreview() {
    HospitalUnicaesTheme {
        NuevaCitaScreen(onCerrar = {}, onGuardar = {}, errorChoque = true)
    }
}

@PreviewPantalla
@Composable
private fun SolicitudesCitasPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        SolicitudesCitasScreen(estado = estado.con(DatosMuestra.solicitudesCitas), onVolver = {}, onConvertir = {})
    }
}
