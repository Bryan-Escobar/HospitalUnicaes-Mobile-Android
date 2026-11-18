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
import sv.edu.unicaes.hospital.core.CODIGO_AMBULANCIA_NO_DISPONIBLE
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.AmbulanciaRepository
import sv.edu.unicaes.hospital.domain.model.Ambulancia
import sv.edu.unicaes.hospital.domain.model.EntradaBitacora
import sv.edu.unicaes.hospital.domain.model.EstadoAmbulancia
import sv.edu.unicaes.hospital.domain.model.FiltroBitacora
import sv.edu.unicaes.hospital.domain.model.SolicitudAmbulancia
import sv.edu.unicaes.hospital.ui.modelo.AmbulanciaUi
import sv.edu.unicaes.hospital.ui.modelo.CierreServicioUi
import sv.edu.unicaes.hospital.ui.modelo.DetalleServicioUi
import sv.edu.unicaes.hospital.ui.modelo.EstadoServicio
import sv.edu.unicaes.hospital.ui.modelo.ServicioBitacoraUi
import sv.edu.unicaes.hospital.ui.modelo.SolicitudAmbulanciaUi
import sv.edu.unicaes.hospital.ui.modelo.UN_DIA_MS
import sv.edu.unicaes.hospital.ui.modelo.ZONA_HOSPITAL
import sv.edu.unicaes.hospital.ui.modelo.claveDia
import sv.edu.unicaes.hospital.ui.modelo.diaMes
import sv.edu.unicaes.hospital.ui.modelo.fechaDesdeIso
import sv.edu.unicaes.hospital.ui.modelo.fechaHoraHospital
import sv.edu.unicaes.hospital.ui.modelo.hora
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// Ambulancia de recepción (HUNI-71): estado en vivo, despacho, rechazo y cierre contra la API de HUNI-70.
// Realtime solo avisa; el detalle se vuelve a pedir. El aviso de conflicto solo queda si, tras el 409, la unidad ya no está disponible.

data class AmbulanciaUiState(
    val ambulancia: UiState<AmbulanciaUi> = UiState.Cargando,
    val conflicto: Boolean = false,
    val error: AppError? = null,
    val enviando: Boolean = false,
)

/** AND-R13 · Despacho. */
@HiltViewModel
class AmbulanciaViewModel
    @Inject
    constructor(
        private val repositorio: AmbulanciaRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AmbulanciaUiState())
        val uiState: StateFlow<AmbulanciaUiState> = _uiState.asStateFlow()

        private var servicioId: String? = null
        private var carga: Job? = null

        init {
            cargar()
            viewModelScope.launch { repositorio.cambios().collect { refrescar() } }
        }

        fun cargar() {
            _uiState.update { it.copy(ambulancia = UiState.Cargando, error = null) }
            lanzarRecarga()
        }

        fun despachar(solicitud: SolicitudAmbulanciaUi) {
            if (_uiState.value.enviando) return
            _uiState.update { it.copy(enviando = true, error = null) }
            viewModelScope.launch {
                try {
                    when (val resultado = repositorio.despachar(solicitud.id)) {
                        is Resultado.Exito -> {
                            _uiState.update { it.copy(conflicto = false) }
                            recargar()
                        }

                        is Resultado.Fallo -> {
                            val rechazada = resultado.error as? AppError.Rechazada
                            if (rechazada?.codigo == CODIGO_AMBULANCIA_NO_DISPONIBLE) {
                                recargar()
                                val disponible =
                                    (_uiState.value.ambulancia as? UiState.Exito)?.datos?.estado ==
                                        EstadoAmbulancia.DISPONIBLE
                                _uiState.update {
                                    if (disponible) {
                                        it.copy(conflicto = false, error = resultado.error)
                                    } else {
                                        it.copy(conflicto = true, error = null)
                                    }
                                }
                            } else {
                                _uiState.update { it.copy(error = resultado.error) }
                            }
                        }
                    }
                } finally {
                    _uiState.update { it.copy(enviando = false) }
                }
            }
        }

        fun rechazar(solicitud: SolicitudAmbulanciaUi) {
            if (_uiState.value.enviando) return
            _uiState.update { it.copy(enviando = true, error = null) }
            viewModelScope.launch {
                try {
                    when (val resultado = repositorio.cancelar(solicitud.id)) {
                        is Resultado.Exito -> recargar()
                        is Resultado.Fallo -> _uiState.update { it.copy(error = resultado.error) }
                    }
                } finally {
                    _uiState.update { it.copy(enviando = false) }
                }
            }
        }

        /** La API cierra con `descripcion`. */
        fun cerrar(cierre: CierreServicioUi) {
            val id = servicioId ?: return
            if (_uiState.value.enviando) return
            _uiState.update { it.copy(enviando = true, error = null) }
            viewModelScope.launch {
                try {
                    when (val resultado = repositorio.cerrar(id, descripcionDeCierre(cierre))) {
                        is Resultado.Exito -> {
                            _uiState.update { it.copy(conflicto = false) }
                            recargar()
                        }

                        is Resultado.Fallo -> {
                            _uiState.update { it.copy(error = resultado.error) }
                        }
                    }
                } finally {
                    _uiState.update { it.copy(enviando = false) }
                }
            }
        }

        private fun refrescar() {
            val actual = _uiState.value
            if (actual.ambulancia is UiState.Cargando || actual.enviando) return
            lanzarRecarga()
        }

        private fun lanzarRecarga() {
            carga?.cancel()
            carga = viewModelScope.launch { recargar() }
        }

        private suspend fun recargar() {
            val estado = repositorio.estado()
            val pendientes = repositorio.pendientes()
            if (estado is Resultado.Fallo || pendientes is Resultado.Fallo) {
                val error = (estado as? Resultado.Fallo)?.error ?: (pendientes as Resultado.Fallo).error
                _uiState.update { actual ->
                    if (actual.ambulancia is UiState.Exito) actual else actual.copy(ambulancia = UiState.Error(error))
                }
                return
            }
            val unidad = (estado as Resultado.Exito).datos
            val activas = unidad.solicitudActiva
            val detalle = activas?.let { (repositorio.obtener(it.id) as? Resultado.Exito)?.datos }
            servicioId = activas?.id
            _uiState.update { actual ->
                actual.copy(
                    ambulancia =
                        UiState.Exito(
                            unidad.aUi(
                                pendientes = (pendientes as Resultado.Exito).datos,
                                activa = detalle,
                            ),
                        ),
                    conflicto = actual.conflicto && unidad.estado != EstadoAmbulancia.DISPONIBLE,
                )
            }
        }
    }

/** Hoy, la última semana o sin límite. Las fechas van en la zona del hospital, como el resto de la app. */
enum class RangoBitacora { HOY, SIETE_DIAS, TODAS }

data class BitacoraUiState(
    val servicios: UiState<List<ServicioBitacoraUi>> = UiState.Cargando,
    val detalle: DetalleServicioUi? = null,
    val seleccionado: String? = null,
    val rango: RangoBitacora = RangoBitacora.TODAS,
    val filtroEstado: EstadoAmbulancia? = null,
    val filtroTipo: String? = null,
)

/** AND-R14 · Bitácora filtrable por fecha, estado nuevo y tipo de emergencia. */
@HiltViewModel
class BitacoraViewModel
    @Inject
    constructor(
        private val repositorio: AmbulanciaRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BitacoraUiState())
        val uiState: StateFlow<BitacoraUiState> = _uiState.asStateFlow()

        private var entradas: List<EntradaBitacora> = emptyList()
        private var carga: Job? = null

        init {
            cargar()
            viewModelScope.launch { repositorio.cambios().collect { refrescar() } }
        }

        fun cargar() {
            _uiState.update { it.copy(servicios = UiState.Cargando) }
            lanzarRecarga()
        }

        fun elegirRango(rango: RangoBitacora) {
            if (rango == _uiState.value.rango) return
            _uiState.update { it.copy(rango = rango) }
            cargar()
        }

        fun elegirEstado(estado: EstadoAmbulancia?) {
            if (estado == _uiState.value.filtroEstado) return
            _uiState.update { it.copy(filtroEstado = estado) }
            cargar()
        }

        fun elegirTipo(tipo: String?) {
            if (tipo == _uiState.value.filtroTipo) return
            _uiState.update { it.copy(filtroTipo = tipo) }
            cargar()
        }

        fun quitarFiltros() {
            _uiState.update {
                it.copy(rango = RangoBitacora.TODAS, filtroEstado = null, filtroTipo = null)
            }
            cargar()
        }

        fun seleccionar(id: String) {
            val entrada = entradas.firstOrNull { it.id == id } ?: return
            _uiState.update { it.copy(seleccionado = id, detalle = entrada.aDetalle()) }
        }

        private fun refrescar() {
            if (_uiState.value.servicios is UiState.Cargando) return
            lanzarRecarga()
        }

        private fun lanzarRecarga() {
            carga?.cancel()
            carga = viewModelScope.launch { recargar() }
        }

        private suspend fun recargar() {
            val actual = _uiState.value
            val (desde, hasta) = fechasDe(actual.rango)
            when (
                val resultado =
                    repositorio.bitacora(
                        FiltroBitacora(
                            estado = actual.filtroEstado,
                            tipo = actual.filtroTipo,
                            desde = desde,
                            hasta = hasta,
                        ),
                    )
            ) {
                is Resultado.Exito -> {
                    entradas = resultado.datos
                    val servicios = entradas.map { it.aLista() }
                    val elegido = entradas.firstOrNull { it.id == actual.seleccionado }
                    _uiState.update {
                        it.copy(
                            servicios = if (servicios.isEmpty()) UiState.Vacio else UiState.Exito(servicios),
                            detalle = elegido?.aDetalle(),
                            seleccionado = elegido?.id,
                        )
                    }
                }

                is Resultado.Fallo -> {
                    _uiState.update { estado ->
                        if (estado.servicios is UiState.Exito) {
                            estado
                        } else {
                            estado.copy(servicios = UiState.Error(resultado.error))
                        }
                    }
                }
            }
        }
    }

/** `desde` y `hasta` inclusivos (AAAA-MM-DD), o null si no hay que filtrar. */
internal fun fechasDe(
    rango: RangoBitacora,
    ahora: Date = Date(),
): Pair<String?, String?> {
    val hoy = claveDia(ahora)
    return when (rango) {
        RangoBitacora.TODAS -> {
            null to null
        }

        RangoBitacora.HOY -> {
            hoy to hoy
        }

        RangoBitacora.SIETE_DIAS -> {
            val inicio = fechaHoraHospital(hoy)?.time ?: ahora.time
            claveDia(Date(inicio - DIAS_ATRAS * UN_DIA_MS)) to hoy
        }
    }
}

/** La API cierra con `descripcion`. Vacío se manda como null. */
internal fun descripcionDeCierre(cierre: CierreServicioUi): String? =
    cierre.observaciones
        .trim()
        .take(MAX_DESCRIPCION)
        .ifEmpty { null }

internal fun Ambulancia.aUi(
    pendientes: List<SolicitudAmbulancia>,
    activa: SolicitudAmbulancia?,
    ahora: Date = Date(),
): AmbulanciaUi {
    val resumen = solicitudActiva
    val servicio =
        if (estado == EstadoAmbulancia.EN_SERVICIO) {
            when {
                activa != null -> {
                    textoServicio(
                        paciente = activa.paciente,
                        contacto = activa.nombreContacto,
                        descripcion = activa.descripcion,
                        tipo = activa.tipo,
                        prioridad = activa.prioridad,
                        fechaDespacho = activa.fechaDespacho ?: resumen?.fechaDespacho,
                    )
                }

                resumen != null -> {
                    textoServicio(
                        paciente = null,
                        contacto = null,
                        descripcion = null,
                        tipo = resumen.tipo,
                        prioridad = resumen.prioridad,
                        fechaDespacho = resumen.fechaDespacho,
                    )
                }

                else -> {
                    null
                }
            }
        } else {
            null
        }
    return AmbulanciaUi(
        estado = estado,
        solicitudes = pendientes.map { it.aUi(ahora) },
        servicioActual = servicio,
    )
}

internal fun SolicitudAmbulancia.aUi(ahora: Date = Date()): SolicitudAmbulanciaUi {
    val fecha = fechaDesdeIso(fechaSolicitud)
    return SolicitudAmbulanciaUi(
        id = id,
        paciente = (paciente ?: nombreContacto).orEmpty(),
        hora = fecha?.let { hora(it, ZONA_HOSPITAL) }.orEmpty(),
        hace = fecha?.let { textoHace(it, ahora) }.orEmpty(),
        tipo = etiquetaTipo(tipo) ?: etiquetaPrioridad(prioridad),
        ubicacion =
            when {
                !direccion.isNullOrBlank() -> direccion
                latitud != null && longitud != null -> "$latitud, $longitud"
                else -> ""
            },
        descripcion = descripcion.orEmpty(),
    )
}

internal fun EntradaBitacora.aLista(): ServicioBitacoraUi {
    val creada = fechaDesdeIso(creadaEn)
    val inicio = solicitud?.fechaDespacho?.let { fechaDesdeIso(it) }
    val fin = solicitud?.fechaCierre?.let { fechaDesdeIso(it) }
    val horario =
        when {
            inicio != null && fin != null -> "${hora(inicio, ZONA_HOSPITAL)} – ${hora(fin, ZONA_HOSPITAL)}"
            inicio != null -> hora(inicio, ZONA_HOSPITAL)
            creada != null -> hora(creada, ZONA_HOSPITAL)
            else -> ""
        }
    val minutos =
        if (inicio != null && fin != null) {
            TimeUnit.MILLISECONDS.toMinutes(fin.time - inicio.time).coerceAtLeast(0)
        } else {
            null
        }
    return ServicioBitacoraUi(
        id = id,
        fecha = creada?.let { diaMes(it, ZONA_HOSPITAL) }.orEmpty(),
        horario = horario,
        tipo = etiquetaTipo(solicitud?.tipo) ?: descripcion?.ifBlank { null } ?: etiquetaEstado(estadoNuevo),
        paciente = solicitud?.paciente ?: solicitud?.nombreContacto,
        duracion = minutos?.let { "$it min" },
        estado =
            when {
                solicitud?.estado == CANCELADA -> EstadoServicio.RECHAZADA
                estadoNuevo == EstadoAmbulancia.EN_MANTENIMIENTO -> EstadoServicio.EN_MANTENIMIENTO
                else -> EstadoServicio.FINALIZADO
            },
    )
}

internal fun EntradaBitacora.aDetalle(): DetalleServicioUi {
    val creada = fechaDesdeIso(creadaEn)
    return DetalleServicioUi(
        fecha = creada?.let { diaMes(it, ZONA_HOSPITAL) }.orEmpty(),
        despacho = registradoPor.orEmpty(),
        observaciones = descripcion?.ifBlank { null } ?: solicitud?.descripcion.orEmpty(),
    )
}

internal fun etiquetaTipo(tipo: String?): String? =
    when (tipo) {
        "accidente" -> "Accidente"
        "cardiaca" -> "Cardíaca"
        "respiratoria" -> "Respiratoria"
        "neurologica" -> "Neurológica"
        "obstetrica" -> "Obstétrica"
        "intoxicacion" -> "Intoxicación"
        "traslado" -> "Traslado"
        "otra" -> "Otra"
        null, "" -> null
        else -> tipo.replaceFirstChar { it.uppercase() }
    }

internal fun etiquetaPrioridad(prioridad: String): String =
    when (prioridad) {
        "critica" -> "Crítica"
        "urgente" -> "Urgente"
        "normal" -> "Normal"
        else -> prioridad.replaceFirstChar { it.uppercase() }
    }

private fun etiquetaEstado(estado: EstadoAmbulancia): String =
    when (estado) {
        EstadoAmbulancia.DISPONIBLE -> "Disponible"
        EstadoAmbulancia.EN_SERVICIO -> "En Servicio"
        EstadoAmbulancia.EN_MANTENIMIENTO -> "Mantenimiento"
    }

private fun textoServicio(
    paciente: String?,
    contacto: String?,
    descripcion: String?,
    tipo: String?,
    prioridad: String,
    fechaDespacho: String?,
): String {
    val quien = paciente?.ifBlank { null } ?: contacto?.ifBlank { null }
    val motivo = descripcion?.ifBlank { null } ?: etiquetaTipo(tipo) ?: etiquetaPrioridad(prioridad)
    val cuando = fechaDespacho?.let { fechaDesdeIso(it) }?.let { "despachada ${hora(it, ZONA_HOSPITAL)}" }
    return listOfNotNull(quien, motivo, cuando).joinToString(" · ")
}

private fun textoHace(
    desde: Date,
    ahora: Date,
): String {
    val minutos = TimeUnit.MILLISECONDS.toMinutes(ahora.time - desde.time).coerceAtLeast(0)
    return when {
        minutos < 1 -> "hace un momento"
        minutos == 1L -> "hace 1 min"
        minutos < MINUTOS_HORA -> "hace $minutos min"
        minutos < MINUTOS_HORA * HORAS_DIA -> "hace ${minutos / MINUTOS_HORA} h"
        else -> "hace ${minutos / (MINUTOS_HORA * HORAS_DIA)} d"
    }
}

private const val MAX_DESCRIPCION = 2000
private const val DIAS_ATRAS = 6
private const val MINUTOS_HORA = 60
private const val HORAS_DIA = 24
private const val CANCELADA = "cancelada"
