@file:OptIn(ExperimentalMaterial3Api::class)

package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion
import sv.edu.unicaes.hospital.ui.components.AccionTexto
import sv.edu.unicaes.hospital.ui.components.Avatar
import sv.edu.unicaes.hospital.ui.components.Banner
import sv.edu.unicaes.hospital.ui.components.BotonPeligro
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.CabeceraPersona
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.DialogoConfirmacion
import sv.edu.unicaes.hospital.ui.components.EstadoPaso
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.FabHospital
import sv.edu.unicaes.hospital.ui.components.FilaDato
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.PestanasHospital
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.SegmentosHospital
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TarjetaLista
import sv.edu.unicaes.hospital.ui.components.TextosFila
import sv.edu.unicaes.hospital.ui.components.TipoBanner
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.components.iniciales
import sv.edu.unicaes.hospital.ui.modelo.AsignacionCamaUi
import sv.edu.unicaes.hospital.ui.modelo.CamaUi
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.FichaPacienteUi
import sv.edu.unicaes.hospital.ui.modelo.HabitacionUi
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import sv.edu.unicaes.hospital.ui.theme.BordeSuave
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.Fondo
import sv.edu.unicaes.hospital.ui.theme.HospitalTheme
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.Neutro700
import sv.edu.unicaes.hospital.ui.theme.Primario200
import sv.edu.unicaes.hospital.ui.theme.Primario50
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Superficie
import sv.edu.unicaes.hospital.ui.theme.Texto2
import sv.edu.unicaes.hospital.ui.theme.Texto3

// Camas: mapa, ingreso, traslado y alta (AND-R8 a AND-R11). Capacidad fija de 11 habitaciones y
// 17 camas; ninguna pantalla crea ni elimina habitaciones.

private fun HabitacionUi.codigoCama(cama: CamaUi) =
    if (cama.etiqueta.isEmpty()) codigo else "$codigo · ${cama.etiqueta}"

private fun List<HabitacionUi>.ocupadas() = sumOf { h -> h.camas.count { !it.libre } }

private fun List<HabitacionUi>.totalCamas() = sumOf { it.camas.size }

private fun List<HabitacionUi>.libres() = flatMap { h -> h.camas.filter { it.libre }.map { h to it } }

/** Botón de cama (alto 48, radio 10): Libre ○ · Ocupada ● · Elegida ✓. */
@Composable
private fun CamaBoton(
    cama: CamaUi,
    elegida: Boolean,
    mostrarNombre: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val estados = HospitalTheme.estados
    val colores =
        when {
            elegida -> estados.camaElegida
            cama.libre -> estados.camaLibre
            else -> estados.camaOcupada
        }
    val separador = if (mostrarNombre) " · " else " "
    val prefijo = if (cama.etiqueta.isEmpty()) "" else "${cama.etiqueta}$separador"
    val texto =
        when {
            elegida -> "✓ $prefijo${stringResource(R.string.camas_elegida)}"
            cama.libre -> "○ $prefijo${stringResource(R.string.camas_libre)}"
            mostrarNombre -> "● $prefijo${cama.ocupante}"
            else -> "● $prefijo${stringResource(R.string.camas_ocupada)}"
        }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(10.dp),
        color = colores.fondo,
        contentColor = colores.texto,
        border = BorderStroke(if (elegida) 3.dp else 1.dp, if (elegida) Primario200 else colores.borde),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(
                text = texto,
                style = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** Sala: fondo blanco, borde #E2E8F0, radio 14 y padding 8; título 13/700. */
@Composable
private fun SalaHabitacion(
    habitacion: HabitacionUi,
    camaElegida: String?,
    mostrarNombres: Boolean,
    onCama: (CamaUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Superficie)
                .border(1.dp, BordeSuave, RoundedCornerShape(14.dp))
                .padding(Espacio.xs),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = habitacion.codigo,
            style = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
        )
        habitacion.camas.forEach { cama ->
            CamaBoton(
                cama = cama,
                elegida = cama.id == camaElegida,
                mostrarNombre = mostrarNombres,
                onClick = { onCama(cama) },
            )
        }
    }
}

@Composable
private fun RejillaHabitaciones(
    habitaciones: List<HabitacionUi>,
    columnas: Int,
    camaElegida: String?,
    mostrarNombres: Boolean,
    onCama: (HabitacionUi, CamaUi) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        habitaciones.chunked(columnas).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                fila.forEach { habitacion ->
                    SalaHabitacion(
                        habitacion = habitacion,
                        camaElegida = camaElegida,
                        mostrarNombres = mostrarNombres,
                        onCama = { onCama(habitacion, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LeyendaCamas() {
    Text(
        text = stringResource(R.string.camas_leyenda),
        style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
        color = Texto2,
    )
}

/** AND-R8 · Mapa de camas por planta. Vista interna con sesión: muestra el nombre abreviado. */
@Composable
fun MapaCamasScreen(
    estado: UiState<List<HabitacionUi>>,
    onIngreso: () -> Unit,
    onVerFicha: () -> Unit,
    onTrasladar: () -> Unit,
    onAlta: () -> Unit,
    onReintentar: () -> Unit = {},
) {
    var planta by rememberSaveable { mutableIntStateOf(0) }
    var menuCama by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.tab_camas),
                enVivo = true,
                acciones = { IconoAccion(R.drawable.ic_more_vert, stringResource(R.string.accion_mas_opciones), {}) },
            )
        },
        floatingActionButton = { FabHospital(texto = stringResource(R.string.camas_ingreso), onClick = onIngreso) },
    ) { padding ->
        ContenidoConEstado(
            estado = estado,
            mensajeError = stringResource(R.string.camas_error),
            onReintentar = onReintentar,
            modifier = Modifier.padding(padding),
            vacio = {
                EstadoVacio(
                    titulo = stringResource(R.string.camas_vacio_titulo),
                    detalle = stringResource(R.string.camas_vacio_detalle),
                    icono = R.drawable.ic_bed,
                )
            },
        ) { habitaciones ->
            val p2 = habitaciones.filter { it.tipo == TipoHabitacion.SEMIPRIVADA }
            val p3 = habitaciones.filter { it.tipo == TipoHabitacion.VIP }
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(start = Espacio.md, end = Espacio.md, top = Espacio.xs, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                PestanasHospital(
                    titulos =
                        listOf(
                            stringResource(R.string.camas_tab_planta2, p2.ocupadas(), p2.totalCamas()),
                            stringResource(R.string.camas_tab_planta3, p3.ocupadas(), p3.totalCamas()),
                        ),
                    seleccionada = planta,
                    onSeleccion = { planta = it },
                )
                LeyendaCamas()
                RejillaHabitaciones(
                    habitaciones = if (planta == 0) p2 else p3,
                    columnas = 2,
                    camaElegida = null,
                    mostrarNombres = true,
                    onCama = { _, cama -> if (cama.libre) onIngreso() else menuCama = cama.id },
                )
                DropdownMenu(expanded = menuCama != null, onDismissRequest = { menuCama = null }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.camas_ver_ficha)) }, onClick = {
                        menuCama = null
                        onVerFicha()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.camas_trasladar)) }, onClick = {
                        menuCama = null
                        onTrasladar()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.camas_dar_alta)) }, onClick = {
                        menuCama = null
                        onAlta()
                    })
                }
            }
        }
    }
}

/** Stepper horizontal: ✓ Paciente — ✓ Tipo — (3) Cama. */
@Composable
private fun StepperIngreso() {
    val pasos =
        listOf(
            stringResource(R.string.ingreso_paso_paciente) to EstadoPaso.HECHO,
            stringResource(R.string.ingreso_paso_tipo) to EstadoPaso.HECHO,
            stringResource(R.string.ingreso_paso_cama) to EstadoPaso.ACTUAL,
        )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        pasos.forEachIndexed { indice, (titulo, estado) ->
            val circulo = Modifier.size(22.dp).clip(CircleShape)
            Box(
                modifier =
                    if (estado == EstadoPaso.HECHO) {
                        circulo.background(Primario700)
                    } else {
                        circulo.border(2.dp, Primario700, CircleShape)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (estado == EstadoPaso.HECHO) "✓" else "${indice + 1}",
                    color = if (estado == EstadoPaso.HECHO) Superficie else Primario700,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = titulo,
                style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                color = Neutro700,
            )
            if (indice < pasos.lastIndex) {
                Box(Modifier.weight(1f).height(2.dp).background(Primario700))
            }
        }
    }
}

/**
 * AND-R9 · Ingreso y asignación de cama (alta fidelidad). Sugiere la primera cama libre de la
 * planta. Si otra persona gana la cama (409), se avisa y se propone la siguiente libre.
 */
@Composable
fun IngresoCamaScreen(
    paciente: PacienteUi,
    habitaciones: List<HabitacionUi>,
    onVolver: () -> Unit,
    onCambiarPaciente: () -> Unit,
    onConfirmar: (String) -> Unit,
    camaEnConflicto: String? = null,
) {
    var tipo by rememberSaveable { mutableStateOf(TipoHabitacion.SEMIPRIVADA) }
    val delTipo = habitaciones.filter { it.tipo == tipo }
    val libres = delTipo.libres()
    val libresPorTipo = TipoHabitacion.entries.associateWith { t -> habitaciones.filter { it.tipo == t }.libres().size }
    val sugerida = libres.firstOrNull { (h, c) -> h.codigoCama(c) != camaEnConflicto }
    var elegida by rememberSaveable(tipo) { mutableStateOf(sugerida?.second?.id) }
    val codigoElegida = libres.firstOrNull { it.second.id == elegida }?.let { (h, c) -> h.codigoCama(c) }

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.ingreso_titulo),
                onVolver = onVolver,
                enVivo = true,
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(Fondo).navigationBarsPadding()) {
                HorizontalDivider(color = BordeSuave)
                Column(
                    modifier = Modifier.padding(start = Espacio.md, end = Espacio.md, top = Espacio.sm, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (camaEnConflicto != null) {
                        SnackbarConflicto(
                            texto = stringResource(R.string.ingreso_conflicto, camaEnConflicto),
                            accion = stringResource(R.string.ingreso_elegir_otra),
                        )
                    }
                    BotonPrimario(
                        texto = "✓ " + stringResource(R.string.ingreso_confirmar, codigoElegida.orEmpty()),
                        onClick = { codigoElegida?.let(onConfirmar) },
                        habilitado = codigoElegida != null,
                    )
                }
            }
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
            StepperIngreso()
            TarjetaHospital {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
                ) {
                    Avatar(iniciales(paciente.nombre))
                    TextosFila(
                        titulo = paciente.nombre,
                        soporte = stringResource(R.string.ingreso_doc, paciente.dui, paciente.etiqueta.orEmpty()),
                        modifier = Modifier.weight(1f),
                    )
                    AccionTexto(texto = stringResource(R.string.accion_cambiar), onClick = onCambiarPaciente)
                }
            }
            TituloSeccion(stringResource(R.string.ingreso_tipo))
            SelectorTipo(tipo = tipo, libresPorTipo = libresPorTipo, onTipo = { tipo = it })
            if (libres.isEmpty()) {
                val otro = if (tipo == TipoHabitacion.VIP) TipoHabitacion.SEMIPRIVADA else TipoHabitacion.VIP
                EstadoVacio(
                    titulo = stringResource(R.string.ingreso_sin_camas, nombreTipo(tipo)),
                    detalle =
                        stringResource(
                            R.string.ingreso_sin_camas_detalle,
                            nombreTipo(otro),
                            libresPorTipo[otro] ?: 0,
                        ),
                    accion = stringResource(R.string.ingreso_cambiar_a, nombreTipo(otro)),
                    onAccion = { tipo = otro },
                )
            } else {
                TituloSeccion(
                    stringResource(
                        R.string.ingreso_planta,
                        tipo.planta,
                        nombreTipoPlural(tipo),
                        delTipo.ocupadas(),
                        delTipo.totalCamas(),
                    ),
                )
                // En esta vista no se muestran nombres de pacientes.
                RejillaHabitaciones(
                    habitaciones = delTipo,
                    columnas = 3,
                    camaElegida = elegida,
                    mostrarNombres = false,
                    onCama = { _, cama -> if (cama.libre) elegida = cama.id },
                )
                sugerida?.let { (h, c) ->
                    val codigo = h.codigoCama(c).replace(" · ", " · Cama ")
                    val texto = stringResource(R.string.ingreso_sugerida, codigo)
                    Banner(
                        tipo = TipoBanner.INFO,
                        texto =
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(codigo) }
                                append(texto.removePrefix(codigo))
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun nombreTipo(tipo: TipoHabitacion) =
    stringResource(if (tipo == TipoHabitacion.VIP) R.string.tipo_vip else R.string.tipo_semiprivada)

@Composable
private fun nombreTipoPlural(tipo: TipoHabitacion) =
    stringResource(if (tipo == TipoHabitacion.VIP) R.string.ocupacion_vip else R.string.ocupacion_semiprivadas)

@Composable
private fun SelectorTipo(
    tipo: TipoHabitacion,
    libresPorTipo: Map<TipoHabitacion, Int>,
    onTipo: (TipoHabitacion) -> Unit,
) {
    val opciones = listOf(TipoHabitacion.VIP, TipoHabitacion.SEMIPRIVADA)
    SegmentosHospital(
        opciones =
            opciones.map {
                stringResource(
                    R.string.ingreso_tipo_libres,
                    nombreTipo(it),
                    libresPorTipo[it] ?: 0,
                )
            },
        seleccionada = opciones.indexOf(tipo),
        onSeleccion = { onTipo(opciones[it]) },
    )
}

/** Snackbar oscuro (#334155, radio 8) con acción en #A5F3FC. */
@Composable
private fun SnackbarConflicto(
    texto: String,
    accion: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Neutro700)
                .padding(start = 14.dp, end = Espacio.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = texto,
            color = Superficie,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
        )
        TextButton(onClick = {}) { Text(accion, color = Primario200) }
    }
}

/** AND-R10 · Traslado de cama. */
@Composable
fun TrasladoScreen(
    paciente: String,
    asignacion: AsignacionCamaUi,
    habitaciones: List<HabitacionUi>,
    onVolver: () -> Unit,
    onConfirmar: (String) -> Unit,
) {
    var tipo by rememberSaveable { mutableStateOf(TipoHabitacion.VIP) }
    val libres = habitaciones.filter { it.tipo == tipo }.libres()
    var elegida by rememberSaveable(tipo) { mutableStateOf(libres.firstOrNull()?.let { (h, c) -> h.codigoCama(c) }) }
    var motivo by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = { TopBarHospital(titulo = stringResource(R.string.camas_trasladar), onVolver = onVolver) },
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
            TarjetaHospital {
                TextosFila(
                    titulo = paciente,
                    soporte = stringResource(R.string.traslado_ingresado_desde, asignacion.desde),
                )
            }
            TarjetaHospital {
                Text(
                    text = stringResource(R.string.traslado_cama_actual),
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.sp),
                    color = Texto2,
                )
                Text(
                    text = "${stringResource(
                        R.string.ficha_cama,
                        asignacion.habitacion,
                        asignacion.cama,
                    )} · ${nombreTipo(asignacion.tipo).lowercase()}",
                    style = EstiloTituloTarjeta,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = Texto3,
                modifier = Modifier.align(Alignment.CenterHorizontally).rotate(90f),
            )
            TituloSeccion(stringResource(R.string.traslado_nueva_cama))
            val opciones = listOf(TipoHabitacion.VIP, TipoHabitacion.SEMIPRIVADA)
            SegmentosHospital(
                opciones =
                    opciones.map {
                        stringResource(R.string.traslado_opcion_planta, it.planta, nombreTipo(it).lowercase())
                            .replace("· vip", "· VIP")
                    },
                seleccionada = opciones.indexOf(tipo),
                onSeleccion = { tipo = opciones[it] },
            )
            if (libres.isEmpty()) {
                EstadoVacio(
                    titulo = stringResource(R.string.traslado_vacio_titulo),
                    detalle = stringResource(R.string.traslado_vacio_detalle),
                    icono = R.drawable.ic_bed,
                )
            }
            libres.forEach { (habitacion, cama) ->
                val codigo = habitacion.codigoCama(cama)
                val activa = elegida == codigo
                TarjetaLista(
                    elementos = listOf(codigo),
                    colorBorde = if (activa) Primario700 else BordeSuave,
                    anchoBorde = if (activa) 2.dp else 1.dp,
                    colorFondo = if (activa) Primario50 else Superficie,
                    modifier =
                        Modifier.selectable(
                            selected = activa,
                            onClick = { elegida = codigo },
                            role = Role.RadioButton,
                        ),
                ) { _, _ ->
                    FilaLista(
                        titulo = codigo,
                        soporte =
                            if (cama.etiqueta.isEmpty()) {
                                stringResource(R.string.traslado_cama_unica)
                            } else {
                                stringResource(R.string.traslado_cama_libre, cama.etiqueta)
                            },
                        divisor = false,
                        enTarjeta = true,
                        final = {
                            if (activa) {
                                Text(
                                    text = "✓",
                                    style =
                                        TextStyle(
                                            fontSize = 18.sp,
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    color = Primario700,
                                )
                            }
                        },
                    )
                }
            }
            CampoTexto(
                etiqueta = stringResource(R.string.traslado_motivo),
                valor = motivo,
                onValor = { motivo = it },
                placeholder = stringResource(R.string.traslado_motivo_ejemplo),
            )
            BotonPrimario(
                texto = stringResource(R.string.traslado_confirmar, elegida.orEmpty()),
                onClick = { elegida?.let(onConfirmar) },
                habilitado = elegida != null,
            )
        }
    }
}

/** AND-R11 · Alta. Destructiva: pide confirmación y la cama queda Libre al instante. */
@Composable
fun AltaScreen(
    ficha: FichaPacienteUi,
    onVolver: () -> Unit,
    onDarAlta: () -> Unit,
) {
    var confirmar by rememberSaveable { mutableStateOf(false) }
    val asignacion = ficha.asignacion ?: return

    Scaffold(
        topBar = { TopBarHospital(titulo = stringResource(R.string.camas_dar_alta), onVolver = onVolver) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(RellenoCuerpo),
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            CabeceraPersona(nombre = ficha.paciente.nombre, soporte = ficha.paciente.dui)
            TarjetaHospital {
                Column {
                    FilaDato(stringResource(R.string.alta_cama), asignacion.codigo)
                    FilaDato(stringResource(R.string.alta_ingreso), asignacion.ingreso)
                    FilaDato(stringResource(R.string.alta_dias), asignacion.diasHospitalizado.toString())
                    FilaDato(stringResource(R.string.alta_medico), asignacion.medicoTratante, divisor = false)
                }
            }
            Banner(
                tipo = TipoBanner.AVISO,
                texto = stringResource(R.string.alta_aviso),
                icono = R.drawable.ic_notifications,
            )
            Spacer(Modifier.weight(1f))
            BotonPeligro(
                texto = stringResource(R.string.camas_dar_alta),
                onClick = { confirmar = true },
                icono = R.drawable.ic_logout,
                alto = 52.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmar) {
        DialogoConfirmacion(
            titulo = stringResource(R.string.alta_dialogo_titulo, ficha.paciente.nombre),
            texto = stringResource(R.string.alta_dialogo_texto, asignacion.codigo),
            confirmar = stringResource(R.string.camas_dar_alta),
            onConfirmar = {
                confirmar = false
                onDarAlta()
            },
            onCancelar = { confirmar = false },
            destructivo = true,
        )
    }
}

@PreviewPantalla
@Composable
private fun MapaCamasPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        MapaCamasScreen(
            estado = estado.con(DatosMuestra.planta2 + DatosMuestra.planta3),
            onIngreso = {},
            onVerFicha = {},
            onTrasladar = {},
            onAlta = {},
        )
    }
}

@PreviewPantalla
@Composable
private fun IngresoCamaPreview() {
    HospitalUnicaesTheme {
        IngresoCamaScreen(
            paciente = DatosMuestra.pacienteIngreso,
            habitaciones = DatosMuestra.planta2 + DatosMuestra.planta3,
            onVolver = {},
            onCambiarPaciente = {},
            onConfirmar = {},
        )
    }
}

@PreviewPantalla
@Composable
private fun IngresoCamaConflictoPreview() {
    HospitalUnicaesTheme {
        IngresoCamaScreen(
            paciente = DatosMuestra.pacienteIngreso,
            habitaciones = DatosMuestra.planta2 + DatosMuestra.planta3,
            onVolver = {},
            onCambiarPaciente = {},
            onConfirmar = {},
            camaEnConflicto = "HAB-202 · B",
        )
    }
}

@PreviewPantalla
@Composable
private fun TrasladoPreview() {
    HospitalUnicaesTheme {
        TrasladoScreen(
            paciente = "Luis Martínez",
            asignacion = DatosMuestra.asignacionLuis,
            habitaciones = DatosMuestra.planta2 + DatosMuestra.planta3,
            onVolver = {},
            onConfirmar = {},
        )
    }
}

@PreviewPantalla
@Composable
private fun AltaPreview() {
    HospitalUnicaesTheme { AltaScreen(ficha = DatosMuestra.fichaLuis, onVolver = {}, onDarAlta = {}) }
}
