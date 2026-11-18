package sv.edu.unicaes.hospital.ui.doctor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.ConsultasRepository
import sv.edu.unicaes.hospital.data.repository.LaboratorioRepository
import sv.edu.unicaes.hospital.domain.model.ExamenCatalogo
import sv.edu.unicaes.hospital.domain.model.NuevaConsulta
import sv.edu.unicaes.hospital.domain.model.NuevaOrden
import sv.edu.unicaes.hospital.domain.model.NuevaReceta
import java.text.Normalizer
import javax.inject.Inject

/** Paciente de la consulta: nombre para la cabecera y alergias para avisar al recetar. */
data class ContextoConsultaUi(
    val paciente: String,
    val alergias: String?,
)

/** Receta agregada al formulario; [enviada] cuando la API ya la registró (no se vuelve a enviar). */
data class RecetaBorradorUi(
    val id: Long,
    val receta: NuevaReceta,
    val enviada: Boolean = false,
)

/** Orden de un examen agregada al formulario; nace Pendiente al guardar. */
data class OrdenBorradorUi(
    val id: Long,
    val examen: ExamenCatalogo,
    val indicaciones: String,
    val enviada: Boolean = false,
)

data class NuevaConsultaUiState(
    val contexto: UiState<ContextoConsultaUi> = UiState.Cargando,
    /** Hora `HH:mm` de la cita que se atiende, o null si la consulta no viene de una cita. */
    val citaHora: String? = null,
    val catalogo: UiState<List<ExamenCatalogo>> = UiState.Cargando,
    val diagnostico: String = "",
    val notas: String = "",
    val recetas: List<RecetaBorradorUi> = emptyList(),
    val ordenes: List<OrdenBorradorUi> = emptyList(),
    /** Se pulsó Guardar al menos una vez: desde ahí se marcan los campos obligatorios vacíos. */
    val intentoGuardar: Boolean = false,
    val guardando: Boolean = false,
    val error: AppError? = null,
    /** La consulta ya existe en la API: las notas viajaron con ella y no se pueden cambiar. */
    val consultaCreada: Boolean = false,
    val diagnosticoEnviado: Boolean = false,
    /** Error sin arreglo antes de crear la consulta (la cita ya tiene consulta, sin permiso…). */
    val bloqueada: Boolean = false,
    val guardada: Boolean = false,
) {
    val diagnosticoFaltante: Boolean get() = intentoGuardar && diagnostico.isBlank()

    val puedeGuardar: Boolean get() = contexto is UiState.Exito && !guardando && !bloqueada && !guardada

    /** Mientras se guarda (o ya se guardó) el formulario no se toca. */
    val editable: Boolean get() = !guardando && !guardada

    /** Un examen se pide una sola vez por consulta. */
    val examenesAgregados: Set<String> get() = ordenes.mapTo(mutableSetOf()) { it.examen.id }
}

/**
 * AND-D5 y D6 · Nueva consulta (HUNI-61): diagnóstico, notas, recetas y órdenes de laboratorio.
 *
 * La API no guarda todo en una transacción, así que se guarda por pasos, como en iOS (HUNI-58): crear la
 * consulta, registrar el diagnóstico, cada receta, cada orden (nace Pendiente y Recepción la ve en su cola)
 * y cerrar la consulta. Si un paso falla, "Guardar" reintenta desde ahí sin repetir lo que ya se envió.
 */
@HiltViewModel
class NuevaConsultaViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val consultasRepository: ConsultasRepository,
        private val laboratorioRepository: LaboratorioRepository,
    ) : ViewModel() {
        // Mismos nombres que las propiedades de NuevaConsultaRuta.
        private val pacienteId: String = checkNotNull(savedStateHandle["pacienteId"])
        private val citaId: String? = savedStateHandle["citaId"]

        private val _uiState = MutableStateFlow(NuevaConsultaUiState(citaHora = savedStateHandle["citaHora"]))
        val uiState: StateFlow<NuevaConsultaUiState> = _uiState.asStateFlow()

        private var consultaId: String? = null
        private var siguienteId = 0L

        init {
            cargar()
            cargarCatalogo()
        }

        /** Nombre y alergias del paciente desde su historial. */
        fun cargar() {
            _uiState.update { it.copy(contexto = UiState.Cargando) }
            viewModelScope.launch {
                val contexto =
                    when (val r = consultasRepository.historial(pacienteId)) {
                        is Resultado.Exito -> {
                            UiState.Exito(
                                ContextoConsultaUi(r.datos.paciente.nombreCompleto, r.datos.expediente.alergias),
                            )
                        }

                        is Resultado.Fallo -> {
                            UiState.Error(r.error)
                        }
                    }
                _uiState.update { it.copy(contexto = contexto) }
            }
        }

        fun cargarCatalogo() {
            _uiState.update { it.copy(catalogo = UiState.Cargando) }
            viewModelScope.launch {
                val catalogo =
                    when (val r = laboratorioRepository.examenes()) {
                        is Resultado.Exito -> if (r.datos.isEmpty()) UiState.Vacio else UiState.Exito(r.datos)
                        is Resultado.Fallo -> UiState.Error(r.error)
                    }
                _uiState.update { it.copy(catalogo = catalogo) }
            }
        }

        fun onDiagnostico(texto: String) {
            if (!_uiState.value.editable || _uiState.value.diagnosticoEnviado) return
            _uiState.update { it.copy(diagnostico = texto, error = null) }
        }

        fun onNotas(texto: String) {
            if (!_uiState.value.editable || _uiState.value.consultaCreada) return
            _uiState.update { it.copy(notas = texto, error = null) }
        }

        /** Sin medicamento no se agrega (el formulario ya deshabilita el botón). */
        fun agregarReceta(receta: NuevaReceta) {
            if (!_uiState.value.editable || receta.medicamento.isBlank()) return
            val limpia =
                receta.copy(
                    medicamento = receta.medicamento.trim(),
                    dosis = receta.dosis?.trim(),
                    frecuencia = receta.frecuencia?.trim(),
                    indicaciones = receta.indicaciones?.trim(),
                )
            _uiState.update { it.copy(recetas = it.recetas + RecetaBorradorUi(siguienteId++, limpia)) }
        }

        /** Solo se quita lo que aún no se envió. */
        fun quitarReceta(id: Long) {
            if (!_uiState.value.editable) return
            _uiState.update { estado -> estado.copy(recetas = estado.recetas.filterNot { it.id == id && !it.enviada }) }
        }

        fun agregarOrden(
            examen: ExamenCatalogo,
            indicaciones: String,
        ) {
            val estado = _uiState.value
            if (!estado.editable || examen.id in estado.examenesAgregados) return
            _uiState.update {
                it.copy(ordenes = it.ordenes + OrdenBorradorUi(siguienteId++, examen, indicaciones.trim()))
            }
        }

        fun quitarOrden(id: Long) {
            if (!_uiState.value.editable) return
            _uiState.update { estado -> estado.copy(ordenes = estado.ordenes.filterNot { it.id == id && !it.enviada }) }
        }

        fun guardar() {
            val estado = _uiState.value
            if (!estado.puedeGuardar) return
            if (estado.diagnostico.isBlank()) {
                _uiState.update { it.copy(intentoGuardar = true) }
                return
            }
            _uiState.update { it.copy(intentoGuardar = true, guardando = true, error = null) }
            viewModelScope.launch {
                val error = guardarPendiente()
                _uiState.update {
                    if (error == null) {
                        it.copy(guardando = false, guardada = true)
                    } else {
                        it.copy(
                            guardando = false,
                            error = error,
                            bloqueada = consultaId == null && error.esDefinitivo(),
                        )
                    }
                }
            }
        }

        /** Envía lo que falta, en orden. Devuelve el primer error, o null si todo quedó guardado. */
        private suspend fun guardarPendiente(): AppError? {
            val id =
                consultaId ?: when (
                    val r =
                        consultasRepository.crear(NuevaConsulta(pacienteId, citaId, notas = _uiState.value.notas))
                ) {
                    is Resultado.Exito -> {
                        consultaId = r.datos
                        _uiState.update { it.copy(consultaCreada = true) }
                        r.datos
                    }

                    is Resultado.Fallo -> {
                        return r.error
                    }
                }

            if (!_uiState.value.diagnosticoEnviado) {
                val r = consultasRepository.agregarDiagnostico(id, _uiState.value.diagnostico)
                if (r is Resultado.Fallo) return r.error
                _uiState.update { it.copy(diagnosticoEnviado = true) }
            }

            for (borrador in _uiState.value.recetas.filterNot { it.enviada }) {
                val r = consultasRepository.emitirReceta(id, borrador.receta)
                if (r is Resultado.Fallo) return r.error
                marcarReceta(borrador.id)
            }

            for (borrador in _uiState.value.ordenes.filterNot { it.enviada }) {
                val orden = NuevaOrden(listOf(borrador.examen.id), borrador.indicaciones)
                val r = laboratorioRepository.crearOrden(id, orden)
                if (r is Resultado.Fallo) return r.error
                marcarOrden(borrador.id)
            }

            return (consultasRepository.cerrar(id) as? Resultado.Fallo)?.error
        }

        private fun marcarReceta(id: Long) =
            _uiState.update { e ->
                e.copy(
                    recetas =
                        e.recetas.map {
                            if (it.id ==
                                id
                            ) {
                                it.copy(enviada = true)
                            } else {
                                it
                            }
                        },
                )
            }

        private fun marcarOrden(id: Long) =
            _uiState.update { e ->
                e.copy(
                    ordenes =
                        e.ordenes.map {
                            if (it.id ==
                                id
                            ) {
                                it.copy(enviada = true)
                            } else {
                                it
                            }
                        },
                )
            }
    }

/**
 * Errores al crear la consulta que no se arreglan reintentando: la cita ya tiene consulta (409), no es del
 * doctor (403), el paciente o la cita no existen, o una regla de negocio (cita cancelada, paciente inactivo).
 */
private fun AppError.esDefinitivo(): Boolean =
    this is AppError.Duplicado ||
        this is AppError.SinPermiso ||
        this is AppError.NoEncontrado ||
        this is AppError.Rechazada

/**
 * Alergias registradas que parecen coincidir con [medicamento] (sin tildes ni mayúsculas): "Ibuprofeno"
 * con "ibuprofeno 400 mg". Es una ayuda, no un bloqueo: no conoce familias de fármacos (penicilina y
 * amoxicilina), igual que en iOS.
 */
internal fun alergiasCoincidentes(
    medicamento: String,
    alergias: String?,
): List<String> {
    if (alergias.isNullOrBlank()) return emptyList()
    val buscado = normalizar(medicamento)
    if (buscado.isEmpty()) return emptyList()
    val primeraPalabra = buscado.substringBefore(' ')
    return alergias
        .split(SEPARADORES_ALERGIAS)
        .map { it.trim() }
        .filter { alergia ->
            val normal = normalizar(alergia)
            normal.length >= MINIMO_LETRAS_ALERGIA &&
                normal !in SIN_ALERGIAS &&
                (buscado.contains(normal) || (primeraPalabra.length >= 4 && normal.contains(primeraPalabra)))
        }
}

private val SEPARADORES_ALERGIAS = Regex("""[,;/\n]|\s+y\s+""", RegexOption.IGNORE_CASE)
private val SIN_ALERGIAS = setOf("ninguna", "ninguno", "n/a", "sin alergias")
private const val MINIMO_LETRAS_ALERGIA = 3
private val MARCAS = Regex("""\p{Mn}+""")

private fun normalizar(texto: String): String =
    Normalizer
        .normalize(texto.trim(), Normalizer.Form.NFD)
        .replace(MARCAS, "")
        .lowercase()
