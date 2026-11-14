package sv.edu.unicaes.hospital.ui.recepcion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.CitasRepository
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.domain.model.Cita
import sv.edu.unicaes.hospital.domain.model.DoctorDisponible
import sv.edu.unicaes.hospital.ui.modelo.DiaUi
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import sv.edu.unicaes.hospital.ui.modelo.UN_DIA_MS
import sv.edu.unicaes.hospital.ui.modelo.ZONA_HOSPITAL
import sv.edu.unicaes.hospital.ui.modelo.agruparAgenda
import sv.edu.unicaes.hospital.ui.modelo.claveDia
import sv.edu.unicaes.hospital.ui.modelo.etiquetaDia
import sv.edu.unicaes.hospital.ui.modelo.fechaDesdeIso
import sv.edu.unicaes.hospital.ui.modelo.fechaHoraHospital
import sv.edu.unicaes.hospital.ui.modelo.hora
import sv.edu.unicaes.hospital.ui.modelo.horasConCita
import sv.edu.unicaes.hospital.ui.modelo.horasDisponibles
import sv.edu.unicaes.hospital.ui.modelo.isoConZona
import java.util.Date
import javax.inject.Inject

/** Cómo terminó el formulario; la pantalla vuelve a la anterior y esta recarga. */
enum class ResultadoCita { GUARDADA, CANCELADA }

data class NuevaCitaUiState(
    /** Con id, se está reprogramando esa cita: el paciente y el motivo no cambian. */
    val citaId: String? = null,
    val cargandoCita: Boolean = false,
    val errorCarga: AppError? = null,
    val paciente: PacienteUi? = null,
    val busqueda: String = "",
    /** Null mientras no se ha buscado (menos de 2 letras). */
    val resultados: UiState<List<PacienteUi>>? = null,
    val dias: List<DiaUi> = emptyList(),
    val dia: Int = 0,
    val horas: UiState<List<String>> = UiState.Cargando,
    /** Horas libres para algún doctor en las que el paciente elegido ya tiene otra cita: se muestran bloqueadas. */
    val horasConCitaPaciente: Set<String> = emptySet(),
    val hora: String? = null,
    /** Null hasta elegir una hora. */
    val doctores: UiState<List<DoctorDisponible>>? = null,
    val doctorId: String? = null,
    val motivo: String = "",
    val guardando: Boolean = false,
    val error: AppError? = null,
    val resultado: ResultadoCita? = null,
) {
    val reprogramar: Boolean get() = citaId != null

    val puedeGuardar: Boolean
        get() = paciente != null && hora != null && doctorId != null && !guardando && !cargandoCita
}

/**
 * AND-R6 · Crear o reprogramar una cita. Las horas son los huecos libres de los médicos de turno ese día; al
 * elegir una se piden los doctores disponibles (RPC `doctores_disponibles`), así que solo se ofrecen doctores
 * de turno. El choque de último momento llega como [AppError.Rechazada] con el mensaje de la base de datos.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class NuevaCitaViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val citasRepository: CitasRepository,
        private val pacientesRepository: PacientesRepository,
    ) : ViewModel() {
        // Mismos nombres que las propiedades de NuevaCitaRuta.
        private val citaId: String? = savedStateHandle["citaId"]
        private val pacienteId: String? = savedStateHandle["pacienteId"]
        private val fecha: String? = savedStateHandle["fecha"]

        private val _uiState =
            MutableStateFlow(
                NuevaCitaUiState(
                    citaId = citaId,
                    dias = diasAgenda(),
                    hora = savedStateHandle["hora"],
                    doctorId = savedStateHandle["doctorId"],
                ),
            )
        val uiState: StateFlow<NuevaCitaUiState> = _uiState.asStateFlow()

        private var cargaHoras: Job? = null

        /** Citas activas del día elegido (sin la que se reprograma), para marcar las del paciente. */
        private var citasDelDia: List<Cita> = emptyList()
        private var cargaDoctores: Job? = null
        private var busqueda: Job? = null

        init {
            when {
                citaId != null -> cargarCita(citaId)
                else -> iniciarNueva()
            }
            viewModelScope.launch {
                _uiState
                    .map { it.busqueda.trim() }
                    .distinctUntilChanged()
                    .drop(1)
                    .debounce(ESPERA_BUSQUEDA_MS)
                    .collect { buscar(it) }
            }
        }

        private fun iniciarNueva() {
            fecha?.let { elegirDia(it) }
            cargarHoras()
            val id = pacienteId ?: return
            _uiState.update { it.copy(cargandoCita = true) }
            viewModelScope.launch {
                when (val resultado = pacientesRepository.obtener(id)) {
                    is Resultado.Exito -> {
                        _uiState.update {
                            it.copy(
                                cargandoCita = false,
                                paciente = resultado.datos.aUi(),
                            )
                        }
                        marcarCitasDelPaciente()
                    }

                    is Resultado.Fallo -> {
                        _uiState.update {
                            it.copy(
                                cargandoCita = false,
                                errorCarga = resultado.error,
                            )
                        }
                    }
                }
            }
        }

        /** Reprogramar: se precargan paciente, día, hora, doctor y motivo de la cita. */
        fun cargarCita(id: String = citaId.orEmpty()) {
            _uiState.update { it.copy(cargandoCita = true, errorCarga = null) }
            viewModelScope.launch {
                when (val resultado = citasRepository.obtener(id)) {
                    is Resultado.Exito -> {
                        precargar(resultado.datos)
                        cargarHoras()
                    }

                    is Resultado.Fallo -> {
                        _uiState.update { it.copy(cargandoCita = false, errorCarga = resultado.error) }
                    }
                }
            }
        }

        private fun precargar(cita: Cita) {
            val fechaCita = fechaDesdeIso(cita.fechaHora)
            fechaCita?.let { elegirDia(claveDia(it)) }
            _uiState.update {
                it.copy(
                    cargandoCita = false,
                    paciente = PacienteUi(id = cita.pacienteId, nombre = cita.paciente, dui = ""),
                    hora = fechaCita?.let { f -> hora(f, ZONA_HOSPITAL) },
                    doctorId = cita.doctorId,
                    motivo = cita.motivo.orEmpty(),
                )
            }
        }

        /** Elige el día [clave]; si no está entre los chips (cita de otra semana), se agrega al inicio. */
        private fun elegirDia(clave: String) {
            _uiState.update { estado ->
                val indice = estado.dias.indexOfFirst { it.clave == clave }
                if (indice >= 0) {
                    estado.copy(dia = indice)
                } else {
                    val etiqueta = fechaHoraHospital(clave)?.let { etiquetaDia(it) } ?: clave
                    estado.copy(dias = listOf(DiaUi(clave, etiqueta)) + estado.dias, dia = 0)
                }
            }
        }

        fun onBusqueda(valor: String) = _uiState.update { it.copy(busqueda = valor) }

        fun onPaciente(paciente: PacienteUi) {
            _uiState.update { it.copy(paciente = paciente, busqueda = "", resultados = null, error = null) }
            marcarCitasDelPaciente()
        }

        fun onCambiarPaciente() {
            if (_uiState.value.reprogramar) return
            _uiState.update { it.copy(paciente = null, horasConCitaPaciente = emptySet()) }
        }

        fun onDia(indice: Int) {
            _uiState.update { it.copy(dia = indice, error = null) }
            cargarHoras()
        }

        fun onHora(hora: String) {
            if (hora in _uiState.value.horasConCitaPaciente) return
            _uiState.update { it.copy(hora = hora, error = null) }
            cargarDoctores()
        }

        fun onDoctor(id: String) = _uiState.update { it.copy(doctorId = id, error = null) }

        fun onMotivo(valor: String) = _uiState.update { it.copy(motivo = valor) }

        /** Huecos del día elegido; la hora elegida se conserva si sigue libre. */
        fun cargarHoras() {
            cargaHoras?.cancel()
            cargaDoctores?.cancel()
            val desde =
                _uiState.value.dias
                    .getOrNull(_uiState.value.dia)
                    ?.let { fechaHoraHospital(it.clave) } ?: return
            val hasta = Date(desde.time + UN_DIA_MS)
            _uiState.update { it.copy(horas = UiState.Cargando, doctores = null) }
            cargaHoras =
                viewModelScope.launch {
                    val citas = async { citasRepository.entre(isoConZona(desde), isoConZona(hasta)) }
                    val turnos = async { citasRepository.turnosMedicos(isoConZona(desde), isoConZona(hasta)) }
                    val c = citas.await()
                    val t = turnos.await()
                    val horas =
                        when {
                            c is Resultado.Fallo -> {
                                UiState.Error(c.error)
                            }

                            t is Resultado.Fallo -> {
                                UiState.Error(t.error)
                            }

                            else -> {
                                // Al reprogramar, el horario de la propia cita cuenta como libre.
                                val otras = (c as Resultado.Exito).datos.filter { it.id != citaId }
                                citasDelDia = otras
                                val libres =
                                    horasDisponibles(agruparAgenda(otras, (t as Resultado.Exito).datos, desde, hasta))
                                if (libres.isEmpty()) UiState.Vacio else UiState.Exito(libres)
                            }
                        }
                    val hora = _uiState.value.hora?.takeIf { (horas as? UiState.Exito)?.datos?.contains(it) == true }
                    _uiState.update { it.copy(horas = horas, hora = hora) }
                    if (hora != null) cargarDoctores()
                    marcarCitasDelPaciente()
                }
        }

        /**
         * Bloquea las horas en que el paciente ya tiene otra cita ese día (sin importar el doctor), para no
         * enterarse hasta guardar. Si la hora elegida queda bloqueada, se quita junto con los doctores.
         */
        private fun marcarCitasDelPaciente() {
            val estado = _uiState.value
            val pacienteId = estado.paciente?.id
            val dia = estado.dias.getOrNull(estado.dia)?.clave
            val horas = (estado.horas as? UiState.Exito)?.datos
            if (pacienteId == null || dia == null || horas == null) {
                _uiState.update { it.copy(horasConCitaPaciente = emptySet()) }
                return
            }
            val bloqueadas = horasConCita(dia, horas, citasDelDia.filter { it.pacienteId == pacienteId })
            if (estado.hora != null && estado.hora in bloqueadas) cargaDoctores?.cancel()
            _uiState.update {
                if (it.hora != null && it.hora in bloqueadas) {
                    it.copy(horasConCitaPaciente = bloqueadas, hora = null, doctores = null)
                } else {
                    it.copy(horasConCitaPaciente = bloqueadas)
                }
            }
        }

        private fun cargarDoctores() {
            cargaDoctores?.cancel()
            val estado = _uiState.value
            val dia = estado.dias.getOrNull(estado.dia) ?: return
            val fechaHora = estado.hora?.let { fechaHoraHospital(dia.clave, it) } ?: return
            _uiState.update { it.copy(doctores = UiState.Cargando) }
            cargaDoctores =
                viewModelScope.launch {
                    val doctores =
                        when (val resultado = citasRepository.doctoresDisponibles(isoConZona(fechaHora), citaId)) {
                            is Resultado.Exito -> {
                                if (resultado.datos.isEmpty()) {
                                    UiState.Vacio
                                } else {
                                    UiState.Exito(
                                        resultado.datos,
                                    )
                                }
                            }

                            is Resultado.Fallo -> {
                                UiState.Error(resultado.error)
                            }
                        }
                    val lista = (doctores as? UiState.Exito)?.datos.orEmpty()
                    _uiState.update { actual ->
                        actual.copy(
                            doctores = doctores,
                            doctorId = actual.doctorId?.takeIf { id -> lista.any { it.id == id } },
                        )
                    }
                }
        }

        private fun buscar(termino: String) {
            busqueda?.cancel()
            if (_uiState.value.paciente != null) return
            if (termino.length < MINIMO_BUSQUEDA) {
                _uiState.update { it.copy(resultados = null) }
                return
            }
            _uiState.update { it.copy(resultados = UiState.Cargando) }
            busqueda =
                viewModelScope.launch {
                    val resultados =
                        when (val resultado = pacientesRepository.listar(termino, 1)) {
                            is Resultado.Exito -> {
                                val pacientes =
                                    resultado.datos.items
                                        .take(MAXIMO_RESULTADOS)
                                        .map { it.aUi() }
                                if (pacientes.isEmpty()) UiState.Vacio else UiState.Exito(pacientes)
                            }

                            is Resultado.Fallo -> {
                                UiState.Error(resultado.error)
                            }
                        }
                    _uiState.update { it.copy(resultados = resultados) }
                }
        }

        fun guardar() {
            val estado = _uiState.value
            if (!estado.puedeGuardar) return
            val paciente = estado.paciente ?: return
            val doctorId = estado.doctorId ?: return
            val dia = estado.dias.getOrNull(estado.dia) ?: return
            val fechaHora = estado.hora?.let { fechaHoraHospital(dia.clave, it) } ?: return
            _uiState.update { it.copy(guardando = true, error = null) }
            viewModelScope.launch {
                val iso = isoConZona(fechaHora)
                val resultado =
                    if (citaId != null) {
                        citasRepository.reprogramar(citaId, iso, doctorId)
                    } else {
                        citasRepository.crear(paciente.id, doctorId, iso, estado.motivo.trim().ifEmpty { null })
                    }
                when (resultado) {
                    is Resultado.Exito -> {
                        _uiState.update { it.copy(guardando = false, resultado = ResultadoCita.GUARDADA) }
                    }

                    is Resultado.Fallo -> {
                        _uiState.update { it.copy(guardando = false, error = resultado.error) }
                        // Otro usuario pudo ocupar el horario: se refrescan las horas libres.
                        if (resultado.error is AppError.Rechazada) cargarHoras()
                    }
                }
            }
        }

        fun cancelarCita() {
            val id = citaId ?: return
            _uiState.update { it.copy(guardando = true, error = null) }
            viewModelScope.launch {
                when (val resultado = citasRepository.cancelar(id)) {
                    is Resultado.Exito -> {
                        _uiState.update {
                            it.copy(
                                guardando = false,
                                resultado = ResultadoCita.CANCELADA,
                            )
                        }
                    }

                    is Resultado.Fallo -> {
                        _uiState.update { it.copy(guardando = false, error = resultado.error) }
                    }
                }
            }
        }

        private companion object {
            const val ESPERA_BUSQUEDA_MS = 300L
            const val MINIMO_BUSQUEDA = 2
            const val MAXIMO_RESULTADOS = 5
        }
    }
