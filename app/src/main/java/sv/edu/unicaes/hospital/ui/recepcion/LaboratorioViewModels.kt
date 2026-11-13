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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.local.ArchivosLocales
import sv.edu.unicaes.hospital.data.repository.LaboratorioRepository
import sv.edu.unicaes.hospital.domain.model.ArchivoAdjunto
import sv.edu.unicaes.hospital.domain.model.DatosResultado
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.ExamenOrden
import sv.edu.unicaes.hospital.domain.model.OrdenLaboratorio
import javax.inject.Inject

// Laboratorio de Recepción/Asistente (HUNI-63): cola filtrable por estado, detalle con el avance de cada
// examen y el formulario de resultado. La API valida las transiciones; Supabase Realtime avisa de cambios
// hechos en otro dispositivo (iOS, Web u otro Android) y la pantalla vuelve a pedir los datos.

/** Espera entre un aviso de Realtime y la recarga: un cambio de examen también cambia su orden. */
internal const val ESPERA_AVISOS_MS = 400L

data class ColaLaboratorioUi(
    val filtro: EstadoLaboratorio = EstadoLaboratorio.PENDIENTE,
    val ordenes: UiState<List<OrdenLaboratorio>> = UiState.Cargando,
    val conteos: Map<EstadoLaboratorio, Int> = emptyMap(),
    /** Pull-to-refresh o aviso en tiempo real: se recarga sin quitar la lista. */
    val refrescando: Boolean = false,
    val pagina: Int = 1,
    val hayMas: Boolean = false,
    val cargandoMas: Boolean = false,
)

/** AND-R15 · Cola de laboratorio: GET /laboratorio/ordenes?estado=…, con conteos por pestaña. */
@OptIn(FlowPreview::class)
@HiltViewModel
class ColaLaboratorioViewModel
    @Inject
    constructor(
        private val repositorio: LaboratorioRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ColaLaboratorioUi())
        val uiState: StateFlow<ColaLaboratorioUi> = _uiState.asStateFlow()

        private var carga: Job? = null

        init {
            cargar()
            viewModelScope.launch {
                repositorio
                    .cambios()
                    .debounce(ESPERA_AVISOS_MS)
                    // Sin Realtime la lista sigue funcionando con pull-to-refresh y al volver del detalle.
                    .catch { }
                    .collect { refrescar() }
            }
        }

        fun elegirFiltro(estado: EstadoLaboratorio) {
            if (estado == _uiState.value.filtro) return
            _uiState.update { it.copy(filtro = estado) }
            cargar()
        }

        /** Carga desde cero con skeleton (al entrar, al cambiar de pestaña o al reintentar). */
        fun cargar() {
            _uiState.update { it.copy(ordenes = UiState.Cargando, refrescando = false) }
            recargar()
        }

        /** Recarga sin skeleton: pull-to-refresh, aviso en tiempo real o al volver del detalle. */
        fun refrescar() {
            if (_uiState.value.ordenes is UiState.Cargando) return
            _uiState.update { it.copy(refrescando = true) }
            recargar()
        }

        fun cargarMas() {
            val estado = _uiState.value
            if (!estado.hayMas || estado.cargandoMas || carga?.isActive == true) return
            val actuales = (estado.ordenes as? UiState.Exito)?.datos ?: return
            _uiState.update { it.copy(cargandoMas = true) }
            carga =
                viewModelScope.launch {
                    when (val r = repositorio.listar(estado.filtro, estado.pagina + 1)) {
                        is Resultado.Exito -> {
                            val nuevas = r.datos.items.filter { n -> actuales.none { it.id == n.id } }
                            _uiState.update {
                                it.copy(
                                    ordenes = UiState.Exito(actuales + nuevas),
                                    pagina = r.datos.pagina,
                                    hayMas = r.datos.hayMas,
                                    cargandoMas = false,
                                )
                            }
                        }

                        // La lista que ya se ve sigue siendo válida; se puede volver a intentar.
                        is Resultado.Fallo -> {
                            _uiState.update { it.copy(cargandoMas = false) }
                        }
                    }
                }
        }

        private fun recargar() {
            carga?.cancel()
            val filtro = _uiState.value.filtro
            carga =
                viewModelScope.launch {
                    val conteos =
                        EstadoLaboratorio.entries.associateWith { estado -> async { repositorio.contar(estado) } }
                    val pagina = repositorio.listar(filtro, 1)
                    val totales =
                        conteos
                            .mapValues { (_, conteo) -> (conteo.await() as? Resultado.Exito)?.datos }
                            .filterValues { it != null }
                            .mapValues { it.value!! }
                    _uiState.update { actual ->
                        when (pagina) {
                            is Resultado.Exito -> {
                                actual.copy(
                                    ordenes =
                                        if (pagina.datos.items.isEmpty()) {
                                            UiState.Vacio
                                        } else {
                                            UiState.Exito(
                                                pagina.datos.items,
                                            )
                                        },
                                    conteos = actual.conteos + totales,
                                    refrescando = false,
                                    pagina = pagina.datos.pagina,
                                    hayMas = pagina.datos.hayMas,
                                    cargandoMas = false,
                                )
                            }

                            is Resultado.Fallo -> {
                                actual.copy(
                                    // Un refresco fallido no borra lo que ya se veía.
                                    ordenes =
                                        if (actual.refrescando && actual.ordenes is UiState.Exito) {
                                            actual.ordenes
                                        } else {
                                            UiState.Error(pagina.error)
                                        },
                                    conteos = actual.conteos + totales,
                                    refrescando = false,
                                    cargandoMas = false,
                                )
                            }
                        }
                    }
                }
        }
    }

data class DetalleOrdenUi(
    val orden: UiState<OrdenLaboratorio> = UiState.Cargando,
    /** Examen que se está pasando a En Proceso (deshabilita su botón). */
    val iniciando: String? = null,
    /** Error de la última acción (p. ej. otro usuario ya inició el examen). */
    val errorAccion: AppError? = null,
    /** Hubo cambios en esta pantalla: la cola se recarga al volver. */
    val huboCambios: Boolean = false,
)

/** AND-R16 · Detalle de orden: cada examen avanza por separado y solo hacia adelante. */
@OptIn(FlowPreview::class)
@HiltViewModel
class DetalleOrdenViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repositorio: LaboratorioRepository,
    ) : ViewModel() {
        // Mismo nombre que la propiedad de DetalleOrdenRuta.
        private val ordenId: String = checkNotNull(savedStateHandle["ordenId"])

        private val _uiState = MutableStateFlow(DetalleOrdenUi())
        val uiState: StateFlow<DetalleOrdenUi> = _uiState.asStateFlow()

        init {
            cargar()
            viewModelScope.launch {
                repositorio
                    .cambios()
                    .debounce(ESPERA_AVISOS_MS)
                    .catch { }
                    .collect { recargar(mostrarCargando = false) }
            }
        }

        fun cargar() = recargar(mostrarCargando = true)

        /** Después de guardar un resultado en la pantalla siguiente. */
        fun alVolverConCambios() {
            _uiState.update { it.copy(huboCambios = true) }
            recargar(mostrarCargando = false)
        }

        fun descartarError() = _uiState.update { it.copy(errorAccion = null) }

        /** Pendiente → En Proceso. Si otro usuario se adelantó, la API responde 422 y se muestra la orden actual. */
        fun iniciarExamen(examenId: String) {
            if (_uiState.value.iniciando != null) return
            _uiState.update { it.copy(iniciando = examenId, errorAccion = null) }
            viewModelScope.launch {
                when (val r = repositorio.iniciarExamen(ordenId, examenId)) {
                    is Resultado.Exito -> {
                        _uiState.update { it.copy(huboCambios = true, orden = it.orden.conExamen(r.datos)) }
                    }

                    is Resultado.Fallo -> {
                        _uiState.update { it.copy(errorAccion = r.error) }
                    }
                }
                // El estado de la orden lo deriva la API: se vuelve a pedir en ambos casos.
                recargar(mostrarCargando = false)
                _uiState.update { it.copy(iniciando = null) }
            }
        }

        private fun recargar(mostrarCargando: Boolean) {
            if (mostrarCargando) _uiState.update { it.copy(orden = UiState.Cargando) }
            viewModelScope.launch {
                val r = repositorio.obtener(ordenId)
                _uiState.update { actual ->
                    when (r) {
                        is Resultado.Exito -> {
                            actual.copy(orden = UiState.Exito(r.datos))
                        }

                        is Resultado.Fallo -> {
                            if (!mostrarCargando &&
                                actual.orden is UiState.Exito
                            ) {
                                actual
                            } else {
                                actual.copy(orden = UiState.Error(r.error))
                            }
                        }
                    }
                }
            }
        }
    }

private fun UiState<OrdenLaboratorio>.conExamen(examen: ExamenOrden): UiState<OrdenLaboratorio> =
    if (this is UiState.Exito) {
        UiState.Exito(datos.copy(examenes = datos.examenes.map { if (it.id == examen.id) examen else it }))
    } else {
        this
    }

/** Por qué no se aceptó el archivo elegido (se valida antes de subir; la API vuelve a validar). */
enum class ProblemaArchivo { TIPO, TAMANO, ILEGIBLE }

data class ContextoResultado(
    val orden: OrdenLaboratorio,
    val examen: ExamenOrden,
)

data class CargarResultadoUi(
    val carga: UiState<ContextoResultado> = UiState.Cargando,
    val valor: String = "",
    val unidad: String = "",
    val rangoReferencia: String = "",
    val observaciones: String = "",
    val archivo: ArchivoAdjunto? = null,
    val problemaArchivo: ProblemaArchivo? = null,
    val guardando: Boolean = false,
    val error: AppError? = null,
    val guardado: Boolean = false,
) {
    /** Solo un examen En Proceso recibe resultado. */
    val examenEnProceso: Boolean
        get() = (carga as? UiState.Exito)?.datos?.examen?.estado == EstadoLaboratorio.EN_PROCESO

    val tieneResultado: Boolean get() = valor.isNotBlank() || archivo != null

    val puedeGuardar: Boolean get() = !guardando && examenEnProceso && tieneResultado
}

/** AND-R17 · Cargar resultado: texto y/o archivo PDF o imagen (≤ 10 MB) → En Proceso pasa a Completado. */
@HiltViewModel
class CargarResultadoViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repositorio: LaboratorioRepository,
        private val archivos: ArchivosLocales,
    ) : ViewModel() {
        // Mismos nombres que las propiedades de CargarResultadoRuta.
        private val ordenId: String = checkNotNull(savedStateHandle["ordenId"])
        private val examenId: String = checkNotNull(savedStateHandle["examenId"])

        private val _uiState = MutableStateFlow(CargarResultadoUi())
        val uiState: StateFlow<CargarResultadoUi> = _uiState.asStateFlow()

        init {
            cargar()
        }

        fun cargar() {
            _uiState.update { it.copy(carga = UiState.Cargando) }
            viewModelScope.launch {
                val r = repositorio.obtener(ordenId)
                _uiState.update { actual ->
                    actual.copy(
                        carga =
                            when (r) {
                                is Resultado.Fallo -> {
                                    UiState.Error(r.error)
                                }

                                is Resultado.Exito -> {
                                    r.datos.examenes
                                        .firstOrNull { it.id == examenId }
                                        ?.let { UiState.Exito(ContextoResultado(r.datos, it)) }
                                        ?: UiState.Error(AppError.NoEncontrado)
                                }
                            },
                    )
                }
            }
        }

        fun cambiarValor(texto: String) = _uiState.update { it.copy(valor = texto, error = null) }

        fun cambiarUnidad(texto: String) = _uiState.update { it.copy(unidad = texto) }

        fun cambiarRango(texto: String) = _uiState.update { it.copy(rangoReferencia = texto) }

        fun cambiarObservaciones(texto: String) = _uiState.update { it.copy(observaciones = texto) }

        /** [uri] viene del Photo Picker o de OpenDocument; se valida tipo y tamaño antes de aceptarlo. */
        fun adjuntar(uri: String) {
            viewModelScope.launch {
                val archivo = archivos.describir(uri)
                val problema =
                    when {
                        archivo == null -> ProblemaArchivo.ILEGIBLE
                        archivo.mime !in ArchivoAdjunto.MIME_PERMITIDOS -> ProblemaArchivo.TIPO
                        archivo.tamanoBytes > ArchivoAdjunto.MAXIMO_BYTES -> ProblemaArchivo.TAMANO
                        else -> null
                    }
                _uiState.update {
                    if (problema == null) {
                        it.copy(archivo = archivo, problemaArchivo = null, error = null)
                    } else {
                        it.copy(problemaArchivo = problema)
                    }
                }
            }
        }

        fun quitarArchivo() = _uiState.update { it.copy(archivo = null, problemaArchivo = null) }

        fun guardar() {
            val estado = _uiState.value
            if (!estado.puedeGuardar) return
            _uiState.update { it.copy(guardando = true, error = null) }
            viewModelScope.launch {
                val datos =
                    DatosResultado(
                        valor = estado.valor,
                        unidad = estado.unidad,
                        rangoReferencia = estado.rangoReferencia,
                        observaciones = estado.observaciones,
                    )
                when (val r = repositorio.registrarResultado(ordenId, examenId, datos, estado.archivo)) {
                    is Resultado.Exito -> _uiState.update { it.copy(guardando = false, guardado = true) }
                    is Resultado.Fallo -> _uiState.update { it.copy(guardando = false, error = r.error) }
                }
            }
        }
    }
