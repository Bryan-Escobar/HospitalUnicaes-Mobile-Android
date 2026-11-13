package sv.edu.unicaes.hospital.ui.recepcion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.CODIGOS_CAMA_PERDIDA
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.CamasRepository
import sv.edu.unicaes.hospital.data.repository.HospitalizacionesRepository
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.domain.model.DoctorDisponible
import sv.edu.unicaes.hospital.domain.model.Habitacion
import sv.edu.unicaes.hospital.ui.modelo.AsignacionCamaUi
import sv.edu.unicaes.hospital.ui.modelo.CamaUi
import sv.edu.unicaes.hospital.ui.modelo.FichaPacienteUi
import sv.edu.unicaes.hospital.ui.modelo.HabitacionUi
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import sv.edu.unicaes.hospital.ui.modelo.codigoDeCama
import javax.inject.Inject

// Habitaciones (AND-R8 a AND-R11, HUNI-67): mapa, ingreso, traslado y alta contra la API de HUNI-66.
// La asignación la valida la BD con bloqueo de fila; aquí solo se muestran sus conflictos (409) y se
// recarga el mapa cuando Supabase Realtime avisa de un cambio de ocupación.

internal fun Habitacion.aUi(): HabitacionUi =
    HabitacionUi(
        codigo = codigo,
        tipo = tipo,
        camas =
            camas.map { cama ->
                CamaUi(
                    id = cama.id,
                    etiqueta = cama.letra,
                    ocupante = cama.ocupante?.nombreCorto,
                    estado = cama.estado,
                    pacienteId = cama.ocupante?.pacienteId,
                    hospitalizacionId = cama.ocupante?.hospitalizacionId,
                )
            },
    )

/** AND-R8 · Mapa de camas en vivo. */
@HiltViewModel
class MapaCamasViewModel
    @Inject
    constructor(
        private val camasRepository: CamasRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<UiState<List<HabitacionUi>>>(UiState.Cargando)
        val uiState: StateFlow<UiState<List<HabitacionUi>>> = _uiState.asStateFlow()

        /** Error de una acción sobre una cama (marcar disponible); el mapa sigue visible. */
        private val _aviso = MutableStateFlow<AppError?>(null)
        val aviso: StateFlow<AppError?> = _aviso.asStateFlow()

        init {
            cargar()
            viewModelScope.launch { camasRepository.cambios().collect { recargar() } }
        }

        fun cargar() {
            _uiState.value = UiState.Cargando
            viewModelScope.launch { recargar() }
        }

        /** Recarga sin pasar por Cargando: la ocupación cambia en su sitio sin parpadear. */
        private suspend fun recargar() {
            val resultado = camasRepository.habitaciones()
            _uiState.value =
                when (resultado) {
                    is Resultado.Exito -> {
                        val habitaciones = resultado.datos.map { it.aUi() }
                        if (habitaciones.isEmpty()) UiState.Vacio else UiState.Exito(habitaciones)
                    }

                    // Si una recarga en vivo falla, se conserva el último mapa bueno.
                    is Resultado.Fallo -> {
                        (_uiState.value as? UiState.Exito) ?: UiState.Error(resultado.error)
                    }
                }
        }

        fun marcarDisponible(cama: CamaUi) {
            viewModelScope.launch {
                when (val r = camasRepository.marcarDisponible(cama.id)) {
                    is Resultado.Exito -> recargar()
                    is Resultado.Fallo -> _aviso.value = r.error
                }
            }
        }

        fun cerrarAviso() {
            _aviso.value = null
        }
    }

data class IngresoCamaUi(
    val paciente: PacienteUi,
    val habitaciones: List<HabitacionUi>,
    val doctores: List<DoctorDisponible> = emptyList(),
    /** El paciente ya tiene cama: no se puede ingresar otra vez (sin doble asignación). */
    val yaHospitalizado: Boolean = false,
)

data class IngresoCamaUiState(
    val estado: UiState<IngresoCamaUi> = UiState.Cargando,
    val doctorId: String? = null,
    val motivo: String = "",
    val enviando: Boolean = false,
    /** Código (`HAB-202 · B`) de la cama que otra persona ganó primero. */
    val camaEnConflicto: String? = null,
    val error: AppError? = null,
    val terminado: Boolean = false,
)

/** AND-R9 · Ingreso y asignación de cama del paciente de la ruta. */
@HiltViewModel
class IngresoCamaViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val pacientesRepository: PacientesRepository,
        private val hospitalizacionesRepository: HospitalizacionesRepository,
        private val camasRepository: CamasRepository,
    ) : ViewModel() {
        private val pacienteId: String = checkNotNull(savedStateHandle["pacienteId"])

        private val _uiState = MutableStateFlow(IngresoCamaUiState())
        val uiState: StateFlow<IngresoCamaUiState> = _uiState.asStateFlow()

        init {
            cargar()
            viewModelScope.launch { camasRepository.cambios().collect { refrescarCamas() } }
        }

        fun cargar() {
            _uiState.update { it.copy(estado = UiState.Cargando, error = null) }
            viewModelScope.launch {
                val paciente = async { pacientesRepository.obtener(pacienteId) }
                val hospitalizacion = async { hospitalizacionesRepository.activaDe(pacienteId) }
                val habitaciones = async { camasRepository.habitaciones() }
                val doctores = async { camasRepository.doctores() }
                val p = paciente.await()
                val h = hospitalizacion.await()
                val c = habitaciones.await()
                val d = doctores.await()
                val estado =
                    when {
                        p is Resultado.Fallo -> {
                            UiState.Error(p.error)
                        }

                        h is Resultado.Fallo -> {
                            UiState.Error(h.error)
                        }

                        c is Resultado.Fallo -> {
                            UiState.Error(c.error)
                        }

                        d is Resultado.Fallo -> {
                            UiState.Error(d.error)
                        }

                        else -> {
                            val datosPaciente = (p as Resultado.Exito).datos
                            UiState.Exito(
                                IngresoCamaUi(
                                    paciente =
                                        datosPaciente.aUi().copy(
                                            etiqueta = datosPaciente.numeroExpediente.ifEmpty { null },
                                        ),
                                    habitaciones = (c as Resultado.Exito).datos.map { it.aUi() },
                                    doctores = (d as Resultado.Exito).datos,
                                    yaHospitalizado = (h as Resultado.Exito).datos != null,
                                ),
                            )
                        }
                    }
                _uiState.update { it.copy(estado = estado) }
            }
        }

        fun elegirDoctor(id: String) = _uiState.update { it.copy(doctorId = id) }

        fun cambiarMotivo(motivo: String) = _uiState.update { it.copy(motivo = motivo) }

        fun descartarConflicto() = _uiState.update { it.copy(camaEnConflicto = null, error = null) }

        fun confirmar(camaId: String) {
            val actual = _uiState.value
            val doctorId = actual.doctorId ?: return
            if (actual.enviando) return
            _uiState.update { it.copy(enviando = true, error = null, camaEnConflicto = null) }
            viewModelScope.launch {
                when (val r = camasRepository.ingresar(pacienteId, doctorId, camaId, actual.motivo)) {
                    is Resultado.Exito -> _uiState.update { it.copy(enviando = false, terminado = true) }
                    is Resultado.Fallo -> manejarFallo(r.error, camaId)
                }
            }
        }

        /** La cama elegida se perdió: se avisa, se recarga el mapa y la pantalla sugiere la siguiente libre. */
        private suspend fun manejarFallo(
            error: AppError,
            camaId: String,
        ) {
            val perdida = error is AppError.Rechazada && error.codigo in CODIGOS_CAMA_PERDIDA
            val codigo = habitacionesActuales()?.codigoDeCama(camaId)
            _uiState.update {
                it.copy(
                    enviando = false,
                    camaEnConflicto = if (perdida) codigo else null,
                    error = if (perdida && codigo != null) null else error,
                )
            }
            if (perdida) refrescarCamas()
        }

        private fun habitacionesActuales() = (_uiState.value.estado as? UiState.Exito)?.datos?.habitaciones

        private suspend fun refrescarCamas() {
            val r = camasRepository.habitaciones() as? Resultado.Exito ?: return
            _uiState.update { estado ->
                val datos = (estado.estado as? UiState.Exito)?.datos ?: return@update estado
                estado.copy(estado = UiState.Exito(datos.copy(habitaciones = r.datos.map { it.aUi() })))
            }
        }
    }

data class TrasladoUi(
    val paciente: String,
    val asignacion: AsignacionCamaUi,
    val habitaciones: List<HabitacionUi>,
    val hospitalizacionId: String = "",
)

data class TrasladoUiState(
    val estado: UiState<TrasladoUi> = UiState.Cargando,
    val enviando: Boolean = false,
    val camaEnConflicto: String? = null,
    val error: AppError? = null,
    val terminado: Boolean = false,
)

/** AND-R10 · Traslado: libera la cama actual y ocupa la nueva en una sola operación. */
@HiltViewModel
class TrasladoViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val pacientesRepository: PacientesRepository,
        private val hospitalizacionesRepository: HospitalizacionesRepository,
        private val camasRepository: CamasRepository,
    ) : ViewModel() {
        private val pacienteId: String = checkNotNull(savedStateHandle["pacienteId"])

        private val _uiState = MutableStateFlow(TrasladoUiState())
        val uiState: StateFlow<TrasladoUiState> = _uiState.asStateFlow()

        init {
            cargar()
            viewModelScope.launch { camasRepository.cambios().collect { refrescarCamas() } }
        }

        fun cargar() {
            _uiState.update { it.copy(estado = UiState.Cargando, error = null) }
            viewModelScope.launch {
                val paciente = async { pacientesRepository.obtener(pacienteId) }
                val hospitalizacion = async { hospitalizacionesRepository.activaDe(pacienteId) }
                val habitaciones = async { camasRepository.habitaciones() }
                val p = paciente.await()
                val h = hospitalizacion.await()
                val c = habitaciones.await()
                val estado =
                    when {
                        p is Resultado.Fallo -> {
                            UiState.Error(p.error)
                        }

                        h is Resultado.Fallo -> {
                            UiState.Error(h.error)
                        }

                        c is Resultado.Fallo -> {
                            UiState.Error(c.error)
                        }

                        else -> {
                            // Sin hospitalización activa no hay nada que trasladar (ya se dio de alta).
                            val activa = (h as Resultado.Exito).datos
                            if (activa == null) {
                                UiState.Vacio
                            } else {
                                UiState.Exito(
                                    TrasladoUi(
                                        paciente = (p as Resultado.Exito).datos.nombreCompleto,
                                        asignacion = activa.aUi(),
                                        habitaciones = (c as Resultado.Exito).datos.map { it.aUi() },
                                        hospitalizacionId = activa.id,
                                    ),
                                )
                            }
                        }
                    }
                _uiState.update { it.copy(estado = estado) }
            }
        }

        fun descartarConflicto() = _uiState.update { it.copy(camaEnConflicto = null, error = null) }

        fun confirmar(camaId: String) {
            val actual = _uiState.value
            val datos = (actual.estado as? UiState.Exito)?.datos ?: return
            if (actual.enviando) return
            _uiState.update { it.copy(enviando = true, error = null, camaEnConflicto = null) }
            viewModelScope.launch {
                when (val r = camasRepository.trasladar(datos.hospitalizacionId, camaId)) {
                    is Resultado.Exito -> {
                        _uiState.update { it.copy(enviando = false, terminado = true) }
                    }

                    is Resultado.Fallo -> {
                        val error = r.error
                        val perdida = error is AppError.Rechazada && error.codigo in CODIGOS_CAMA_PERDIDA
                        val codigo = datos.habitaciones.codigoDeCama(camaId)
                        _uiState.update {
                            it.copy(
                                enviando = false,
                                camaEnConflicto = if (perdida) codigo else null,
                                error = if (perdida && codigo != null) null else error,
                            )
                        }
                        if (perdida) refrescarCamas()
                    }
                }
            }
        }

        private suspend fun refrescarCamas() {
            val r = camasRepository.habitaciones() as? Resultado.Exito ?: return
            _uiState.update { estado ->
                val datos = (estado.estado as? UiState.Exito)?.datos ?: return@update estado
                estado.copy(estado = UiState.Exito(datos.copy(habitaciones = r.datos.map { it.aUi() })))
            }
        }
    }

data class AltaUiState(
    val estado: UiState<FichaPacienteUi> = UiState.Cargando,
    val hospitalizacionId: String? = null,
    val enviando: Boolean = false,
    val error: AppError? = null,
    val terminado: Boolean = false,
)

/** AND-R11 · Alta: cierra la hospitalización y la cama pasa a limpieza. */
@HiltViewModel
class AltaViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val pacientesRepository: PacientesRepository,
        private val hospitalizacionesRepository: HospitalizacionesRepository,
        private val camasRepository: CamasRepository,
    ) : ViewModel() {
        private val pacienteId: String = checkNotNull(savedStateHandle["pacienteId"])

        private val _uiState = MutableStateFlow(AltaUiState())
        val uiState: StateFlow<AltaUiState> = _uiState.asStateFlow()

        init {
            cargar()
        }

        fun cargar() {
            _uiState.update { it.copy(estado = UiState.Cargando, error = null) }
            viewModelScope.launch {
                val paciente = async { pacientesRepository.obtener(pacienteId) }
                val hospitalizacion = async { hospitalizacionesRepository.activaDe(pacienteId) }
                val p = paciente.await()
                val h = hospitalizacion.await()
                _uiState.update {
                    when {
                        p is Resultado.Fallo -> {
                            it.copy(estado = UiState.Error(p.error))
                        }

                        h is Resultado.Fallo -> {
                            it.copy(estado = UiState.Error(h.error))
                        }

                        else -> {
                            val activa = (h as Resultado.Exito).datos
                            val datosPaciente = (p as Resultado.Exito).datos
                            if (activa == null) {
                                it.copy(estado = UiState.Vacio)
                            } else {
                                it.copy(
                                    estado =
                                        UiState.Exito(
                                            FichaPacienteUi(
                                                paciente = datosPaciente.aUi().copy(hospitalizado = true),
                                                edad = null,
                                                telefono = datosPaciente.telefono.orEmpty(),
                                                contactoEmergencia = "",
                                                asignacion = activa.aUi(),
                                                citas = emptyList(),
                                            ),
                                        ),
                                    hospitalizacionId = activa.id,
                                )
                            }
                        }
                    }
                }
            }
        }

        fun darAlta() {
            val actual = _uiState.value
            val id = actual.hospitalizacionId ?: return
            if (actual.enviando) return
            _uiState.update { it.copy(enviando = true, error = null) }
            viewModelScope.launch {
                when (val r = camasRepository.darAlta(id, notas = null)) {
                    is Resultado.Exito -> _uiState.update { it.copy(enviando = false, terminado = true) }
                    is Resultado.Fallo -> _uiState.update { it.copy(enviando = false, error = r.error) }
                }
            }
        }
    }
