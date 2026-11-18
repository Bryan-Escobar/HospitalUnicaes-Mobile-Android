package sv.edu.unicaes.hospital.domain.model

/** La única ambulancia y, si va en camino, el servicio que tiene abierto (GET /ambulancia). */
data class Ambulancia(
    val id: String,
    val placa: String,
    val estado: EstadoAmbulancia,
    val solicitudActiva: SolicitudActiva? = null,
)

/** Resumen del servicio en curso que viene junto al estado. El detalle completo se pide aparte. */
data class SolicitudActiva(
    val id: String,
    val prioridad: String,
    val estado: String,
    val tipo: String? = null,
    val direccion: String? = null,
    val fechaDespacho: String? = null,
)

/** Solicitud de ambulancia: pendiente (iOS o botón público), en servicio o ya cerrada. */
data class SolicitudAmbulancia(
    val id: String,
    val paciente: String? = null,
    val nombreContacto: String? = null,
    val direccion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val destino: String? = null,
    val descripcion: String? = null,
    val tipo: String? = null,
    val prioridad: String = "urgente",
    val estado: String = "pendiente",
    val fechaSolicitud: String = "",
    val fechaDespacho: String? = null,
    val fechaCierre: String? = null,
    val canal: String = "app",
)

/** Una fila de la bitácora: cada cambio de estado, con la emergencia si la hubo. */
data class EntradaBitacora(
    val id: String,
    val estadoAnterior: EstadoAmbulancia?,
    val estadoNuevo: EstadoAmbulancia,
    val descripcion: String?,
    val registradoPor: String?,
    val solicitud: SolicitudAmbulancia?,
    val creadaEn: String,
)

/** Filtros de GET /ambulancia/bitacora. Null no se envía y la API no filtra por ese campo. */
data class FiltroBitacora(
    val estado: EstadoAmbulancia? = null,
    val tipo: String? = null,
    val desde: String? = null,
    val hasta: String? = null,
)
