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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.core.mensaje
import sv.edu.unicaes.hospital.domain.model.Empleado
import sv.edu.unicaes.hospital.domain.model.TipoTurno
import sv.edu.unicaes.hospital.ui.components.BadgeEstado
import sv.edu.unicaes.hospital.ui.components.Banner
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.ChipsFiltro
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.EtiquetaPersonal
import sv.edu.unicaes.hospital.ui.components.FabHospital
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaLista
import sv.edu.unicaes.hospital.ui.components.TipoBanner
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.AreaTurnosUi
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.DiaUi
import sv.edu.unicaes.hospital.ui.modelo.TurnoPersonalUi
import sv.edu.unicaes.hospital.ui.modelo.etiquetaClave
import sv.edu.unicaes.hospital.ui.modelo.etiquetaTipoPersonal
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalTheme
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.Neutro700

/** Lo que la pantalla le pide al [TurnosPersonalViewModel]; las previews usan [SinAccionesTurnos]. */
interface AccionesTurnos {
    fun cargar()

    fun onSemana(delta: Int)

    fun onDia(indice: Int)

    fun onTipo(indice: Int)

    fun abrirNuevo()

    fun abrirAjuste(turnoId: String)

    fun cerrarHoja()

    fun cargarEmpleados()

    fun onPersona(id: String)

    fun onArea(valor: String)

    fun onFecha(clave: String)

    fun onInicio(valor: String)

    fun onFin(valor: String)

    fun onTipoTurno(tipo: TipoTurno)

    fun onNotas(valor: String)

    fun guardar()
}

/**
 * AND-R12 · Control de turnos (HUNI-53): semana con flechas, días como chips y los turnos del día agrupados por
 * área, filtrables por tipo de personal. Tocar un turno lo abre para ajustarlo; el solape lo rechaza la base.
 */
@Composable
fun TurnosPersonalScreen(
    estado: TurnosPersonalUiState,
    onVolver: () -> Unit,
    acciones: AccionesTurnos,
    onMisTurnos: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.tab_turnos),
                onVolver = onVolver,
                acciones = {
                    IconoAccion(R.drawable.ic_schedule, stringResource(R.string.turnos_mis_turnos), onMisTurnos)
                },
            )
        },
        floatingActionButton = {
            FabHospital(texto = stringResource(R.string.turnos_nuevo), onClick = acciones::abrirNuevo)
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(start = Espacio.md, end = Espacio.md, top = Espacio.xs),
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            SelectorSemana(estado.etiquetaSemana, onSemana = acciones::onSemana)
            ChipsFiltro(
                opciones = estado.dias.map { it.etiqueta },
                seleccionada = estado.dia,
                onSeleccion = acciones::onDia,
            )
            if (estado.tipos.size > 1) {
                ChipsFiltro(
                    opciones = listOf(stringResource(R.string.turnos_tipo_todos)) + estado.tipos,
                    seleccionada = estado.tipo,
                    onSeleccion = acciones::onTipo,
                )
            }
            ContenidoConEstado(
                estado = estado.estado,
                mensajeError = stringResource(R.string.turnos_error),
                onReintentar = acciones::cargar,
                vacio = {
                    EstadoVacio(
                        titulo = stringResource(R.string.turnos_vacio_titulo),
                        detalle = stringResource(R.string.turnos_vacio_detalle),
                        icono = R.drawable.ic_schedule,
                        accion = stringResource(R.string.turnos_nuevo),
                        onAccion = acciones::abrirNuevo,
                    )
                },
            ) { areas ->
                ListaTurnosDia(areas, onTurno = acciones::abrirAjuste)
            }
        }
    }

    estado.hoja?.let { hoja ->
        HojaTurno(hoja = hoja, dias = estado.dias, empleados = estado.empleados, acciones = acciones)
    }
}

@Composable
private fun SelectorSemana(
    etiqueta: String,
    onSemana: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Espacio.md, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoAccion(
            R.drawable.ic_chevron_left,
            stringResource(R.string.turnos_ir_semana_anterior),
            { onSemana(-1) },
            tinte = Neutro700,
        )
        Text(text = etiqueta, style = EstiloTituloTarjeta)
        IconoAccion(
            R.drawable.ic_chevron_right,
            stringResource(R.string.turnos_ir_semana_siguiente),
            { onSemana(1) },
            tinte = Neutro700,
        )
    }
}

/** Un título por área y una tarjeta con su personal: nombre, horario, "En curso" y el tipo de personal. */
@Composable
private fun ListaTurnosDia(
    areas: List<AreaTurnosUi>,
    onTurno: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(Espacio.sm),
    ) {
        areas.forEach { area ->
            item(key = "area-${area.area}") { TituloSeccion(area.area) }
            item(key = "turnos-${area.area}") {
                TarjetaLista(area.turnos) { turno, ultimo ->
                    FilaTurnoPersonal(turno, divisor = !ultimo, onClick = { onTurno(turno.id) })
                }
            }
        }
    }
}

@Composable
private fun FilaTurnoPersonal(
    turno: TurnoPersonalUi,
    divisor: Boolean,
    onClick: () -> Unit,
) {
    FilaLista(
        titulo = turno.persona,
        soporte = turno.horario,
        onClick = onClick,
        divisor = divisor,
        enTarjeta = true,
        final = {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xxs)) {
                if (turno.enCurso) {
                    BadgeEstado(
                        texto = stringResource(R.string.turno_en_curso),
                        colores = HospitalTheme.estados.turnoEnCurso,
                        glifo = "●",
                    )
                }
                EtiquetaPersonal(turno.tipo)
            }
        },
    )
}

/** Hoja "Nuevo turno" / "Ajustar turno". Guardar se habilita con persona, área y horas `HH:mm` distintas. */
@Composable
private fun HojaTurno(
    hoja: HojaTurnoUiState,
    dias: List<DiaUi>,
    empleados: UiState<List<Empleado>>,
    acciones: AccionesTurnos,
) {
    val tipos = TipoTurno.entries
    ModalBottomSheet(
        onDismissRequest = acciones::cerrarHoja,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Espacio.md)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            Text(
                text = stringResource(if (hoja.ajustando) R.string.turnos_ajustar else R.string.turnos_nuevo),
                style = MaterialTheme.typography.titleLarge,
            )
            SelectorPersona(
                empleados = empleados,
                elegido = hoja.personalId,
                habilitado = !hoja.ajustando,
                onPersona = acciones::onPersona,
                onReintentar = acciones::cargarEmpleados,
            )
            CampoTexto(
                etiqueta = stringResource(R.string.campo_area),
                valor = hoja.area,
                onValor = acciones::onArea,
                placeholder = stringResource(R.string.turnos_area_ejemplo),
            )
            Text(stringResource(R.string.campo_fecha), style = MaterialTheme.typography.bodySmall)
            val fecha = dias.indexOfFirst { it.clave == hoja.fecha }
            ChipsFiltro(
                opciones = if (fecha >= 0) dias.map { it.etiqueta } else listOf(etiquetaClave(hoja.fecha)),
                seleccionada = fecha.coerceAtLeast(0),
                onSeleccion = { indice -> dias.getOrNull(indice)?.let { acciones.onFecha(it.clave) } },
            )
            Text(stringResource(R.string.turnos_tipo_turno), style = MaterialTheme.typography.bodySmall)
            ChipsFiltro(
                opciones = tipos.map { stringResource(it.etiqueta()) },
                seleccionada = tipos.indexOf(hoja.tipoTurno),
                onSeleccion = { acciones.onTipoTurno(tipos[it]) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                CampoHora(
                    etiqueta = stringResource(R.string.campo_inicio),
                    valor = hoja.inicio,
                    valido = hoja.inicioValido,
                    onValor = acciones::onInicio,
                    modifier = Modifier.weight(1f),
                )
                CampoHora(
                    etiqueta = stringResource(R.string.campo_fin),
                    valor = hoja.fin,
                    valido = hoja.finValido,
                    onValor = acciones::onFin,
                    modifier = Modifier.weight(1f),
                )
            }
            if (hoja.inicioValido && hoja.finValido && hoja.fin < hoja.inicio) {
                Banner(tipo = TipoBanner.INFO, texto = stringResource(R.string.turnos_termina_dia_siguiente))
            }
            CampoTexto(
                etiqueta = stringResource(R.string.turnos_notas),
                valor = hoja.notas,
                onValor = acciones::onNotas,
                singleLine = false,
            )
            hoja.error?.let { Banner(tipo = TipoBanner.AVISO, texto = textoError(it)) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.sm),
                horizontalArrangement = Arrangement.spacedBy(Espacio.xs, Alignment.End),
            ) {
                TextButton(onClick = acciones::cerrarHoja) { Text(stringResource(R.string.accion_cancelar)) }
                Button(onClick = acciones::guardar, enabled = hoja.puedeGuardar) {
                    Text(stringResource(if (hoja.guardando) R.string.turnos_guardando else R.string.accion_guardar))
                }
            }
        }
    }
}

/** Persona del turno: menú con el personal activo. Al ajustar no cambia (el turno es de esa persona). */
@Composable
private fun SelectorPersona(
    empleados: UiState<List<Empleado>>,
    elegido: String?,
    habilitado: Boolean,
    onPersona: (String) -> Unit,
    onReintentar: () -> Unit,
) {
    val lista = (empleados as? UiState.Exito)?.datos.orEmpty()
    var abierto by remember { mutableStateOf(false) }
    val texto =
        lista.firstOrNull { it.id == elegido }?.let { "${it.nombre} · ${etiquetaTipoPersonal(it.tipoPersonal)}" }
            ?: if (empleados is UiState.Cargando) stringResource(R.string.turnos_cargando_personal) else ""
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = habilitado && it }) {
        CampoTexto(
            etiqueta = stringResource(R.string.campo_persona),
            valor = texto,
            onValor = {},
            soloLectura = true,
            placeholder = stringResource(R.string.turnos_elige_persona),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, habilitado),
            final = { if (habilitado) ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            lista.forEach { empleado ->
                DropdownMenuItem(
                    text = { Text("${empleado.nombre} · ${etiquetaTipoPersonal(empleado.tipoPersonal)}") },
                    onClick = {
                        onPersona(empleado.id)
                        abierto = false
                    },
                )
            }
        }
    }
    if (empleados is UiState.Error) {
        Banner(
            tipo = TipoBanner.ERROR,
            texto = stringResource(R.string.turnos_error_personal),
            accion = stringResource(R.string.estado_reintentar),
            onAccion = onReintentar,
        )
    }
}

@Composable
private fun CampoHora(
    etiqueta: String,
    valor: String,
    valido: Boolean,
    onValor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CampoTexto(
        etiqueta = etiqueta,
        valor = valor,
        onValor = onValor,
        placeholder = "07:00",
        error = if (valido || valor.isEmpty()) null else stringResource(R.string.turnos_hora_invalida),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

private fun TipoTurno.etiqueta(): Int =
    when (this) {
        TipoTurno.MANANA -> R.string.turno_tipo_manana
        TipoTurno.TARDE -> R.string.turno_tipo_tarde
        TipoTurno.NOCHE -> R.string.turno_tipo_noche
        TipoTurno.GUARDIA -> R.string.turno_tipo_guardia
    }

/** El solape (`TURNO_SOLAPADO`) ya viene redactado por la base; los demás errores usan su texto genérico. */
@Composable
private fun textoError(error: AppError): String =
    (error as? AppError.Rechazada)?.detalle ?: stringResource(error.mensaje())

/** Para las previews: no hace nada. */
object SinAccionesTurnos : AccionesTurnos {
    override fun cargar() = Unit

    override fun onSemana(delta: Int) = Unit

    override fun onDia(indice: Int) = Unit

    override fun onTipo(indice: Int) = Unit

    override fun abrirNuevo() = Unit

    override fun abrirAjuste(turnoId: String) = Unit

    override fun cerrarHoja() = Unit

    override fun cargarEmpleados() = Unit

    override fun onPersona(id: String) = Unit

    override fun onArea(valor: String) = Unit

    override fun onFecha(clave: String) = Unit

    override fun onInicio(valor: String) = Unit

    override fun onFin(valor: String) = Unit

    override fun onTipoTurno(tipo: TipoTurno) = Unit

    override fun onNotas(valor: String) = Unit

    override fun guardar() = Unit
}

private val diasMuestra =
    listOf("Lun 19", "Mar 20", "Mié 21", "Jue 22", "Vie 23", "Sáb 24", "Dom 25")
        .mapIndexed { i, etiqueta -> DiaUi("2026-10-${19 + i}", etiqueta) }

@PreviewPantalla
@Composable
private fun TurnosPersonalPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        TurnosPersonalScreen(
            estado =
                TurnosPersonalUiState(
                    etiquetaSemana = "19 – 25 oct",
                    dias = diasMuestra,
                    dia = 2,
                    tipos = listOf("Alergista", "Emergencia", "Enfermería"),
                    estado = estado.con(DatosMuestra.turnosPersonal),
                ),
            onVolver = {},
            acciones = SinAccionesTurnos,
        )
    }
}

@PreviewPantalla
@Composable
private fun HojaTurnoSolapePreview() {
    HospitalUnicaesTheme {
        TurnosPersonalScreen(
            estado =
                TurnosPersonalUiState(
                    etiquetaSemana = "19 – 25 oct",
                    dias = diasMuestra,
                    dia = 3,
                    estado = UiState.Exito(DatosMuestra.turnosPersonal),
                    empleados = UiState.Exito(listOf(Empleado("e1", "Sofía Hernández", "Doctor"))),
                    hoja =
                        HojaTurnoUiState(
                            fecha = "2026-10-22",
                            personalId = "e1",
                            area = "Consulta externa",
                            inicio = "12:00",
                            fin = "20:00",
                            error =
                                AppError.Rechazada(
                                    "El empleado ya tiene un turno el 22/10/2026 de 14:00 a 22:00",
                                ),
                        ),
                ),
            onVolver = {},
            acciones = SinAccionesTurnos,
        )
    }
}
