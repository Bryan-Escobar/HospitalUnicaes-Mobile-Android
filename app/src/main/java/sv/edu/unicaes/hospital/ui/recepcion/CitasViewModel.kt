package sv.edu.unicaes.hospital.ui.recepcion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.CitasRepository
import sv.edu.unicaes.hospital.ui.modelo.AgendaDoctorUi
import sv.edu.unicaes.hospital.ui.modelo.DiaUi
import sv.edu.unicaes.hospital.ui.modelo.UN_DIA_MS
import sv.edu.unicaes.hospital.ui.modelo.agruparAgenda
import sv.edu.unicaes.hospital.ui.modelo.etiquetaDia
import sv.edu.unicaes.hospital.ui.modelo.fechaHoraHospital
import sv.edu.unicaes.hospital.ui.modelo.isoConZona
import sv.edu.unicaes.hospital.ui.modelo.proximosDias
import java.util.Date
import javax.inject.Inject

data class CitasUiState(
    val dias: List<DiaUi> = emptyList(),
    val dia: Int = 0,
    val estado: UiState<List<AgendaDoctorUi>> = UiState.Cargando,
) {
    val diaElegido: DiaUi? get() = dias.getOrNull(dia)
}

/** Días que se ven en la agenda y en Nueva cita, desde hoy. */
internal const val DIAS_AGENDA = 7

/** Días de la semana desde hoy como chips (`Jue 29`). */
internal fun diasAgenda(): List<DiaUi> =
    proximosDias(DIAS_AGENDA).map { clave ->
        DiaUi(clave, fechaHoraHospital(clave)?.let { etiquetaDia(it) } ?: clave)
    }

/**
 * AND-R5 · Agenda del día: turnos de los médicos (`v_turnos`) y citas no canceladas (`v_citas`), cargados en
 * paralelo y agrupados por doctor con sus huecos libres.
 */
@HiltViewModel
class CitasViewModel
    @Inject
    constructor(
        private val citasRepository: CitasRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CitasUiState(dias = diasAgenda()))
        val uiState: StateFlow<CitasUiState> = _uiState.asStateFlow()

        private var carga: Job? = null

        init {
            cargar()
        }

        fun onDia(indice: Int) {
            _uiState.update { it.copy(dia = indice) }
            cargar()
        }

        /** Al abrir, al cambiar de día, al reintentar y al volver de guardar o cancelar una cita. */
        fun cargar() {
            carga?.cancel()
            val desde = _uiState.value.diaElegido?.let { fechaHoraHospital(it.clave) }
            if (desde == null) {
                _uiState.update { it.copy(estado = UiState.Error(AppError.Desconocido())) }
                return
            }
            val hasta = Date(desde.time + UN_DIA_MS)
            _uiState.update { it.copy(estado = UiState.Cargando) }
            carga =
                viewModelScope.launch {
                    val citas = async { citasRepository.entre(isoConZona(desde), isoConZona(hasta)) }
                    val turnos = async { citasRepository.turnosMedicos(isoConZona(desde), isoConZona(hasta)) }
                    val estado =
                        when (val c = citas.await()) {
                            is Resultado.Fallo -> {
                                UiState.Error(c.error)
                            }

                            is Resultado.Exito -> {
                                when (val t = turnos.await()) {
                                    is Resultado.Fallo -> {
                                        UiState.Error(t.error)
                                    }

                                    is Resultado.Exito -> {
                                        val agendas = agruparAgenda(c.datos, t.datos, desde, hasta)
                                        if (agendas.isEmpty()) UiState.Vacio else UiState.Exito(agendas)
                                    }
                                }
                            }
                        }
                    _uiState.update { it.copy(estado = estado) }
                }
        }
    }
