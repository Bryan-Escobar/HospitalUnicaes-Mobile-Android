package sv.edu.unicaes.hospital.ui.turnos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.TurnosRepository
import sv.edu.unicaes.hospital.ui.modelo.TurnoUi
import sv.edu.unicaes.hospital.ui.modelo.misTurnosSemana
import sv.edu.unicaes.hospital.ui.modelo.semanaDe
import javax.inject.Inject

/**
 * AND-D7 · Mis turnos (HUNI-53): esta semana y la siguiente del usuario con sesión, para el doctor y para
 * recepción. Vacío solo si no tiene ningún turno en las dos semanas; los días libres salen como "Sin turno".
 */
@HiltViewModel
class MisTurnosViewModel
    @Inject
    constructor(
        private val turnosRepository: TurnosRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<UiState<Pair<List<TurnoUi>, List<TurnoUi>>>>(UiState.Cargando)
        val uiState: StateFlow<UiState<Pair<List<TurnoUi>, List<TurnoUi>>>> = _uiState.asStateFlow()

        private var carga: Job? = null

        init {
            cargar()
        }

        fun cargar() {
            carga?.cancel()
            val esta = semanaDe(0)
            val siguiente = semanaDe(1)
            if (esta.isEmpty() || siguiente.isEmpty()) return
            _uiState.value = UiState.Cargando
            carga =
                viewModelScope.launch {
                    _uiState.value =
                        when (val resultado = turnosRepository.mios(esta.first(), siguiente.last())) {
                            is Resultado.Exito -> {
                                if (resultado.datos.isEmpty()) {
                                    UiState.Vacio
                                } else {
                                    UiState.Exito(
                                        misTurnosSemana(esta, resultado.datos) to
                                            misTurnosSemana(siguiente, resultado.datos),
                                    )
                                }
                            }

                            is Resultado.Fallo -> {
                                UiState.Error(resultado.error)
                            }
                        }
                }
        }
    }
