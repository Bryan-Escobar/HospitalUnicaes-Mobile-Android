@file:OptIn(ExperimentalMaterial3Api::class)

package sv.edu.unicaes.hospital.ui.doctor

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.ui.components.AccionTexto
import sv.edu.unicaes.hospital.ui.components.BadgeLaboratorio
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.Chevron
import sv.edu.unicaes.hospital.ui.components.ChipsFiltro
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.EstiloFilaSoporte
import sv.edu.unicaes.hospital.ui.components.EstiloFilaTitulo
import sv.edu.unicaes.hospital.ui.components.FabHospital
import sv.edu.unicaes.hospital.ui.components.FilaDato
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.PestanasHospital
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TarjetaLista
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.ExpedienteUi
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import sv.edu.unicaes.hospital.ui.recepcion.EstiloTituloTarjeta
import sv.edu.unicaes.hospital.ui.recepcion.FilaPaciente
import sv.edu.unicaes.hospital.ui.recepcion.IconoDesplegable
import sv.edu.unicaes.hospital.ui.recepcion.RellenoCuerpo
import sv.edu.unicaes.hospital.ui.recepcion.RellenoCuerpoConFab
import sv.edu.unicaes.hospital.ui.theme.AmbarFondo
import sv.edu.unicaes.hospital.ui.theme.AmbarTexto
import sv.edu.unicaes.hospital.ui.theme.AmbarTextoBanner
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.Fondo
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Primario800
import sv.edu.unicaes.hospital.ui.theme.Superficie2
import sv.edu.unicaes.hospital.ui.theme.Texto2

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
        Column(
            modifier = Modifier.padding(padding).padding(start = Espacio.md, end = Espacio.md, top = Espacio.xs),
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            BarraBusqueda(
                valor = busqueda,
                onValor = { busqueda = it },
                placeholder = stringResource(R.string.doctor_buscar_paciente),
            )
            ChipsFiltro(
                opciones =
                    listOf(
                        stringResource(R.string.filtro_todos),
                        stringResource(R.string.filtro_hospitalizados),
                    ),
                seleccionada = filtro,
                onSeleccion = { filtro = it },
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
                LazyColumn(contentPadding = PaddingValues(bottom = Espacio.md)) {
                    item {
                        TarjetaLista(visibles) { paciente, ultimo ->
                            FilaPaciente(
                                paciente = paciente,
                                soporte = paciente.ultimaVisita.orEmpty(),
                                divisor = !ultimo,
                                onClick = { onPaciente(paciente) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Barra de búsqueda M3 del diseño: alto 56, píldora n100 sin borde, lupa n600 y texto 16/24. */
@Composable
private fun BarraBusqueda(
    valor: String,
    onValor: (String) -> Unit,
    placeholder: String,
) {
    val estilo = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = MaterialTheme.colorScheme.onSurface)
    BasicTextField(
        value = valor,
        onValueChange = onValor,
        singleLine = true,
        textStyle = estilo,
        cursorBrush = SolidColor(Primario700),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { campo ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Superficie2)
                        .padding(horizontal = Espacio.md),
                horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = Texto2,
                    modifier = Modifier.size(22.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (valor.isEmpty()) Text(text = placeholder, style = estilo, color = Texto2)
                    campo()
                }
            }
        },
    )
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
            FabHospital(
                texto = stringResource(R.string.consulta_nueva),
                onClick = onNuevaConsulta,
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
            LazyColumn(contentPadding = RellenoCuerpoConFab, verticalArrangement = Arrangement.spacedBy(Espacio.sm)) {
                item {
                    PestanasHospital(
                        titulos =
                            listOf(
                                stringResource(R.string.expediente_consultas),
                                stringResource(R.string.mas_laboratorio),
                                stringResource(R.string.expediente_datos),
                            ),
                        seleccionada = pestana,
                        onSeleccion = { pestana = it },
                    )
                }
                expediente.alergias?.let { alergias ->
                    // Tarjeta prioritaria: borde #92400E sobre #FEF3C7.
                    item {
                        TarjetaConIcono(
                            icono = R.drawable.ic_alerta,
                            tinte = AmbarTexto,
                            titulo = stringResource(R.string.expediente_alergias),
                            detalle = alergias,
                            colorTitulo = AmbarTexto,
                            colorDetalle = AmbarTextoBanner,
                            colorBorde = AmbarTexto,
                            colorFondo = AmbarFondo,
                        )
                    }
                }
                when (pestana) {
                    0 -> {
                        expediente.hospitalizacion?.let { hospitalizacion ->
                            item {
                                TarjetaConIcono(
                                    icono = R.drawable.ic_bed,
                                    tinte = Primario800,
                                    titulo = stringResource(R.string.expediente_hospitalizacion),
                                    detalle = hospitalizacion,
                                )
                            }
                        }
                        item {
                            TarjetaLista(expediente.consultas) { consulta, ultimo ->
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
                                    divisor = !ultimo,
                                    enTarjeta = true,
                                    final = { Chevron() },
                                    onClick = {},
                                )
                            }
                        }
                        item { TituloSeccion(stringResource(R.string.expediente_lab_preview)) }
                        item {
                            TarjetaLista(expediente.laboratorio) { orden, ultimo ->
                                FilaLista(
                                    titulo = "${orden.examen} · ${orden.hora}",
                                    divisor = !ultimo,
                                    enTarjeta = true,
                                    final = { BadgeLaboratorio(orden.estado) },
                                )
                            }
                        }
                    }

                    1 -> {
                        item {
                            TarjetaLista(expediente.laboratorio) { orden, ultimo ->
                                FilaLista(
                                    titulo = orden.examen,
                                    soporte = "${orden.doctor} · ${orden.hora}",
                                    divisor = !ultimo,
                                    enTarjeta = true,
                                    final = { BadgeLaboratorio(orden.estado) },
                                )
                            }
                        }
                    }

                    else -> {
                        item {
                            TarjetaHospital {
                                FilaDato(stringResource(R.string.expediente_dato_dui), expediente.paciente.dui)
                                FilaDato(
                                    stringResource(R.string.expediente_dato_edad),
                                    stringResource(R.string.expediente_edad_valor, expediente.edad),
                                )
                                FilaDato(stringResource(R.string.campo_telefono_simple), expediente.telefono)
                                FilaDato(
                                    stringResource(R.string.campo_contacto_emergencia),
                                    expediente.contactoEmergencia,
                                    divisor = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tarjeta con icono a la izquierda y título 16/22 500 + detalle 14/20 (alergias, hospitalización). */
@Composable
private fun TarjetaConIcono(
    @DrawableRes icono: Int,
    tinte: Color,
    titulo: String,
    detalle: String,
    colorTitulo: Color = MaterialTheme.colorScheme.onSurface,
    colorDetalle: Color = Texto2,
    colorBorde: Color? = null,
    colorFondo: Color? = null,
) {
    val contenido: @Composable ColumnScope.() -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icono), contentDescription = null, tint = tinte, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = titulo, style = EstiloFilaTitulo, color = colorTitulo)
                Text(text = detalle, style = EstiloFilaSoporte, color = colorDetalle)
            }
        }
    }
    if (colorBorde != null && colorFondo != null) {
        TarjetaHospital(colorBorde = colorBorde, colorFondo = colorFondo, anchoBorde = 2.5.dp, contenido = contenido)
    } else {
        TarjetaHospital(contenido = contenido)
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
    val guardar = {
        intentoGuardar = true
        if (diagnostico.isNotBlank()) onGuardar()
    }

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.consulta_nueva),
                onVolver = onCerrar,
                cerrar = true,
                acciones = {
                    AccionTexto(
                        texto = stringResource(R.string.accion_guardar),
                        onClick = guardar,
                        modifier = Modifier.padding(horizontal = Espacio.sm),
                    )
                },
            )
        },
        bottomBar = {
            BotonPrimario(
                texto = stringResource(R.string.consulta_guardar),
                onClick = guardar,
                modifier = Modifier.navigationBarsPadding().imePadding().padding(Espacio.md),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(RellenoCuerpo),
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            Text("$paciente · $contexto", style = EstiloFilaSoporte, color = Texto2)
            CampoTexto(
                etiqueta = stringResource(R.string.consulta_diagnostico),
                valor = diagnostico,
                onValor = { diagnostico = it },
                placeholder = stringResource(R.string.campo_obligatorio),
                error =
                    if (intentoGuardar &&
                        diagnostico.isBlank()
                    ) {
                        stringResource(R.string.campo_obligatorio)
                    } else {
                        null
                    },
            )
            CampoTexto(
                etiqueta = stringResource(R.string.consulta_notas),
                valor = notas,
                onValor = { notas = it },
                placeholder = stringResource(R.string.consulta_notas_placeholder),
                singleLine = false,
                minLines = 3,
            )
            TarjetaConAccion(
                titulo = stringResource(R.string.consulta_recetas, recetas.size),
                accion = stringResource(R.string.consulta_agregar_receta),
                onAccion = { hojaReceta = true },
            ) {
                if (recetas.isEmpty()) {
                    Text(stringResource(R.string.consulta_sin_recetas), style = EstiloFilaSoporte, color = Texto2)
                }
                recetas.forEachIndexed { indice, receta ->
                    FilaLista(
                        titulo = receta.medicamento,
                        soporte = "${receta.dosis} · ${receta.frecuencia}",
                        divisor = indice < recetas.lastIndex,
                        enTarjeta = true,
                        margenHorizontal = 0.dp,
                    )
                }
            }
            TarjetaConAccion(
                titulo = stringResource(R.string.consulta_ordenes, ordenes.size),
                accion = stringResource(R.string.consulta_agregar_orden),
                onAccion = { hojaOrden = true },
            ) {
                ordenes.forEachIndexed { indice, orden ->
                    FilaLista(
                        titulo = orden.examen,
                        soporte = orden.indicaciones,
                        divisor = indice < ordenes.lastIndex,
                        enTarjeta = true,
                        margenHorizontal = 0.dp,
                        final = { BadgeLaboratorio(EstadoLaboratorio.PENDIENTE) },
                    )
                }
            }
        }
    }

    if (hojaOrden) {
        PantallaOrdenLaboratorio(
            paciente = paciente,
            onCerrar = { hojaOrden = false },
            onAgregar = {
                ordenes += it
                hojaOrden = false
            },
        )
    }
    if (hojaReceta) {
        PantallaReceta(
            paciente = paciente,
            onCerrar = { hojaReceta = false },
            onAgregar = {
                recetas += it
                hojaReceta = false
            },
        )
    }
}

/** Tarjeta con título 16/24 500 y acción de texto a la derecha ("+ Receta", "+ Orden"). */
@Composable
private fun TarjetaConAccion(
    titulo: String,
    accion: String,
    onAccion: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    TarjetaHospital {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = titulo, style = EstiloTituloTarjeta, modifier = Modifier.weight(1f))
            AccionTexto(texto = accion, onClick = onAccion)
        }
        contenido()
    }
}

/**
 * Formulario a pantalla completa sobre AND-D5 (AND-D6 en el diseño): barra con volver, subtítulo
 * "Paciente · Nueva consulta", campos y abajo a la derecha Cancelar + acción principal.
 */
@Composable
private fun PantallaFormulario(
    titulo: String,
    subtitulo: String,
    accion: String,
    accionHabilitada: Boolean,
    onCerrar: () -> Unit,
    onAccion: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            containerColor = Fondo,
            topBar = { TopBarHospital(titulo = titulo, onVolver = onCerrar) },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .imePadding()
                        .padding(RellenoCuerpo),
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                Text(subtitulo, style = EstiloFilaSoporte, color = Texto2)
                contenido()
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.xs, Alignment.End),
                ) {
                    val estiloBoton = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
                    TextButton(onClick = onCerrar, modifier = Modifier.height(52.dp), shape = CircleShape) {
                        Text(stringResource(R.string.accion_cancelar), style = estiloBoton)
                    }
                    Button(
                        onClick = onAccion,
                        enabled = accionHabilitada,
                        modifier = Modifier.height(52.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                    ) { Text(accion, style = estiloBoton) }
                }
            }
        }
    }
}

/** AND-D6 · Orden de laboratorio sobre AND-D5. Toda orden nace Pendiente. */
@Composable
private fun PantallaOrdenLaboratorio(
    paciente: String,
    onCerrar: () -> Unit,
    onAgregar: (OrdenNuevaUi) -> Unit,
) {
    var examen by rememberSaveable { mutableStateOf(DatosMuestra.tiposExamen.first()) }
    var indicaciones by rememberSaveable { mutableStateOf("Ayuno de 8 horas") }
    PantallaFormulario(
        titulo = stringResource(R.string.orden_titulo),
        subtitulo = "$paciente · ${stringResource(R.string.consulta_nueva)}",
        accion = stringResource(R.string.orden_agregar),
        accionHabilitada = examen.isNotBlank(),
        onCerrar = onCerrar,
        onAccion = { onAgregar(OrdenNuevaUi(examen, indicaciones)) },
    ) {
        CampoTexto(
            etiqueta = stringResource(R.string.orden_tipo_examen),
            valor = examen,
            onValor = { examen = it },
            final = { IconoDesplegable() },
        )
        CampoTexto(
            etiqueta = stringResource(R.string.orden_indicaciones),
            valor = indicaciones,
            onValor = { indicaciones = it },
            singleLine = false,
            minLines = 3,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.orden_se_crea_en), style = EstiloFilaSoporte, color = Texto2)
            BadgeLaboratorio(EstadoLaboratorio.PENDIENTE)
        }
    }
}

@Composable
private fun PantallaReceta(
    paciente: String,
    onCerrar: () -> Unit,
    onAgregar: (RecetaNuevaUi) -> Unit,
) {
    var medicamento by rememberSaveable { mutableStateOf("") }
    var dosis by rememberSaveable { mutableStateOf("") }
    var frecuencia by rememberSaveable { mutableStateOf("") }
    var indicaciones by rememberSaveable { mutableStateOf("") }
    PantallaFormulario(
        titulo = stringResource(R.string.receta_titulo),
        subtitulo = "$paciente · ${stringResource(R.string.consulta_nueva)}",
        accion = stringResource(R.string.receta_agregar),
        accionHabilitada = medicamento.isNotBlank(),
        onCerrar = onCerrar,
        onAccion = { onAgregar(RecetaNuevaUi(medicamento, dosis, frecuencia)) },
    ) {
        CampoTexto(stringResource(R.string.receta_medicamento), medicamento, { medicamento = it })
        CampoTexto(stringResource(R.string.receta_dosis), dosis, { dosis = it })
        CampoTexto(stringResource(R.string.receta_frecuencia), frecuencia, { frecuencia = it })
        CampoTexto(
            etiqueta = stringResource(R.string.orden_indicaciones),
            valor = indicaciones,
            onValor = { indicaciones = it },
            singleLine = false,
            minLines = 3,
        )
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
