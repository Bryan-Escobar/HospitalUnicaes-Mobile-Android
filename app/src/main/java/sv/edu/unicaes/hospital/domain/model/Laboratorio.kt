package sv.edu.unicaes.hospital.domain.model

/** Prioridad de una orden de laboratorio (`prioridad` de la API). */
enum class PrioridadLaboratorio(
    val valorApi: String,
) {
    NORMAL("normal"),
    URGENTE("urgente"),
    CRITICA("critica"),
    ;

    companion object {
        fun desdeApi(valor: String): PrioridadLaboratorio = entries.firstOrNull { it.valorApi == valor } ?: NORMAL
    }
}

/** Tipo del archivo adjunto a un resultado (HUNI-63: PDF o imagen). */
enum class TipoArchivoResultado { PDF, IMAGEN }

/**
 * Orden de laboratorio (GET /laboratorio/ordenes). Su [estado] lo deriva la API de sus exámenes:
 * completado cuando todos lo están, en proceso cuando alguno avanzó.
 */
data class OrdenLaboratorio(
    val id: String,
    val fechaSolicitud: String,
    val prioridad: PrioridadLaboratorio,
    val estado: EstadoLaboratorio,
    val pacienteId: String,
    val paciente: String,
    val doctor: String,
    val especialidad: String? = null,
    val indicaciones: String? = null,
    val examenes: List<ExamenOrden> = emptyList(),
)

/** Un examen de la orden; cada uno sigue Pendiente → En Proceso → Completado por su cuenta. */
data class ExamenOrden(
    val id: String,
    val nombre: String,
    val tipoMuestra: String? = null,
    val estado: EstadoLaboratorio,
    val fechaInicio: String? = null,
    val fechaCompletado: String? = null,
    val resultado: ResultadoExamen? = null,
)

data class ResultadoExamen(
    val valor: String? = null,
    val unidad: String? = null,
    val rangoReferencia: String? = null,
    val observaciones: String? = null,
    /** Null si el resultado no trae archivo. */
    val tipoArchivo: TipoArchivoResultado? = null,
    val validadoPor: String? = null,
    val fecha: String,
)

/** Lo que Recepción escribe al registrar el resultado. La API exige valor o archivo. */
data class DatosResultado(
    val valor: String? = null,
    val unidad: String? = null,
    val rangoReferencia: String? = null,
    val observaciones: String? = null,
)

/**
 * Archivo elegido con el selector del sistema. [uri] es un `content://` que el repositorio lee al subir;
 * [mime] y [tamanoBytes] vienen del ContentResolver.
 */
data class ArchivoAdjunto(
    val uri: String,
    val nombre: String,
    val mime: String,
    val tamanoBytes: Long,
) {
    val esPdf: Boolean get() = mime == MIME_PDF

    companion object {
        const val MIME_PDF = "application/pdf"

        /** Tipos que acepta la API (HUNI-63). */
        val MIME_PERMITIDOS = setOf(MIME_PDF, "image/jpeg", "image/png", "image/webp")

        /** Límite de la API y del bucket de Storage. */
        const val MAXIMO_BYTES = 10L * 1024 * 1024
    }
}
