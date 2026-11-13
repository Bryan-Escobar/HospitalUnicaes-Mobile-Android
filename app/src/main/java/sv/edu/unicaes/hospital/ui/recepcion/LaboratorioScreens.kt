@file:OptIn(ExperimentalMaterial3Api::class)

package sv.edu.unicaes.hospital.ui.recepcion

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.core.mensaje
import sv.edu.unicaes.hospital.domain.model.ArchivoAdjunto
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.ExamenOrden
import sv.edu.unicaes.hospital.domain.model.OrdenLaboratorio
import sv.edu.unicaes.hospital.domain.model.PrioridadLaboratorio
import sv.edu.unicaes.hospital.domain.model.ResultadoExamen
import sv.edu.unicaes.hospital.domain.model.TipoArchivoResultado
import sv.edu.unicaes.hospital.ui.components.AccionTexto
import sv.edu.unicaes.hospital.ui.components.BadgeEstado
import sv.edu.unicaes.hospital.ui.components.BadgeLaboratorio
import sv.edu.unicaes.hospital.ui.components.Banner
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.BotonSecundario
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.Chevron
import sv.edu.unicaes.hospital.ui.components.ChipsFiltro
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.DialogoConfirmacion
import sv.edu.unicaes.hospital.ui.components.EstadoPaso
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.EstiloFilaSoporte
import sv.edu.unicaes.hospital.ui.components.FilaDato
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.LineaTiempo
import sv.edu.unicaes.hospital.ui.components.PasoLinea
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TarjetaLista
import sv.edu.unicaes.hospital.ui.components.TextosFila
import sv.edu.unicaes.hospital.ui.components.TipoBanner
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.diaMes
import sv.edu.unicaes.hospital.ui.modelo.fechaDesdeIso
import sv.edu.unicaes.hospital.ui.modelo.hora
import sv.edu.unicaes.hospital.ui.theme.BordeSuave
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalTheme
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Texto2
import java.util.Locale

// Laboratorio (HUNI-63): Pendiente → En Proceso → Completado, solo hacia adelante y por examen.
// El doctor crea la orden; recepción/asistencia avanza cada examen y Completado exige resultado
// (texto y/o archivo PDF o imagen). La API valida cada transición y la concurrencia.

private val glifosLab =
    mapOf(
        EstadoLaboratorio.PENDIENTE to "○",
        EstadoLaboratorio.EN_PROCESO to "◐",
        EstadoLaboratorio.COMPLETADO to "✓",
    )

@Composable
private fun nombreEstado(estado: EstadoLaboratorio) =
    stringResource(
        when (estado) {
            EstadoLaboratorio.PENDIENTE -> R.string.lab_pendiente
            EstadoLaboratorio.EN_PROCESO -> R.string.lab_en_proceso
            EstadoLaboratorio.COMPLETADO -> R.string.lab_completado
        },
    )

/** Los mensajes de la API (p. ej. "Otro usuario ya inició este examen") ya vienen redactados. */
@Composable
private fun textoError(error: AppError): String =
    (error as? AppError.Rechazada)?.detalle ?: stringResource(error.mensaje())

/** `26 oct · 13:00` en la zona del dispositivo. */
internal fun fechaCorta(iso: String?): String =
    iso?.let(::fechaDesdeIso)?.let { "${diaMes(it)} · ${hora(it)}" }.orEmpty()

/** `310 KB`, `1.4 MB`. */
internal fun tamanoLegible(bytes: Long): String =
    when {
        bytes < 0 -> ""
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${(bytes + 512) / 1024} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

/** Nombres de los exámenes de la orden para la fila de la cola. */
internal fun OrdenLaboratorio.titulo(): String = examenes.joinToString(", ") { it.nombre }.ifEmpty { "—" }

@Composable
private fun BadgePrioridad(prioridad: PrioridadLaboratorio) {
    val texto =
        when (prioridad) {
            PrioridadLaboratorio.NORMAL -> return
            PrioridadLaboratorio.URGENTE -> stringResource(R.string.lab_prioridad_urgente)
            PrioridadLaboratorio.CRITICA -> stringResource(R.string.lab_prioridad_critica)
        }
    BadgeEstado(texto = texto, colores = HospitalTheme.estados.personalEmergencia, glifo = "!")
}

/** AND-R15 · Cola de laboratorio filtrable por estado, con pull-to-refresh y avisos en tiempo real. */
@Composable
fun ColaLaboratorioScreen(
    estado: ColaLaboratorioUi,
    onVolver: () -> Unit,
    onFiltro: (EstadoLaboratorio) -> Unit,
    onOrden: (OrdenLaboratorio) -> Unit,
    onRefrescar: () -> Unit,
    onReintentar: () -> Unit,
    onCargarMas: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.mas_laboratorio),
                onVolver = onVolver,
                enVivo = true,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(start = Espacio.md, end = Espacio.md, top = Espacio.xs),
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            ChipsFiltro(
                opciones =
                    EstadoLaboratorio.entries.map { e ->
                        val conteo = estado.conteos[e]?.let { " $it" }.orEmpty()
                        "${glifosLab[e]} ${nombreEstado(e)}$conteo"
                    },
                seleccionada = estado.filtro.ordinal,
                onSeleccion = { onFiltro(EstadoLaboratorio.entries[it]) },
            )
            PullToRefreshBox(
                isRefreshing = estado.refrescando,
                onRefresh = onRefrescar,
                modifier = Modifier.fillMaxSize(),
            ) {
                ContenidoConEstado(
                    estado = estado.ordenes,
                    mensajeError = stringResource(R.string.lab_error_cola),
                    onReintentar = onReintentar,
                    vacio = {
                        // Desplazable para que el gesto de refrescar también funcione sin órdenes.
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            EstadoVacio(
                                titulo = stringResource(R.string.lab_vacio_estado, nombreEstado(estado.filtro)),
                                detalle = stringResource(R.string.lab_vacio_detalle),
                                icono = R.drawable.ic_laboratorio,
                            )
                        }
                    },
                ) { ordenes ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Espacio.md),
                        verticalArrangement = Arrangement.spacedBy(Espacio.sm),
                    ) {
                        item {
                            TarjetaLista(elementos = ordenes) { orden, ultima ->
                                FilaLista(
                                    titulo = orden.titulo(),
                                    soporte = "${orden.paciente} · ${orden.doctor} · ${fechaCorta(
                                        orden.fechaSolicitud,
                                    )}",
                                    onClick = { onOrden(orden) },
                                    divisor = !ultima,
                                    enTarjeta = true,
                                    final = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
                                        ) {
                                            BadgePrioridad(orden.prioridad)
                                            BadgeLaboratorio(orden.estado)
                                            Chevron()
                                        }
                                    },
                                )
                            }
                        }
                        if (estado.hayMas) {
                            item {
                                if (estado.cargandoMas) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                } else {
                                    BotonSecundario(
                                        texto = stringResource(R.string.lab_cargar_mas),
                                        onClick = onCargarMas,
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

/**
 * AND-R16 · Detalle de orden: datos del paciente y cada examen con su línea de tiempo. Un único botón
 * de "siguiente estado" por examen; sin retroceso. Completado se alcanza registrando el resultado.
 */
@Composable
fun DetalleOrdenScreen(
    estado: DetalleOrdenUi,
    onVolver: () -> Unit,
    onIniciarExamen: (String) -> Unit,
    onCargarResultado: (String) -> Unit,
    onDescartarError: () -> Unit,
    onReintentar: () -> Unit,
) {
    var porConfirmar by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopBarHospital(titulo = stringResource(R.string.lab_orden), onVolver = onVolver, enVivo = true) },
    ) { padding ->
        ContenidoConEstado(
            estado = estado.orden,
            mensajeError = stringResource(R.string.lab_error_orden),
            onReintentar = onReintentar,
            modifier = Modifier.padding(padding),
            vacio = {},
        ) { orden ->
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(RellenoCuerpo),
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                TarjetaHospital {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = orden.paciente,
                            style = TextStyle(fontSize = 22.sp, lineHeight = 28.sp),
                            modifier = Modifier.weight(1f),
                        )
                        BadgeLaboratorio(orden.estado)
                    }
                    Column {
                        val doctor = listOfNotNull(orden.doctor, orden.especialidad).joinToString(" · ")
                        FilaDato(stringResource(R.string.lab_ordeno), doctor)
                        FilaDato(stringResource(R.string.lab_solicitada), fechaCorta(orden.fechaSolicitud))
                        FilaDato(
                            stringResource(R.string.lab_prioridad),
                            when (orden.prioridad) {
                                PrioridadLaboratorio.NORMAL -> stringResource(R.string.lab_prioridad_normal)
                                PrioridadLaboratorio.URGENTE -> stringResource(R.string.lab_prioridad_urgente)
                                PrioridadLaboratorio.CRITICA -> stringResource(R.string.lab_prioridad_critica)
                            },
                            divisor = orden.indicaciones != null,
                        )
                        orden.indicaciones?.let {
                            FilaDato(stringResource(R.string.lab_indicaciones), it, divisor = false)
                        }
                    }
                }
                estado.errorAccion?.let {
                    Banner(
                        tipo = TipoBanner.ERROR,
                        texto = textoError(it),
                        accion = stringResource(R.string.accion_cerrar),
                        onAccion = onDescartarError,
                    )
                }
                TituloSeccion(stringResource(R.string.lab_examenes))
                orden.examenes.forEach { examen ->
                    TarjetaExamen(
                        orden = orden,
                        examen = examen,
                        iniciando = estado.iniciando == examen.id,
                        bloqueado = estado.iniciando != null,
                        onIniciar = { porConfirmar = examen.id },
                        onCargarResultado = { onCargarResultado(examen.id) },
                    )
                }
            }

            orden.examenes.firstOrNull { it.id == porConfirmar }?.let { examen ->
                DialogoConfirmacion(
                    titulo = stringResource(R.string.lab_confirmar_en_proceso_titulo),
                    texto = stringResource(R.string.lab_confirmar_en_proceso_texto, examen.nombre),
                    confirmar = stringResource(R.string.lab_pasar_en_proceso),
                    onConfirmar = {
                        porConfirmar = null
                        onIniciarExamen(examen.id)
                    },
                    onCancelar = { porConfirmar = null },
                )
            }
        }
    }
}

@Composable
private fun TarjetaExamen(
    orden: OrdenLaboratorio,
    examen: ExamenOrden,
    iniciando: Boolean,
    bloqueado: Boolean,
    onIniciar: () -> Unit,
    onCargarResultado: () -> Unit,
) {
    TarjetaHospital {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextosFila(
                titulo = examen.nombre,
                soporte = examen.tipoMuestra?.let { stringResource(R.string.lab_muestra, it) },
                modifier = Modifier.weight(1f),
            )
            BadgeLaboratorio(examen.estado)
        }
        LineaTiempo(pasos = pasosExamen(orden, examen), modifier = Modifier.padding(vertical = Espacio.xs))
        when (examen.estado) {
            EstadoLaboratorio.PENDIENTE -> {
                BotonPrimario(
                    texto =
                        stringResource(
                            if (iniciando) R.string.lab_pasando_en_proceso else R.string.lab_pasar_en_proceso,
                        ),
                    onClick = onIniciar,
                    habilitado = !bloqueado,
                )
            }

            EstadoLaboratorio.EN_PROCESO -> {
                BotonPrimario(
                    texto = stringResource(R.string.lab_cargar_resultado),
                    onClick = onCargarResultado,
                    icono = R.drawable.ic_upload,
                )
            }

            EstadoLaboratorio.COMPLETADO -> {
                examen.resultado?.let { BloqueResultado(it) }
            }
        }
    }
}

@Composable
private fun BloqueResultado(resultado: ResultadoExamen) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.lab_resultado),
            style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.sp),
            color = Texto2,
        )
        resultado.valor?.let { valor ->
            Text(listOfNotNull(valor, resultado.unidad).joinToString(" "), style = EstiloFilaSoporte)
        }
        resultado.rangoReferencia?.let {
            Text(stringResource(R.string.lab_resultado_rango, it), style = EstiloFilaSoporte, color = Texto2)
        }
        resultado.observaciones?.let { Text(it, style = EstiloFilaSoporte, color = Texto2) }
        resultado.tipoArchivo?.let { tipo ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_documento),
                    contentDescription = null,
                    tint = Primario700,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(
                        if (tipo == TipoArchivoResultado.PDF) R.string.lab_adjunto_pdf else R.string.lab_adjunto_imagen,
                    ),
                    style = EstiloFilaSoporte,
                )
            }
            // Recepción registra el archivo pero no lo descarga (privacidad, context.md §3.4).
            Text(
                stringResource(R.string.lab_adjunto_privado),
                style = MaterialTheme.typography.bodySmall,
                color = Texto2,
            )
        }
        resultado.validadoPor?.let {
            Text(
                stringResource(R.string.lab_resultado_registro, it, fechaCorta(resultado.fecha)),
                style = MaterialTheme.typography.bodySmall,
                color = Texto2,
            )
        }
    }
}

@Composable
private fun pasosExamen(
    orden: OrdenLaboratorio,
    examen: ExamenOrden,
): List<PasoLinea> {
    fun paso(estado: EstadoLaboratorio): EstadoPaso =
        when {
            examen.estado.ordinal >= estado.ordinal -> EstadoPaso.HECHO
            examen.estado.siguiente == estado -> EstadoPaso.ACTUAL
            else -> EstadoPaso.PENDIENTE
        }
    return listOf(
        PasoLinea(
            nombreEstado(EstadoLaboratorio.PENDIENTE),
            fechaCorta(orden.fechaSolicitud),
            paso(EstadoLaboratorio.PENDIENTE),
        ),
        PasoLinea(
            nombreEstado(EstadoLaboratorio.EN_PROCESO),
            examen.fechaInicio?.let(::fechaCorta) ?: stringResource(R.string.lab_aun_no),
            paso(EstadoLaboratorio.EN_PROCESO),
        ),
        PasoLinea(
            nombreEstado(EstadoLaboratorio.COMPLETADO),
            examen.fechaCompletado?.let(::fechaCorta) ?: stringResource(R.string.lab_requiere_resultado),
            paso(EstadoLaboratorio.COMPLETADO),
        ),
    )
}

/**
 * AND-R17 · Cargar resultado: texto y/o un archivo PDF (OpenDocument) o una imagen (Photo Picker,
 * sin permisos de almacenamiento), de hasta 10 MB. Guardar pasa el examen a Completado.
 */
@Composable
fun CargarResultadoScreen(
    estado: CargarResultadoUi,
    onCerrar: () -> Unit,
    onValor: (String) -> Unit,
    onUnidad: (String) -> Unit,
    onRango: (String) -> Unit,
    onObservaciones: (String) -> Unit,
    onArchivo: (String) -> Unit,
    onQuitarArchivo: () -> Unit,
    onGuardar: () -> Unit,
    onReintentar: () -> Unit,
) {
    val elegirImagen =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { onArchivo(it.toString()) }
        }
    val elegirPdf =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onArchivo(it.toString()) }
        }

    Scaffold(
        topBar = {
            TopBarHospital(titulo = stringResource(R.string.lab_resultado), onVolver = onCerrar, cerrar = true)
        },
    ) { padding ->
        ContenidoConEstado(
            estado = estado.carga,
            mensajeError = stringResource(R.string.lab_error_orden),
            onReintentar = onReintentar,
            modifier = Modifier.padding(padding),
            vacio = {},
        ) { contexto ->
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(RellenoCuerpo),
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                Text(
                    "${contexto.examen.nombre} · ${contexto.orden.paciente}",
                    style = EstiloFilaSoporte,
                    color = Texto2,
                )
                if (!estado.examenEnProceso) {
                    Banner(tipo = TipoBanner.AVISO, texto = stringResource(R.string.lab_examen_no_en_proceso))
                }
                val editable = estado.examenEnProceso && !estado.guardando
                CampoTexto(
                    etiqueta = stringResource(R.string.lab_resultado_texto),
                    valor = estado.valor,
                    onValor = onValor,
                    ayuda = stringResource(R.string.lab_resultado_ayuda),
                    singleLine = false,
                    minLines = 3,
                    soloLectura = !editable,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.sm)) {
                    CampoTexto(
                        etiqueta = stringResource(R.string.lab_unidad),
                        valor = estado.unidad,
                        onValor = onUnidad,
                        modifier = Modifier.weight(1f),
                        soloLectura = !editable,
                    )
                    CampoTexto(
                        etiqueta = stringResource(R.string.lab_rango),
                        valor = estado.rangoReferencia,
                        onValor = onRango,
                        modifier = Modifier.weight(1.4f),
                        soloLectura = !editable,
                    )
                }
                CampoTexto(
                    etiqueta = stringResource(R.string.lab_observaciones),
                    valor = estado.observaciones,
                    onValor = onObservaciones,
                    singleLine = false,
                    minLines = 2,
                    soloLectura = !editable,
                )
                TituloSeccion(stringResource(R.string.lab_archivo))
                estado.archivo?.let { archivo ->
                    TarjetaArchivo(archivo = archivo, onQuitar = onQuitarArchivo, habilitado = editable)
                }
                estado.problemaArchivo?.let { problema ->
                    Banner(
                        tipo = TipoBanner.ERROR,
                        texto =
                            stringResource(
                                when (problema) {
                                    ProblemaArchivo.TIPO -> R.string.lab_archivo_tipo
                                    ProblemaArchivo.TAMANO -> R.string.lab_archivo_tamano
                                    ProblemaArchivo.ILEGIBLE -> R.string.lab_archivo_ilegible
                                },
                            ),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.sm)) {
                    AccionTexto(
                        texto = stringResource(R.string.lab_elegir_imagen),
                        onClick = {
                            if (editable) {
                                elegirImagen.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            }
                        },
                    )
                    AccionTexto(
                        texto = stringResource(R.string.lab_elegir_pdf),
                        onClick = { if (editable) elegirPdf.launch(arrayOf(ArchivoAdjunto.MIME_PDF)) },
                    )
                }
                estado.error?.let {
                    Banner(
                        tipo = TipoBanner.ERROR,
                        texto = "${stringResource(R.string.lab_error_guardar)} ${textoError(it)}",
                    )
                }
                if (estado.guardando) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        trackColor = BordeSuave,
                    )
                }
                BotonPrimario(
                    texto =
                        stringResource(
                            if (estado.guardando) R.string.lab_guardando else R.string.lab_guardar_completar,
                        ),
                    onClick = onGuardar,
                    habilitado = estado.puedeGuardar,
                )
            }
        }
    }
}

@Composable
private fun TarjetaArchivo(
    archivo: ArchivoAdjunto,
    onQuitar: () -> Unit,
    habilitado: Boolean,
) {
    TarjetaHospital {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.sm)) {
            Icon(
                painter = painterResource(R.drawable.ic_documento),
                contentDescription = null,
                tint = Primario700,
                modifier = Modifier.size(28.dp),
            )
            TextosFila(
                titulo = archivo.nombre,
                soporte =
                    listOf(if (archivo.esPdf) "PDF" else "Imagen", tamanoLegible(archivo.tamanoBytes))
                        .filter { it.isNotEmpty() }
                        .joinToString(" · "),
                modifier = Modifier.weight(1f),
            )
            if (habilitado) {
                IconoAccion(R.drawable.ic_close, stringResource(R.string.lab_quitar_archivo), onQuitar, tinte = Texto2)
            }
        }
    }
}

// Previews ------------------------------------------------------------------------------------

private val examenPreview =
    ExamenOrden(
        id = "oe-1",
        nombre = "Examen general de orina",
        tipoMuestra = "Orina",
        estado = EstadoLaboratorio.EN_PROCESO,
        fechaInicio = "2026-11-09T16:55:00Z",
    )

private val ordenPreview =
    OrdenLaboratorio(
        id = "o-1",
        fechaSolicitud = "2026-11-09T16:41:00Z",
        prioridad = PrioridadLaboratorio.URGENTE,
        estado = EstadoLaboratorio.EN_PROCESO,
        pacienteId = "p-1",
        paciente = "Luis Martínez",
        doctor = "Carlos Molina",
        especialidad = "Medicina interna",
        indicaciones = "Primera orina de la mañana",
        examenes =
            listOf(
                examenPreview,
                ExamenOrden(id = "oe-2", nombre = "Hemograma completo", estado = EstadoLaboratorio.PENDIENTE),
                ExamenOrden(
                    id = "oe-3",
                    nombre = "Glucosa en ayunas",
                    estado = EstadoLaboratorio.COMPLETADO,
                    fechaInicio = "2026-11-09T17:00:00Z",
                    fechaCompletado = "2026-11-09T18:10:00Z",
                    resultado =
                        ResultadoExamen(
                            valor = "92",
                            unidad = "mg/dL",
                            rangoReferencia = "70 - 100",
                            tipoArchivo = TipoArchivoResultado.PDF,
                            validadoPor = "Lucía Herrera",
                            fecha = "2026-11-09T18:10:00Z",
                        ),
                ),
            ),
    )

@PreviewPantalla
@Composable
private fun ColaLaboratorioPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        ColaLaboratorioScreen(
            estado =
                ColaLaboratorioUi(
                    filtro = EstadoLaboratorio.EN_PROCESO,
                    ordenes =
                        estado.con(
                            listOf(
                                ordenPreview,
                                ordenPreview.copy(id = "o-2", prioridad = PrioridadLaboratorio.NORMAL),
                            ),
                        ),
                    conteos = mapOf(EstadoLaboratorio.PENDIENTE to 7, EstadoLaboratorio.EN_PROCESO to 2),
                    hayMas = true,
                ),
            onVolver = {},
            onFiltro = {},
            onOrden = {},
            onRefrescar = {},
            onReintentar = {},
            onCargarMas = {},
        )
    }
}

@PreviewPantalla
@Composable
private fun DetalleOrdenPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        DetalleOrdenScreen(
            estado = DetalleOrdenUi(orden = estado.con(ordenPreview)),
            onVolver = {},
            onIniciarExamen = {},
            onCargarResultado = {},
            onDescartarError = {},
            onReintentar = {},
        )
    }
}

@PreviewPantalla
@Composable
private fun DetalleOrdenConflictoPreview() {
    HospitalUnicaesTheme {
        DetalleOrdenScreen(
            estado =
                DetalleOrdenUi(
                    orden = UiState.Exito(ordenPreview),
                    errorAccion = AppError.Rechazada("Otro usuario ya inició este examen; actualiza la orden"),
                ),
            onVolver = {},
            onIniciarExamen = {},
            onCargarResultado = {},
            onDescartarError = {},
            onReintentar = {},
        )
    }
}

@PreviewPantalla
@Composable
private fun CargarResultadoPreview() {
    HospitalUnicaesTheme {
        CargarResultadoScreen(
            estado =
                CargarResultadoUi(
                    carga = UiState.Exito(ContextoResultado(ordenPreview, examenPreview)),
                    valor = "Densidad 1.020, pH 6.0. Sin leucocitos ni nitritos.",
                    archivo = ArchivoAdjunto("content://x", "ego-luis-martinez.pdf", ArchivoAdjunto.MIME_PDF, 317_440),
                ),
            onCerrar = {},
            onValor = {},
            onUnidad = {},
            onRango = {},
            onObservaciones = {},
            onArchivo = {},
            onQuitarArchivo = {},
            onGuardar = {},
            onReintentar = {},
        )
    }
}
