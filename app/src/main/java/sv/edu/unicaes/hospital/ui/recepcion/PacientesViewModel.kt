package sv.edu.unicaes.hospital.ui.recepcion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import javax.inject.Inject

data class PacientesUiState(
    val busqueda: String = "",
    val estado: UiState<List<PacienteUi>> = UiState.Cargando,
    val cargandoMas: Boolean = false,
    val hayMas: Boolean = false,
)

/** AND-R2 · Pacientes: la búsqueda se hace en la API, paginada de 20 en 20. */
@OptIn(FlowPreview::class)
@HiltViewModel
class PacientesViewModel
    @Inject
    constructor(
        private val pacientesRepository: PacientesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PacientesUiState())
        val uiState: StateFlow<PacientesUiState> = _uiState.asStateFlow()

        private var pagina = 1
        private var carga: Job? = null

        init {
            cargar()
            viewModelScope.launch {
                _uiState
                    .map { terminoDeBusqueda(it.busqueda) }
                    .distinctUntilChanged()
                    .drop(1) // la primera carga ya salió en cargar()
                    .debounce(ESPERA_BUSQUEDA_MS)
                    .collect { cargar() }
            }
        }

        fun onBusquedaChange(valor: String) = _uiState.update { it.copy(busqueda = valor) }

        /** Primera página con la búsqueda actual (al abrir, al reintentar o al volver de guardar). */
        fun cargar() {
            carga?.cancel()
            pagina = 1
            _uiState.update { it.copy(estado = UiState.Cargando, cargandoMas = false) }
            carga =
                viewModelScope.launch {
                    val termino = terminoDeBusqueda(_uiState.value.busqueda)
                    when (val resultado = pacientesRepository.listar(termino, pagina)) {
                        is Resultado.Exito -> {
                            val pacientes = resultado.datos.items.map { it.aUi() }
                            _uiState.update {
                                it.copy(
                                    estado = if (pacientes.isEmpty()) UiState.Vacio else UiState.Exito(pacientes),
                                    hayMas = resultado.datos.hayMas,
                                )
                            }
                        }

                        is Resultado.Fallo -> {
                            _uiState.update { it.copy(estado = UiState.Error(resultado.error), hayMas = false) }
                        }
                    }
                }
        }

        /** Siguiente página al llegar al final de la lista. Si falla, la lista se queda como está. */
        fun cargarMas() {
            val actual = _uiState.value
            val lista = (actual.estado as? UiState.Exito)?.datos ?: return
            if (!actual.hayMas || actual.cargandoMas) return
            _uiState.update { it.copy(cargandoMas = true) }
            carga =
                viewModelScope.launch {
                    val termino = terminoDeBusqueda(actual.busqueda)
                    when (val resultado = pacientesRepository.listar(termino, pagina + 1)) {
                        is Resultado.Exito -> {
                            pagina++
                            _uiState.update {
                                it.copy(
                                    estado = UiState.Exito(lista + resultado.datos.items.map { p -> p.aUi() }),
                                    hayMas = resultado.datos.hayMas,
                                    cargandoMas = false,
                                )
                            }
                        }

                        is Resultado.Fallo -> {
                            _uiState.update { it.copy(cargandoMas = false) }
                        }
                    }
                }
        }

        private companion object {
            const val ESPERA_BUSQUEDA_MS = 300L
            const val MINIMO_BUSQUEDA = 2
        }

        /** La API pide al menos 2 caracteres; con menos se lista sin filtro. */
        private fun terminoDeBusqueda(texto: String): String? = texto.trim().takeIf { it.length >= MINIMO_BUSQUEDA }
    }

/** Fila de la lista: debajo del nombre va el documento o, si no tiene, el número de expediente. */
fun Paciente.aUi(): PacienteUi = PacienteUi(id = id, nombre = nombreCompleto, dui = documento ?: numeroExpediente)
