@file:OptIn(ExperimentalMaterial3Api::class)

package sv.edu.unicaes.hospital.ui.doctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.ui.components.BadgeLaboratorio
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.Chevron
import sv.edu.unicaes.hospital.ui.components.ChipsFiltro
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.Etiqueta
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.ExpedienteUi
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import sv.edu.unicaes.hospital.ui.recepcion.FilaPaciente
import sv.edu.unicaes.hospital.ui.theme.AmbarFondo
import sv.edu.unicaes.hospital.ui.theme.AmbarTexto
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

/** AND-D3 · Mis pacientes: solo los que el doctor atiende (médico tratante). */
@Composable
fun MisPacientesScreen(
    estado: UiState<List<PacienteUi>>,
    onPaciente: (PacienteUi) -> Unit,
    onReintentar: () -> Unit = {},
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(topBar = { TopBarHospital(titulo = stringResource(R.string.doctor_mis_pacientes)) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Barra de búsqueda M3: alto 56, radio 28.
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text(stringResource(R.string.doctor_buscar_paciente)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.padding(horizontal = Espacio.md).fillMaxWidth(),
            )
            ChipsFiltro(
                opciones =
                    listOf(
                        stringResource(R.string.filtro_todos),
                        stringResource(R.string.filtro_hospitalizados),
                    ),
                seleccionada = filtro,
                onSeleccion = { filtro = it },
                modifier = Modifier.padding(vertical = Espacio.xs),
            )
            ContenidoConEstado(
                estado = estado,
                mensajeError = stringResource(R.string.error_cargar_lista),
                onReintentar = onReintentar,
                vacio = {
                    EstadoVacio(
                        titulo = stringResource(R.string.doctor_pacientes_vacio_titulo),
                        detalle = stringResource(R.string.doctor_pacientes_vacio_detalle),
                        icono = R.drawable.ic_group,
                    )
                },
            ) { pacientes ->
                val visibles =
                    pacientes.filter {
                        (busqueda.isBlank() || it.nombre.contains(busqueda, true)) && (filtro == 0 || it.hospitalizado)
                    }
                LazyColumn {
                    items(visibles, key = { it.id }) { paciente ->
                        FilaPaciente(
                            paciente = paciente,
                            soporte = paciente.ultimaVisita.orEmpty(),
                            onClick = { onPaciente(paciente) },
                        )
                    }
                }
            }
        }
    }
}

/** AND-D4 · Expediente (solo médico tratante, paciente y administración). */
@Composable
fun ExpedienteScreen(
    estado: UiState<ExpedienteUi>,
    onVolver: () -> Unit,
    onNuevaConsulta: () -> Unit,
    onReintentar: () -> Unit = {},
) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val nombre = (estado as? UiState.Exito)?.datos?.paciente?.nombre

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = nombre ?: stringResource(R.string.expediente_titulo),
                onVolver = onVolver,
                enVivo = true,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevaConsulta,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.consulta_nueva)) },
            )
        },
    ) { padding ->
        ContenidoConEstado(
            estado = estado,
            mensajeError = stringResource(R.string.expediente_error),
            onReintentar = onReintentar,
            modifier = Modifier.padding(padding),
            vacio = {
                EstadoVacio(
                    titulo = stringResource(R.string.expediente_vacio_titulo),
                    detalle = stringResource(R.string.expediente_vacio_detalle),
                )
            },
        ) { expediente ->
            Column {
                PrimaryTabRow(selectedTabIndex = pestana) {
                    listOf(
                        R.string.expediente_consultas,
                        R.string.mas_laboratorio,
                        R.string.expediente_datos,
                    ).forEachIndexed { indice, titulo ->
                        Tab(
                            selected = pestana == indice,
                            onClick = { pestana = indice },
                            text = { Text(stringResource(titulo)) },
                        )
                    }
                }
                LazyColumn(
                    contentPadding =
                        PaddingValues(
                            start = Espacio.md,
                            end = Espacio.md,
                            top = Espacio.md,
                            bottom = 96.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(Espacio.sm),
                ) {
                    expediente.alergias?.let { alergias ->
                        // Tarjeta prioritaria: borde #92400E sobre #FEF3C7.
                        item {
                            TarjetaHospital(colorBorde = AmbarTexto, colorFondo = AmbarFondo, anchoBorde = 2.5.dp) {
                                Text(
                                    text = "⚠ ${stringResource(R.string.expediente_alergias)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmbarTexto,
                                )
                                Text(alergias, style = MaterialTheme.typography.titleMedium, color = AmbarTexto)
                            }
                        }
                    }
                    when (pestana) {
                        0 -> {
                            expediente.hospitalizacion?.let { hospitalizacion ->
                                item {
                                    TarjetaHospital {
                                        Text(
                                            stringResource(R.string.expediente_hospitalizacion),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        Text(hospitalizacion, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                            item { TituloSeccion(stringResource(R.string.expediente_consultas)) }
                            items(expediente.consultas) { consulta ->
                                FilaLista(
                                    titulo = "${consulta.fecha} · ${consulta.diagnostico}",
                                    soporte =
                                        if (consulta.recetas + consulta.ordenes > 0) {
                                            stringResource(
                                                R.string.expediente_consulta_resumen,
                                                consulta.doctor,
                                                consulta.recetas,
                                                consulta.ordenes,
                                            )
                                        } else {
                                            consulta.doctor
                                        },
                                    final = { Chevron() },
                                    onClick = {},
                                )
                            }
                            item { TituloSeccion(stringResource(R.string.expediente_lab_preview)) }
                            items(expediente.laboratorio, key = { it.id }) { orden ->
                                FilaLista(
                                    titulo = "${orden.examen} · ${orden.hora}",
                                    final = { BadgeLaboratorio(orden.estado) },
                                )
                            }
                        }

                        1 -> {
                            items(expediente.laboratorio, key = { it.id }) { orden ->
                                FilaLista(
                                    titulo = orden.examen,
                                    soporte = "${orden.doctor} · ${orden.hora}",
                                    final = { BadgeLaboratorio(orden.estado) },
                                )
                            }
                        }

                        else -> {
                            item {
                                TarjetaHospital {
                                    Text(expediente.paciente.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        stringResource(
                                            R.string.ficha_cabecera,
                                            expediente.paciente.dui,
                                            expediente.edad,
                                            expediente.telefono,
                                        ),
                                    )
                                    Text(
                                        text = "${stringResource(
                                            R.string.campo_contacto_emergencia,
                                        )}: ${expediente.contactoEmergencia}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class OrdenNuevaUi(
    val examen: String,
    val indicaciones: String,
)

data class RecetaNuevaUi(
    val medicamento: String,
    val dosis: String,
    val frecuencia: String,
)

/** AND-D5 · Nueva consulta: diagnóstico, notas, recetas y órdenes de laboratorio. */
@Composable
fun NuevaConsultaScreen(
    paciente: String,
    contexto: String,
    onCerrar: () -> Unit,
    onGuardar: () -> Unit,
    ordenesIniciales: List<OrdenNuevaUi> = emptyList(),
    hojaOrdenAbierta: Boolean = false,
) {
    var diagnostico by rememberSaveable { mutableStateOf("") }
    var notas by rememberSaveable { mutableStateOf("") }
    var intentoGuardar by rememberSaveable { mutableStateOf(false) }
    var hojaOrden by rememberSaveable { mutableStateOf(hojaOrdenAbierta) }
    var hojaReceta by rememberSaveable { mutableStateOf(false) }
    val ordenes = remember { mutableStateListOf<OrdenNuevaUi>().apply { addAll(ordenesIniciales) } }
    val recetas = remember { mutableStateListOf<RecetaNuevaUi>() }

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.consulta_nueva),
                onVolver = onCerrar,
                cerrar = true,
                acciones = { TextButton(onClick = onGuardar) { Text(stringResource(R.string.accion_guardar)) } },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(Espacio.md),
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            Text("$paciente · $contexto", style = MaterialTheme.typography.titleMedium)
            CampoTexto(
                etiqueta = stringResource(R.string.consulta_diagnostico),
                valor = diagnostico,
                onValor = { diagnostico = it },
                error =
                    if (intentoGuardar &&
                        diagnostico.isBlank()
                    ) {
                        stringResource(R.string.campo_obligatorio)
                    } else {
                        null
                    },
                ayuda = stringResource(R.string.campo_obligatorio),
            )
            CampoTexto(
                etiqueta = stringResource(R.string.consulta_notas),
                valor = notas,
                onValor = { notas = it },
                ayuda = stringResource(R.string.consulta_notas_ayuda),
                singleLine = false,
                minLines = 3,
            )
            TituloSeccion(
                texto = stringResource(R.string.consulta_recetas, recetas.size),
                accion = stringResource(R.string.consulta_agregar_receta),
                onAccion = { hojaReceta = true },
            )
            if (recetas.isEmpty()) {
                Text(stringResource(R.string.consulta_sin_recetas), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            recetas.forEach { receta ->
                FilaLista(
                    titulo = receta.medicamento,
                    soporte = "${receta.dosis} · ${receta.frecuencia}",
                    divisor = false,
                )
            }
            TituloSeccion(
                texto = stringResource(R.string.consulta_ordenes, ordenes.size),
                accion = stringResource(R.string.consulta_agregar_orden),
                onAccion = { hojaOrden = true },
            )
            ordenes.forEach { orden ->
                FilaLista(
                    titulo = orden.examen,
                    soporte = orden.indicaciones,
                    divisor = false,
                    final = { BadgeLaboratorio(EstadoLaboratorio.PENDIENTE) },
                )
            }
            BotonPrimario(
                texto = stringResource(R.string.consulta_guardar),
                onClick = {
                    intentoGuardar = true
                    if (diagnostico.isNotBlank()) onGuardar()
                },
                modifier = Modifier.padding(top = Espacio.sm),
            )
        }
    }

    if (hojaOrden) {
        HojaOrdenLaboratorio(
            onCerrar = { hojaOrden = false },
            onAgregar = {
                ordenes += it
                hojaOrden = false
            },
        )
    }
    if (hojaReceta) {
        HojaReceta(
            onCerrar = { hojaReceta = false },
            onAgregar = {
                recetas += it
                hojaReceta = false
            },
        )
    }
}

/** AND-D6 · Orden de laboratorio (ModalBottomSheet sobre AND-D5). Toda orden nace Pendiente. */
@Composable
private fun HojaOrdenLaboratorio(
    onCerrar: () -> Unit,
    onAgregar: (OrdenNuevaUi) -> Unit,
) {
    var examen by rememberSaveable { mutableStateOf(DatosMuestra.tiposExamen.first()) }
    var indicaciones by rememberSaveable { mutableStateOf("Ayuno de 8 horas") }
    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(
            modifier = Modifier.padding(horizontal = Espacio.md).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            Text(stringResource(R.string.orden_titulo), style = MaterialTheme.typography.titleLarge)
            CampoTexto(stringResource(R.string.orden_tipo_examen), examen, { examen = it })
            CampoTexto(stringResource(R.string.orden_indicaciones), indicaciones, { indicaciones = it })
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
            ) {
                Text(stringResource(R.string.orden_se_crea_en))
                BadgeLaboratorio(EstadoLaboratorio.PENDIENTE)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.sm),
                horizontalArrangement = Arrangement.spacedBy(Espacio.xs, Alignment.End),
            ) {
                TextButton(onClick = onCerrar) { Text(stringResource(R.string.accion_cancelar)) }
                Button(onClick = { onAgregar(OrdenNuevaUi(examen, indicaciones)) }, enabled = examen.isNotBlank()) {
                    Text(stringResource(R.string.orden_agregar))
                }
            }
        }
    }
}

@Composable
private fun HojaReceta(
    onCerrar: () -> Unit,
    onAgregar: (RecetaNuevaUi) -> Unit,
) {
    var medicamento by rememberSaveable { mutableStateOf("") }
    var dosis by rememberSaveable { mutableStateOf("") }
    var frecuencia by rememberSaveable { mutableStateOf("") }
    var indicaciones by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(
            modifier = Modifier.padding(horizontal = Espacio.md).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            Text(stringResource(R.string.receta_titulo), style = MaterialTheme.typography.titleLarge)
            CampoTexto(stringResource(R.string.receta_medicamento), medicamento, { medicamento = it })
            CampoTexto(stringResource(R.string.receta_dosis), dosis, { dosis = it })
            CampoTexto(stringResource(R.string.receta_frecuencia), frecuencia, { frecuencia = it })
            CampoTexto(stringResource(R.string.orden_indicaciones), indicaciones, { indicaciones = it })
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.sm),
                horizontalArrangement = Arrangement.spacedBy(Espacio.xs, Alignment.End),
            ) {
                TextButton(onClick = onCerrar) { Text(stringResource(R.string.accion_cancelar)) }
                Button(
                    onClick = { onAgregar(RecetaNuevaUi(medicamento, dosis, frecuencia)) },
                    enabled = medicamento.isNotBlank(),
                ) { Text(stringResource(R.string.receta_agregar)) }
            }
        }
    }
}

@PreviewPantalla
@Composable
private fun MisPacientesPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme { MisPacientesScreen(estado = estado.con(DatosMuestra.misPacientes), onPaciente = {}) }
}

@PreviewPantalla
@Composable
private fun ExpedientePreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        ExpedienteScreen(estado = estado.con(DatosMuestra.expedienteTomas), onVolver = {}, onNuevaConsulta = {})
    }
}

@PreviewPantalla
@Composable
private fun NuevaConsultaPreview() {
    HospitalUnicaesTheme {
        NuevaConsultaScreen(
            paciente = "María López",
            contexto = "Cita 10:00",
            onCerrar = {},
            onGuardar = {},
            ordenesIniciales = listOf(OrdenNuevaUi("Química sanguínea", "Ayuno de 8 h")),
        )
    }
}
