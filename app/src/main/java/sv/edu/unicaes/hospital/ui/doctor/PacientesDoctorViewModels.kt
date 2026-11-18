package sv.edu.unicaes.hospital.ui.doctor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.ConsultasRepository
import sv.edu.unicaes.hospital.data.repository.HospitalizacionesRepository
import sv.edu.unicaes.hospital.data.repository.LaboratorioRepository
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.domain.model.ConsultaClinica
import sv.edu.unicaes.hospital.domain.model.HistorialClinico
import sv.edu.unicaes.hospital.domain.model.Hospitalizacion
import sv.edu.unicaes.hospital.domain.model.OrdenLaboratorio
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.ui.modelo.ConsultaUi
import sv.edu.unicaes.hospital.ui.modelo.ExpedienteUi
import sv.edu.unicaes.hospital.ui.modelo.OrdenExpedienteUi
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import sv.edu.unicaes.hospital.ui.modelo.diaMes
import sv.edu.unicaes.hospital.ui.modelo.fechaDesdeIso
import sv.edu.unicaes.hospital.ui.recepcion.aUi
import sv.edu.unicaes.hospital.ui.recepcion.edadDesde
import javax.inject.Inject

/**
 * AND-D3 · Mis pacientes (HUNI-61): GET /pacientes, que al doctor solo le devuelve los que atiende, y
 * GET /hospitalizaciones activas para marcar quién está internado y en qué cama. Si las hospitalizaciones
 * fallan la lista se muestra igual, sin marcas: el doctor sigue pudiendo abrir cualquier expediente.
 */
@HiltViewModel
class MisPacientesViewModel
    @Inject
    constructor(
        private val pacientesRepository: PacientesRepository,
        private val hospitalizacionesRepository: HospitalizacionesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<UiState<List<PacienteUi>>>(UiState.Cargando)
        val uiState: StateFlow<UiState<List<PacienteUi>>> = _uiState.asStateFlow()

        private var carga: Job? = null

        init {
            cargar()
        }

        fun cargar() {
            carga?.cancel()
            _uiState.value = UiState.Cargando
            carga =
                viewModelScope.launch {
                    val hospitalizaciones = async { hospitalizacionesRepository.activas() }
                    val pacientes = pacientesRepository.atendidos()
                    val camas =
                        (hospitalizaciones.await() as? Resultado.Exito)
                            ?.datos
                            .orEmpty()
                            .associateBy { it.pacienteId }
                    _uiState.value =
                        when (pacientes) {
                            is Resultado.Fallo -> {
                                UiState.Error(pacientes.error)
                            }

                            is Resultado.Exito -> {
                                if (pacientes.datos.isEmpty()) {
                                    UiState.Vacio
                                } else {
                                    UiState.Exito(pacientes.datos.map { it.aPacienteDoctor(camas[it.id]) })
                                }
                            }
                        }
                }
        }
    }

/**
 * AND-D4 · Expediente (HUNI-61): historial clínico (GET /pacientes/:id/historial), su cama si está internado
 * y sus órdenes de laboratorio con los nombres de los exámenes, en paralelo. Si cualquiera falla se muestra
 * el error con Reintentar: un expediente a medias (sin la alergia o sin la orden pendiente) sería peor.
 * Un 403 llega como [sv.edu.unicaes.hospital.core.AppError.SinPermiso]: el doctor no es médico tratante.
 */
@HiltViewModel
class ExpedienteViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val consultasRepository: ConsultasRepository,
        private val hospitalizacionesRepository: HospitalizacionesRepository,
        private val laboratorioRepository: LaboratorioRepository,
    ) : ViewModel() {
        // Mismo nombre que la propiedad de ExpedienteRuta.
        val pacienteId: String = checkNotNull(savedStateHandle["pacienteId"])

        private val _uiState = MutableStateFlow<UiState<ExpedienteUi>>(UiState.Cargando)
        val uiState: StateFlow<UiState<ExpedienteUi>> = _uiState.asStateFlow()

        private var carga: Job? = null

        init {
            cargar()
        }

        /** También al volver de una consulta guardada. */
        fun cargar() {
            carga?.cancel()
            _uiState.value = UiState.Cargando
            carga =
                viewModelScope.launch {
                    val historial = async { consultasRepository.historial(pacienteId) }
                    val hospitalizacion = async { hospitalizacionesRepository.activaDe(pacienteId) }
                    val ordenes = async { laboratorioRepository.dePaciente(pacienteId) }
                    val h = historial.await()
                    val cama = hospitalizacion.await()
                    val lab = ordenes.await()
                    _uiState.value =
                        when {
                            h is Resultado.Fallo -> {
                                UiState.Error(h.error)
                            }

                            cama is Resultado.Fallo -> {
                                UiState.Error(cama.error)
                            }

                            lab is Resultado.Fallo -> {
                                UiState.Error(lab.error)
                            }

                            else -> {
                                UiState.Exito(
                                    (h as Resultado.Exito).datos.aExpediente(
                                        hospitalizacion = (cama as Resultado.Exito).datos,
                                        ordenes = (lab as Resultado.Exito).datos,
                                    ),
                                )
                            }
                        }
                }
        }
    }

/** Fila de "Mis pacientes": la cama (`HAB-206 · A`) como etiqueta si está internado. */
internal fun Paciente.aPacienteDoctor(hospitalizacion: Hospitalizacion?): PacienteUi =
    aUi().copy(
        etiqueta = hospitalizacion?.aUi()?.codigo,
        hospitalizado = hospitalizacion != null,
    )

internal fun HistorialClinico.aExpediente(
    hospitalizacion: Hospitalizacion?,
    ordenes: List<OrdenLaboratorio>,
): ExpedienteUi =
    ExpedienteUi(
        paciente = paciente.aPacienteDoctor(hospitalizacion),
        edad = edadDesde(paciente.fechaNacimiento),
        alergias = expediente.alergias,
        hospitalizacion = hospitalizacion?.aUi(),
        consultas = consultas.map { it.aUi() },
        laboratorio = ordenes.map { it.aExpediente() },
        telefono = paciente.telefono.orEmpty(),
        // Mismo formato que la ficha de recepción: nombre · teléfono.
        contactoEmergencia =
            listOfNotNull(paciente.contactoEmergencia, paciente.telefonoEmergencia).joinToString(" · "),
    )

internal fun ConsultaClinica.aUi(): ConsultaUi =
    ConsultaUi(
        id = id,
        fecha = fechaDesdeIso(fecha)?.let { diaMes(it) }.orEmpty(),
        diagnostico = tituloDiagnostico.orEmpty(),
        doctor = doctor,
        recetas = recetas.size,
        ordenes = ordenes.size,
    )

internal fun OrdenLaboratorio.aExpediente(): OrdenExpedienteUi =
    OrdenExpedienteUi(
        id = id,
        examen = examenes.joinToString(", ") { it.nombre },
        doctor = doctor,
        fecha = fechaDesdeIso(fechaSolicitud)?.let { diaMes(it) }.orEmpty(),
        estado = estado,
    )
