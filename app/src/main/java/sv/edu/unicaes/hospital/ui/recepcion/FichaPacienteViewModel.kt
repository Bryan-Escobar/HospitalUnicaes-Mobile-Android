package sv.edu.unicaes.hospital.ui.recepcion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.ui.modelo.FichaPacienteUi
import javax.inject.Inject

/**
 * AND-R4 · Ficha del paciente con sus datos personales de GET /pacientes/:id. La cama asignada y
 * las citas llegan con sus propias historias; mientras tanto la ficha no las muestra.
 */
@HiltViewModel
class FichaPacienteViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val pacientesRepository: PacientesRepository,
    ) : ViewModel() {
        // Mismo nombre que la propiedad de FichaPacienteRuta.
        private val pacienteId: String = checkNotNull(savedStateHandle["pacienteId"])

        private val _uiState = MutableStateFlow<UiState<FichaPacienteUi>>(UiState.Cargando)
        val uiState: StateFlow<UiState<FichaPacienteUi>> = _uiState.asStateFlow()

        init {
            cargar()
        }

        /** También al volver de editar, para mostrar los datos nuevos. */
        fun cargar() {
            _uiState.value = UiState.Cargando
            viewModelScope.launch {
                _uiState.value =
                    when (val resultado = pacientesRepository.obtener(pacienteId)) {
                        is Resultado.Exito -> UiState.Exito(resultado.datos.aFicha())
                        is Resultado.Fallo -> UiState.Error(resultado.error)
                    }
            }
        }
    }

private fun Paciente.aFicha(): FichaPacienteUi =
    FichaPacienteUi(
        paciente = aUi(),
        edad = edadDesde(fechaNacimiento),
        telefono = telefono.orEmpty(),
        // TarjetaContacto separa nombre y teléfono por " · ".
        contactoEmergencia = listOfNotNull(contactoEmergencia, telefonoEmergencia).joinToString(" · "),
        asignacion = null,
        citas = emptyList(),
    )
