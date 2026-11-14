package sv.edu.unicaes.hospital.ui.recepcion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.TurnosRepository
import sv.edu.unicaes.hospital.domain.model.DatosTurno
import sv.edu.unicaes.hospital.domain.model.Empleado
import sv.edu.unicaes.hospital.domain.model.TipoTurno
import sv.edu.unicaes.hospital.domain.model.Turno
import sv.edu.unicaes.hospital.ui.modelo.AreaTurnosUi
import sv.edu.unicaes.hospital.ui.modelo.DiaUi
import sv.edu.unicaes.hospital.ui.modelo.agruparPorArea
import sv.edu.unicaes.hospital.ui.modelo.claveDia
import sv.edu.unicaes.hospital.ui.modelo.etiquetaClave
import sv.edu.unicaes.hospital.ui.modelo.etiquetaSemana
import sv.edu.unicaes.hospital.ui.modelo.etiquetaTipoPersonal
import sv.edu.unicaes.hospital.ui.modelo.horaValida
import sv.edu.unicaes.hospital.ui.modelo.horarioSugerido
import sv.edu.unicaes.hospital.ui.modelo.semanaDe
import java.util.Date
import javax.inject.Inject

/** Formulario "Nuevo turno" / "Ajustar turno". Con [turnoId] se está ajustando ese turno. */
data class HojaTurnoUiState(
    val fecha: String,
    val turnoId: String? = null,
    val personalId: String? = null,
    val area: String = "",
    val inicio: String = horarioSugerido(TipoTurno.MANANA).first,
    val fin: String = horarioSugerido(TipoTurno.MANANA).second,
    val tipoTurno: TipoTurno = TipoTurno.MANANA,
    val notas: String = "",
    val guardando: Boolean = false,
    /** Un solape llega como [AppError.Rechazada] con el turno que choca. */
    val error: AppError? = null,
) {
    val ajustando: Boolean get() = turnoId != null

    val inicioValido: Boolean get() = horaValida(inicio)

    val finValido: Boolean get() = horaValida(fin) && fin != inicio

    val puedeGuardar: Boolean
        get() = personalId != null && area.isNotBlank() && inicioValido && finValido && !guardando
}

data class TurnosPersonalUiState(
    /** 0 = esta semana, -1 la anterior, 1 la siguiente… */
    val semana: Int = 0,
    val etiquetaSemana: String = "",
    val dias: List<DiaUi> = emptyList(),
    val dia: Int = 0,
    /** Tipos de personal con turno el día elegido, para filtrar; 0 en [tipo] es "Todos". */
    val tipos: List<String> = emptyList(),
    val tipo: Int = 0,
    val estado: UiState<List<AreaTurnosUi>> = UiState.Cargando,
    val empleados: UiState<List<Empleado>> = UiState.Cargando,
    val hoja: HojaTurnoUiState? = null,
)

/**
 * AND-R12 · Control de turnos (HUNI-53). Lee de `v_turnos` la semana elegida y muestra los del día agrupados
 * por área y tipo de personal. Recepción registra o ajusta un turno; la regla de no solape la valida el
 * trigger de la base y el mensaje se muestra en el formulario sin cerrarlo.
 */
@HiltViewModel
class TurnosPersonalViewModel
    @Inject
    constructor(
        private val turnosRepository: TurnosRepository,
    ) : ViewModel(),
        AccionesTurnos {
        private val _uiState = MutableStateFlow(TurnosPersonalUiState())
        val uiState: StateFlow<TurnosPersonalUiState> = _uiState.asStateFlow()

        /** Turnos de la semana cargada; el día y el filtro se derivan de aquí sin volver a pedirlos. */
        private var turnosSemana: List<Turno> = emptyList()
        private var carga: Job? = null

        init {
            val dias = semanaDe(0)
            val hoy = dias.indexOf(claveDia(Date())).coerceAtLeast(0)
            irASemana(0, hoy)
        }

        override fun onSemana(delta: Int) = irASemana(_uiState.value.semana + delta, _uiState.value.dia)

        override fun onDia(indice: Int) {
            _uiState.update { it.copy(dia = indice, tipo = 0) }
            derivar()
        }

        override fun onTipo(indice: Int) {
            _uiState.update { it.copy(tipo = indice) }
            derivar()
        }

        private fun irASemana(
            semana: Int,
            dia: Int,
        ) {
            val dias = semanaDe(semana)
            _uiState.update {
                it.copy(
                    semana = semana,
                    etiquetaSemana = etiquetaSemana(dias),
                    dias = dias.map { clave -> DiaUi(clave, etiquetaClave(clave)) },
                    dia = dia.coerceIn(0, dias.lastIndex),
                    tipo = 0,
                )
            }
            cargar()
        }

        override fun cargar() {
            carga?.cancel()
            val dias = _uiState.value.dias
            if (dias.isEmpty()) return
            _uiState.update { it.copy(estado = UiState.Cargando) }
            carga =
                viewModelScope.launch {
                    when (val resultado = turnosRepository.entre(dias.first().clave, dias.last().clave)) {
                        is Resultado.Exito -> {
                            turnosSemana = resultado.datos
                            derivar()
                        }

                        is Resultado.Fallo -> {
                            turnosSemana = emptyList()
                            _uiState.update { it.copy(estado = UiState.Error(resultado.error), tipos = emptyList()) }
                        }
                    }
                }
        }

        private fun derivar() {
            val estado = _uiState.value
            if (estado.estado is UiState.Error) return
            val clave = estado.dias.getOrNull(estado.dia)?.clave ?: return
            val delDia = turnosSemana.filter { it.fecha == clave }
            val tipos = delDia.map { etiquetaTipoPersonal(it.tipoPersonal) }.distinct().sorted()
            val tipo = estado.tipo.takeIf { it <= tipos.size } ?: 0
            val filtrados =
                if (tipo == 0) delDia else delDia.filter { etiquetaTipoPersonal(it.tipoPersonal) == tipos[tipo - 1] }
            val grupos = agruparPorArea(filtrados)
            _uiState.update {
                it.copy(
                    tipos = tipos,
                    tipo = tipo,
                    estado = if (grupos.isEmpty()) UiState.Vacio else UiState.Exito(grupos),
                )
            }
        }

        // Formulario

        override fun abrirNuevo() {
            val estado = _uiState.value
            val fecha = estado.dias.getOrNull(estado.dia)?.clave ?: return
            _uiState.update { it.copy(hoja = HojaTurnoUiState(fecha = fecha)) }
            cargarEmpleados()
        }

        override fun abrirAjuste(turnoId: String) {
            val turno = turnosSemana.firstOrNull { it.id == turnoId } ?: return
            _uiState.update {
                it.copy(
                    hoja =
                        HojaTurnoUiState(
                            fecha = turno.fecha,
                            turnoId = turno.id,
                            personalId = turno.personalId,
                            area = turno.area,
                            inicio = turno.horaInicio,
                            fin = turno.horaFin,
                            tipoTurno = turno.tipoTurno,
                            notas = turno.notas.orEmpty(),
                        ),
                )
            }
            cargarEmpleados()
        }

        override fun cerrarHoja() = _uiState.update { it.copy(hoja = null) }

        override fun cargarEmpleados() {
            if (_uiState.value.empleados is UiState.Exito) return
            _uiState.update { it.copy(empleados = UiState.Cargando) }
            viewModelScope.launch {
                val empleados =
                    when (val resultado = turnosRepository.empleados()) {
                        is Resultado.Exito -> {
                            if (resultado.datos.isEmpty()) UiState.Vacio else UiState.Exito(resultado.datos)
                        }

                        is Resultado.Fallo -> {
                            UiState.Error(resultado.error)
                        }
                    }
                _uiState.update { it.copy(empleados = empleados) }
            }
        }

        private fun editarHoja(cambio: (HojaTurnoUiState) -> HojaTurnoUiState) =
            _uiState.update { estado -> estado.copy(hoja = estado.hoja?.let { cambio(it).copy(error = null) }) }

        override fun onPersona(id: String) = editarHoja { it.copy(personalId = id) }

        override fun onArea(valor: String) = editarHoja { it.copy(area = valor) }

        override fun onFecha(clave: String) = editarHoja { it.copy(fecha = clave) }

        override fun onInicio(valor: String) = editarHoja { it.copy(inicio = valor.take(LARGO_HORA)) }

        override fun onFin(valor: String) = editarHoja { it.copy(fin = valor.take(LARGO_HORA)) }

        override fun onNotas(valor: String) = editarHoja { it.copy(notas = valor) }

        /** Al registrar, el tipo propone su horario; al ajustar se respetan las horas que ya tenía. */
        override fun onTipoTurno(tipo: TipoTurno) =
            editarHoja { hoja ->
                if (hoja.ajustando) {
                    hoja.copy(tipoTurno = tipo)
                } else {
                    val (inicio, fin) = horarioSugerido(tipo)
                    hoja.copy(tipoTurno = tipo, inicio = inicio, fin = fin)
                }
            }

        override fun guardar() {
            val hoja = _uiState.value.hoja ?: return
            if (!hoja.puedeGuardar) return
            val personalId = hoja.personalId ?: return
            val datos =
                DatosTurno(
                    personalId = personalId,
                    area = hoja.area.trim(),
                    fecha = hoja.fecha,
                    horaInicio = hoja.inicio,
                    horaFin = hoja.fin,
                    tipoTurno = hoja.tipoTurno,
                    notas = hoja.notas.trim().ifEmpty { null },
                )
            _uiState.update { it.copy(hoja = hoja.copy(guardando = true, error = null)) }
            viewModelScope.launch {
                val resultado =
                    hoja.turnoId?.let { turnosRepository.actualizar(it, datos) } ?: turnosRepository.crear(datos)
                when (resultado) {
                    is Resultado.Exito -> {
                        // Se muestra el día del turno guardado (dentro de la semana a la vista).
                        val indice = _uiState.value.dias.indexOfFirst { it.clave == hoja.fecha }
                        _uiState.update {
                            it.copy(hoja = null, dia = if (indice >= 0) indice else it.dia, tipo = 0)
                        }
                        cargar()
                    }

                    is Resultado.Fallo -> {
                        _uiState.update {
                            it.copy(hoja = it.hoja?.copy(guardando = false, error = resultado.error))
                        }
                    }
                }
            }
        }

        private companion object {
            const val LARGO_HORA = 5
        }
    }
