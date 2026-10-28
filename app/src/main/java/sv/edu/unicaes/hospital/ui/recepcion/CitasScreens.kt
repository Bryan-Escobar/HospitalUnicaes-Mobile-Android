package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.core.mensaje
import sv.edu.unicaes.hospital.domain.model.DoctorDisponible
import sv.edu.unicaes.hospital.ui.components.AccionTexto
import sv.edu.unicaes.hospital.ui.components.Banner
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.BotonSecundario
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.ChipOpcion
import sv.edu.unicaes.hospital.ui.components.ChipsFiltro
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.DialogoConfirmacion
import sv.edu.unicaes.hospital.ui.components.ErrorCarga
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.FabHospital
import sv.edu.unicaes.hospital.ui.components.FilaChips
import sv.edu.unicaes.hospital.ui.components.FilaCita
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaLista
import sv.edu.unicaes.hospital.ui.components.TipoBanner
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.AgendaDoctorUi
import sv.edu.unicaes.hospital.ui.modelo.CitaUi
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.DiaUi
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.Texto2

/** Una fila de la agenda del doctor: una cita o un hueco libre. */
private sealed interface EntradaAgenda {
    val hora: String

    data class Ocupada(
        val cita: CitaUi,
    ) : EntradaAgenda {
        override val hora: String get() = cita.hora
    }

    data class Libre(
        override val hora: String,
    ) : EntradaAgenda
}

/** Hueco libre de la agenda que se toca para agendar ahí. */
data class HuecoAgenda(
    val fecha: String,
    val hora: String,
    val doctorId: String,
)

@Composable
fun CitasScreen(
    onNuevaCita: (fecha: String?) -> Unit,
    onCita: (CitaUi) -> Unit,
    onHueco: (HuecoAgenda) -> Unit,
    viewModel: CitasViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CitasContent(
        uiState = uiState,
        onDia = viewModel::onDia,
        onNuevaCita = { onNuevaCita(uiState.diaElegido?.clave) },
        onCita = onCita,
        onHueco = { agenda, hora ->
            uiState.diaElegido?.let { onHueco(HuecoAgenda(it.clave, hora, agenda.doctorId)) }
        },
        onReintentar = viewModel::cargar,
    )
}

/** AND-R5 · Citas del día agrupadas por doctor de turno, con sus huecos libres para agendar. */
@Composable
fun CitasContent(
    uiState: CitasUiState,
    onDia: (Int) -> Unit,
    onNuevaCita: () -> Unit,
    onCita: (CitaUi) -> Unit,
    onHueco: (AgendaDoctorUi, String) -> Unit,
    onReintentar: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.tab_citas),
                acciones = { IconoAccion(R.drawable.ic_calendar, stringResource(R.string.accion_calendario), {}) },
            )
        },
        floatingActionButton = { FabHospital(texto = stringResource(R.string.citas_nueva), onClick = onNuevaCita) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(start = Espacio.md, end = Espacio.md, top = Espacio.xs),
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            ChipsFiltro(opciones = uiState.dias.map { it.etiqueta }, seleccionada = uiState.dia, onSeleccion = onDia)
            ContenidoConEstado(
                estado = uiState.estado,
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
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(Espacio.sm),
                ) {
                    agendas.forEach { agenda ->
                        item(key = "titulo-${agenda.doctorId}") { TituloSeccion(texto = tituloAgenda(agenda)) }
                        item(key = "agenda-${agenda.doctorId}") { TarjetaAgenda(agenda, onCita, onHueco) }
                    }
                }
            }
        }
    }
}

/** `Ana Rivas · Alergista · 08:00–14:00`, sin las partes que no hay. */
@Composable
private fun tituloAgenda(agenda: AgendaDoctorUi): String =
    listOf(agenda.doctor, agenda.especialidad, agenda.horario.ifEmpty { stringResource(R.string.citas_sin_turno) })
        .filter { it.isNotBlank() }
        .joinToString(" · ")

@Composable
private fun TarjetaAgenda(
    agenda: AgendaDoctorUi,
    onCita: (CitaUi) -> Unit,
    onHueco: (AgendaDoctorUi, String) -> Unit,
) {
    val entradas =
        (agenda.citas.map { EntradaAgenda.Ocupada(it) } + agenda.horasLibres.map { EntradaAgenda.Libre(it) })
            .sortedBy { it.hora }
    TarjetaLista(entradas) { entrada, ultima ->
        when (entrada) {
            is EntradaAgenda.Ocupada -> {
                FilaCita(cita = entrada.cita, divisor = !ultima, onClick = { onCita(entrada.cita) })
            }

            is EntradaAgenda.Libre -> {
                FilaLista(
                    titulo = "${entrada.hora} ${stringResource(R.string.citas_libre)}",
                    soporte = stringResource(R.string.citas_hueco_disponible),
                    divisor = !ultima,
                    enTarjeta = true,
                    onClick = { onHueco(agenda, entrada.hora) },
                    final = {
                        AccionTexto(
                            texto = stringResource(R.string.citas_agendar),
                            onClick = { onHueco(agenda, entrada.hora) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun NuevaCitaScreen(
    onCerrar: () -> Unit,
    onTerminado: (ResultadoCita) -> Unit,
    viewModel: NuevaCitaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.resultado) { uiState.resultado?.let(onTerminado) }
    NuevaCitaContent(
        uiState = uiState,
        acciones =
            AccionesNuevaCita(
                onCerrar = onCerrar,
                onBusqueda = viewModel::onBusqueda,
                onPaciente = viewModel::onPaciente,
                onCambiarPaciente = viewModel::onCambiarPaciente,
                onDia = viewModel::onDia,
                onHora = viewModel::onHora,
                onDoctor = viewModel::onDoctor,
                onMotivo = viewModel::onMotivo,
                onGuardar = viewModel::guardar,
                onCancelarCita = viewModel::cancelarCita,
                onReintentarCarga = { viewModel.cargarCita() },
                onReintentarHoras = viewModel::cargarHoras,
            ),
    )
}

/** Eventos del formulario de cita, agrupados para no pasar doce lambdas sueltas. */
data class AccionesNuevaCita(
    val onCerrar: () -> Unit = {},
    val onBusqueda: (String) -> Unit = {},
    val onPaciente: (PacienteUi) -> Unit = {},
    val onCambiarPaciente: () -> Unit = {},
    val onDia: (Int) -> Unit = {},
    val onHora: (String) -> Unit = {},
    val onDoctor: (String) -> Unit = {},
    val onMotivo: (String) -> Unit = {},
    val onGuardar: () -> Unit = {},
    val onCancelarCita: () -> Unit = {},
    val onReintentarCarga: () -> Unit = {},
    val onReintentarHoras: () -> Unit = {},
)

/**
 * AND-R6 · Crear / reprogramar cita. Solo se ofrecen los huecos libres y, para la hora elegida, los doctores
 * de turno sin otra cita; el choque de último momento llega de Supabase y se muestra tal cual.
 */
@Composable
fun NuevaCitaContent(
    uiState: NuevaCitaUiState,
    acciones: AccionesNuevaCita,
) {
    var confirmarCancelacion by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(if (uiState.reprogramar) R.string.citas_reprogramar else R.string.citas_nueva),
                onVolver = acciones.onCerrar,
                cerrar = true,
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(RellenoCuerpo),
                verticalArrangement = Arrangement.spacedBy(Espacio.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BotonPrimario(
                    texto = stringResource(R.string.citas_guardar),
                    onClick = acciones.onGuardar,
                    habilitado = uiState.puedeGuardar,
                )
                if (uiState.reprogramar) {
                    BotonSecundario(
                        texto = stringResource(R.string.citas_cancelar_cita),
                        onClick = { confirmarCancelacion = true },
                        habilitado = !uiState.guardando && !uiState.cargandoCita,
                        destructivo = true,
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
            uiState.errorCarga?.let {
                ErrorCarga(
                    mensaje = stringResource(R.string.citas_error_carga),
                    error = it,
                    onReintentar = acciones.onReintentarCarga,
                )
            }
            SeccionPaciente(uiState, acciones)
            TituloSeccion(stringResource(R.string.campo_fecha))
            ChipsFiltro(
                opciones = uiState.dias.map(DiaUi::etiqueta),
                seleccionada = uiState.dia,
                onSeleccion = acciones.onDia,
            )
            TituloSeccion(stringResource(R.string.campo_hora))
            SeccionHoras(uiState, acciones)
            TituloSeccion(stringResource(R.string.campo_doctor_turno))
            SeccionDoctores(uiState.doctores, uiState.doctorId, acciones.onDoctor)
            CampoTexto(
                etiqueta = stringResource(R.string.campo_motivo),
                valor = uiState.motivo,
                onValor = acciones.onMotivo,
                soloLectura = uiState.reprogramar,
                ayuda = if (uiState.reprogramar) stringResource(R.string.citas_motivo_fijo) else null,
            )
            uiState.error?.let { Banner(tipo = TipoBanner.AVISO, texto = textoError(it)) }
        }
    }

    if (confirmarCancelacion) {
        DialogoConfirmacion(
            titulo = stringResource(R.string.citas_cancelar_titulo),
            texto = stringResource(R.string.citas_cancelar_texto),
            confirmar = stringResource(R.string.citas_cancelar_cita),
            onConfirmar = {
                confirmarCancelacion = false
                acciones.onCancelarCita()
            },
            onCancelar = { confirmarCancelacion = false },
            destructivo = true,
        )
    }
}

/** El mensaje de una regla de Supabase ya viene redactado; los demás errores usan su texto genérico. */
@Composable
private fun textoError(error: AppError): String =
    (error as? AppError.Rechazada)?.detalle ?: stringResource(error.mensaje())

/** Paciente elegido (con "Cambiar" si es cita nueva) o la búsqueda con sus resultados. */
@Composable
private fun SeccionPaciente(
    uiState: NuevaCitaUiState,
    acciones: AccionesNuevaCita,
) {
    val paciente = uiState.paciente
    if (paciente != null) {
        CampoTexto(
            etiqueta = stringResource(R.string.campo_paciente),
            valor = listOf(paciente.nombre, paciente.dui).filter { it.isNotBlank() }.joinToString(" · "),
            onValor = {},
            soloLectura = true,
            final =
                if (uiState.reprogramar) {
                    null
                } else {
                    { AccionTexto(stringResource(R.string.accion_cambiar), acciones.onCambiarPaciente) }
                },
        )
        return
    }
    if (uiState.cargandoCita) return
    CampoTexto(
        etiqueta = stringResource(R.string.campo_paciente),
        valor = uiState.busqueda,
        onValor = acciones.onBusqueda,
        placeholder = stringResource(R.string.citas_buscar_paciente),
        ayuda = if (uiState.resultados == null) stringResource(R.string.citas_buscar_minimo) else null,
    )
    when (val resultados = uiState.resultados) {
        null, UiState.Cargando -> {
            Unit
        }

        UiState.Vacio -> {
            TextoAyuda(stringResource(R.string.citas_sin_pacientes))
        }

        is UiState.Error -> {
            Banner(tipo = TipoBanner.ERROR, texto = textoError(resultados.error))
        }

        is UiState.Exito -> {
            TarjetaLista(resultados.datos) { candidato, ultimo ->
                FilaLista(
                    titulo = candidato.nombre,
                    soporte = candidato.dui.ifBlank { null },
                    divisor = !ultimo,
                    enTarjeta = true,
                    onClick = { acciones.onPaciente(candidato) },
                )
            }
        }
    }
}

@Composable
private fun SeccionHoras(
    uiState: NuevaCitaUiState,
    acciones: AccionesNuevaCita,
) {
    when (val horas = uiState.horas) {
        UiState.Cargando -> {
            TextoAyuda(stringResource(R.string.citas_cargando_horarios))
        }

        UiState.Vacio -> {
            Banner(tipo = TipoBanner.INFO, texto = stringResource(R.string.citas_sin_horarios))
        }

        is UiState.Error -> {
            ErrorCarga(
                mensaje = stringResource(R.string.citas_error),
                error = horas.error,
                onReintentar = acciones.onReintentarHoras,
            )
        }

        is UiState.Exito -> {
            FilaChips {
                horas.datos.forEach { opcion ->
                    val conCita = opcion in uiState.horasConCitaPaciente
                    ChipOpcion(
                        texto = if (conCita) stringResource(R.string.citas_hora_con_cita, opcion) else opcion,
                        elegida = opcion == uiState.hora,
                        habilitada = !conCita,
                        onClick = { acciones.onHora(opcion) },
                    )
                }
            }
            if (uiState.horasConCitaPaciente.isNotEmpty()) {
                TextoAyuda(stringResource(R.string.citas_ayuda_hora_con_cita))
            }
        }
    }
}

@Composable
private fun SeccionDoctores(
    doctores: UiState<List<DoctorDisponible>>?,
    doctorId: String?,
    onDoctor: (String) -> Unit,
) {
    when (doctores) {
        null -> {
            TextoAyuda(stringResource(R.string.citas_elige_hora))
        }

        UiState.Cargando -> {
            TextoAyuda(stringResource(R.string.citas_cargando_doctores))
        }

        UiState.Vacio -> {
            Banner(tipo = TipoBanner.AVISO, texto = stringResource(R.string.citas_sin_doctores))
        }

        is UiState.Error -> {
            Banner(tipo = TipoBanner.ERROR, texto = textoError(doctores.error))
        }

        is UiState.Exito -> {
            FilaChips {
                doctores.datos.forEach { doctor ->
                    ChipOpcion(
                        texto = listOfNotNull(doctor.nombre, doctor.especialidad).joinToString(" · "),
                        elegida = doctor.id == doctorId,
                        onClick = { onDoctor(doctor.id) },
                    )
                }
            }
            TextoAyuda(stringResource(R.string.citas_ayuda_doctor))
        }
    }
}

@Composable
private fun TextoAyuda(texto: String) {
    Text(
        text = texto,
        style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
        color = Texto2,
    )
}

@PreviewPantalla
@Composable
private fun CitasPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        CitasContent(
            uiState =
                CitasUiState(
                    dias = listOf(DiaUi("2026-10-29", "Jue 29"), DiaUi("2026-10-30", "Vie 30")),
                    estado = estado.con(DatosMuestra.agendasDelDia),
                ),
            onDia = {},
            onNuevaCita = {},
            onCita = {},
            onHueco = { _, _ -> },
        )
    }
}

@PreviewPantalla
@Composable
private fun NuevaCitaPreview() {
    HospitalUnicaesTheme {
        NuevaCitaContent(
            uiState =
                NuevaCitaUiState(
                    paciente = PacienteUi("p-1", "Luis Martínez", "01234567-8"),
                    dias = listOf(DiaUi("2026-10-29", "Jue 29"), DiaUi("2026-10-30", "Vie 30")),
                    horas = UiState.Exito(listOf("08:00", "08:30", "10:30", "11:00")),
                    hora = "10:30",
                    doctores = UiState.Exito(listOf(DoctorDisponible("d-1", "Ana Rivas", "Alergista"))),
                    doctorId = "d-1",
                    motivo = "Control de alergia",
                    error = AppError.Rechazada("El doctor ya tiene una cita el 29/10/2026 a las 10:30"),
                ),
            acciones = AccionesNuevaCita(),
        )
    }
}
