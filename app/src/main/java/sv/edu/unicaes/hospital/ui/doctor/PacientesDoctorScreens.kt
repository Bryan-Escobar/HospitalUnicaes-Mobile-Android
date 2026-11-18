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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.core.mensaje
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.ExamenCatalogo
import sv.edu.unicaes.hospital.domain.model.NuevaReceta
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion
import sv.edu.unicaes.hospital.ui.components.AccionTexto
import sv.edu.unicaes.hospital.ui.components.BadgeLaboratorio
import sv.edu.unicaes.hospital.ui.components.Banner
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.CampoTexto
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
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.PestanasHospital
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TarjetaLista
import sv.edu.unicaes.hospital.ui.components.TipoBanner
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.AsignacionCamaUi
import sv.edu.unicaes.hospital.ui.modelo.ConsultaUi
import sv.edu.unicaes.hospital.ui.modelo.ExpedienteUi
import sv.edu.unicaes.hospital.ui.modelo.OrdenExpedienteUi
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import sv.edu.unicaes.hospital.ui.modelo.hora12
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
import sv.edu.unicaes.hospital.ui.theme.VerdeTexto

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
                val texto = busqueda.trim()
                val visibles =
                    pacientes.filter {
                        val coincide =
                            texto.isEmpty() || it.nombre.contains(texto, true) || it.dui.contains(texto, true)
                        coincide && (filtro == 0 || it.hospitalizado)
                    }
                LazyColumn(contentPadding = PaddingValues(bottom = Espacio.md)) {
                    item {
                        TarjetaLista(visibles) { paciente, ultimo ->
                            FilaPaciente(
                                paciente = paciente,
                                soporte = paciente.ultimaVisita ?: paciente.dui,
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
                                    detalle =
                                        stringResource(
                                            R.string.expediente_hospitalizacion_valor,
                                            hospitalizacion.codigo,
                                            hospitalizacion.desde,
                                        ),
                                )
                            }
                        }
                        if (expediente.consultas.isEmpty()) {
                            item {
                                EstadoVacio(
                                    titulo = stringResource(R.string.expediente_vacio_titulo),
                                    detalle = stringResource(R.string.expediente_vacio_detalle),
                                    modifier = Modifier.padding(top = Espacio.sm),
                                )
                            }
                        }
                        item {
                            TarjetaLista(expediente.consultas) { consulta, ultimo ->
                                val diagnostico =
                                    consulta.diagnostico.ifEmpty {
                                        stringResource(R.string.expediente_consulta_sin_diagnostico)
                                    }
                                FilaLista(
                                    titulo = "${consulta.fecha} · $diagnostico",
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
                                )
                            }
                        }
                        if (expediente.laboratorio.isNotEmpty()) {
                            item { TituloSeccion(stringResource(R.string.expediente_lab_preview)) }
                            item {
                                TarjetaLista(expediente.laboratorio.take(ORDENES_VISTA_PREVIA)) { orden, ultimo ->
                                    FilaLista(
                                        titulo = "${orden.examen} · ${orden.fecha}",
                                        divisor = !ultimo,
                                        enTarjeta = true,
                                        final = { BadgeLaboratorio(orden.estado) },
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        if (expediente.laboratorio.isEmpty()) {
                            item {
                                EstadoVacio(
                                    titulo = stringResource(R.string.expediente_sin_ordenes_titulo),
                                    detalle = stringResource(R.string.expediente_sin_ordenes_detalle),
                                    modifier = Modifier.padding(top = Espacio.sm),
                                )
                            }
                        }
                        item {
                            TarjetaLista(expediente.laboratorio) { orden, ultimo ->
                                FilaLista(
                                    titulo = orden.examen,
                                    soporte = "${orden.doctor} · ${orden.fecha}",
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
                                val sinRegistro = stringResource(R.string.expediente_dato_sin_registro)
                                FilaDato(stringResource(R.string.expediente_dato_dui), expediente.paciente.dui)
                                FilaDato(
                                    stringResource(R.string.expediente_dato_edad),
                                    expediente.edad?.let { stringResource(R.string.expediente_edad_valor, it) }
                                        ?: sinRegistro,
                                )
                                FilaDato(
                                    stringResource(R.string.campo_telefono_simple),
                                    expediente.telefono.ifEmpty { sinRegistro },
                                )
                                FilaDato(
                                    stringResource(R.string.campo_contacto_emergencia),
                                    expediente.contactoEmergencia.ifEmpty { sinRegistro },
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

/** Vista previa del expediente: las órdenes más recientes; todas están en la pestaña Laboratorio. */
private const val ORDENES_VISTA_PREVIA = 3

/** Los mensajes de la API (p. ej. "La cita ya tiene una consulta registrada") ya vienen redactados. */
@Composable
private fun textoError(error: AppError): String =
    (error as? AppError.Rechazada)?.detalle ?: stringResource(error.mensaje())

/**
 * AND-D5 · Nueva consulta: diagnóstico, notas, recetas y órdenes de laboratorio. El estado vive en
 * [NuevaConsultaViewModel]; Guardar envía lo que falta y, si algo falla, se puede reintentar.
 */
@Composable
fun NuevaConsultaScreen(
    estado: NuevaConsultaUiState,
    onCerrar: () -> Unit,
    onReintentar: () -> Unit,
    onDiagnostico: (String) -> Unit,
    onNotas: (String) -> Unit,
    onAgregarReceta: (NuevaReceta) -> Unit,
    onQuitarReceta: (Long) -> Unit,
    onAgregarOrden: (ExamenCatalogo, String) -> Unit,
    onQuitarOrden: (Long) -> Unit,
    onReintentarCatalogo: () -> Unit,
    onGuardar: () -> Unit,
    hojaOrdenAbierta: Boolean = false,
) {
    var hojaOrden by rememberSaveable { mutableStateOf(hojaOrdenAbierta) }
    var hojaReceta by rememberSaveable { mutableStateOf(false) }
    val contexto = (estado.contexto as? UiState.Exito)?.datos

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.consulta_nueva),
                onVolver = onCerrar,
                cerrar = true,
                acciones = {
                    if (estado.puedeGuardar) {
                        AccionTexto(
                            texto = stringResource(R.string.accion_guardar),
                            onClick = onGuardar,
                            modifier = Modifier.padding(horizontal = Espacio.sm),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (contexto != null) {
                BotonPrimario(
                    texto =
                        stringResource(
                            if (estado.guardando) R.string.consulta_guardando else R.string.consulta_guardar,
                        ),
                    onClick = onGuardar,
                    habilitado = estado.puedeGuardar,
                    modifier = Modifier.navigationBarsPadding().imePadding().padding(Espacio.md),
                )
            }
        },
    ) { padding ->
        ContenidoConEstado(
            estado = estado.contexto,
            mensajeError = stringResource(R.string.consulta_error_paciente),
            onReintentar = onReintentar,
            modifier = Modifier.padding(padding),
            vacio = {},
        ) { paciente ->
            FormularioConsulta(
                estado = estado,
                paciente = paciente,
                onDiagnostico = onDiagnostico,
                onNotas = onNotas,
                onNuevaReceta = { hojaReceta = true },
                onQuitarReceta = onQuitarReceta,
                onNuevaOrden = { hojaOrden = true },
                onQuitarOrden = onQuitarOrden,
            )
        }
    }

    if (hojaOrden && contexto != null) {
        PantallaOrdenLaboratorio(
            paciente = contexto.paciente,
            catalogo = estado.catalogo,
            excluidos = estado.examenesAgregados,
            onReintentarCatalogo = onReintentarCatalogo,
            onCerrar = { hojaOrden = false },
            onAgregar = { examen, indicaciones ->
                onAgregarOrden(examen, indicaciones)
                hojaOrden = false
            },
        )
    }
    if (hojaReceta && contexto != null) {
        PantallaReceta(
            paciente = contexto.paciente,
            alergias = contexto.alergias,
            onCerrar = { hojaReceta = false },
            onAgregar = {
                onAgregarReceta(it)
                hojaReceta = false
            },
        )
    }
}

@Composable
private fun FormularioConsulta(
    estado: NuevaConsultaUiState,
    paciente: ContextoConsultaUi,
    onDiagnostico: (String) -> Unit,
    onNotas: (String) -> Unit,
    onNuevaReceta: () -> Unit,
    onQuitarReceta: (Long) -> Unit,
    onNuevaOrden: () -> Unit,
    onQuitarOrden: (Long) -> Unit,
) {
    val cita = estado.citaHora?.let { stringResource(R.string.cita_titulo, hora12(it)) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(RellenoCuerpo),
        verticalArrangement = Arrangement.spacedBy(Espacio.sm),
    ) {
        Text(listOfNotNull(paciente.paciente, cita).joinToString(" · "), style = EstiloFilaSoporte, color = Texto2)
        paciente.alergias?.let {
            Banner(tipo = TipoBanner.AVISO, texto = stringResource(R.string.expediente_alergias_valor, it))
        }
        estado.error?.let { error ->
            Banner(
                tipo = TipoBanner.ERROR,
                texto = "${stringResource(R.string.consulta_error_guardar)} ${textoError(error)}",
            )
            if (estado.consultaCreada) {
                Banner(tipo = TipoBanner.INFO, texto = stringResource(R.string.consulta_parcial))
            }
        }
        CampoTexto(
            etiqueta = stringResource(R.string.consulta_diagnostico),
            valor = estado.diagnostico,
            onValor = onDiagnostico,
            placeholder = stringResource(R.string.campo_obligatorio),
            soloLectura = estado.diagnosticoEnviado || !estado.editable,
            error = if (estado.diagnosticoFaltante) stringResource(R.string.campo_obligatorio) else null,
        )
        CampoTexto(
            etiqueta = stringResource(R.string.consulta_notas),
            valor = estado.notas,
            onValor = onNotas,
            placeholder = stringResource(R.string.consulta_notas_placeholder),
            soloLectura = estado.consultaCreada || !estado.editable,
            singleLine = false,
            minLines = 3,
        )
        TarjetaConAccion(
            titulo = stringResource(R.string.consulta_recetas, estado.recetas.size),
            accion = stringResource(R.string.consulta_agregar_receta).takeIf { estado.editable },
            onAccion = onNuevaReceta,
        ) {
            if (estado.recetas.isEmpty()) {
                Text(stringResource(R.string.consulta_sin_recetas), style = EstiloFilaSoporte, color = Texto2)
            }
            estado.recetas.forEachIndexed { indice, borrador ->
                val receta = borrador.receta
                FilaLista(
                    titulo = receta.medicamento,
                    soporte =
                        listOfNotNull(receta.dosis, receta.frecuencia)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .ifEmpty { null },
                    divisor = indice < estado.recetas.lastIndex,
                    enTarjeta = true,
                    margenHorizontal = 0.dp,
                    final = {
                        AccionBorrador(
                            enviado = borrador.enviada,
                            editable = estado.editable,
                            onQuitar = { onQuitarReceta(borrador.id) },
                        )
                    },
                )
            }
        }
        TarjetaConAccion(
            titulo = stringResource(R.string.consulta_ordenes, estado.ordenes.size),
            accion = stringResource(R.string.consulta_agregar_orden).takeIf { estado.editable },
            onAccion = onNuevaOrden,
        ) {
            if (estado.ordenes.isEmpty()) {
                Text(stringResource(R.string.consulta_sin_ordenes), style = EstiloFilaSoporte, color = Texto2)
            }
            estado.ordenes.forEachIndexed { indice, orden ->
                FilaLista(
                    titulo = orden.examen.nombre,
                    soporte = orden.indicaciones.ifEmpty { null },
                    divisor = indice < estado.ordenes.lastIndex,
                    enTarjeta = true,
                    margenHorizontal = 0.dp,
                    final = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BadgeLaboratorio(EstadoLaboratorio.PENDIENTE)
                            AccionBorrador(
                                enviado = orden.enviada,
                                editable = estado.editable,
                                onQuitar = { onQuitarOrden(orden.id) },
                            )
                        }
                    },
                )
            }
        }
    }
}

/** Al final de una receta u orden agregada: ✓ si ya se guardó, o "Quitar" mientras se puede editar. */
@Composable
private fun AccionBorrador(
    enviado: Boolean,
    editable: Boolean,
    onQuitar: () -> Unit,
) {
    when {
        enviado -> {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.consulta_enviado),
                tint = VerdeTexto,
                modifier = Modifier.padding(start = Espacio.xs).size(20.dp),
            )
        }

        editable -> {
            IconoAccion(
                icono = R.drawable.ic_close,
                descripcion = stringResource(R.string.consulta_quitar),
                onClick = onQuitar,
                tinte = Texto2,
            )
        }
    }
}

/** Tarjeta con título 16/24 500 y acción de texto a la derecha ("+ Receta", "+ Orden"). */
@Composable
private fun TarjetaConAccion(
    titulo: String,
    accion: String?,
    onAccion: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    TarjetaHospital {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = titulo, style = EstiloTituloTarjeta, modifier = Modifier.weight(1f))
            if (accion != null) AccionTexto(texto = accion, onClick = onAccion)
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
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Espacio.sm),
                    content = contenido,
                )
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

/** AND-D6 · Orden de laboratorio sobre AND-D5: un examen del catálogo. Toda orden nace Pendiente. */
@Composable
private fun PantallaOrdenLaboratorio(
    paciente: String,
    catalogo: UiState<List<ExamenCatalogo>>,
    excluidos: Set<String>,
    onReintentarCatalogo: () -> Unit,
    onCerrar: () -> Unit,
    onAgregar: (ExamenCatalogo, String) -> Unit,
) {
    var examenId by rememberSaveable { mutableStateOf<String?>(null) }
    var indicaciones by rememberSaveable { mutableStateOf("") }
    val opciones = (catalogo as? UiState.Exito)?.datos.orEmpty().filterNot { it.id in excluidos }
    val elegido = opciones.firstOrNull { it.id == examenId }
    PantallaFormulario(
        titulo = stringResource(R.string.orden_titulo),
        subtitulo = "$paciente · ${stringResource(R.string.consulta_nueva)}",
        accion = stringResource(R.string.orden_agregar),
        accionHabilitada = elegido != null,
        onCerrar = onCerrar,
        onAccion = { elegido?.let { onAgregar(it, indicaciones) } },
    ) {
        SelectorExamen(
            opciones = opciones,
            elegido = elegido,
            cargando = catalogo is UiState.Cargando,
            onElegir = { examenId = it.id },
        )
        when (catalogo) {
            is UiState.Error -> {
                Banner(
                    tipo = TipoBanner.ERROR,
                    texto = stringResource(R.string.orden_catalogo_error),
                    accion = stringResource(R.string.estado_reintentar),
                    onAccion = onReintentarCatalogo,
                )
            }

            UiState.Vacio -> {
                Banner(tipo = TipoBanner.INFO, texto = stringResource(R.string.orden_catalogo_vacio))
            }

            else -> {}
        }
        CampoTexto(
            etiqueta = stringResource(R.string.orden_indicaciones),
            valor = indicaciones,
            onValor = { indicaciones = it },
            placeholder = stringResource(R.string.orden_indicaciones_placeholder),
            singleLine = false,
            minLines = 3,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.orden_se_crea_en), style = EstiloFilaSoporte, color = Texto2)
            BadgeLaboratorio(EstadoLaboratorio.PENDIENTE)
        }
    }
}

/** "Tipo de examen *": desplegable con el catálogo, sin los exámenes ya pedidos en esta consulta. */
@Composable
private fun SelectorExamen(
    opciones: List<ExamenCatalogo>,
    elegido: ExamenCatalogo?,
    cargando: Boolean,
    onElegir: (ExamenCatalogo) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    val habilitado = opciones.isNotEmpty()
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = habilitado && it }) {
        CampoTexto(
            etiqueta = stringResource(R.string.orden_tipo_examen),
            valor = elegido?.nombre ?: if (cargando) stringResource(R.string.orden_cargando_catalogo) else "",
            onValor = {},
            soloLectura = true,
            placeholder = stringResource(R.string.orden_elige_examen),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, habilitado),
            final = { IconoDesplegable() },
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { examen ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(examen.nombre)
                            examen.tipoMuestra?.let { Text(it, style = EstiloFilaSoporte, color = Texto2) }
                        }
                    },
                    onClick = {
                        onElegir(examen)
                        abierto = false
                    },
                )
            }
        }
    }
}

/** AND-D6 · Receta sobre AND-D5. Avisa (sin bloquear) si el medicamento coincide con una alergia registrada. */
@Composable
private fun PantallaReceta(
    paciente: String,
    alergias: String?,
    onCerrar: () -> Unit,
    onAgregar: (NuevaReceta) -> Unit,
) {
    var medicamento by rememberSaveable { mutableStateOf("") }
    var dosis by rememberSaveable { mutableStateOf("") }
    var frecuencia by rememberSaveable { mutableStateOf("") }
    var indicaciones by rememberSaveable { mutableStateOf("") }
    val coincidencias = remember(medicamento, alergias) { alergiasCoincidentes(medicamento, alergias) }
    PantallaFormulario(
        titulo = stringResource(R.string.receta_titulo),
        subtitulo = "$paciente · ${stringResource(R.string.consulta_nueva)}",
        accion = stringResource(R.string.receta_agregar),
        accionHabilitada = medicamento.isNotBlank(),
        onCerrar = onCerrar,
        onAccion = { onAgregar(NuevaReceta(medicamento, dosis, frecuencia, indicaciones)) },
    ) {
        CampoTexto(stringResource(R.string.receta_medicamento), medicamento, { medicamento = it })
        if (coincidencias.isNotEmpty()) {
            Banner(
                tipo = TipoBanner.AVISO,
                texto = stringResource(R.string.receta_alerta_alergia, coincidencias.joinToString(", ")),
            )
        }
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

// Previews --------------------------------------------------------------------------------------

private val pacientesPreview =
    listOf(
        PacienteUi("p-2", "María López", "04512378-9"),
        PacienteUi("p-6", "Tomás Mora", "06677889-3", etiqueta = "HAB-206 · A", hospitalizado = true),
        PacienteUi("p-7", "Ana Vega", "07788990-4"),
    )

private val expedientePreview =
    ExpedienteUi(
        paciente = pacientesPreview[1],
        edad = 52,
        alergias = "Ibuprofeno",
        hospitalizacion =
            AsignacionCamaUi(
                habitacion = "HAB-206",
                cama = "A",
                tipo = TipoHabitacion.SEMIPRIVADA,
                desde = "20 oct",
                ingreso = "20 oct · 4:40 p. m.",
                diasHospitalizado = 1,
                medicoTratante = "Jorge Peña",
            ),
        consultas =
            listOf(
                ConsultaUi("c-1", "20 oct", "Apendicitis aguda", "Jorge Peña", recetas = 1, ordenes = 2),
                ConsultaUi("c-2", "3 jun", "Gastritis", "Carlos Molina", recetas = 0, ordenes = 0),
            ),
        laboratorio =
            listOf(
                OrdenExpedienteUi("o-1", "Hemograma completo", "Jorge Peña", "20 oct", EstadoLaboratorio.COMPLETADO),
                OrdenExpedienteUi("o-2", "Química sanguínea", "Jorge Peña", "21 oct", EstadoLaboratorio.EN_PROCESO),
            ),
        telefono = "7000-4444",
        contactoEmergencia = "Lucía Mora · 7000-5555",
    )

@PreviewPantalla
@Composable
private fun MisPacientesPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme { MisPacientesScreen(estado = estado.con(pacientesPreview), onPaciente = {}) }
}

@PreviewPantalla
@Composable
private fun ExpedientePreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        ExpedienteScreen(estado = estado.con(expedientePreview), onVolver = {}, onNuevaConsulta = {})
    }
}

@PreviewPantalla
@Composable
private fun NuevaConsultaPreview() {
    val hemograma = ExamenCatalogo("x-1", "Química sanguínea", tipoMuestra = "Sangre venosa")
    HospitalUnicaesTheme {
        NuevaConsultaScreen(
            estado =
                NuevaConsultaUiState(
                    contexto = UiState.Exito(ContextoConsultaUi("María López", alergias = "Penicilina")),
                    citaHora = "10:00",
                    catalogo = UiState.Exito(listOf(hemograma)),
                    ordenes = listOf(OrdenBorradorUi(1, hemograma, "Ayuno de 8 h")),
                ),
            onCerrar = {},
            onReintentar = {},
            onDiagnostico = {},
            onNotas = {},
            onAgregarReceta = {},
            onQuitarReceta = {},
            onAgregarOrden = { _, _ -> },
            onQuitarOrden = {},
            onReintentarCatalogo = {},
            onGuardar = {},
        )
    }
}
