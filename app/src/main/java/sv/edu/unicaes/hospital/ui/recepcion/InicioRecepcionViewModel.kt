package sv.edu.unicaes.hospital.ui.recepcion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.AmbulanciaRepository
import sv.edu.unicaes.hospital.data.repository.CamasRepository
import sv.edu.unicaes.hospital.data.repository.CitasRepository
import sv.edu.unicaes.hospital.data.repository.LaboratorioRepository
import sv.edu.unicaes.hospital.data.repository.TurnosRepository
import sv.edu.unicaes.hospital.domain.model.Cita
import sv.edu.unicaes.hospital.domain.model.EstadoCama
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.Habitacion
import sv.edu.unicaes.hospital.domain.model.SolicitudAmbulancia
import sv.edu.unicaes.hospital.domain.model.TipoTurno
import sv.edu.unicaes.hospital.domain.model.Turno
import sv.edu.unicaes.hospital.ui.modelo.InicioRecepcionUi
import sv.edu.unicaes.hospital.ui.modelo.OcupacionUi
import sv.edu.unicaes.hospital.ui.modelo.TurnoUi
import sv.edu.unicaes.hospital.ui.modelo.UN_DIA_MS
import sv.edu.unicaes.hospital.ui.modelo.claveDia
import sv.edu.unicaes.hospital.ui.modelo.enCurso
import sv.edu.unicaes.hospital.ui.modelo.etiquetaTipoPersonal
import sv.edu.unicaes.hospital.ui.modelo.fechaHoraHospital
import sv.edu.unicaes.hospital.ui.modelo.horario
import sv.edu.unicaes.hospital.ui.modelo.isoConZona
import sv.edu.unicaes.hospital.ui.modelo.terminado
import java.util.Date
import javax.inject.Inject

/**
 * AND-R1 · Inicio. Turno, bandeja, ocupación y citas salen de los mismos repositorios que sus pantallas.
 * Ambulancia, laboratorio y camas avisan por Realtime; citas y turnos se vuelven a pedir al reanudar.
 */
@HiltViewModel
class InicioRecepcionViewModel
    @Inject
    constructor(
        private val turnos: TurnosRepository,
        private val ambulancia: AmbulanciaRepository,
        private val laboratorio: LaboratorioRepository,
        private val camas: CamasRepository,
        private val citas: CitasRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<UiState<InicioRecepcionUi>>(UiState.Cargando)
        val uiState: StateFlow<UiState<InicioRecepcionUi>> = _uiState.asStateFlow()

        private var carga: Job? = null

        init {
            cargar()
            viewModelScope.launch {
                merge(
                    ambulancia.cambios().ignorarFallo(),
                    laboratorio.cambios().ignorarFallo(),
                    camas.cambios().ignorarFallo(),
                ).collect { refrescar() }
            }
        }

        fun cargar() {
            val mostrar = _uiState.value !is UiState.Exito
            carga?.cancel()
            if (mostrar) _uiState.value = UiState.Cargando
            carga = viewModelScope.launch { recargar() }
        }

        private fun refrescar() {
            if (_uiState.value is UiState.Cargando) return
            cargar()
        }

        private suspend fun recargar() {
            val ahora = Date()
            val hoy = claveDia(ahora)
            val desde = fechaHoraHospital(hoy)
            if (desde == null) {
                if (_uiState.value !is UiState.Exito) _uiState.value = UiState.Error(AppError.Desconocido())
                return
            }
            val hasta = Date(desde.time + UN_DIA_MS)
            val (turno, pendientes, ordenes, habitaciones, citasHoy) =
                coroutineScope {
                    val turnoDiferido = async { turnos.mios(hoy, hoy) }
                    val pendientesDiferido = async { ambulancia.pendientes() }
                    val ordenesDiferido = async { laboratorio.contar(EstadoLaboratorio.PENDIENTE) }
                    val habitacionesDiferido = async { camas.habitaciones() }
                    val citasDiferido = async { citas.entre(isoConZona(desde), isoConZona(hasta)) }
                    DatosInicio(
                        turnoDiferido.await(),
                        pendientesDiferido.await(),
                        ordenesDiferido.await(),
                        habitacionesDiferido.await(),
                        citasDiferido.await(),
                    )
                }
            val fallo =
                listOf(turno, pendientes, ordenes, habitaciones, citasHoy)
                    .firstNotNullOfOrNull { (it as? Resultado.Fallo)?.error }
            if (fallo != null) {
                if (_uiState.value !is UiState.Exito) _uiState.value = UiState.Error(fallo)
                return
            }
            _uiState.value =
                UiState.Exito(
                    armarInicio(
                        turnos = (turno as Resultado.Exito).datos,
                        pendientes = (pendientes as Resultado.Exito).datos,
                        laboratorioPendiente = (ordenes as Resultado.Exito).datos,
                        habitaciones = (habitaciones as Resultado.Exito).datos,
                        citas = (citasHoy as Resultado.Exito).datos,
                        ahora = ahora,
                    ),
                )
        }
    }

private data class DatosInicio(
    val turno: Resultado<List<Turno>>,
    val pendientes: Resultado<List<SolicitudAmbulancia>>,
    val ordenes: Resultado<Int>,
    val habitaciones: Resultado<List<Habitacion>>,
    val citas: Resultado<List<Cita>>,
)

private fun Flow<Unit>.ignorarFallo(): Flow<Unit> = catch { error -> if (error is CancellationException) throw error }

/** Arma la bandeja. La tarjeta de ambulancia es la solicitud pendiente más reciente. */
internal fun armarInicio(
    turnos: List<Turno>,
    pendientes: List<SolicitudAmbulancia>,
    laboratorioPendiente: Int,
    habitaciones: List<Habitacion>,
    citas: List<Cita>,
    ahora: Date = Date(),
): InicioRecepcionUi {
    val recientes = pendientes.sortedByDescending { it.fechaSolicitud }
    val citasUi = citas.map { it.aUi(it.paciente) }
    return InicioRecepcionUi(
        turno = turnoDeInicio(turnos, ahora),
        solicitudAmbulancia = recientes.firstOrNull()?.aUi(ahora),
        solicitudesAmbulancia = pendientes.size,
        laboratorioPendiente = laboratorioPendiente,
        ocupacion = ocupacionDe(habitaciones),
        citasHoyTotal = citasUi.size,
        citasHoy = citasUi,
    )
}

/** El turno de hoy que está en curso; si no, el próximo; si ya terminaron, el último. */
internal fun turnoDeInicio(
    turnos: List<Turno>,
    ahora: Date = Date(),
): TurnoUi {
    val delDia = turnos.filter { it.fecha == claveDia(ahora) }.sortedBy { it.horaInicio }
    val elegido =
        delDia.firstOrNull { it.enCurso(ahora) }
            ?: delDia.firstOrNull { !it.terminado(ahora) }
            ?: delDia.lastOrNull()
    if (elegido == null) return TurnoUi(dia = "Hoy", horario = "", area = "", sinTurno = true)
    val area = if (elegido.tipoTurno == TipoTurno.NOCHE) "${elegido.area} · nocturno" else elegido.area
    return TurnoUi(
        dia = "Hoy",
        horario = elegido.horario,
        area = area,
        tipo = etiquetaTipoPersonal(elegido.tipoPersonal),
        enCurso = elegido.enCurso(ahora),
        terminado = elegido.terminado(ahora),
    )
}

/** Libres son las camas `disponible`. El resto cuenta como ocupado para la barra de la planta. */
internal fun ocupacionDe(habitaciones: List<Habitacion>): List<OcupacionUi> =
    habitaciones
        .groupBy { it.tipo }
        .map { (tipo, salas) ->
            val camas = salas.flatMap { it.camas }
            OcupacionUi(
                tipo = tipo,
                ocupadas = camas.count { it.estado != EstadoCama.DISPONIBLE },
                total = camas.size,
            )
        }.filter { it.total > 0 }
        .sortedBy { it.tipo.planta }
