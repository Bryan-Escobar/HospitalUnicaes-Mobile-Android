@file:OptIn(ExperimentalMaterial3Api::class)

package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.ui.components.BadgeLaboratorio
import sv.edu.unicaes.hospital.ui.components.Banner
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.Chevron
import sv.edu.unicaes.hospital.ui.components.ChipDesplegable
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.DialogoConfirmacion
import sv.edu.unicaes.hospital.ui.components.EstadoPaso
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.EstiloFilaSoporte
import sv.edu.unicaes.hospital.ui.components.FilaChips
import sv.edu.unicaes.hospital.ui.components.FilaDato
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.LineaTiempo
import sv.edu.unicaes.hospital.ui.components.PasoLinea
import sv.edu.unicaes.hospital.ui.components.PestanasHospital
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TarjetaLista
import sv.edu.unicaes.hospital.ui.components.TextosFila
import sv.edu.unicaes.hospital.ui.components.TipoBanner
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.OrdenLabUi
import sv.edu.unicaes.hospital.ui.theme.BordeSuave
import sv.edu.unicaes.hospital.ui.theme.Error
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.Primario50
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Superficie
import sv.edu.unicaes.hospital.ui.theme.Texto2

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
        Column(
            modifier = Modifier.padding(padding).padding(start = Espacio.md, end = Espacio.md, top = Espacio.xs),
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            PestanasHospital(
                titulos =
                    EstadoLaboratorio.entries.map { estadoLab ->
                        val conteo = conteos[estadoLab]?.let { " $it" }.orEmpty()
                        "${glifosLab[estadoLab]} ${nombreEstado(estadoLab)}$conteo"
                    },
                seleccionada = pestana,
                onSeleccion = { pestana = it },
            )
            FilaChips {
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
                // Las órdenes recién llegadas van en su propia tarjeta resaltada.
                val (nuevas, resto) = visibles.partition { it.nueva }
                LazyColumn(
                    contentPadding = PaddingValues(bottom = Espacio.md),
                    verticalArrangement = Arrangement.spacedBy(Espacio.sm),
                ) {
                    listOf(nuevas, resto).filter { it.isNotEmpty() }.forEach { grupo ->
                        item {
                            val resaltada = grupo === nuevas
                            TarjetaLista(
                                elementos = grupo,
                                colorBorde = if (resaltada) Primario700 else BordeSuave,
                                anchoBorde = if (resaltada) 2.dp else 1.dp,
                                colorFondo = if (resaltada) Primario50 else Superficie,
                            ) { orden, ultima ->
                                FilaLista(
                                    titulo = orden.examen,
                                    soporte = "${orden.paciente} · ${orden.doctor} · ${orden.hora}",
                                    onClick = { onOrden(orden) },
                                    divisor = !ultima,
                                    enTarjeta = true,
                                    final = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
                                        ) {
                                            BadgeLaboratorio(orden.estado)
                                            Chevron()
                                        }
                                    },
                                )
                            }
                        }
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
                modifier = Modifier.fillMaxSize().padding(RellenoCuerpo),
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                TarjetaHospital {
                    Text(orden.examen, style = TextStyle(fontSize = 22.sp, lineHeight = 28.sp))
                    Column {
                        FilaDato(stringResource(R.string.lab_paciente), "${orden.paciente} · ${orden.dui}")
                        FilaDato(stringResource(R.string.lab_ordeno), "${orden.doctor} · ${orden.ordenada}")
                        FilaDato(stringResource(R.string.lab_indicaciones), orden.indicaciones, divisor = false)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = Superficie,
                    border = BorderStroke(1.dp, BordeSuave),
                ) {
                    LineaTiempo(
                        pasos = pasosOrden(orden),
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 18.dp),
                    )
                }
                if (faltaResultado) {
                    Banner(
                        tipo = TipoBanner.AVISO,
                        texto = stringResource(R.string.lab_falta_resultado),
                        accion = stringResource(R.string.lab_cargar_resultado),
                        onAccion = onCargarResultado,
                    )
                }
                Spacer(Modifier.weight(1f))
                when (orden.estado) {
                    EstadoLaboratorio.PENDIENTE -> {
                        BotonPrimario(texto = stringResource(R.string.lab_pasar_en_proceso), onClick = {
                            confirmar =
                                true
                        })
                    }

                    EstadoLaboratorio.EN_PROCESO -> {
                        BotonPrimario(
                            texto = stringResource(R.string.lab_cargar_resultado),
                            onClick = onCargarResultado,
                            icono = R.drawable.ic_upload,
                        )
                        BotonPrimario(
                            texto =
                                if (orden.resultado == null) {
                                    stringResource(R.string.lab_marcar_completado_falta)
                                } else {
                                    stringResource(R.string.lab_marcar_completado)
                                },
                            onClick = onCompletar,
                            habilitado = orden.resultado != null,
                        )
                    }

                    EstadoLaboratorio.COMPLETADO -> {
                        TarjetaHospital {
                            Text(
                                text = stringResource(R.string.lab_resultado),
                                style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.sp),
                                color = Texto2,
                            )
                            Text(orden.resultado.orEmpty(), style = EstiloFilaSoporte)
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
                    .padding(RellenoCuerpo),
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            Text("${orden.examen} · ${orden.paciente}", style = EstiloFilaSoporte, color = Texto2)
            CampoTexto(
                etiqueta = stringResource(R.string.lab_resultado_texto),
                valor = resultado,
                onValor = { resultado = it },
                singleLine = false,
                minLines = 3,
            )
            TituloSeccion(stringResource(R.string.lab_archivo_pdf))
            if (progresoSubida != null) {
                TarjetaHospital {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_documento),
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(28.dp),
                        )
                        TextosFila(
                            titulo = "ego-luis-martinez.pdf",
                            soporte = stringResource(R.string.lab_subiendo, (progresoSubida * 100).toInt()),
                            modifier = Modifier.weight(1f),
                        )
                        IconoAccion(
                            R.drawable.ic_close,
                            stringResource(R.string.lab_quitar_archivo),
                            {},
                            tinte = Texto2,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progresoSubida },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        trackColor = BordeSuave,
                        drawStopIndicator = {},
                        gapSize = 0.dp,
                    )
                }
            }
            if (errorArchivo != null) Banner(tipo = TipoBanner.ERROR, texto = errorArchivo)
            Row(
                modifier = Modifier.clickable(onClick = {}).padding(vertical = Espacio.xs),
                horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = Primario700,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.lab_adjuntar_otro),
                    style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                    color = Primario700,
                )
            }
            Spacer(Modifier.weight(1f))
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
