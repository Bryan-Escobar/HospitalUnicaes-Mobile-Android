@file:OptIn(ExperimentalMaterial3Api::class)

package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.ui.components.BadgeLaboratorio
import sv.edu.unicaes.hospital.ui.components.Banner
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.BotonSecundario
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.ChipDesplegable
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.DialogoConfirmacion
import sv.edu.unicaes.hospital.ui.components.EstadoPaso
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.LineaTiempo
import sv.edu.unicaes.hospital.ui.components.PasoLinea
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TipoBanner
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.OrdenLabUi
import sv.edu.unicaes.hospital.ui.theme.BordeSuave
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

// Laboratorio: Pendiente → En Proceso → Completado, solo hacia adelante. El doctor crea la orden,
// recepción/asistencia la avanza y Completado exige resultado (texto y/o PDF).

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

/** AND-R15 · Cola de laboratorio con pestañas por estado. */
@Composable
fun ColaLaboratorioScreen(
    estado: UiState<List<OrdenLabUi>>,
    conteos: Map<EstadoLaboratorio, Int>,
    onVolver: () -> Unit,
    onOrden: (OrdenLabUi) -> Unit,
    onReintentar: () -> Unit = {},
) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val estadoPestana = EstadoLaboratorio.entries[pestana]

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.mas_laboratorio),
                onVolver = onVolver,
                enVivo = true,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = pestana) {
                EstadoLaboratorio.entries.forEachIndexed { indice, estadoLab ->
                    val conteo = conteos[estadoLab]?.let { " $it" }.orEmpty()
                    Tab(
                        selected = pestana == indice,
                        onClick = { pestana = indice },
                        text = { Text("${glifosLab[estadoLab]} ${nombreEstado(estadoLab)}$conteo") },
                    )
                }
            }
            Row(
                modifier =
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Espacio.md, vertical = Espacio.xs),
                horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
            ) {
                ChipDesplegable(stringResource(R.string.lab_filtro_hoy))
                ChipDesplegable(stringResource(R.string.citas_filtro_doctor))
            }
            ContenidoConEstado(
                estado = estado,
                mensajeError = stringResource(R.string.lab_error_cola),
                onReintentar = onReintentar,
                vacio = {
                    EstadoVacio(
                        titulo = stringResource(R.string.lab_vacio_titulo),
                        detalle = stringResource(R.string.lab_vacio_detalle),
                    )
                },
            ) { ordenes ->
                val visibles = ordenes.filter { it.estado == estadoPestana }
                if (visibles.isEmpty()) {
                    EstadoVacio(
                        titulo = stringResource(R.string.lab_vacio_estado, nombreEstado(estadoPestana)),
                        detalle = stringResource(R.string.lab_vacio_detalle),
                    )
                }
                LazyColumn {
                    items(visibles, key = { it.id }) { orden ->
                        FilaLista(
                            titulo = orden.examen,
                            soporte = "${orden.paciente} · ${orden.doctor} · ${orden.hora}",
                            resaltada = orden.nueva,
                            onClick = { onOrden(orden) },
                            final = { BadgeLaboratorio(orden.estado) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * AND-R16 · Detalle de orden. Un único botón de "siguiente estado"; sin retroceso.
 * Completado sin resultado devuelve 422 y se muestra el aviso correspondiente.
 */
@Composable
fun DetalleOrdenScreen(
    estado: UiState<OrdenLabUi>,
    onVolver: () -> Unit,
    onPasarAEnProceso: () -> Unit,
    onCargarResultado: () -> Unit,
    onCompletar: () -> Unit,
    onReintentar: () -> Unit = {},
    faltaResultado: Boolean = false,
) {
    var confirmar by rememberSaveable { mutableStateOf(false) }
    val titulo = (estado as? UiState.Exito)?.datos?.numero?.let { stringResource(R.string.lab_orden_numero, it) }

    Scaffold(
        topBar = { TopBarHospital(titulo = titulo ?: stringResource(R.string.lab_orden), onVolver = onVolver) },
    ) { padding ->
        ContenidoConEstado(
            estado = estado,
            mensajeError = stringResource(R.string.lab_error_orden),
            onReintentar = onReintentar,
            modifier = Modifier.padding(padding),
            vacio = {},
        ) { orden ->
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.md),
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                TarjetaHospital {
                    Text(orden.examen, style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.lab_paciente, orden.paciente, orden.dui))
                    Text(stringResource(R.string.lab_ordeno, orden.doctor, orden.ordenada))
                    Text(
                        text = stringResource(R.string.lab_indicaciones, orden.indicaciones),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LineaTiempo(pasos = pasosOrden(orden))
                if (faltaResultado) {
                    Banner(
                        tipo = TipoBanner.AVISO,
                        texto = stringResource(R.string.lab_falta_resultado),
                        accion = stringResource(R.string.lab_cargar_resultado),
                        onAccion = onCargarResultado,
                    )
                }
                when (orden.estado) {
                    EstadoLaboratorio.PENDIENTE -> {
                        BotonPrimario(texto = stringResource(R.string.lab_pasar_en_proceso), onClick = {
                            confirmar =
                                true
                        })
                    }

                    EstadoLaboratorio.EN_PROCESO -> {
                        if (orden.resultado == null) {
                            EstadoVacio(
                                titulo = stringResource(R.string.lab_sin_resultado_titulo),
                                detalle = stringResource(R.string.lab_sin_resultado_detalle),
                            )
                        }
                        BotonPrimario(
                            texto = stringResource(R.string.lab_cargar_resultado),
                            onClick = onCargarResultado,
                        )
                        BotonSecundario(
                            texto =
                                if (orden.resultado == null) {
                                    stringResource(R.string.lab_marcar_completado_falta)
                                } else {
                                    stringResource(R.string.lab_marcar_completado)
                                },
                            onClick = onCompletar,
                            habilitado = orden.resultado != null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    EstadoLaboratorio.COMPLETADO -> {
                        TarjetaHospital {
                            Text(stringResource(R.string.lab_resultado), style = MaterialTheme.typography.bodySmall)
                            Text(orden.resultado.orEmpty())
                        }
                    }
                }
            }

            if (confirmar) {
                DialogoConfirmacion(
                    titulo = stringResource(R.string.lab_confirmar_en_proceso_titulo),
                    texto = stringResource(R.string.lab_confirmar_en_proceso_texto),
                    confirmar = stringResource(R.string.lab_pasar_en_proceso),
                    onConfirmar = {
                        confirmar = false
                        onPasarAEnProceso()
                    },
                    onCancelar = { confirmar = false },
                )
            }
        }
    }
}

@Composable
private fun pasosOrden(orden: OrdenLabUi): List<PasoLinea> {
    fun paso(estado: EstadoLaboratorio): EstadoPaso =
        when {
            orden.estado.ordinal >= estado.ordinal -> EstadoPaso.HECHO
            orden.estado.siguiente == estado -> EstadoPaso.ACTUAL
            else -> EstadoPaso.PENDIENTE
        }
    return listOf(
        PasoLinea(
            nombreEstado(EstadoLaboratorio.PENDIENTE),
            orden.ordenada.substringAfterLast(" "),
            paso(EstadoLaboratorio.PENDIENTE),
        ),
        PasoLinea(
            nombreEstado(EstadoLaboratorio.EN_PROCESO),
            orden.enProcesoDesde?.let {
                stringResource(
                    R.string.lab_por_ti,
                    it,
                )
            } ?: stringResource(R.string.lab_aun_no),
            paso(EstadoLaboratorio.EN_PROCESO),
        ),
        PasoLinea(
            nombreEstado(EstadoLaboratorio.COMPLETADO),
            if (orden.estado == EstadoLaboratorio.COMPLETADO) "" else stringResource(R.string.lab_requiere_resultado),
            paso(EstadoLaboratorio.COMPLETADO),
        ),
    )
}

/** AND-R17 · Cargar resultado (texto y/o PDF de hasta 10 MB). */
@Composable
fun CargarResultadoScreen(
    orden: OrdenLabUi,
    onCerrar: () -> Unit,
    onGuardar: () -> Unit,
    progresoSubida: Float? = 0.6f,
    errorArchivo: String? = null,
) {
    var resultado by rememberSaveable {
        mutableStateOf("Densidad 1.020, pH 6.0. Sin leucocitos ni nitritos. Sedimento normal.")
    }

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.lab_resultado),
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
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            Text("${orden.examen} · ${orden.paciente}", style = MaterialTheme.typography.titleMedium)
            CampoTexto(
                etiqueta = stringResource(R.string.lab_resultado_texto),
                valor = resultado,
                onValor = { resultado = it },
                singleLine = false,
                minLines = 4,
            )
            Text(stringResource(R.string.lab_archivo_pdf), style = MaterialTheme.typography.titleSmall)
            if (progresoSubida != null) {
                TarjetaHospital {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ego-luis-martinez.pdf", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(R.string.lab_subiendo, (progresoSubida * 100).toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconoAccion(R.drawable.ic_close, stringResource(R.string.lab_quitar_archivo), {})
                    }
                    LinearProgressIndicator(
                        progress = { progresoSubida },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        trackColor = BordeSuave,
                    )
                }
            }
            if (errorArchivo != null) Banner(tipo = TipoBanner.ERROR, texto = errorArchivo)
            TextButton(onClick = {}) { Text(stringResource(R.string.lab_adjuntar_otro)) }
            BotonPrimario(
                texto = stringResource(R.string.lab_guardar_completar),
                onClick = onGuardar,
                habilitado = resultado.isNotBlank(),
            )
        }
    }
}

@PreviewPantalla
@Composable
private fun ColaLaboratorioPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        ColaLaboratorioScreen(
            estado = estado.con(DatosMuestra.colaLaboratorio),
            conteos = DatosMuestra.conteoLaboratorio,
            onVolver = {},
            onOrden = {},
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
            estado = estado.con(DatosMuestra.ordenOrina),
            onVolver = {},
            onPasarAEnProceso = {},
            onCargarResultado = {},
            onCompletar = {},
        )
    }
}

@PreviewPantalla
@Composable
private fun DetalleOrden422Preview() {
    HospitalUnicaesTheme {
        DetalleOrdenScreen(
            estado = UiState.Exito(DatosMuestra.ordenOrina),
            onVolver = {},
            onPasarAEnProceso = {},
            onCargarResultado = {},
            onCompletar = {},
            faltaResultado = true,
        )
    }
}

@PreviewPantalla
@Composable
private fun CargarResultadoPreview() {
    HospitalUnicaesTheme { CargarResultadoScreen(orden = DatosMuestra.ordenOrina, onCerrar = {}, onGuardar = {}) }
}
