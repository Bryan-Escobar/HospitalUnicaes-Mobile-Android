@file:OptIn(ExperimentalMaterial3Api::class)

package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.domain.model.EstadoAmbulancia
import sv.edu.unicaes.hospital.ui.components.AccionTexto
import sv.edu.unicaes.hospital.ui.components.BadgeAmbulancia
import sv.edu.unicaes.hospital.ui.components.BadgeEstado
import sv.edu.unicaes.hospital.ui.components.Banner
import sv.edu.unicaes.hospital.ui.components.BotonPeligro
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.BotonSecundario
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.ChipDesplegable
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.DialogoConfirmacion
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.EstiloFilaSoporte
import sv.edu.unicaes.hospital.ui.components.FilaChips
import sv.edu.unicaes.hospital.ui.components.FilaDato
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TarjetaLista
import sv.edu.unicaes.hospital.ui.components.TipoBanner
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.AmbulanciaUi
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.DetalleServicioUi
import sv.edu.unicaes.hospital.ui.modelo.EstadoServicio
import sv.edu.unicaes.hospital.ui.modelo.ServicioBitacoraUi
import sv.edu.unicaes.hospital.ui.modelo.SolicitudAmbulanciaUi
import sv.edu.unicaes.hospital.ui.theme.Error
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalTheme
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.Texto2

/**
 * AND-R13 · Ambulancia: el paciente solicita (iOS) y recepción despacha. Solo se despacha si está
 * Disponible; si otro usuario la despachó primero (409) el botón queda en "No disponible".
 */
@Composable
fun AmbulanciaScreen(
    estado: UiState<AmbulanciaUi>,
    onVolver: () -> Unit,
    onDespachar: (SolicitudAmbulanciaUi) -> Unit,
    onBitacora: () -> Unit,
    onReintentar: () -> Unit = {},
    conflicto: Boolean = false,
) {
    var despachando by rememberSaveable { mutableStateOf<String?>(null) }
    var cerrando by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.mas_ambulancia),
                onVolver = onVolver,
                enVivo = true,
            )
        },
    ) { padding ->
        ContenidoConEstado(
            estado = estado,
            mensajeError = stringResource(R.string.ambulancia_error),
            onReintentar = onReintentar,
            modifier = Modifier.padding(padding),
            vacio = {
                EstadoVacio(
                    titulo = stringResource(R.string.ambulancia_vacio_titulo),
                    detalle = stringResource(R.string.ambulancia_vacio_detalle),
                )
            },
        ) { ambulancia ->
            LazyColumn(
                contentPadding = RellenoCuerpo,
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                item {
                    TarjetaHospital {
                        Text(
                            text = stringResource(R.string.ambulancia_unidad),
                            style = EstiloFilaSoporte,
                            color = Texto2,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_ambulancia),
                                contentDescription = null,
                                tint = coloresAmbulancia(ambulancia.estado).texto,
                            )
                            BadgeAmbulancia(ambulancia.estado, grande = true)
                        }
                    }
                }
                if (conflicto) {
                    item { Banner(tipo = TipoBanner.AVISO, texto = stringResource(R.string.ambulancia_conflicto)) }
                }
                item {
                    TituloSeccion(stringResource(R.string.ambulancia_pendientes, ambulancia.solicitudes.size))
                }
                if (ambulancia.solicitudes.isEmpty()) {
                    item {
                        EstadoVacio(
                            titulo = stringResource(R.string.ambulancia_vacio_titulo),
                            detalle = stringResource(R.string.ambulancia_vacio_detalle),
                        )
                    }
                }
                items(ambulancia.solicitudes, key = { it.id }) { solicitud ->
                    TarjetaSolicitudAmbulancia(
                        solicitud = solicitud,
                        puedeDespachar = ambulancia.estado.puedeDespachar && !conflicto,
                        onDespachar = { despachando = solicitud.id },
                    )
                }
                item { TituloSeccion(stringResource(R.string.ambulancia_servicio_actual)) }
                item {
                    if (ambulancia.estado == EstadoAmbulancia.EN_SERVICIO) {
                        TarjetaHospital {
                            Text(ambulancia.servicioActual.orEmpty(), style = MaterialTheme.typography.titleMedium)
                            BotonPrimario(texto = stringResource(R.string.ambulancia_cerrar_servicio), onClick = {
                                cerrando =
                                    true
                            })
                        }
                    } else {
                        TarjetaHospital {
                            Text(
                                text = stringResource(R.string.ambulancia_sin_servicio),
                                style = EstiloFilaSoporte,
                                color = Texto2,
                            )
                        }
                    }
                }
                item { AccionTexto(texto = stringResource(R.string.ambulancia_ver_bitacora), onClick = onBitacora) }
            }

            val solicitud = ambulancia.solicitudes.firstOrNull { it.id == despachando }
            if (solicitud != null) {
                DialogoConfirmacion(
                    titulo = stringResource(R.string.ambulancia_despachar_titulo),
                    texto = stringResource(R.string.ambulancia_despachar_texto),
                    confirmar = stringResource(R.string.ambulancia_despachar),
                    onConfirmar = {
                        despachando = null
                        onDespachar(solicitud)
                    },
                    onCancelar = { despachando = null },
                )
            }
        }
    }

    if (cerrando) HojaCerrarServicio(onCerrar = { cerrando = false })
}

@Composable
private fun TarjetaSolicitudAmbulancia(
    solicitud: SolicitudAmbulanciaUi,
    puedeDespachar: Boolean,
    onDespachar: () -> Unit,
) {
    TarjetaHospital(colorBorde = Error, anchoBorde = 2.5.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = solicitud.paciente, style = EstiloTituloTarjeta, modifier = Modifier.weight(1f))
            Text(
                text = "${solicitud.hora} · ${solicitud.hace}",
                style = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
                color = Texto2,
            )
        }
        Column {
            TextoEtiquetado(stringResource(R.string.ambulancia_tipo, solicitud.tipo))
            TextoEtiquetado(stringResource(R.string.ambulancia_ubicacion, solicitud.ubicacion))
            TextoEtiquetado(stringResource(R.string.ambulancia_descripcion, solicitud.descripcion))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            BotonSecundario(
                texto = stringResource(R.string.accion_rechazar),
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            if (puedeDespachar) {
                BotonPeligro(
                    texto = stringResource(R.string.ambulancia_despachar),
                    onClick = onDespachar,
                    icono = R.drawable.ic_sirena,
                    modifier = Modifier.weight(1f),
                )
            } else {
                BotonSecundario(
                    texto = stringResource(R.string.ambulancia_no_disponible),
                    onClick = {},
                    habilitado = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** "Etiqueta: valor" con la etiqueta en gris (14/22). */
@Composable
private fun TextoEtiquetado(texto: String) {
    val corte = texto.indexOf(": ")
    Text(
        text =
            buildAnnotatedString {
                if (corte > 0) {
                    withStyle(SpanStyle(color = Texto2)) { append(texto.substring(0, corte + 1)) }
                    append(texto.substring(corte + 1))
                } else {
                    append(texto)
                }
            },
        style = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    )
}

@Composable
private fun coloresAmbulancia(estado: EstadoAmbulancia) =
    with(HospitalTheme.estados) {
        when (estado) {
            EstadoAmbulancia.DISPONIBLE -> ambulanciaDisponible
            EstadoAmbulancia.EN_SERVICIO -> ambulanciaEnServicio
            EstadoAmbulancia.EN_MANTENIMIENTO -> ambulanciaEnMantenimiento
        }
    }

/** Hoja de cierre del servicio: hora de llegada, destino, observaciones y kilometraje. */
@Composable
private fun HojaCerrarServicio(onCerrar: () -> Unit) {
    var llegada by rememberSaveable { mutableStateOf("") }
    var destino by rememberSaveable { mutableStateOf("") }
    var observaciones by rememberSaveable { mutableStateOf("") }
    var kilometraje by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(
            modifier = Modifier.padding(horizontal = Espacio.md).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            Text(stringResource(R.string.ambulancia_cerrar_servicio), style = MaterialTheme.typography.titleLarge)
            CampoTexto(stringResource(R.string.ambulancia_hora_llegada), llegada, { llegada = it })
            CampoTexto(stringResource(R.string.ambulancia_destino), destino, { destino = it })
            CampoTexto(stringResource(R.string.ambulancia_kilometraje), kilometraje, { kilometraje = it })
            CampoTexto(
                etiqueta = stringResource(R.string.ambulancia_observaciones),
                valor = observaciones,
                onValor = { observaciones = it },
                singleLine = false,
                minLines = 2,
            )
            BotonPrimario(
                texto = stringResource(R.string.ambulancia_cerrar_servicio),
                onClick = onCerrar,
                modifier = Modifier.padding(vertical = Espacio.sm),
            )
        }
    }
}

/** AND-R14 · Bitácora de ambulancia (solo lectura) con filtros por fecha, estado y tipo. */
@Composable
fun BitacoraScreen(
    estado: UiState<List<ServicioBitacoraUi>>,
    detalle: DetalleServicioUi?,
    onVolver: () -> Unit,
    onReintentar: () -> Unit = {},
) {
    Scaffold(
        topBar = { TopBarHospital(titulo = stringResource(R.string.bitacora_titulo), onVolver = onVolver) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(start = Espacio.md, end = Espacio.md, top = Espacio.xs),
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            FilaChips {
                ChipDesplegable(stringResource(R.string.bitacora_filtro_fecha))
                ChipDesplegable(stringResource(R.string.citas_filtro_estado))
                ChipDesplegable(stringResource(R.string.bitacora_filtro_tipo))
            }
            ContenidoConEstado(
                estado = estado,
                mensajeError = stringResource(R.string.bitacora_error),
                onReintentar = onReintentar,
                vacio = {
                    EstadoVacio(
                        titulo = stringResource(R.string.bitacora_vacio_titulo),
                        detalle = stringResource(R.string.bitacora_vacio_detalle),
                        accion = stringResource(R.string.accion_quitar_filtros),
                    )
                },
            ) { servicios ->
                LazyColumn(
                    contentPadding = PaddingValues(bottom = Espacio.md),
                    verticalArrangement = Arrangement.spacedBy(Espacio.sm),
                ) {
                    item {
                        TarjetaLista(servicios) { servicio, ultimo ->
                            FilaLista(
                                titulo =
                                    listOfNotNull(
                                        servicio.fecha,
                                        servicio.horario.ifEmpty { null },
                                    ).joinToString(" · "),
                                soporte =
                                    listOfNotNull(
                                        servicio.tipo,
                                        servicio.paciente,
                                        servicio.duracion,
                                    ).joinToString(" · "),
                                divisor = !ultimo,
                                enTarjeta = true,
                                final = { BadgeServicio(servicio.estado) },
                            )
                        }
                    }
                    if (detalle != null) {
                        item {
                            TarjetaHospital {
                                Text(
                                    text = stringResource(R.string.bitacora_detalle, detalle.fecha),
                                    style = EstiloTituloTarjeta,
                                )
                                Column {
                                    FilaDato(stringResource(R.string.bitacora_despacho), detalle.despacho)
                                    FilaDato(stringResource(R.string.ambulancia_destino), detalle.destino)
                                    FilaDato(
                                        stringResource(R.string.bitacora_km),
                                        detalle.kilometraje.toString(),
                                        divisor = false,
                                    )
                                }
                                TextoEtiquetado(stringResource(R.string.bitacora_observaciones, detalle.observaciones))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeServicio(estado: EstadoServicio) {
    val estados = HospitalTheme.estados
    when (estado) {
        EstadoServicio.FINALIZADO -> {
            BadgeEstado(stringResource(R.string.bitacora_finalizado), estados.labCompletado, glifo = "✓")
        }

        EstadoServicio.RECHAZADA -> {
            BadgeEstado(stringResource(R.string.solicitudes_rechazada), estados.solicitudRechazada, glifo = "✕")
        }

        EstadoServicio.EN_MANTENIMIENTO -> {
            BadgeAmbulancia(EstadoAmbulancia.EN_MANTENIMIENTO)
        }
    }
}

@PreviewPantalla
@Composable
private fun AmbulanciaPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        AmbulanciaScreen(estado = estado.con(DatosMuestra.ambulancia), onVolver = {}, onDespachar = {}, onBitacora = {})
    }
}

@PreviewPantalla
@Composable
private fun AmbulanciaConflictoPreview() {
    HospitalUnicaesTheme {
        AmbulanciaScreen(
            estado =
                UiState.Exito(
                    DatosMuestra.ambulancia.copy(
                        estado = EstadoAmbulancia.EN_SERVICIO,
                        servicioActual = "Luis Martínez · Dolor de pecho · despachada 10:14",
                    ),
                ),
            onVolver = {},
            onDespachar = {},
            onBitacora = {},
            conflicto = true,
        )
    }
}

@PreviewPantalla
@Composable
private fun BitacoraPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        BitacoraScreen(
            estado = estado.con(DatosMuestra.bitacora),
            detalle = DatosMuestra.detalleServicio,
            onVolver = {},
        )
    }
}
