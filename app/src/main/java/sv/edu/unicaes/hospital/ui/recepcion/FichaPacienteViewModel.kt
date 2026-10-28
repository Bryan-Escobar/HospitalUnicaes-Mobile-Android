package sv.edu.unicaes.hospital.ui.recepcion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.CitasRepository
import sv.edu.unicaes.hospital.data.repository.HospitalizacionesRepository
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.domain.model.Cita
import sv.edu.unicaes.hospital.domain.model.Hospitalizacion
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.ui.modelo.AsignacionCamaUi
import sv.edu.unicaes.hospital.ui.modelo.CitaUi
import sv.edu.unicaes.hospital.ui.modelo.FichaPacienteUi
import sv.edu.unicaes.hospital.ui.modelo.diaMes
import sv.edu.unicaes.hospital.ui.modelo.diaMesNumerico
import sv.edu.unicaes.hospital.ui.modelo.diasEntre
import sv.edu.unicaes.hospital.ui.modelo.fechaDesdeIso
import sv.edu.unicaes.hospital.ui.modelo.hora
import javax.inject.Inject

/**
 * AND-R4 · Ficha del paciente: datos personales (GET /pacientes/:id), su cama si está internado
 * (GET /hospitalizaciones) y sus citas (vista `v_citas` de Supabase), cargados en paralelo.
 */
@HiltViewModel
class FichaPacienteViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val pacientesRepository: PacientesRepository,
        private val hospitalizacionesRepository: HospitalizacionesRepository,
        private val citasRepository: CitasRepository,
    ) : ViewModel() {
        // Mismo nombre que la propiedad de FichaPacienteRuta.
        private val pacienteId: String = checkNotNull(savedStateHandle["pacienteId"])

        private val _uiState = MutableStateFlow<UiState<FichaPacienteUi>>(UiState.Cargando)
        val uiState: StateFlow<UiState<FichaPacienteUi>> = _uiState.asStateFlow()

        init {
            cargar()
        }

        /**
         * También al volver de editar. Si falla cualquiera de las tres lecturas se muestra el error con
         * Reintentar: mostrar "Sin cama asignada" sin saberlo sería peor que no mostrar la ficha.
         */
        fun cargar() {
            _uiState.value = UiState.Cargando
            viewModelScope.launch {
                val paciente = async { pacientesRepository.obtener(pacienteId) }
                val hospitalizacion = async { hospitalizacionesRepository.activaDe(pacienteId) }
                val citas = async { citasRepository.dePaciente(pacienteId) }
                _uiState.value =
                    when (val p = paciente.await()) {
                        is Resultado.Fallo -> {
                            UiState.Error(p.error)
                        }

                        is Resultado.Exito -> {
                            when (val h = hospitalizacion.await()) {
                                is Resultado.Fallo -> {
                                    UiState.Error(h.error)
                                }

                                is Resultado.Exito -> {
                                    when (val c = citas.await()) {
                                        is Resultado.Fallo -> UiState.Error(c.error)
                                        is Resultado.Exito -> UiState.Exito(p.datos.aFicha(h.datos, c.datos))
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }

private fun Paciente.aFicha(
    hospitalizacion: Hospitalizacion?,
    citas: List<Cita>,
): FichaPacienteUi =
    FichaPacienteUi(
        paciente = aUi().copy(hospitalizado = hospitalizacion != null),
        edad = edadDesde(fechaNacimiento),
        telefono = telefono.orEmpty(),
        // TarjetaContacto separa nombre y teléfono por " · ".
        contactoEmergencia = listOfNotNull(contactoEmergencia, telefonoEmergencia).joinToString(" · "),
        asignacion = hospitalizacion?.aUi(),
        citas = citas.map { it.aUi(paciente = nombreCompleto) },
        numeroExpediente = numeroExpediente.ifEmpty { null },
        nacimiento = fechaParaFormulario(fechaNacimiento).ifEmpty { null },
        sexo = sexo,
        direccion = direccion,
    )

internal fun Hospitalizacion.aUi(): AsignacionCamaUi {
    val ingreso = fechaDesdeIso(fechaIngreso)
    return AsignacionCamaUi(
        habitacion = habitacion,
        cama = cama,
        tipo = tipo,
        desde = ingreso?.let { diaMes(it) }.orEmpty(),
        ingreso = ingreso?.let { "${diaMes(it)} · ${hora(it)}" }.orEmpty(),
        diasHospitalizado = ingreso?.let { diasEntre(it) } ?: 0,
        medicoTratante = medicoTratante,
    )
}

internal fun Cita.aUi(paciente: String): CitaUi {
    val fecha = fechaDesdeIso(fechaHora)
    return CitaUi(
        id = id,
        hora = fecha?.let { hora(it) }.orEmpty(),
        paciente = paciente,
        doctor = doctor,
        motivo = motivo.orEmpty(),
        estado = estado,
        fecha = fecha?.let { diaMesNumerico(it) }.orEmpty(),
    )
}
